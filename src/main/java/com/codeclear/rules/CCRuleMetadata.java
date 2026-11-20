package com.codeclear.rules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The metadata table at the top of a {@code rule.md}.
 *
 * @param appliesTo     file globs the rule applies to, for example {@code **}{@code /*.java}
 * @param category      the rule's category, kept as written
 * @param severity      the severity cap for findings
 * @param confidence    the confidence ceiling for findings
 * @param detectionType how the rule is decided
 */
public record CCRuleMetadata(
        List<String> appliesTo,
        String category,
        CCRuleSeverity severity,
        CCRuleConfidence confidence,
        CCDetectionType detectionType) {

    /** What "any source file" means: the code files of every supported language. */
    static final List<String> SOURCE_FILES = List.of("**/*.java", "**/*.py", "**/*.ts", "**/*.tsx", "**/*.js",
            "**/*.jsx", "**/*.mjs", "**/*.cjs", "**/*.go");

    private static final Pattern ROW = Pattern.compile("^\\|\\s*([^|]+?)\\s*\\|\\s*(.*?)\\s*\\|\\s*$");

    /** Reads the metadata table from the text of a rule file. */
    public static CCRuleMetadata parse(String ruleMarkdown) {
        Map<String, String> fields = new HashMap<>();
        for (String line : ruleMarkdown.split("\n")) {
            Matcher m = ROW.matcher(line.stripTrailing());
            if (m.matches()) {
                fields.putIfAbsent(m.group(1).toLowerCase(Locale.ROOT), m.group(2));
            }
        }
        String appliesTo = require(fields, "applies to");
        List<String> globs = appliesTo.strip().equalsIgnoreCase("any source file")
                ? SOURCE_FILES
                : Arrays.stream(appliesTo.split(","))
                        .map(g -> g.replace("`", "").strip())
                        .filter(g -> !g.isEmpty())
                        .toList();
        return new CCRuleMetadata(
                globs,
                require(fields, "category"),
                CCRuleSeverity.valueOf(keyword(fields, "severity")),
                CCRuleConfidence.valueOf(keyword(fields, "confidence")),
                CCDetectionType.valueOf(keyword(fields, "detection type")));
    }

    /** The leading word of a value, upper-cased. Some rules add a remark: {@code INFO (normal severity when ...)}. */
    private static String keyword(Map<String, String> fields, String key) {
        Matcher m = Pattern.compile("^[A-Za-z_]+").matcher(require(fields, key));
        if (!m.find()) {
            throw new IllegalArgumentException("rule file field '" + key + "' has no value");
        }
        return m.group().toUpperCase(Locale.ROOT);
    }

    private static String require(Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("rule file has no '" + key + "' field");
        }
        return value;
    }
}
