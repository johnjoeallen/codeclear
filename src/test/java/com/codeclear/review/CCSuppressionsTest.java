package com.codeclear.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CCSuppressionsTest {

    private static CCSuppressions java(String code) {
        return CCSuppressions.parse("src/A.java", code);
    }

    @Test
    void aCommentOnItsOwnLineCoversTheNextLine() {
        CCSuppressions s = java("a();\n// CodeClear: bounded by the caller\nb();\nc();\n");

        assertThat(s.covers(3)).isTrue();
        assertThat(s.covers(1)).isFalse();
        assertThat(s.covers(2)).isFalse();
        assertThat(s.covers(4)).isFalse();
    }

    @Test
    void aTrailingCommentCoversItsOwnLineOnly() {
        CCSuppressions s = java("a();\nb(); // CodeClear: fire and forget\nc();\n");

        assertThat(s.covers(2)).isTrue();
        assertThat(s.covers(1)).isFalse();
        assertThat(s.covers(3)).isFalse();
    }

    @Test
    void blankLinesBetweenTheCommentAndTheCodeAreSkipped() {
        CCSuppressions s = java("// CodeClear: reason\n\n   \nb();\nc();\n");

        assertThat(s.covers(4)).isTrue();
        assertThat(s.covers(5)).isFalse();
    }

    @Test
    void blockCommentsWork() {
        CCSuppressions s = java("/* CodeClear: reason */\nb();\nc(); /* CodeClear: other */\n");

        assertThat(s.covers(2)).isTrue();
        assertThat(s.covers(3)).isTrue();
    }

    @Test
    void aBlockCommentOverSeveralLinesCoversTheCodeAfterIt() {
        CCSuppressions s = java("/* CodeClear: a long reason\n   that goes on\n   and on */\nb();\nc();\n");

        assertThat(s.covers(4)).isTrue();
        assertThat(s.covers(5)).isFalse();
        assertThat(s.covers(2)).isFalse();
    }

    @Test
    void theKeywordIsCaseInsensitiveAndNeedsTheColon() {
        assertThat(java("// codeclear: x\nb();\n").covers(2)).isTrue();
        assertThat(java("// CODECLEAR:x\nb();\n").covers(2)).isTrue();
        assertThat(java("// CodeClear this is not it\nb();\n").covers(2)).isFalse();
        assertThat(java("// see CodeClear: docs\nb();\n").covers(2)).isFalse();
    }

    @Test
    void windowsLineEndingsAreHandled() {
        assertThat(java("// CodeClear: x\r\nb();\r\nc();\r\n").covers(2)).isTrue();
    }

    @Test
    void hashCommentsCountOnlyInFilesThatUseThem() {
        assertThat(CCSuppressions.parse("a.yml", "# CodeClear: x\nkey: value\n").covers(2)).isTrue();
        assertThat(CCSuppressions.parse("a.py", "x = 1  # CodeClear: x\n").covers(1)).isTrue();
        assertThat(java("# CodeClear: x\nb();\n").covers(2)).isFalse();
    }

    @Test
    void aCommentOnTheLastLineCoversNothing() {
        assertThat(java("a();\n// CodeClear: x").covers(3)).isFalse();
    }
}
