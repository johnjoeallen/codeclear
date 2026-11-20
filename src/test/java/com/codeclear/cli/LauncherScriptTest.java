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
        // bash (Git Bash included) takes forward slashes; dirname in the script does not understand backslashes
        List<String> cmd = new java.util.ArrayList<>(List.of("bash",
                Path.of("codeclear.sh").toAbsolutePath().toString().replace('\\', '/')));
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("PATH", dir + java.io.File.pathSeparator + env.get("PATH"));
        env.put("CODECLEAR_JAR", dir.resolve("codeclear.jar").toString().replace('\\', '/'));
        env.put("CODECLEAR_NO_BROWSER", "1"); // a test must never open a browser
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
        assertThat(r.output()).contains("FAKEJAVA --enable-native-access=ALL-UNNAMED -jar").contains("review --help").doesNotContain("is required");
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

    @Test
    void aFolderArgumentReviewsItWithoutStartingTheWebApplication(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\"");

        Result r = launch(dir, "../redkite");

        assertThat(r.exit()).isZero();
        assertThat(r.output()).contains("FAKEJAVA --enable-native-access=ALL-UNNAMED -jar").contains(" review --rules=").contains("../redkite")
                .doesNotContain("Starting CodeClear").doesNotContain("--server.port");
    }

    @Test
    void noArgumentsReviewsTheCurrentFolderToo(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\"");

        Result r = launch(dir);

        assertThat(r.output()).contains(" review --rules=").doesNotContain("Starting CodeClear").doesNotContain("--server.port");
    }

    @Test
    void aLeadingReviewIsAcceptedButNotRepeated(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\"");

        assertThat(launch(dir, "review", "src").output()).contains(" review --rules=").doesNotContain("review review");
    }

    @Test
    void onlyServeStartsTheWebApplication(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\"");

        Result r = launch(dir, "serve");

        assertThat(r.output()).contains("Starting CodeClear on http://127.0.0.1:8080/").contains("--server.port=8080")
                .doesNotContain(" review ").doesNotContainPattern("\\bserve\\b");
    }

    @Test
    void underGitBashTheRulesFolderIsGivenToJavaAsAWindowsPath(@TempDir Path dir) throws Exception {
        requireBash();
        stubJava(dir, "openjdk version \"25.0.4\"");
        Path cygpath = dir.resolve("cygpath");
        Files.writeString(cygpath, "#!/bin/sh\necho C:/converted/review\n", StandardCharsets.UTF_8);
        assertThat(cygpath.toFile().setExecutable(true)).isTrue();

        assertThat(launch(dir, "src").output()).contains("--rules=C:/converted/review");
    }
}
