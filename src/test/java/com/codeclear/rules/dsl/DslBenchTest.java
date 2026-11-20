package com.codeclear.rules.dsl;

import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCFileAnalysis;
import com.codeclear.rules.JavaFileAnalyzer;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Timing over a real source tree: {@code -Dbench.dir=<dir> -Dbench.n=<files>}. Prints only; skipped without the property. */
class DslBenchTest {

    @Test
    void bench() throws Exception {
        String dir = System.getProperty("bench.dir");
        if (dir == null) {
            return;
        }
        int limit = Integer.getInteger("bench.n", 400);
        List<Path> paths;
        try (Stream<Path> s = Files.walk(Path.of(dir))) {
            paths = s.filter(p -> p.toString().endsWith(".java") && !p.toString().contains("/target/")
                    && !p.toString().contains("/src/test/")).sorted().limit(limit).toList();
        }
        JavaFileAnalyzer analyzer = new JavaFileAnalyzer();
        List<CCFileAnalysis> files = new ArrayList<>();
        List<CompilationUnit> units = new ArrayList<>();
        long lines = 0;
        long t0 = System.nanoTime();
        for (Path p : paths) {
            try {
                String src = Files.readString(p);
                CCFileAnalysis fa = analyzer.analyze(p.toString(), src, CCChangedLines.all());
                files.add(fa);
                units.add((CompilationUnit) fa.ast());
                lines += src.lines().count();
            } catch (Exception e) {
                // unparseable file: skipped
            }
        }
        long parseMs = (System.nanoTime() - t0) / 1_000_000;
        long t1 = System.nanoTime();
        DslProject project = new DslProject(units, new HashMap<>());
        long projMs = (System.nanoTime() - t1) / 1_000_000;
        Map<String, RuleAst> rules = new LinkedHashMap<>();
        for (String n : List.of("java/empty-catch-block/rule.dsl", "java/optional-get-without-check/rule.dsl",
                "common/log-injection/rule.dsl", "java/lazy-association-outside-session/rule.dsl",
                "java/lazy-association-outside-session/serialized.dsl")) {
            rules.put(n, DslParser.parse(Files.readString(Path.of("review/" + n))));
        }
        System.out.printf("BENCH files=%d lines=%d parse=%dms project-model=%dms%n", files.size(), lines, parseMs, projMs);
        for (int round = 0; round < 3; round++) {
            for (var e : rules.entrySet()) {
                long t = System.nanoTime();
                int found = 0;
                for (CCFileAnalysis fa : files) {
                    try {
                        found += new DslInterpreter().run(e.getValue(), fa, "MEDIUM", project).size();
                    } catch (RuntimeException ex) {
                        // a rule that cannot evaluate a file contributes nothing
                    }
                }
                if (round == 2 && e.getKey().equals("common/log-injection/rule.dsl") && System.getProperty("bench.out") != null) {
                    StringBuilder b = new StringBuilder();
                    for (CCFileAnalysis fa : files) {
                        try {
                            new DslInterpreter().run(e.getValue(), fa, "MEDIUM", project)
                                    .forEach(f -> b.append(fa.file().getPath()).append(':').append(f.line()).append('\n'));
                        } catch (RuntimeException ex) {
                            // ignored
                        }
                    }
                    try {
                        Files.writeString(Path.of(System.getProperty("bench.out")), b.toString());
                    } catch (java.io.IOException io) {
                        throw new RuntimeException(io);
                    }
                }
                if (round == 2) {
                    System.out.printf("BENCH %-34s %5d ms  (%d findings)%n", e.getKey(), (System.nanoTime() - t) / 1_000_000, found);
                }
            }
        }
    }
}
