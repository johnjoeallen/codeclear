package com.codeclear.ai;

import java.time.Duration;
import java.util.OptionalInt;

/**
 * Raw driver output.
 *
 * @param text         response text
 * @param model        model actually used, if the backend reports it
 * @param inputTokens  absent when the backend does not report usage (e.g. copilot.exe)
 * @param outputTokens absent when the backend does not report usage
 * @param duration     wall-clock time of the call
 */
public record CCAiResponse(
        String text, String model, OptionalInt inputTokens, OptionalInt outputTokens, Duration duration) {}
