package com.codeclear.review;

import com.codeclear.rules.CCChangedLines;

/**
 * One file under review.
 *
 * @param path         repository-relative path
 * @param content      the file's full text
 * @param changedLines the lines the change touches; findings elsewhere are dropped
 * @param baseContent  the file as it was at the diff base, or null (a new file, or no diff); lets a config rule see what was removed
 */
public record CCReviewFile(String path, String content, CCChangedLines changedLines, String baseContent) {

    public CCReviewFile(String path, String content, CCChangedLines changedLines) {
        this(path, content, changedLines, null);
    }

    /** A whole-file review: every line counts as changed. */
    public static CCReviewFile whole(String path, String content) {
        return new CCReviewFile(path, content, CCChangedLines.all());
    }
}
