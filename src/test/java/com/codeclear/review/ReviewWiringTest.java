package com.codeclear.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.ai.CCAiDriverRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The application context builds the review service with the dummy AI driver and the bundled rules. */
@SpringBootTest
class ReviewWiringTest {

    @Autowired
    CCReviewService service;

    @Autowired
    CCAiDriverRegistry registry;

    @Test
    void theDummyDriverIsRegisteredAndTheDefault() {
        assertThat(registry.ids()).contains("dummy");
        assertThat(registry.get("dummy").status().ready()).isFalse();
    }

    @Test
    void aReviewRunsEndToEndWithNoAi() {
        CCReviewResult r = service.review(new CCReviewRequest(java.util.List.of(CCReviewFile.whole("src/main/java/A.java",
                "class A {\n  void f() {\n    try { g(); } catch (Exception e) { }\n  }\n}\n"))));

        assertThat(r.aiDriver()).isEqualTo("dummy");
        assertThat(r.aiStatus()).contains("no AI backend");
        assertThat(r.findings()).extracting(CCReviewFinding::ruleId).contains("java/empty-catch-block");
        assertThat(r.skipped(CCSkippedRule.Reason.NEEDS_AI)).isGreaterThan(0);
    }
}
