package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.rules.dsl.DslRuleDetector;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/** The engine chooses a detector per rule and reports failures instead of throwing. */
class CCRuleEngineTest {

    private static final GroovyRuleDetector GROOVY = new GroovyRuleDetector();
    private static final DslRuleDetector DSL = new DslRuleDetector();
    private static final JavaFileAnalyzer ANALYZER = new JavaFileAnalyzer();
    private static final String SOURCE = "class A {\n  void f() {\n    try { g(); } catch (Exception e) { }\n  }\n}\n";
    private static final String HASH = "hash";

    private static final String DSL_FINDING = """
            rule 'x'
            Catch.each { c ->
                report c, "from dsl"
            }
            """;

    @AfterAll
    static void close() {
        GROOVY.close();
    }

    private static CCRuleDefinition rule(CCDetectionType type, CCRuleConfidence confidence, String groovy, String dsl) {
        return new CCRuleDefinition("java/test-rule", Optional.empty(),
                new CCRuleMetadata(List.of("**/*.java"), "CORRECTNESS", CCRuleSeverity.MEDIUM, confidence, type),
                HASH, Optional.ofNullable(groovy), dsl == null ? Map.of() : Map.of("rule.dsl", dsl));
    }

    private static List<CCRuleResult> run(CCRuleEngine.Prefer prefer, CCRuleDefinition rule, CCChangedLines changed) {
        CCFileAnalysis file = ANALYZER.analyze("src/main/java/A.java", SOURCE, changed);
        return new CCRuleEngine(GROOVY, DSL, prefer).run(List.of(rule), List.of(file), Map.of());
    }

    @Test
    void theRestrictedLanguageWinsByDefaultAndGroovyWhenPreferred() {
        CCRuleDefinition both = rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, "[finding(3, 'from groovy')]", DSL_FINDING);

        assertThat(run(CCRuleEngine.Prefer.DSL, both, CCChangedLines.all()).get(0).findings())
                .extracting(CCRuleFinding::message).containsExactly("from dsl");
        assertThat(run(CCRuleEngine.Prefer.GROOVY, both, CCChangedLines.all()).get(0).findings())
                .extracting(CCRuleFinding::message).containsExactly("from groovy");
    }

    @Test
    void aRuleWithOnlyOneDetectorUsesIt() {
        CCRuleDefinition dslOnly = rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, DSL_FINDING);
        CCRuleDefinition groovyOnly = rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, "[finding(3, 'g')]", null);

        assertThat(run(CCRuleEngine.Prefer.GROOVY, dslOnly, CCChangedLines.all()).get(0).findings()).hasSize(1);
        assertThat(run(CCRuleEngine.Prefer.DSL, groovyOnly, CCChangedLines.all()).get(0).findings()).hasSize(1);
    }

    @Test
    void findingsCarryTheRulesSeverityAndPath() {
        CCRuleFinding f = run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AST, CCRuleConfidence.HIGH, null, DSL_FINDING),
                CCChangedLines.all()).get(0).findings().get(0);

        assertThat(f.ruleId()).isEqualTo("java/test-rule");
        assertThat(f.path()).isEqualTo("src/main/java/A.java");
        assertThat(f.line()).isEqualTo(3);
        assertThat(f.severity()).isEqualTo(CCRuleSeverity.MEDIUM);
        assertThat(f.confidence()).isEqualTo(CCRuleConfidence.HIGH);
    }

    @Test
    void aCandidateClauseHandsEvidenceToTheAiInsteadOfReportingAFinding() {
        CCRuleResult r = run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.HYBRID, CCRuleConfidence.MEDIUM, null, """
                rule 'x'
                Catch.each { c ->
                    candidate c, "catch of ${c.param.type} to confirm"
                }
                """), CCChangedLines.all()).get(0);

        assertThat(r.findings()).isEmpty();
        assertThat(r.candidates()).extracting(CCRuleCandidate::evidence).containsExactly("catch of Exception to confirm");
        assertThat(r.candidates().get(0).line()).isEqualTo(3);
    }

    @Test
    void resultsOffTheChangedLinesAreDropped() {
        CCRuleResult r = run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, DSL_FINDING),
                CCChangedLines.of(Set.of(1))).get(0);

        assertThat(r.findings()).isEmpty();
        assertThat(r.droppedOffDiff()).isEqualTo(1);
    }

    @Test
    void aRuleMayLowerItsConfidenceButNeverRaiseIt() {
        String raise = DSL_FINDING.replace("\"from dsl\"", "\"from dsl\", confidence: HIGH");
        String lower = DSL_FINDING.replace("\"from dsl\"", "\"from dsl\", confidence: LOW");

        assertThat(run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, raise),
                CCChangedLines.all()).get(0).findings().get(0).confidence()).isEqualTo(CCRuleConfidence.MEDIUM);
        assertThat(run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, lower),
                CCChangedLines.all()).get(0).findings().get(0).confidence()).isEqualTo(CCRuleConfidence.LOW);
    }

    @Test
    void aBrokenRuleIsAFailureNotAnException() {
        CCRuleResult parse = run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, "rule 'x' for"),
                CCChangedLines.all()).get(0);
        CCRuleResult field = run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, """
                rule 'x'
                Catch.each { c ->
                    report c, "x", c.nonsenseField != null
                }
                """), CCChangedLines.all()).get(0);

        assertThat(parse.ok()).isFalse();
        assertThat(field.ok()).isFalse();
        assertThat(field.failure().get()).contains("nonsenseField");
    }

    @Test
    void anExecutableRuleWithNoDetectorIsReportedAsNotRun() {
        CCRuleResult r = run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, null),
                CCChangedLines.all()).get(0);

        assertThat(r.ok()).isFalse();
    }

    @Test
    void anAiRuleWithoutADetectorIsSimplySkipped() {
        CCRuleResult r = run(CCRuleEngine.Prefer.DSL, rule(CCDetectionType.AI, CCRuleConfidence.MEDIUM, null, null),
                CCChangedLines.all()).get(0);

        assertThat(r.ok()).isTrue();
        assertThat(r.findings()).isEmpty();
    }

    @Test
    void staleCoversTheDslHeaderToo() {
        String current = "// rule-hash: " + HASH + "\n// api-version: " + CCRuleDefinition.DSL_API_VERSION + "\n" + DSL_FINDING;
        String oldHash = "// rule-hash: other\n// api-version: " + CCRuleDefinition.DSL_API_VERSION + "\n" + DSL_FINDING;
        String oldApi = "// rule-hash: " + HASH + "\n// api-version: 0\n" + DSL_FINDING;

        assertThat(rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, current).stale()).isFalse();
        assertThat(rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, oldHash).stale()).isTrue();
        assertThat(rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, oldApi).stale()).isTrue();
        assertThat(rule(CCDetectionType.AST, CCRuleConfidence.MEDIUM, null, null).stale()).isTrue();
    }

    @Test
    void crossFileRulesSeeAllFilesOfTheRun() {
        CCRuleDefinition lazy = CCRuleLoader.load(java.nio.file.Path.of("review/java/lazy-association-outside-session"));
        List<CCFileAnalysis> files = List.of(
                ANALYZER.analyze("Order.java", """
                        import jakarta.persistence.*;
                        import java.util.*;
                        @Entity class Order { @OneToMany List<Item> items;
                          public List<Item> getItems() { return items; } }
                        @Entity class Item { }
                        """, CCChangedLines.all()),
                ANALYZER.analyze("C.java", """
                        class C {
                          Order o;
                          int n() { return o.getItems().size(); }
                        }
                        """, CCChangedLines.all()));

        List<CCRuleResult> results = new CCRuleEngine(GROOVY, DSL, CCRuleEngine.Prefer.DSL)
                .run(List.of(lazy), files, Map.of("spring.jpa.open-in-view", "false"));

        assertThat(results.stream().flatMap(r -> r.findings().stream()))
                .extracting(CCRuleFinding::path, CCRuleFinding::line).containsExactly(org.assertj.core.api.Assertions.tuple("C.java", 3));
    }
}
