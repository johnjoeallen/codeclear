package com.codeclear.review;

import java.util.List;
import java.util.Map;

/**
 * What to review.
 *
 * @param files         the files of the change
 * @param projectConfig project configuration facts for cross-file rules, by key (for example
 *                      {@code spring.jpa.open-in-view})
 * @param contextSources source files outside the change (path to text), searched for readers of a removed configuration key
 */
public record CCReviewRequest(List<CCReviewFile> files, Map<String, String> projectConfig, Map<String, String> contextSources) {

    public CCReviewRequest(List<CCReviewFile> files, Map<String, String> projectConfig) {
        this(files, projectConfig, Map.of());
    }

    public CCReviewRequest(List<CCReviewFile> files) {
        this(files, Map.of());
    }
}
