package com.codeclear.review;

/**
 * A rule that applies to the change but did not run, and why. A review lists these so a missing finding is never
 * mistaken for a clean result.
 */
public record CCSkippedRule(String ruleId, Reason reason, String detail) {

    public enum Reason {
        /** The rule has no deterministic part and needs the AI, which is not available. */
        NEEDS_AI,
        /** The rule is executable but nobody has written its detector yet. */
        NO_DETECTOR,
        /** The rule only applies to files of a language that has no analyzer yet. */
        NO_ANALYZER,
        /** The detector failed (bad rule, timeout, ...). */
        FAILED
    }
}
