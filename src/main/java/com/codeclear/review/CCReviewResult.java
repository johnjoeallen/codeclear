package com.codeclear.review;

import com.codeclear.rules.CCRuleCandidate;
import java.util.List;

/**
 * The outcome of a review.
 *
 * @param findings              deterministic findings, and candidates the AI confirmed
 * @param unconfirmedCandidates candidates from hybrid rules that the AI could not judge (no AI, or no answer); a person
 *                              may want to look at them, but they are not findings
 * @param skippedRules          applicable rules that did not run, with the reason
 * @param notes                 anything else worth telling the reader (files that could not be analysed, AI problems)
 * @param rulesRun              how many rules ran a detector
 * @param dismissedCandidates   candidates the AI judged not to be findings
 * @param aiDriver              id of the AI driver in use
 * @param aiStatus              whether it is ready, and why not if it is not
 * @param suppressed            findings and candidates a {@code CodeClear:} comment ignored; reported without code
 */
public record CCReviewResult(
        List<CCReviewFinding> findings,
        List<CCRuleCandidate> unconfirmedCandidates,
        List<CCSkippedRule> skippedRules,
        List<String> notes,
        int rulesRun,
        int dismissedCandidates,
        String aiDriver,
        String aiStatus,
        List<CCSuppressedFinding> suppressed) {

    public long skipped(CCSkippedRule.Reason reason) {
        return skippedRules.stream().filter(s -> s.reason() == reason).count();
    }
}
