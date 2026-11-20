package com.codeclear.rules;

/** A candidate from a hybrid rule, to be confirmed by the AI reviewer with the rule's AI prompt. */
public record CCRuleCandidate(String ruleId, String path, int line, String evidence) { }
