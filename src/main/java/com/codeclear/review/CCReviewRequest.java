package com.codeclear.review;

import java.util.List;
import java.util.Map;

/**
 * What to review.
 *
 * @param files         the files of the change
 * @param projectConfig project configuration facts for cross-file rules, by key (for example
 *                      {@code spring.jpa.open-in-view})
 */
public record CCReviewRequest(List<CCReviewFile> files, Map<String, String> projectConfig) {

    public CCReviewRequest(List<CCReviewFile> files) {
        this(files, Map.of());
    }
}
