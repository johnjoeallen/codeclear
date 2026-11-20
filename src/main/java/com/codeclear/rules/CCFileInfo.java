package com.codeclear.rules;

/**
 * File facts exposed to scripts as {@code file}. A plain bean, so that scripts read {@code file.path}.
 */
public final class CCFileInfo {

    private final String path;
    private final String language;

    public CCFileInfo(String path, String language) {
        this.path = path.replace('\\', '/');
        this.language = language;
    }

    /** Repository-relative path with forward slashes. */
    public String getPath() {
        return path;
    }

    /** Language folder name: {@code java}, {@code python}, ... */
    public String getLanguage() {
        return language;
    }
}
