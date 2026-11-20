package com.codeclear.rules.dsl;

import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.CCRuleLoader;
import com.codeclear.rules.JavaFileAnalyzer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Scores AI-generated .rule files (directory given by -Dgen.dir) against the held-out tests.json. Prints only. */
class DslGeneratedScoreTest {

    @Test
    void score() throws Exception {
        String dir = System.getProperty("gen.dir");
        if (dir == null) {
            return;
        }
        JavaFileAnalyzer analyzer = new JavaFileAnalyzer();
        JsonMapper json = JsonMapper.builder().build();
        List<Path> files;
        try (var s = Files.list(Path.of(dir))) {
            files = s.filter(p -> p.toString().endsWith(".rule")).sorted().toList();
        }
        for (Path f : files) {
            String id = f.getFileName().toString().replaceAll("\\..*", "");
            Path rdir = Path.of("review/java", id);
            String conf = CCRuleLoader.load(rdir).metadata().confidence().name();
            JsonNode tests = json.readTree(rdir.resolve("tests.json"));
            String shared = tests.has("prelude") ? tests.get("prelude").asString() : "";
            RuleAst rule;
            try {
                rule = DslParser.parse(Files.readString(f));
            } catch (RuntimeException e) {
                System.out.println("GEN " + f.getFileName() + ": PARSE ERROR " + e.getMessage());
                continue;
            }
            int pass = 0;
            int total = 0;
            List<String> fails = new ArrayList<>();
            for (JsonNode c : tests.get("cases")) {
                total++;
                List<String> exp = new ArrayList<>();
                for (JsonNode e : c.get("expect")) {
                    exp.add(e.get("line").asInt() + ":" + (e.has("confidence") ? e.get("confidence").asString() : conf));
                }
                try {
                    String prelude = c.has("prelude") ? c.get("prelude").asString() : shared;
                    int offset = (int) prelude.chars().filter(ch -> ch == '\n').count() + 1;
                    CCFileAnalysis fa = analyzer.analyze(c.get("path").asString(),
                            prelude + "\n" + c.get("source").asString(), CCChangedLines.all());
                    String got = new DslInterpreter().run(rule, fa, conf).stream()
                            .map(x -> (x.line() - offset) + ":" + x.confidence()).toList().toString();
                    if (got.equals(exp.toString())) {
                        pass++;
                    } else {
                        fails.add(c.get("name").asString() + " got " + got + " expected " + exp);
                    }
                } catch (RuntimeException e) {
                    fails.add(c.get("name").asString() + " ERROR " + e.getMessage());
                }
            }
            System.out.println("GEN " + f.getFileName() + ": " + pass + "/" + total);
            fails.forEach(x -> System.out.println("  FAIL " + x));
        }
    }
}
