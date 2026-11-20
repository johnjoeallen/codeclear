package com.codeclear.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** A rule's globs are relative to a repository root, but a review may be started from a folder above it. */
class CCRuleAppliesToTest {

    private final CCRuleDefinition testsOnly = CCRuleLoader.load(Path.of("review/java/test-without-meaningful-assertion"));

    @Test
    void aGlobMatchesFromTheRepositoryRoot() {
        assertThat(testsOnly.appliesTo("src/test/java/ATest.java")).isTrue();
        assertThat(testsOnly.appliesTo("src/main/java/A.java")).isFalse();
    }

    @Test
    void aGlobAlsoMatchesWhenTheReviewWasStartedAboveTheRoot() {
        assertThat(testsOnly.appliesTo("clipper/src/test/java/ATest.java")).isTrue();
        assertThat(testsOnly.appliesTo("../../work/clipper/src/test/java/ATest.java")).isTrue();
        assertThat(testsOnly.appliesTo("clipper/src/main/java/A.java")).isFalse();
    }

    @Test
    void windowsSeparatorsAreAccepted() {
        assertThat(testsOnly.appliesTo("clipper\\src\\test\\java\\ATest.java")).isTrue();
    }
}
