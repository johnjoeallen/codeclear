package com.codeclear;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeClearApplicationTest {

    @Test
    void theRulesOptionIsRenamedToTheSpringSetting() {
        assertThat(CodeClearApplication.cliArguments(new String[]{"review", "--rules=/x/review", "src", "--fail-on=high"}))
                .containsExactly("review", "--codeclear.rules-dir=/x/review", "src", "--fail-on=high");
    }
}
