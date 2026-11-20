package com.codeclear.review;

import com.codeclear.rules.CCRuleConfidence;
import com.codeclear.rules.CCRuleSeverity;

/**
 * A finding in a review result.
 *
 * @param source {@link Source#RULE} when a rule's detector reported it, {@link Source#AI_CONFIRMED} when a hybrid rule's
 *               candidate was confirmed by the AI
 */
public record CCReviewFinding(
        String ruleId, String path, int line, String message, CCRuleSeverity severity, CCRuleConfidence confidence,
        Source source) {

    public enum Source { RULE, AI_CONFIRMED }
}
