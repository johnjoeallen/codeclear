package com.codeclear.review;

import com.codeclear.rules.CCChangedLines;

/**
 * One file under review.
 *
 * @param path         repository-relative path
 * @param content      the file's full text
 * @param changedLines the lines the change touches; findings elsewhere are dropped
 */
public record CCReviewFile(String path, String content, CCChangedLines changedLines) {

    /** A whole-file review: every line counts as changed. */
    public static CCReviewFile whole(String path, String content) {
        return new CCReviewFile(path, content, CCChangedLines.all());
    }
}
