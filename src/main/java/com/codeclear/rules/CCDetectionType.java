package com.codeclear.rules;

/**
 * The detection type recorded in a rule file. All executable types run as Groovy scripts; the value
 * describes the mechanism and whether the AI confirms the result.
 */
public enum CCDetectionType {
    AST, REGEX, LINTER, HYBRID, AI;

    /** True when the rule has a deterministic part that runs as a script. */
    public boolean executable() {
        return this != AI;
    }
}
