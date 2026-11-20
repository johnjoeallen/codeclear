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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Throwaway proof that the rule language can express the two Groovy rules: parse each {@code .rule} file and run it
 * against the same {@code tests.json} the Groovy versions pass.
 */
class DslRulesTest {

    private static final JavaFileAnalyzer ANALYZER = new JavaFileAnalyzer();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void emptyCatchBlock() throws IOException {
        check("empty-catch-block");
    }

    @Test
    void optionalGetWithoutCheck() throws IOException {
        check("optional-get-without-check");
    }

    private void check(String id) throws IOException {
        RuleAst rule = DslParser.parse(Files.readString(Path.of("src/test/resources/dsl/" + id + ".rule")));
        Path dir = Path.of("review/java", id);
        CCRuleDefinition def = CCRuleLoader.load(dir);
        String confidence = def.metadata().confidence().name();
        JsonNode tests = JSON.readTree(dir.resolve("tests.json"));
        String shared = tests.has("prelude") ? tests.get("prelude").asString() : "";

        List<String> failures = new ArrayList<>();
        int n = 0;
        for (JsonNode c : tests.get("cases")) {
            n++;
            String got = actual(rule, c, shared, confidence).toString();
            List<String> exp = new ArrayList<>();
            for (JsonNode e : c.get("expect")) {
                exp.add(e.get("line").asInt() + ":" + (e.has("confidence") ? e.get("confidence").asString() : confidence));
            }
            if (!got.equals(exp.toString())) {
                failures.add(c.get("name").asString() + ": got " + got + ", expected " + exp);
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
            for (JsonNode c : tests.get("cases")) {
                List<String> exp = new ArrayList<>();
                for (JsonNode e : c.get("expect")) {
                    exp.add(e.get("line").asInt() + ":" + (e.has("confidence") ? e.get("confidence").asString() : confidence));
                }
                try {
                    if (!actual(m.rule(), c, shared, confidence).toString().equals(exp.toString())) {
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

    private static List<String> actual(RuleAst rule, JsonNode c, String shared, String confidence) {
        String prelude = c.has("prelude") ? c.get("prelude").asString() : shared;
        int offset = (int) prelude.chars().filter(ch -> ch == '\n').count() + 1;
        CCFileAnalysis file = ANALYZER.analyze(c.get("path").asString(), prelude + "\n" + c.get("source").asString(),
                CCChangedLines.all());
        return new DslInterpreter().run(rule, file, confidence).stream()
                .map(f -> (f.line() - offset) + ":" + f.confidence()).toList();
    }
}
