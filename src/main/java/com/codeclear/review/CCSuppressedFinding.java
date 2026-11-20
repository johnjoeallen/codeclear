package com.codeclear.review;

/** A finding or candidate that a {@code CodeClear:} comment ignored. The report lists it as suppressed, without code. */
public record CCSuppressedFinding(String ruleId, String path, int line, String message) { }
