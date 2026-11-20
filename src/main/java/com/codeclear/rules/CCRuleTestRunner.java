package com.codeclear.rules;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the {@code tests.json} of a rule through the real {@link GroovyRuleDetector}.
 *
 * <p>Each case is a Java snippet and the findings it must produce. A case passes when the rule produces exactly
 * the expected lines and confidences, twice in a row (determinism). Each mutation names a flagged case and a fixed
 * case: the first must produce a finding and the second none.
 *
 * <p>Case fields: {@code name}, {@code path}, {@code source}, optional {@code prelude} (a line of declarations placed
 * before the source and excluded from line numbers), {@code expect} (list of {@code line}, optional
 * {@code confidence}; the rule's own confidence when omitted) and {@code covers} (labels tying the case to a rule
 * section).
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
    private final JsonMapper json = JsonMapper.builder().build();

    public CCRuleTestRunner(GroovyRuleDetector detector) {
        this.detector = detector;
    }

    public Report run(CCRuleDefinition rule) {
        Path dir = rule.dir().orElseThrow(() -> new IllegalArgumentException(rule.id() + " has no folder"));
        JsonNode tests;
        try {
            tests = json.readTree(dir.resolve("tests.json"));
        } catch (RuntimeException e) {
            return new Report(rule.id(), List.of(new CaseResult("tests.json", false, "cannot read: " + e.getMessage())));
        }
        String sharedPrelude = tests.has("prelude") ? tests.get("prelude").asString() : "";
        List<CaseResult> results = new ArrayList<>();
        JsonNode cases = tests.get("cases");
        for (JsonNode c : cases) {
            results.add(runCase(rule, c, sharedPrelude));
        }
        if (tests.has("mutations")) {
            for (JsonNode m : tests.get("mutations")) {
                results.add(runMutation(rule, cases, m, sharedPrelude));
            }
        }
        return new Report(rule.id(), results);
    }

    private CaseResult runCase(CCRuleDefinition rule, JsonNode c, String sharedPrelude) {
        String name = c.get("name").asString();
        try {
            List<String> first = actual(rule, c, sharedPrelude);
            List<String> second = actual(rule, c, sharedPrelude);
            List<String> expected = new ArrayList<>();
            for (JsonNode e : c.get("expect")) {
                String confidence = e.has("confidence")
                        ? e.get("confidence").asString()
                        : rule.metadata().confidence().name();
                expected.add(e.get("line").asInt() + ":" + confidence);
            }
            if (!first.equals(second)) {
                return new CaseResult(name, false, "not deterministic: " + first + " then " + second);
            }
            return first.equals(expected)
                    ? new CaseResult(name, true, "")
                    : new CaseResult(name, false, "got " + first + ", expected " + expected);
        } catch (RuntimeException e) {
            return new CaseResult(name, false, e.getMessage());
        }
    }

    private CaseResult runMutation(CCRuleDefinition rule, JsonNode cases, JsonNode m, String sharedPrelude) {
        String from = m.get("from").asString();
        String to = m.get("to").asString();
        String name = "mutation: " + from + " -> " + to;
        try {
            boolean flagged = !actual(rule, find(cases, from), sharedPrelude).isEmpty();
            boolean cleared = actual(rule, find(cases, to), sharedPrelude).isEmpty();
            if (flagged && cleared) {
                return new CaseResult(name, true, "");
            }
            return new CaseResult(name, false, flagged ? "the fixed form is still flagged" : "the flagged form is not flagged");
        } catch (RuntimeException e) {
            return new CaseResult(name, false, e.getMessage());
        }
    }

    private static JsonNode find(JsonNode cases, String name) {
        for (JsonNode c : cases) {
            if (c.get("name").asString().equals(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("mutation refers to unknown case '" + name + "'");
    }

    /** Findings as {@code line:confidence}, with lines relative to the snippet. Fails if the rule failed. */
    private List<String> actual(CCRuleDefinition rule, JsonNode c, String sharedPrelude) {
        if (!rule.language().equals("java")) {
            throw new IllegalArgumentException("the test runner supports Java rules only");
        }
        String prelude = c.has("prelude") ? c.get("prelude").asString() : sharedPrelude;
        int offset = (int) prelude.chars().filter(ch -> ch == '\n').count() + 1;
        CCFileAnalysis file = analyzer.analyze(c.get("path").asString(), prelude + "\n" + c.get("source").asString(),
                CCChangedLines.all());
        CCRuleResult result = detector.detect(rule, file);
        if (!result.ok()) {
            throw new IllegalStateException("rule failed: " + result.failure().get());
        }
        return result.findings().stream().map(f -> (f.line() - offset) + ":" + f.confidence()).toList();
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
