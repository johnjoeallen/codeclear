package com.codeclear.rules;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The comment header at the top of a generated {@code rule.groovy} or {@code .dsl} file (either {@code //} or
 * {@code #} comment lines).
 *
 * <pre>
 * // generated-from: review/java/empty-catch-block/rule.md
 * // rule-hash: 3f9c...
 * // api-version: 1
 * // generator: claude-sonnet-5-5   generated: 2026-11-20
 * </pre>
 *
 * @param ruleHash   hash of the generation-relevant sections of the rule file the script was made from
 * @param apiVersion version of the script API (bindings and facade) the script was generated against
 */
public record CCRuleScriptHeader(Optional<String> generatedFrom, Optional<String> ruleHash, Optional<Integer> apiVersion) {

    private static final Pattern LINE = Pattern.compile("^(?://|#)\\s*([a-z-]+):\\s*(\\S+).*$");

    /** Reads the leading comment lines of a script. Reading stops at the first other line. */
    public static CCRuleScriptHeader parse(String script) {
        Optional<String> from = Optional.empty();
        Optional<String> hash = Optional.empty();
        Optional<Integer> api = Optional.empty();
        for (String line : script.split("\n")) {
            if (!line.startsWith("//") && !line.startsWith("#")) {
                break;
            }
            Matcher m = LINE.matcher(line.stripTrailing());
            if (!m.matches()) {
                continue;
            }
            switch (m.group(1)) {
                case "generated-from" -> from = Optional.of(m.group(2));
                case "rule-hash" -> hash = Optional.of(m.group(2));
                case "api-version" -> api = parseInt(m.group(2));
                default -> { }
            }
        }
        return new CCRuleScriptHeader(from, hash, api);
    }

    private static Optional<Integer> parseInt(String text) {
        try {
            return Optional.of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
