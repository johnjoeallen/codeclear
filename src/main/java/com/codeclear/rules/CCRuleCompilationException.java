package com.codeclear.rules;

/** A rule script was rejected by the sandbox or failed to compile. */
public class CCRuleCompilationException extends RuntimeException {

    public CCRuleCompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
