package com.codeclear.review;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * {@code /* *}{@code /} comments are recognised in every file, and {@code #} comments in files that use them.
 *
 * <p>Every finding on a covered line is ignored, unless the keyword is followed by rule ids in parentheses, which limit the comment to
 * those rules: {@code // CodeClear(java/empty-catch-block, java/log-and-rethrow): the caller handles it}.
 */
final class CCSuppressions {

    private static final Pattern SLASH = Pattern.compile("(//|/\\*)\\s*CodeClear\\s*(?:\\(([^)]*)\\))?\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern HASH = Pattern.compile("#\\s*CodeClear\\s*(?:\\(([^)]*)\\))?\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern DASH = Pattern.compile("--\\s*CodeClear\\s*(?:\\(([^)]*)\\))?\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Set<String> HASH_EXTENSIONS = Set.of(".py", ".yml", ".yaml", ".properties", ".sh", ".toml", ".conf");

    /** Line number to the rules the comment names; an empty set means every rule. */
    private final Map<Integer, Set<String>> lines;

    private CCSuppressions(Map<Integer, Set<String>> lines) {
        this.lines = lines;
    }

    /** Some comment covers this line for any rule. */
    boolean covers(int line) {
        Set<String> rules = lines.get(line);
        return rules != null && rules.isEmpty();
    }

    /** Some comment covers this line for this rule, either by naming it or by naming no rule. */
    boolean covers(int line, String ruleId) {
        Set<String> rules = lines.get(line);
        return rules != null && (rules.isEmpty() || rules.contains(ruleId));
    }

    static CCSuppressions parse(String path, String content) {
        Map<Integer, Set<String>> covered = new HashMap<>();
        String[] text = content.split("\r?\n", -1);
        boolean hash = HASH_EXTENSIONS.stream().anyMatch(path.toLowerCase()::endsWith);
        for (int i = 0; i < text.length; i++) {
            Matcher m = SLASH.matcher(text[i]);
            boolean found = m.find();
            if (!found && path.toLowerCase().endsWith(".sql")) {
                m = DASH.matcher(text[i]);
                found = m.find();
            }
            if (!found && hash) {
                m = HASH.matcher(text[i]);
                found = m.find();
            }
            if (!found) {
                continue;
            }
            int at = m.start();
            Set<String> rules = ruleIds(m.group(m.groupCount()));
            if (!text[i].substring(0, at).isBlank()) {
                add(covered, i + 1, rules); // a trailing comment covers its own line
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
                add(covered, next + 1, rules);
            }
        }
        return new CCSuppressions(covered);
    }

    /** The rule ids inside the parentheses, {@code CodeClear(java/a, java/b):}; none means every rule. */
    private static Set<String> ruleIds(String inParentheses) {
        Set<String> ids = new HashSet<>();
        if (inParentheses != null) {
            for (String id : inParentheses.split(",")) {
                if (!id.isBlank()) {
                    ids.add(id.strip());
                }
            }
        }
        return ids;
    }

    /** Two comments for one line add up; one that names no rule covers all of them. */
    private static void add(Map<Integer, Set<String>> covered, int line, Set<String> rules) {
        Set<String> had = covered.get(line);
        if (had == null) {
            covered.put(line, rules);
        } else if (had.isEmpty() || rules.isEmpty()) {
            covered.put(line, new HashSet<>());
        } else {
            Set<String> both = new HashSet<>(had);
            both.addAll(rules);
            covered.put(line, both);
        }
    }
}
