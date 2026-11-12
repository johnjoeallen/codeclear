package com.codeclear.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings bound from the {@code codeclear.*} namespace (see design.md, section 11). */
@ConfigurationProperties("codeclear")
public record CodeClearProperties(
        @DefaultValue("30") int maxFindings,
        @DefaultValue("100000") int maxBatchChars,
        @DefaultValue("40") int maxBatches,
        @DefaultValue("48000") int fullFileMaxChars,
        @DefaultValue("300") int promptTimeoutSeconds,
        @DefaultValue("COMMITTED") String defaultScope,
        @DefaultValue(".codeClear/agent.md") String agentFile,
        @DefaultValue(".codeClear/instructions") String instructionsDir,
        @DefaultValue("codeClear/agent.md") String classpathFallback,
        @DefaultValue("~/.codeClear") String userDir,
        @DefaultValue(".codeClear") String repoDir,
        @DefaultValue Copilot copilot) {

    public record Copilot(@DefaultValue("copilot.exe") String cliPath) {}
}
