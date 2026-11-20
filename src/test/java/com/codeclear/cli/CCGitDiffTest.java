package com.codeclear.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CCGitDiffTest {

    private static final String DIFF = """
            diff --git a/src/A.java b/src/A.java
            index 111..222 100644
            --- a/src/A.java
            +++ b/src/A.java
            @@ -3,0 +4,2 @@ class A {
            +  x();
            +  y();
            @@ -10 +12 @@
            -old
            +new
            @@ -20,2 +21,0 @@
            -gone
            -gone
            diff --git a/src/Gone.java b/src/Gone.java
            deleted file mode 100644
            --- a/src/Gone.java
            +++ /dev/null
            @@ -1,3 +0,0 @@
            -a
            diff --git a/src/B.java b/src/B.java
            --- a/src/B.java
            +++ b/src/B.java
            @@ -1 +1 @@
            -a
            +b
            """;

    @Test
    void parsesChangedLinesFromHunks() {
        var changed = CCGitDiff.parse(DIFF);

        assertThat(changed.get("src/A.java")).containsExactlyInAnyOrder(4, 5, 12); // a pure deletion adds no lines
        assertThat(changed.get("src/B.java")).containsExactly(1);
        assertThat(changed).doesNotContainKey("src/Gone.java");
    }

    @Test
    void readsARealRepository(@TempDir Path dir) throws Exception {
        assumeTrue(git(dir, "init", "-q") == 0, "git is not available");
        Files.writeString(dir.resolve("A.java"), "class A {\n  void f() {}\n}\n");
        git(dir, "add", "-A");
        git(dir, "-c", "user.email=a@b", "-c", "user.name=t", "commit", "-qm", "base");
        Files.writeString(dir.resolve("A.java"), "class A {\n  void f() {}\n  void g() {}\n}\n");

        assertThat(CCGitDiff.changedLines(dir, "HEAD")).containsEntry("A.java", Set.of(3));
    }

    @Test
    void reportsAnUnknownRevisionAsAnError(@TempDir Path dir) throws Exception {
        assumeTrue(git(dir, "init", "-q") == 0, "git is not available");

        assertThatThrownBy(() -> CCGitDiff.changedLines(dir, "no-such-rev")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-such-rev");
    }

    private static int git(Path dir, String... args) throws IOException, InterruptedException {
        List<String> cmd = new java.util.ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        return p.waitFor();
    }
}
