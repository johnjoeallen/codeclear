package com.codeclear.ai;

import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleDefinition;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Confirms candidates through whichever {@link CCAiDriver} is active. The prompt is the rule's own AI prompt, the
 * evidence the detector gathered and the code around the candidate; the answer is a small JSON document
 * {@code {"confirmed": true|false, "reason": "..."}}. An empty, unreadable or failed answer is "no verdict", so
 * the candidate is left unconfirmed rather than guessed at.
 */
public final class CCDriverCandidateConfirmer implements CCCandidateConfirmer {

    private static final int CONTEXT_LINES = 8;
    private static final Pattern JSON_OBJECT = Pattern.compile("(?s)\\{.*\\}");
    private static final String SYSTEM = """
            You confirm possible issues found by a static check. Use only the rule question, the evidence and the \
            code shown. Answer with JSON only: {"confirmed": true or false, "reason": "one sentence"}. \
            Answer true only when the code clearly has the problem.""";

    private final CCAiDriver driver;
    private final Duration timeout;
    private final JsonMapper json = JsonMapper.builder().build();

    public CCDriverCandidateConfirmer(CCAiDriver driver, Duration timeout) {
        this.driver = driver;
        this.timeout = timeout;
    }

    @Override
    public CCAiDriverStatus status() {
        return driver.status();
    }

    @Override
    public CCCandidateVerdict confirm(CCRuleDefinition rule, CCRuleCandidate candidate, String source) {
        CCAiDriverStatus status = driver.status();
        if (!status.ready()) {
            return CCCandidateVerdict.noVerdict(status.detail());
        }
        CCAiRequest request = new CCAiRequest(SYSTEM, prompt(rule, candidate, source), Optional.empty(), Optional.empty(),
                timeout, true);
        String text;
        try {
            text = driver.complete(request).text();
        } catch (CCAiDriverException | RuntimeException e) {
            return CCCandidateVerdict.noVerdict("the AI call failed: " + e.getMessage());
        }
        if (text == null || text.isBlank()) {
            return CCCandidateVerdict.noVerdict("the AI returned nothing");
        }
        return parse(text);
    }

    String prompt(CCRuleDefinition rule, CCRuleCandidate candidate, String source) {
        return "Rule: " + rule.id() + "\n"
                + "Question: " + rule.aiPrompt().orElse("Is this a real instance of the rule?") + "\n"
                + "Location: " + candidate.path() + ":" + candidate.line() + "\n"
                + "Evidence: " + candidate.evidence() + "\n"
                + "Code:\n" + context(source, candidate.line());
    }

    private static String context(String source, int line) {
        String[] lines = source.split("\n", -1);
        int from = Math.max(1, line - CONTEXT_LINES);
        int to = Math.min(lines.length, line + CONTEXT_LINES);
        StringBuilder b = new StringBuilder();
        for (int n = from; n <= to; n++) {
            b.append(n == line ? ">> " : "   ").append(n).append(": ").append(lines[n - 1]).append('\n');
        }
        return b.toString();
    }

    private CCCandidateVerdict parse(String text) {
        Matcher m = JSON_OBJECT.matcher(text);
        if (!m.find()) {
            return CCCandidateVerdict.noVerdict("the AI answer was not JSON");
        }
        try {
            JsonNode node = json.readTree(m.group());
            if (!node.has("confirmed") || !node.get("confirmed").isBoolean()) {
                return CCCandidateVerdict.noVerdict("the AI answer had no boolean 'confirmed'");
            }
            String reason = node.has("reason") ? node.get("reason").asString() : "";
            return node.get("confirmed").asBoolean()
                    ? CCCandidateVerdict.confirmed(reason)
                    : CCCandidateVerdict.dismissed(reason);
        } catch (RuntimeException e) {
            return CCCandidateVerdict.noVerdict("the AI answer could not be read: " + e.getMessage());
        }
    }
}
