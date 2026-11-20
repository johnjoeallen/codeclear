package com.codeclear.rules;

import groovy.lang.Script;

/**
 * Base class of every rule script. It supplies {@code finding(...)} and {@code candidate(...)}.
 * The bindings {@code ast}, {@code file}, {@code model} and {@code diff} are set by the detector.
 */
public abstract class CCRuleScriptBase extends Script {

    /** Version of the script API. Bump it when bindings or the facade change; every script then becomes stale. */
    public static final int API_VERSION = 1;

    /** A deterministic finding. {@code confidence} may lower, never raise, the rule's own. */
    public record Finding(int line, String message, String confidence) { }

    /** A candidate that the AI reviewer confirms or drops (hybrid rules). */
    public record Candidate(int line, String evidence) { }

    public Finding finding(int line, String message) {
        return new Finding(line, message, null);
    }

    public Finding finding(int line, String message, String confidence) {
        return new Finding(line, message, confidence);
    }

    public Candidate candidate(int line, String evidence) {
        return new Candidate(line, evidence);
    }
}
