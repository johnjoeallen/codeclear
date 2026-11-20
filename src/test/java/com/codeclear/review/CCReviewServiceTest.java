package com.codeclear.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.ai.CCAiDriverStatus;
import com.codeclear.ai.CCCandidateConfirmer;
import com.codeclear.ai.CCCandidateVerdict;
import com.codeclear.rules.CCChangedLines;
import com.codeclear.rules.CCDetectionType;
import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleConfidence;
import com.codeclear.rules.CCRuleDefinition;
import com.codeclear.rules.CCRuleEngine;
import com.codeclear.rules.CCRuleLoader;
import com.codeclear.rules.CCRuleMetadata;
import com.codeclear.rules.CCRuleSeverity;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class CCReviewServiceTest {

    private static final CCRuleEngine ENGINE = new CCRuleEngine();

    private static final String EMPTY_CATCH = "class A {\n  void f() {\n    try { g(); } catch (Exception e) { }\n  }\n}\n";

    @AfterAll
    static void close() {
        ENGINE.close();
    }

    /** A confirmer with a fixed behaviour. */
    private static final class FakeConfirmer implements CCCandidateConfirmer {
        final CCAiDriverStatus status;
        final CCCandidateVerdict.Kind kind;
        final List<CCRuleCandidate> asked = new ArrayList<>();

        FakeConfirmer(boolean ready, CCCandidateVerdict.Kind kind) {
            this.status = ready ? CCAiDriverStatus.ok() : CCAiDriverStatus.notReady("no AI here");
            this.kind = kind;
        }

        @Override
        public CCAiDriverStatus status() {
            return status;
        }

        @Override
        public CCCandidateVerdict confirm(CCRuleDefinition rule, CCRuleCandidate candidate, String source) {
            asked.add(candidate);
            return new CCCandidateVerdict(kind, kind == CCCandidateVerdict.Kind.NO_VERDICT ? status.detail() : "because");
        }
    }

    private static CCRuleDefinition rule(String id, CCDetectionType type, String dsl) {
        return new CCRuleDefinition(id, Optional.empty(),
                new CCRuleMetadata(List.of("**/*.java"), "X", CCRuleSeverity.MEDIUM, CCRuleConfidence.MEDIUM, type),
                "h", Optional.empty(), dsl == null ? Map.of() : Map.of("rule.dsl", dsl));
    }

    private static final String REPORT = "rule 'r'\nCatch.each { c -> report c, 'deterministic finding' }";
    private static final String CANDIDATE = "rule 'r'\nCatch.each { c -> candidate c, 'maybe a problem' }";

    private static CCReviewService service(List<CCRuleDefinition> rules, CCCandidateConfirmer confirmer) {
        return new CCReviewService(rules, ENGINE, confirmer, "test");
    }

    private static CCReviewRequest oneFile() {
        return new CCReviewRequest(List.of(CCReviewFile.whole("src/main/java/A.java", EMPTY_CATCH)));
    }

    @Test
    void deterministicFindingsNeedNoAi() {
        CCReviewResult r = service(List.of(rule("java/r", CCDetectionType.AST, REPORT)),
                new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT)).review(oneFile());

        assertThat(r.findings()).extracting(CCReviewFinding::ruleId, CCReviewFinding::line, CCReviewFinding::source)
                .containsExactly(org.assertj.core.api.Assertions.tuple("java/r", 3, CCReviewFinding.Source.RULE));
        assertThat(r.rulesRun()).isEqualTo(1);
        assertThat(r.skippedRules()).isEmpty();
    }

    @Test
    void withoutAnAiACandidateStaysUnconfirmedAndTheReviewSaysWhy() {
        FakeConfirmer none = new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT);

        CCReviewResult r = service(List.of(rule("java/r", CCDetectionType.HYBRID, CANDIDATE)), none).review(oneFile());

        assertThat(r.findings()).isEmpty();
        assertThat(r.unconfirmedCandidates()).hasSize(1);
        assertThat(r.unconfirmedCandidates().get(0).evidence()).isEqualTo("maybe a problem");
        assertThat(r.notes()).contains("no AI here");
        assertThat(r.aiStatus()).isEqualTo("no AI here");
    }

    @Test
    void aCandidateTheAiConfirmsBecomesAFindingMarkedAsSuch() {
        CCReviewResult r = service(List.of(rule("java/r", CCDetectionType.HYBRID, CANDIDATE)),
                new FakeConfirmer(true, CCCandidateVerdict.Kind.CONFIRMED)).review(oneFile());

        assertThat(r.findings()).hasSize(1);
        assertThat(r.findings().get(0).source()).isEqualTo(CCReviewFinding.Source.AI_CONFIRMED);
        assertThat(r.findings().get(0).message()).isEqualTo("maybe a problem");
        assertThat(r.findings().get(0).severity()).isEqualTo(CCRuleSeverity.MEDIUM);
        assertThat(r.unconfirmedCandidates()).isEmpty();
        assertThat(r.aiStatus()).isEqualTo("ready");
    }

    @Test
    void aCandidateTheAiDismissesIsDroppedButCounted() {
        CCReviewResult r = service(List.of(rule("java/r", CCDetectionType.HYBRID, CANDIDATE)),
                new FakeConfirmer(true, CCCandidateVerdict.Kind.DISMISSED)).review(oneFile());

        assertThat(r.findings()).isEmpty();
        assertThat(r.unconfirmedCandidates()).isEmpty();
        assertThat(r.dismissedCandidates()).isEqualTo(1);
    }

    @Test
    void rulesThatNeedTheAiOrHaveNoDetectorAreListedAsSkipped() {
        CCReviewResult r = service(List.of(
                rule("java/judgement", CCDetectionType.AI, null),
                rule("java/not-written", CCDetectionType.AST, null)),
                new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT)).review(oneFile());

        assertThat(r.skippedRules()).extracting(CCSkippedRule::ruleId, CCSkippedRule::reason).containsExactly(
                org.assertj.core.api.Assertions.tuple("java/judgement", CCSkippedRule.Reason.NEEDS_AI),
                org.assertj.core.api.Assertions.tuple("java/not-written", CCSkippedRule.Reason.NO_DETECTOR));
        assertThat(r.rulesRun()).isZero();
    }

    @Test
    void aBrokenRuleIsReportedAsFailedAndOthersStillRun() {
        CCReviewResult r = service(List.of(
                rule("java/broken", CCDetectionType.AST, "rule 'x' Catch.each { c -> report c, 'm', c.nonsense != null }"),
                rule("java/r", CCDetectionType.AST, REPORT)),
                new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT)).review(oneFile());

        assertThat(r.skipped(CCSkippedRule.Reason.FAILED)).isEqualTo(1);
        assertThat(r.rulesRun()).isEqualTo(1);
        assertThat(r.findings()).hasSize(1);
    }

    @Test
    void rulesForOtherFilesDoNotAppearAndFindingsOffTheChangedLinesAreDropped() {
        CCReviewRequest request = new CCReviewRequest(List.of(
                new CCReviewFile("src/main/java/A.java", EMPTY_CATCH, CCChangedLines.of(Set.of(1)))));
        CCRuleDefinition tsOnly = new CCRuleDefinition("typescript/t", Optional.empty(),
                new CCRuleMetadata(List.of("**/*.ts"), "X", CCRuleSeverity.LOW, CCRuleConfidence.LOW, CCDetectionType.AI),
                "h", Optional.empty());

        CCReviewResult r = service(List.of(rule("java/r", CCDetectionType.AST, REPORT), tsOnly),
                new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT)).review(request);

        assertThat(r.findings()).isEmpty();
        assertThat(r.skippedRules()).isEmpty();
    }

    @Test
    void nonJavaFilesAreNotedAndRulesThatOnlyApplyToThemAreSkippedForWantOfAnAnalyzer() {
        CCRuleDefinition ts = new CCRuleDefinition("typescript/t", Optional.empty(),
                new CCRuleMetadata(List.of("**/*.ts"), "X", CCRuleSeverity.LOW, CCRuleConfidence.LOW, CCDetectionType.REGEX),
                "h", Optional.empty());

        CCReviewResult r = service(List.of(ts), new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT))
                .review(new CCReviewRequest(List.of(CCReviewFile.whole("web/app.ts", "const x = 1;"))));

        assertThat(r.skippedRules()).extracting(CCSkippedRule::reason).containsExactly(CCSkippedRule.Reason.NO_ANALYZER);
        assertThat(r.notes()).anyMatch(n -> n.contains("only Java has an analyzer"));
    }

    @Test
    void aFileThatDoesNotParseIsNotedNotFatal() {
        CCReviewResult r = service(List.of(rule("java/r", CCDetectionType.AST, REPORT)),
                new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT))
                .review(new CCReviewRequest(List.of(
                        CCReviewFile.whole("src/main/java/Bad.java", "class {{{"),
                        CCReviewFile.whole("src/main/java/A.java", EMPTY_CATCH))));

        assertThat(r.notes()).anyMatch(n -> n.contains("Bad.java"));
        assertThat(r.findings()).hasSize(1);
    }

    @Test
    void theRealRulesRunWithoutAnAiAndTheReviewListsWhatItCouldNotCheck() {
        List<CCRuleDefinition> rules = CCRuleLoader.loadAll(Path.of("review"));
        CCReviewResult r = service(rules, new FakeConfirmer(false, CCCandidateVerdict.Kind.NO_VERDICT)).review(oneFile());

        assertThat(r.findings()).extracting(CCReviewFinding::ruleId).contains("java/empty-catch-block");
        assertThat(r.rulesRun()).isGreaterThanOrEqualTo(4);
        assertThat(r.skipped(CCSkippedRule.Reason.NEEDS_AI)).isGreaterThan(0);
        assertThat(r.skipped(CCSkippedRule.Reason.NO_DETECTOR)).isGreaterThan(0);
        assertThat(r.skipped(CCSkippedRule.Reason.FAILED)).isZero();
    }
}
