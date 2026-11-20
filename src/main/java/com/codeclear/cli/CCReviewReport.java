package com.codeclear.cli;

import com.codeclear.review.CCReviewFinding;
import com.codeclear.review.CCReviewResult;
import com.codeclear.review.CCSkippedRule;
import com.codeclear.rules.CCRuleCandidate;
import java.util.Arrays;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/** Renders a review result for a terminal or as JSON. */
final class CCReviewReport {

    private CCReviewReport() { }

    static String json(CCReviewResult result) {
        return JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    static String text(CCReviewResult r, boolean showSkipped) {
        StringBuilder b = new StringBuilder();
        for (CCReviewFinding f : r.findings()) {
            b.append(f.path()).append(':').append(f.line()).append("  ")
                    .append(f.severity()).append(f.confidence() == com.codeclear.rules.CCRuleConfidence.LOW ? " (low confidence)" : "")
                    .append("  ").append(f.ruleId()).append("\n    ").append(f.message())
                    .append(f.source() == CCReviewFinding.Source.AI_CONFIRMED ? "  [confirmed by the AI]" : "").append('\n');
        }
        if (!r.unconfirmedCandidates().isEmpty()) {
            if (!r.findings().isEmpty()) {
                b.append('\n');
            }
            b.append("Unconfirmed candidates (need the AI to judge; not findings):\n");
            for (CCRuleCandidate c : r.unconfirmedCandidates()) {
                b.append("  ").append(c.path()).append(':').append(c.line()).append("  ").append(c.ruleId())
                        .append("\n    ").append(c.evidence()).append('\n');
            }
        }
        if (b.length() > 0) {
            b.append('\n');
        }
        b.append(r.findings().size()).append(r.findings().size() == 1 ? " finding" : " findings")
                .append(" from ").append(r.rulesRun()).append(r.rulesRun() == 1 ? " rule" : " rules").append('\n');
        long needsAi = r.skipped(CCSkippedRule.Reason.NEEDS_AI);
        long none = r.skipped(CCSkippedRule.Reason.NO_DETECTOR);
        long noAnalyzer = r.skipped(CCSkippedRule.Reason.NO_ANALYZER);
        long failed = r.skipped(CCSkippedRule.Reason.FAILED);
        if (needsAi + none + noAnalyzer + failed > 0) {
            b.append("Not checked: ")
                    .append(Arrays.asList(
                            needsAi > 0 ? needsAi + " need the AI" : "",
                            none > 0 ? none + " have no detector yet" : "",
                            noAnalyzer > 0 ? noAnalyzer + " need an analyzer for another language" : "",
                            failed > 0 ? failed + " failed to run" : "").stream()
                            .filter(s -> !s.isEmpty()).collect(Collectors.joining(", ")))
                    .append(showSkipped ? "" : " (--show-skipped lists them)").append('\n');
        }
        if (showSkipped) {
            for (CCSkippedRule s : r.skippedRules()) {
                b.append("  ").append(s.reason()).append("  ").append(s.ruleId()).append("  ").append(s.detail()).append('\n');
            }
        }
        for (String note : r.notes()) {
            b.append("Note: ").append(note).append('\n');
        }
        b.append("AI: ").append(r.aiDriver()).append(" - ").append(r.aiStatus()).append('\n');
        return b.toString();
    }
}
