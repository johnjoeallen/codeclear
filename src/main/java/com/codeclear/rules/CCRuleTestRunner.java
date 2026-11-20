package com.codeclear.rules;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the {@code tests.md} of a rule through the real {@link GroovyRuleDetector}.
 *
 * <p>Each case is a Java snippet with {@code // FLAG} markers on the lines that must produce findings (see
 * {@link CCRuleTests}). A case passes when the rule produces exactly those lines and confidences, twice in a row
 * (determinism). Each mutation names a flagged case and a fixed case: the first must produce a finding and the
 * second none.
 */
public final class CCRuleTestRunner {

    /** Outcome of one case or mutation. */
    public record CaseResult(String name, boolean passed, String detail) { }

    /** Outcome of every case and mutation of one rule. */
    public record Report(String ruleId, List<CaseResult> cases) {
        public boolean passed() {
            return !cases.isEmpty() && cases.stream().allMatch(CaseResult::passed);
        }
    }

    private final GroovyRuleDetector detector;
    private final JavaFileAnalyzer analyzer = new JavaFileAnalyzer();

    public CCRuleTestRunner(GroovyRuleDetector detector) {
        this.detector = detector;
    }

    public Report run(CCRuleDefinition rule) {
        Path dir = rule.dir().orElseThrow(() -> new IllegalArgumentException(rule.id() + " has no folder"));
        CCRuleTests.Tests tests;
        try {
            tests = CCRuleTests.load(dir);
        } catch (RuntimeException e) {
            return new Report(rule.id(), List.of(new CaseResult("tests.md", false, "cannot read: " + e.getMessage())));
        }
        List<CaseResult> results = new ArrayList<>();
        for (CCRuleTests.Case c : tests.cases()) {
            results.add(runCase(rule, c, tests.sharedPrelude()));
        }
        for (CCRuleTests.Mutation m : tests.mutations()) {
            results.add(runMutation(rule, tests, m));
        }
        return new Report(rule.id(), results);
    }

    private CaseResult runCase(CCRuleDefinition rule, CCRuleTests.Case c, String sharedPrelude) {
        try {
            List<String> first = actual(rule, c, sharedPrelude);
            List<String> second = actual(rule, c, sharedPrelude);
            List<String> expected = new ArrayList<>();
            for (CCRuleTests.Expect e : c.expect()) {
                String confidence = e.confidence() != null ? e.confidence() : rule.metadata().confidence().name();
                expected.add(e.line() + ":" + confidence);
            }
            if (!first.equals(second)) {
                return new CaseResult(c.name(), false, "not deterministic: " + first + " then " + second);
            }
            return first.equals(expected)
                    ? new CaseResult(c.name(), true, "")
                    : new CaseResult(c.name(), false, "got " + first + ", expected " + expected);
        } catch (RuntimeException e) {
            return new CaseResult(c.name(), false, e.getMessage());
        }
    }

    private CaseResult runMutation(CCRuleDefinition rule, CCRuleTests.Tests tests, CCRuleTests.Mutation m) {
        String name = "mutation: " + m.from() + " -> " + m.to();
        try {
            boolean flagged = !actual(rule, find(tests, m.from()), tests.sharedPrelude()).isEmpty();
            boolean cleared = actual(rule, find(tests, m.to()), tests.sharedPrelude()).isEmpty();
            if (flagged && cleared) {
                return new CaseResult(name, true, "");
            }
            return new CaseResult(name, false, flagged ? "the fixed form is still flagged" : "the flagged form is not flagged");
        } catch (RuntimeException e) {
            return new CaseResult(name, false, e.getMessage());
        }
    }

    private static CCRuleTests.Case find(CCRuleTests.Tests tests, String name) {
        return tests.cases().stream().filter(c -> c.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("mutation refers to unknown case '" + name + "'"));
    }

    /** Findings as {@code line:confidence}, with lines relative to the snippet. Fails if the rule failed. */
    private List<String> actual(CCRuleDefinition rule, CCRuleTests.Case c, String sharedPrelude) {
        if (!rule.language().equals("java")) {
            throw new IllegalArgumentException("the test runner supports Java rules only");
        }
        if (c.files().size() != 1) {
            throw new IllegalArgumentException("the Groovy runner supports single-file cases only");
        }
        CCRuleTests.SourceFile f = c.files().get(0);
        String prelude = c.prelude() != null ? c.prelude() : sharedPrelude;
        int offset = CCRuleTests.lineOffset(prelude);
        CCFileAnalysis file = analyzer.analyze(f.path(), CCRuleTests.analysisText(prelude, f.source()),
                CCChangedLines.all());
        CCRuleResult result = detector.detect(rule, file);
        if (!result.ok()) {
            throw new IllegalStateException("rule failed: " + result.failure().get());
        }
        return result.findings().stream().map(x -> (x.line() - offset) + ":" + x.confidence()).toList();
    }

    /** Convenience for callers that read a rule from disk. */
    public static Report runRule(Path ruleDir, GroovyRuleDetector detector) {
        try {
            return new CCRuleTestRunner(detector).run(CCRuleLoader.load(ruleDir));
        } catch (UncheckedIOException e) {
            return new Report(ruleDir.toString(), List.of(new CaseResult("load", false, e.getMessage())));
        }
    }
}
