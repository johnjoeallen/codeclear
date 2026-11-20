package com.codeclear.rules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Loads rules from the {@code review/<language>/<rule-id>/} folders. */
public final class CCRuleLoader {

    /** Sections of {@code rule.md} that affect the generated script. Editing any other section does not make it stale. */
    private static final List<String> HASHED_SECTIONS =
            List.of("Detect", "Do not flag", "Examples", "Implementation notes", "Required tests");

    private static final Pattern METADATA_BLOCK = Pattern.compile("(?s)\\| Applies to.*?\\| Detection type[^\\n]*\\n");

    private CCRuleLoader() { }

    /** Loads one rule from its folder. */
    public static CCRuleDefinition load(Path ruleDir) {
        try {
            String markdown = Files.readString(ruleDir.resolve("rule.md"), StandardCharsets.UTF_8);
            Path scriptFile = ruleDir.resolve("rule.groovy");
            Optional<String> script = Files.exists(scriptFile)
                    ? Optional.of(Files.readString(scriptFile, StandardCharsets.UTF_8))
                    : Optional.empty();
            String id = ruleDir.getParent().getFileName() + "/" + ruleDir.getFileName();
            return new CCRuleDefinition(id, Optional.of(ruleDir), CCRuleMetadata.parse(markdown), hash(markdown), script);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read rule in " + ruleDir, e);
        }
    }

    /** Loads every rule under a review root, sorted by id. */
    public static List<CCRuleDefinition> loadAll(Path reviewRoot) {
        List<CCRuleDefinition> rules = new ArrayList<>();
        try (Stream<Path> langs = Files.list(reviewRoot)) {
            for (Path lang : langs.filter(Files::isDirectory).sorted().toList()) {
                try (Stream<Path> dirs = Files.list(lang)) {
                    for (Path dir : dirs.filter(d -> Files.exists(d.resolve("rule.md"))).sorted().toList()) {
                        rules.add(load(dir));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list rules in " + reviewRoot, e);
        }
        return rules;
    }

    /**
     * SHA-256 over the metadata block and the generation-relevant sections, each trimmed and joined by a newline.
     * The hash recorded in a script header is computed the same way.
     */
    static String hash(String ruleMarkdown) {
        List<String> parts = new ArrayList<>();
        Matcher meta = METADATA_BLOCK.matcher(ruleMarkdown);
        parts.add(meta.find() ? meta.group() : "");
        for (String section : HASHED_SECTIONS) {
            Matcher m = Pattern.compile("(?s)## " + Pattern.quote(section) + "\\n(.*?)(?=\\n## |\\z)").matcher(ruleMarkdown);
            parts.add(section + "\n" + (m.find() ? m.group(1).strip() : ""));
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", parts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
