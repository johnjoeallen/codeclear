package com.codeclear.rules;

/** A deterministic finding on a changed line. */
public record CCRuleFinding(
        String ruleId, String path, int line, String message, CCRuleSeverity severity, CCRuleConfidence confidence) { }
