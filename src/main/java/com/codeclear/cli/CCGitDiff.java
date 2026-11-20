package com.codeclear.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds the lines a change touches, from {@code git diff}. */
final class CCGitDiff {

    private static final Pattern HUNK = Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,(\\d+))? @@.*");

    private CCGitDiff() { }

    /**
     * Changed lines by file for the working tree against {@code rev}, with paths relative to {@code dir}.
     *
     * @throws IllegalStateException when git fails (not a repository, unknown revision, git not installed)
     */
    static Map<String, Set<Integer>> changedLines(Path dir, String rev) {
        try {
            Process p = new ProcessBuilder(List.of("git", "diff", "--unified=0", "--relative", "--no-color", rev))
                    .directory(dir.toFile()).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (p.waitFor() != 0) {
                throw new IllegalStateException("git diff " + rev + " failed: " + out.strip());
            }
            return parse(out);
        } catch (IOException e) {
            throw new IllegalStateException("cannot run git: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted running git", e);
        }
    }

    /** Parses unified diff text ({@code --unified=0} or any context size) into changed line numbers by file. */
    static Map<String, Set<Integer>> parse(String diff) {
        Map<String, Set<Integer>> result = new HashMap<>();
        String file = null;
        for (String line : diff.split("\r?\n")) {
            if (line.startsWith("+++ ")) {
                String name = line.substring(4).strip();
                file = name.equals("/dev/null") ? null : (name.startsWith("b/") ? name.substring(2) : name);
                if (file != null) {
                    result.computeIfAbsent(file, k -> new HashSet<>());
                }
            } else if (file != null) {
                Matcher m = HUNK.matcher(line);
                if (m.matches()) {
                    int start = Integer.parseInt(m.group(1));
                    int count = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
                    for (int i = 0; i < count; i++) {
                        result.get(file).add(start + i);
                    }
                }
            }
        }
        return result;
    }
}
