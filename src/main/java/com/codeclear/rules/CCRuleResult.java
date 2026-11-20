package com.codeclear.rules;

import java.util.List;
import java.util.Optional;

/**
 * Outcome of running one rule on one file. A failed rule never throws into the review: it reports the reason
 * here so the review can list rules that did not run.
 *
 * @param droppedOffDiff results the script produced on lines that were not changed
 * @param failure        why the rule did not run, when it did not
 */
public record CCRuleResult(
        String ruleId,
        List<CCRuleFinding> findings,
        List<CCRuleCandidate> candidates,
        int droppedOffDiff,
        Optional<String> failure) {

    public static CCRuleResult empty(String ruleId) {
        return new CCRuleResult(ruleId, List.of(), List.of(), 0, Optional.empty());
    }

    public static CCRuleResult failed(String ruleId, String reason) {
        return new CCRuleResult(ruleId, List.of(), List.of(), 0, Optional.of(reason));
    }

    public boolean ok() {
        return failure.isEmpty();
    }
}
