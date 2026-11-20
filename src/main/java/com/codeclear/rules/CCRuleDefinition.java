package com.codeclear.rules;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;

/**
 * One rule: its metadata, the hash of its generation-relevant text, and its generated script if any.
 *
 * @param id       {@code <language>/<rule-id>}, for example {@code java/empty-catch-block}
 * @param dir      the rule's folder, when loaded from disk
 * @param ruleHash hash of the sections of {@code rule.md} that affect the script (see {@link CCRuleLoader})
 * @param script   source of {@code rule.groovy}, when present
 */
public record CCRuleDefinition(
        String id, Optional<Path> dir, CCRuleMetadata metadata, String ruleHash, Optional<String> script) {

    /** The language folder, for example {@code java}. */
    public String language() {
        return id.substring(0, id.indexOf('/'));
    }

    /** True when the rule applies to the given repository-relative path. */
    public boolean appliesTo(String path) {
        String normalized = path.replace('\\', '/');
        Path p = Path.of(normalized);
        for (String glob : metadata.appliesTo()) {
            if (FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(p)) {
                return true;
            }
            // The glob **/x also has to match a file at the repository root, which Java's matcher does not do.
            if (glob.startsWith("**/")
                    && FileSystems.getDefault().getPathMatcher("glob:" + glob.substring(3)).matches(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when there is no script, or the script was generated from different rule text or an older API.
     * Always false for rules that have no deterministic part.
     */
    public boolean stale() {
        if (!metadata.detectionType().executable()) {
            return false;
        }
        if (script.isEmpty()) {
            return true;
        }
        CCRuleScriptHeader header = CCRuleScriptHeader.parse(script.get());
        return !header.ruleHash().equals(Optional.of(ruleHash))
                || !header.apiVersion().equals(Optional.of(CCRuleScriptBase.API_VERSION));
    }
}
