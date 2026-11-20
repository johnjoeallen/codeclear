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
import java.util.LinkedHashMap;
import java.util.Map;
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
            String markdown = text(ruleDir.resolve("rule.md"));
            Path scriptFile = ruleDir.resolve("rule.groovy");
            Optional<String> script = Files.exists(scriptFile) ? Optional.of(text(scriptFile)) : Optional.empty();
            String id = ruleDir.getParent().getFileName() + "/" + ruleDir.getFileName();
            return new CCRuleDefinition(id, Optional.of(ruleDir), CCRuleMetadata.parse(markdown), hash(markdown), script,
                    loadDsl(ruleDir), aiPrompt(markdown));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read rule in " + ruleDir, e);
        }
    }

    /** The text of the {@code ## AI prompt} section, when the rule has one. */
    private static Optional<String> aiPrompt(String markdown) {
        Matcher m = Pattern.compile("(?s)## AI prompt\\n(.*?)(?=\\n## |\\z)").matcher(markdown);
        return m.find() && !m.group(1).isBlank() ? Optional.of(m.group(1).strip()) : Optional.empty();
    }

    /** Reads every {@code .dsl} file in the folder, {@code rule.dsl} first and the rest by name. */
    private static Map<String, String> loadDsl(Path ruleDir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(ruleDir)) {
            List<Path> dsl = files.filter(f -> f.getFileName().toString().endsWith(".dsl"))
                    .sorted(java.util.Comparator.comparing((Path f) -> !f.getFileName().toString().equals("rule.dsl"))
                            .thenComparing(f -> f.getFileName().toString()))
                    .toList();
            for (Path f : dsl) {
                out.put(f.getFileName().toString(), text(f));
            }
        }
        return out;
    }

    /**
     * Reads a rule file with LF line endings. Git for Windows may check files out with CRLF, and the rule hash, the section
     * patterns and the metadata parser all expect LF; without this the same rule would hash differently on two machines.
     */
    private static String text(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
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
