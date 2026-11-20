package com.codeclear.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The bash launcher's Java version check, run against a stub {@code java} so no real JVM is needed. */
class LauncherScriptTest {

    private record Result(int exit, String output) { }

    /** Writes a stub java that prints the given lines (to stderr) for -version and echoes its arguments otherwise. */
    private static void stubJava(Path dir, String... versionLines) throws IOException {
        StringBuilder script = new StringBuilder("#!/bin/sh\nif [ \"$1\" = \"-version\" ]; then\n");
        for (String line : versionLines) {
            script.append("  echo '").append(line).append("' >&2\n");
        }
        script.append("  exit 0\nfi\necho \"FAKEJAVA $@\"\n");
        Path java = dir.resolve("java");
        Files.writeString(java, script.toString(), StandardCharsets.UTF_8);
        assertThat(java.toFile().setExecutable(true)).isTrue();
        Files.writeString(dir.resolve("codeclear.jar"), "not a real jar");
    }

    private static Result launch(Path dir, String... args) throws Exception {
        List<String> cmd = new java.util.ArrayList<>(List.of("bash", Path.of("codeclear.sh").toAbsolutePath().toString()));
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("PATH", dir + ":" + env.get("PATH"));
        env.put("CODECLEAR_JAR", dir.resolve("codeclear.jar").toString());
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(p.waitFor(), out);
    }

    private static void requireBash() throws Exception {
        boolean bash;
        try {
            bash = new ProcessBuilder("bash", "-c", "true").start().waitFor() == 0;
        } catch (IOException e) {
            bash = false;
        }
        assumeTrue(bash, "bash is not available");
    }

    @Test
    void readsTheVersionEvenWhenJavaToolOptionsAddsALineFirst(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "Picked up JAVA_TOOL_OPTIONS: -Xmx1g", "openjdk version \"25.0.4\" 2026-07-21 LTS",
                "OpenJDK Runtime Environment (build 25.0.4+7-LTS)");

        Result r = launch(dir, "review", "--help");

        assertThat(r.exit()).isZero();
        assertThat(r.output()).contains("FAKEJAVA -jar").contains("review --help").doesNotContain("is required");
    }

    @Test
    void stillWorksWhenTheVersionIsOnTheFirstLine(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\" 2026-07-21 LTS");

        assertThat(launch(dir, "review", "--help").exit()).isZero();
    }

    @Test
    void anOldJavaIsRefusedWithItsVersion(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "Picked up JAVA_TOOL_OPTIONS: -Xmx1g", "openjdk version \"17.0.9\" 2023-10-17");

        Result r = launch(dir, "review", "--help");

        assertThat(r.exit()).isNotZero();
        assertThat(r.output()).contains("Java 25 or newer is required (found: 17)");
    }

    @Test
    void aMissingVersionLineIsReportedAsUnknown(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "Picked up JAVA_TOOL_OPTIONS: -Xmx1g");

        Result r = launch(dir, "review", "--help");

        assertThat(r.exit()).isNotZero();
        assertThat(r.output()).contains("found: unknown");
    }

    @Test
    void anExplicitRulesFolderComesAfterTheDefaultSoItWins(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\"");

        String out = launch(dir, "review", "--rules=/mine", "src").output();

        assertThat(out.indexOf("--rules=")).isLessThan(out.indexOf("--rules=/mine"));
    }

    @Test
    void theReviewRulesFolderIsPassedToTheJar(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\"");

        assertThat(launch(dir, "review", "src").output()).contains("review --rules=").contains(" src");
    }
}
