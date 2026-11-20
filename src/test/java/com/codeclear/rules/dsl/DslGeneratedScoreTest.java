package com.codeclear.rules.dsl;

import com.codeclear.rules.CCRuleLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.codeclear.rules.CCRuleTests;

/** Scores AI-generated .rule files (directory given by -Dgen.dir) against the held-out tests.md. Prints only. */
class DslGeneratedScoreTest {

    @Test
    void score() throws Exception {
        String dir = System.getProperty("gen.dir");
        if (dir == null) {
            return;
        }
        List<Path> files;
        try (var s = Files.list(Path.of(dir))) {
            files = s.filter(p -> p.toString().endsWith(".rule")).sorted().toList();
        }
        for (Path f : files) {
            String id = f.getFileName().toString().replaceAll("\\..*", "");
            Path rdir = Path.of("review/java", id);
            String conf = CCRuleLoader.load(rdir).metadata().confidence().name();
            CCRuleTests.Tests tests = CCRuleTests.load(rdir);
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
            for (CCRuleTests.Case c : tests.cases()) {
                total++;
                try {
                    String got = DslRulesTest.actual(rule, c, tests.sharedPrelude(), conf).toString();
                    String exp = DslRulesTest.expected(c, conf).toString();
                    if (got.equals(exp)) {
                        pass++;
                    } else {
                        fails.add(c.name() + " got " + got + " expected " + exp);
                    }
                } catch (RuntimeException e) {
                    fails.add(c.name() + " ERROR " + e.getMessage());
                }
            }
            System.out.println("GEN " + f.getFileName() + ": " + pass + "/" + total);
            fails.forEach(x -> System.out.println("  FAIL " + x));
        }
    }
}
