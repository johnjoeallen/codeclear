package com.codeclear.rules;

import java.util.List;
import java.util.Map;

/** What the rules see of a YAML, JSON or properties file: its settings as key paths with a value. */
public record CCConfigModel(List<Setting> settings, List<Removed> removed) {

    public CCConfigModel(List<Setting> settings) {
        this(settings, List.of());
    }

    /**
     * A setting that the change took out of a file: present in the file as it was at the diff base, absent now.
     *
     * @param line     where to report it: the line of the nearest remaining setting (the removal has no line of its own)
     * @param key      the key path in kebab form with dots
     * @param rawKey   the key path as it was written
     * @param oldValue the value it had
     */
    public record Removed(int line, String key, String rawKey, String oldValue) implements CCModelNode { }


    /**
     * One scalar setting.
     *
     * @param line    the line of the value (the key for a properties file)
     * @param key     the key path in lower-case kebab form with dots, {@code spring.datasource.password}; list items are
     *                {@code key[0]}. {@code trustAllCertificates} becomes {@code trust-all-certificates}
     * @param rawKey  the key path as written
     * @param value   the value as text; empty for null
     * @param profile the Spring profile the document is for ({@code application-dev.yml}, or
     *                {@code spring.config.activate.on-profile}), or empty
     * @param doc     the other settings of the same document by key, for a rule that needs a neighbour
     */
    public record Setting(int line, String key, String rawKey, String value, String profile, Map<String, String> doc)
            implements CCModelNode { }
}
