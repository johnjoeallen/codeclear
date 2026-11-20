package com.codeclear.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeclear.rules.CCDetectionType;
import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleConfidence;
import com.codeclear.rules.CCRuleDefinition;
import com.codeclear.rules.CCRuleMetadata;
import com.codeclear.rules.CCRuleSeverity;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class CCDriverCandidateConfirmerTest {

    /** A driver that returns a fixed answer (or throws) and records the prompts it was sent. */
    private static final class FakeDriver implements CCAiDriver {
        final List<CCAiRequest> sent = new ArrayList<>();
        final boolean ready;
        final String answer;
        final boolean fail;

        FakeDriver(boolean ready, String answer, boolean fail) {
            this.ready = ready;
            this.answer = answer;
            this.fail = fail;
        }

        @Override
        public String id() {
            return "fake";
        }

        @Override
        public CCAiDriverStatus status() {
            return ready ? CCAiDriverStatus.ok() : CCAiDriverStatus.notReady("fake is down");
        }

        @Override
        public CCAiResponse complete(CCAiRequest request) throws CCAiDriverException {
            sent.add(request);
            if (fail) {
                throw new CCAiDriverException("boom");
            }
            return new CCAiResponse(answer, "fake", OptionalInt.empty(), OptionalInt.empty(), Duration.ZERO);
        }
    }

    private static final CCRuleDefinition RULE = new CCRuleDefinition("common/log-injection", Optional.empty(),
            new CCRuleMetadata(List.of("**/*.java"), "LOGGING", CCRuleSeverity.LOW, CCRuleConfidence.MEDIUM, CCDetectionType.HYBRID),
            "h", Optional.empty(), Map.of(), Optional.of("Is the logged value attacker-controlled?"));
    private static final CCRuleCandidate CANDIDATE = new CCRuleCandidate("common/log-injection", "A.java", 3, "param flows to log");
    private static final String SOURCE = "line1\nline2\nlog.info(user);\nline4\n";

    private static CCCandidateVerdict confirm(FakeDriver driver) {
        return new CCDriverCandidateConfirmer(driver, Duration.ofSeconds(5)).confirm(RULE, CANDIDATE, SOURCE);
    }

    @Test
    void aConfirmingAnswerConfirms() {
        CCCandidateVerdict v = confirm(new FakeDriver(true, "{\"confirmed\": true, \"reason\": \"user controls it\"}", false));

        assertThat(v.kind()).isEqualTo(CCCandidateVerdict.Kind.CONFIRMED);
        assertThat(v.reason()).isEqualTo("user controls it");
    }

    @Test
    void aDismissingAnswerDismisses() {
        assertThat(confirm(new FakeDriver(true, "{\"confirmed\": false, \"reason\": \"constant\"}", false)).kind())
                .isEqualTo(CCCandidateVerdict.Kind.DISMISSED);
    }

    @Test
    void jsonWrappedInProseOrAFenceIsStillRead() {
        assertThat(confirm(new FakeDriver(true, "Sure!\n```json\n{\"confirmed\": true}\n```", false)).kind())
                .isEqualTo(CCCandidateVerdict.Kind.CONFIRMED);
    }

    @Test
    void anEmptyAnswerIsNoVerdict() {
        CCCandidateVerdict v = confirm(new FakeDriver(true, "", false));

        assertThat(v.kind()).isEqualTo(CCCandidateVerdict.Kind.NO_VERDICT);
        assertThat(v.reason()).contains("returned nothing");
    }

    @Test
    void anUnreadableAnswerIsNoVerdictNotAGuess() {
        assertThat(confirm(new FakeDriver(true, "I think maybe", false)).kind()).isEqualTo(CCCandidateVerdict.Kind.NO_VERDICT);
        assertThat(confirm(new FakeDriver(true, "{\"confirmed\": \"yes\"}", false)).kind()).isEqualTo(CCCandidateVerdict.Kind.NO_VERDICT);
        assertThat(confirm(new FakeDriver(true, "{not json}", false)).kind()).isEqualTo(CCCandidateVerdict.Kind.NO_VERDICT);
    }

    @Test
    void aFailingDriverIsNoVerdict() {
        CCCandidateVerdict v = confirm(new FakeDriver(true, "", true));

        assertThat(v.kind()).isEqualTo(CCCandidateVerdict.Kind.NO_VERDICT);
        assertThat(v.reason()).contains("boom");
    }

    @Test
    void aDriverThatIsNotReadyIsNeverCalled() {
        FakeDriver driver = new FakeDriver(false, "{\"confirmed\": true}", false);

        CCCandidateVerdict v = confirm(driver);

        assertThat(v.kind()).isEqualTo(CCCandidateVerdict.Kind.NO_VERDICT);
        assertThat(v.reason()).isEqualTo("fake is down");
        assertThat(driver.sent).isEmpty();
    }

    @Test
    void thePromptCarriesTheRuleQuestionEvidenceAndTheCodeAroundTheCandidate() {
        FakeDriver driver = new FakeDriver(true, "{\"confirmed\": true}", false);

        confirm(driver);

        CCAiRequest sent = driver.sent.get(0);
        assertThat(sent.jsonOutput()).isTrue();
        assertThat(sent.userPrompt()).contains("common/log-injection", "Is the logged value attacker-controlled?",
                "A.java:3", "param flows to log", ">> 3: log.info(user);", "   2: line2");
    }
}
