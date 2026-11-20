package com.codeclear.rules.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.JavaFileAnalyzer;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.codeclear.rules.CCRuleTests;

/**
 * A tier 2 rule in the restricted language: lazy JPA associations read outside a session. Needs a project-wide
 * model (entities, fetch types, transactions, configuration), so each case is a small multi-file project.
 */
class DslProjectTest {

    private static final JavaFileAnalyzer ANALYZER = new JavaFileAnalyzer();
    private static final Path TESTS = Path.of("review/java/lazy-association-outside-session");

    @Test
    void lazyAssociationRules() throws Exception {
        check("lazy-association", TESTS, List.of("rule.dsl", "serialized.dsl"));
    }

    @Test
    void bigDecimalMisuse() throws Exception {
        check("bigdecimal-misuse", Path.of("review/java/bigdecimal-misuse"), List.of("rule.dsl", "equals.dsl"));
    }

    @Test
    void logInjection() throws Exception {
        check("log-injection", Path.of("review/common/log-injection"), List.of("rule.dsl"));
    }

    private void check(String label, Path testsDir, List<String> ruleNames) throws Exception {
        CCRuleTests.Tests tests = CCRuleTests.load(testsDir);
        List<RuleAst> rules = new ArrayList<>();
        for (String r : ruleNames) {
            rules.add(DslParser.parse(Files.readString(testsDir.resolve(r))));
        }
        List<String> failures = new ArrayList<>();
        for (CCRuleTests.Case c : tests.cases()) {
            String got = run(rules, c, tests.sharedPrelude()).toString();
            String exp = expected(c).toString();
            if (!got.equals(exp)) {
                failures.add(c.name() + ": got " + got + ", expected " + exp);
            }
        }
        int total = tests.cases().size();
        System.out.println("DSL " + label + ": " + (total - failures.size()) + "/" + total + " cases pass");
        failures.forEach(f -> System.out.println("  FAIL " + f));

        if (!failures.isEmpty()) {
            // mutants are only meaningful against a passing baseline: a failing case would "kill" every mutant
            assertThat(failures).isEmpty();
        }
        for (int ri = 0; ri < rules.size(); ri++) {
            List<DslMutator.Mutant> mutants = DslMutator.mutants(rules.get(ri));
            List<String> survivors = new ArrayList<>();
            for (DslMutator.Mutant m : mutants) {
                List<RuleAst> variant = new ArrayList<>(rules);
                variant.set(ri, m.rule());
                boolean caught = false;
                for (CCRuleTests.Case c : tests.cases()) {
                    try {
                        if (!run(variant, c, tests.sharedPrelude()).toString().equals(expected(c).toString())) {
                            caught = true;
                            break;
                        }
                    } catch (RuntimeException e) {
                        caught = true;
                        break;
                    }
                }
                if (!caught) {
                    survivors.add(m.description());
                }
            }
            System.out.println("DSL " + rules.get(ri).id() + ": " + mutants.size() + " mutants, "
                    + (mutants.size() - survivors.size()) + " killed, " + survivors.size() + " survived");
            survivors.forEach(s -> System.out.println("  SURVIVED " + s));
        }
        assertThat(failures).isEmpty();
    }

    private static List<String> expected(CCRuleTests.Case c) {
        return c.expect().stream().map(e -> e.path() + ":" + e.line()).sorted().toList();
    }

    private static List<String> run(List<RuleAst> rules, CCRuleTests.Case c, String sharedPrelude) {
        String prelude = c.prelude() != null ? c.prelude() : sharedPrelude;
        List<CCFileAnalysis> files = new ArrayList<>();
        List<CompilationUnit> units = new ArrayList<>();
        for (CCRuleTests.SourceFile f : c.files()) {
            CCFileAnalysis fa = ANALYZER.analyze(f.path(), CCRuleTests.analysisText(prelude, f.source()), CCChangedLines.all());
            files.add(fa);
            units.add((CompilationUnit) fa.ast());
        }
        DslProject project = new DslProject(units, new HashMap<>(c.config()));
        int offset = CCRuleTests.lineOffset(prelude);
        List<String> out = new ArrayList<>();
        for (RuleAst rule : rules) {
            for (CCFileAnalysis fa : files) {
                new DslInterpreter().run(rule, fa, "MEDIUM", project)
                        .forEach(f -> out.add(fa.file().getPath() + ":" + (f.line() - offset)));
            }
        }
        return out.stream().sorted().toList();
    }
}
