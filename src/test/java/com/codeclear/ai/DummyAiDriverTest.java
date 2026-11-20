package com.codeclear.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DummyAiDriverTest {

    private final DummyAiDriver driver = new DummyAiDriver();

    @Test
    void reportsItselfAsNotReadySoTheReviewSkipsAiChecks() {
        assertThat(driver.id()).isEqualTo("dummy");
        assertThat(driver.status().ready()).isFalse();
        assertThat(driver.status().detail()).contains("no AI backend");
    }

    @Test
    void returnsNothingIfCalledAnyway() {
        CCAiResponse r = driver.complete(new CCAiRequest("", "hello", Optional.empty(), Optional.empty(),
                Duration.ofSeconds(1), false));

        assertThat(r.text()).isEmpty();
        assertThat(r.inputTokens()).isEmpty();
    }
}
