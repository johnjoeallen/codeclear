package com.codeclear.ai;

/**
 * The AI's judgement of a candidate raised by a hybrid rule.
 *
 * @param kind   confirmed, dismissed, or no verdict (AI unavailable, empty or unreadable answer, or a failure)
 * @param reason the AI's reason, or why there is no verdict
 */
public record CCCandidateVerdict(Kind kind, String reason) {

    public enum Kind { CONFIRMED, DISMISSED, NO_VERDICT }

    public static CCCandidateVerdict confirmed(String reason) {
        return new CCCandidateVerdict(Kind.CONFIRMED, reason);
    }

    public static CCCandidateVerdict dismissed(String reason) {
        return new CCCandidateVerdict(Kind.DISMISSED, reason);
    }

    public static CCCandidateVerdict noVerdict(String reason) {
        return new CCCandidateVerdict(Kind.NO_VERDICT, reason);
    }
}
