package com.codeclear.ai;

/**
 * Low-level transport to one AI backend (copilot.exe, Claude, OpenAI, ...).
 *
 * <p>A driver knows only how to send a prompt and return text. Prompt building, batching,
 * JSON parsing and finding validation live above it in the reviewer, so the review pipeline
 * is identical whichever driver is active.
 */
public interface CCAiDriver {

    /** Stable id used in configuration, e.g. {@code copilot}, {@code anthropic}, {@code openai}. */
    String id();

    /** Reports whether this driver is usable right now (executable found, API key present, ...). */
    CCAiDriverStatus status();

    /**
     * Sends one prompt and blocks until the full response is available.
     *
     * @throws CCAiDriverException on transport, auth, timeout or cancellation failures
     */
    CCAiResponse complete(CCAiRequest request) throws CCAiDriverException;
}
