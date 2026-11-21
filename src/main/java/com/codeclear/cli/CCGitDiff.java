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
     * The revision a bare {@code --diff} compares against. On the default branch (main or master) that is its upstream
     * (falling back to {@code origin/<branch>}), so only local, unpushed work is reviewed. On any other branch it is the
     * point where the branch left the default branch, so only the branch's own changes are reviewed.
     *
     * @throws IllegalStateException when no base can be worked out
     */
    static String autoBase(Path dir) {
        String branch = git(dir, "rev-parse", "--abbrev-ref", "HEAD").out().strip();
        boolean onDefault = branch.equals("main") || branch.equals("master");
        if (onDefault) {
            for (String candidate : List.of("@{upstream}", "origin/" + branch)) {
                if (git(dir, "rev-parse", "--verify", "--quiet", candidate + "^{commit}").ok()) {
                    return candidate;
                }
            }
            throw new IllegalStateException("on " + branch + " but there is no upstream or origin/" + branch + " to compare with; pass --diff=<rev>");
        }
        List<String> candidates = new java.util.ArrayList<>();
        Result head = git(dir, "symbolic-ref", "--quiet", "--short", "refs/remotes/origin/HEAD");
        if (head.ok() && !head.out().isBlank()) {
            candidates.add(head.out().strip());
        }
        candidates.addAll(List.of("origin/main", "origin/master", "main", "master"));
        for (String candidate : candidates) {
            Result base = git(dir, "merge-base", "HEAD", candidate);
            if (base.ok() && !base.out().isBlank()) {
                return base.out().strip();
            }
        }
        throw new IllegalStateException("cannot find where " + branch + " began (no main or master branch); pass --diff=<rev>");
    }

    private record Result(boolean ok, String out) { }

    private static Result git(Path dir, String... args) {
        List<String> cmd = new java.util.ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        try {
            Process p = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new Result(p.waitFor() == 0, out);
        } catch (IOException e) {
            throw new IllegalStateException("cannot run git: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted running git", e);
        }
    }

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

    /**
     * The text of {@code path} (relative to {@code dir}) at {@code rev}, or null when the file did not exist there.
     */
    static String contentAt(Path dir, String rev, String path) {
        Result r = git(dir, "show", rev + ":./" + path);
        return r.ok() ? r.out() : null;
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
