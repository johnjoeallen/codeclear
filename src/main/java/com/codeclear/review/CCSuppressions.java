package com.codeclear.review;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lines a developer has asked the review to leave alone with a comment that starts with {@code CodeClear:}, followed by the
 * reason:
 *
 * <pre>
 * // CodeClear: the retry is bounded by the caller        (ignores the next line that is not blank)
 * client.send(message);
 *
 * client.send(message); // CodeClear: fire and forget      (a trailing comment ignores its own line)
 * </pre>
 *
 * A comment alone on its line covers the next non-blank line, and a comment after code covers its own line. {@code //} and
 * {@code /* *}{@code /} comments are recognised in every file, and {@code #} comments in files that use them. Every finding on a
 * covered line is ignored, whatever the rule.
 */
final class CCSuppressions {

    private static final Pattern SLASH = Pattern.compile("(//|/\\*)\\s*CodeClear:", Pattern.CASE_INSENSITIVE);
    private static final Pattern HASH = Pattern.compile("#\\s*CodeClear:", Pattern.CASE_INSENSITIVE);
    private static final Set<String> HASH_EXTENSIONS = Set.of(".py", ".yml", ".yaml", ".properties", ".sh", ".toml", ".conf");

    private final Set<Integer> lines;

    private CCSuppressions(Set<Integer> lines) {
        this.lines = lines;
    }

    boolean covers(int line) {
        return lines.contains(line);
    }

    static CCSuppressions parse(String path, String content) {
        Set<Integer> covered = new HashSet<>();
        String[] text = content.split("\r?\n", -1);
        boolean hash = HASH_EXTENSIONS.stream().anyMatch(path.toLowerCase()::endsWith);
        for (int i = 0; i < text.length; i++) {
            Matcher m = SLASH.matcher(text[i]);
            int at = m.find() ? m.start() : -1;
            if (at < 0 && hash) {
                Matcher h = HASH.matcher(text[i]);
                at = h.find() ? h.start() : -1;
            }
            if (at < 0) {
                continue;
            }
            if (!text[i].substring(0, at).isBlank()) {
                covered.add(i + 1); // a trailing comment covers its own line
                continue;
            }
            int next = i + 1;
            // the comment may be a block comment over several lines: the code starts after it ends
            if (text[i].contains("/*") && !text[i].contains("*/")) {
                while (next < text.length && !text[next].contains("*/")) {
                    next++;
                }
                next++;
            }
            while (next < text.length && text[next].isBlank()) {
                next++;
            }
            if (next < text.length) {
                covered.add(next + 1);
            }
        }
        return new CCSuppressions(covered);
    }
}
