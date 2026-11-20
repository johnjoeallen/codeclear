package com.codeclear.ai;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * A single prompt to send to a driver.
 *
 * @param systemPrompt instructions (agent.md and instruction files); may be empty
 * @param userPrompt   the review prompt
 * @param model        model override; empty means the driver default
 * @param workingDir   repo being reviewed; used by CLI drivers as the process working directory
 * @param timeout      per-prompt timeout
 * @param jsonOutput   true when the caller expects a JSON document back
 */
public record CCAiRequest(
        String systemPrompt,
        String userPrompt,
        Optional<String> model,
        Optional<Path> workingDir,
        Duration timeout,
        boolean jsonOutput) {}
