package com.codeclear.rules;

/**
 * Confidence of a finding. Declared from highest to lowest, so a smaller ordinal is more confident.
 * A script may lower the rule's confidence (for example when a type could not be resolved) but never raise it.
 */
public enum CCRuleConfidence {
    HIGH, MEDIUM, LOW;

    /** The lower (less confident) of the two. */
    public static CCRuleConfidence lowest(CCRuleConfidence a, CCRuleConfidence b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
