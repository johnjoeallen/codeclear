package com.codeclear.rules.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.CCRuleDefinition;
import com.codeclear.rules.CCRuleLoader;
import com.codeclear.rules.JavaFileAnalyzer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.codeclear.rules.CCRuleTests;

/**
 * Throwaway proof that the rule language can express the two Groovy rules: parse each {@code .rule} file and run it
 * against the same {@code tests.md} the Groovy versions pass.
 */
class DslRulesTest {

    private static final JavaFileAnalyzer ANALYZER = new JavaFileAnalyzer();

    @Test
    void emptyCatchBlock() throws IOException {
        check("empty-catch-block");
    }

    @Test
    void optionalGetWithoutCheck() throws IOException {
        check("optional-get-without-check");
    }

    /** Every committed .dsl file records the hash of the rule.md it was written from, as the Groovy scripts do. */
    @Test
    void dslHeadersAreCurrent() throws IOException {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?m)^# rule-hash: (\\w+)$");
        try (var dirs = Files.walk(Path.of("review"))) {
            for (Path f : dirs.filter(x -> x.toString().endsWith(".dsl")).toList()) {
                var m = p.matcher(Files.readString(f));
                assertThat(m.find()).as(f + " has a rule-hash header").isTrue();
                assertThat(m.group(1)).as(f + " matches rule.md").isEqualTo(CCRuleLoader.load(f.getParent()).ruleHash());
            }
        }
    }

    private void check(String id) throws IOException {
        Path dir = Path.of("review/java", id);
        RuleAst rule = DslParser.parse(Files.readString(dir.resolve("rule.dsl")));
        CCRuleDefinition def = CCRuleLoader.load(dir);
        String confidence = def.metadata().confidence().name();
        CCRuleTests.Tests tests = CCRuleTests.load(dir);

        List<String> failures = new ArrayList<>();
        int n = 0;
        for (CCRuleTests.Case c : tests.cases()) {
            n++;
            String got = actual(rule, c, tests.sharedPrelude(), confidence).toString();
            if (!got.equals(expected(c, confidence).toString())) {
                failures.add(c.name() + ": got " + got + ", expected " + expected(c, confidence));
            }
        }
        System.out.println("DSL " + id + ": " + (n - failures.size()) + "/" + n + " cases pass");
        failures.forEach(f -> System.out.println("  FAIL " + f));

        // Automatic mutation testing: every mutant should be caught by at least one case.
        List<DslMutator.Mutant> mutants = DslMutator.mutants(rule);
        List<String> survivors = new ArrayList<>();
        int killed = 0;
        int errored = 0;
        for (DslMutator.Mutant m : mutants) {
            boolean caught = false;
            for (CCRuleTests.Case c : tests.cases()) {
                try {
                    if (!actual(m.rule(), c, tests.sharedPrelude(), confidence).toString()
                            .equals(expected(c, confidence).toString())) {
                        caught = true;
                        break;
                    }
                } catch (RuntimeException ex) {
                    caught = true;
                    errored++;
                    break;
                }
            }
            if (caught) {
                killed++;
            } else {
                survivors.add(m.description());
            }
        }
        System.out.println("DSL " + id + ": " + mutants.size() + " mutants, " + killed + " killed ("
                + errored + " by error), " + survivors.size() + " survived");
        survivors.forEach(s -> System.out.println("  SURVIVED " + s));

        assertThat(failures).isEmpty();
    }

    static List<String> expected(CCRuleTests.Case c, String confidence) {
        return c.expect().stream()
                .map(e -> e.line() + ":" + (e.confidence() != null ? e.confidence() : confidence)).toList();
    }

    static List<String> actual(RuleAst rule, CCRuleTests.Case c, String shared, String confidence) {
        CCRuleTests.SourceFile f = c.files().get(0);
        String prelude = c.prelude() != null ? c.prelude() : shared;
        int offset = CCRuleTests.lineOffset(prelude);
        CCFileAnalysis file = ANALYZER.analyze(f.path(), CCRuleTests.analysisText(prelude, f.source()),
                CCChangedLines.all());
        return new DslInterpreter().run(rule, file, confidence).stream()
                .map(x -> (x.line() - offset) + ":" + x.confidence()).toList();
    }
}
