package com.codeclear.ai;

import com.codeclear.rules.CCRuleCandidate;
import com.codeclear.rules.CCRuleDefinition;

/** Asks the AI whether a candidate from a hybrid rule is a real finding. */
public interface CCCandidateConfirmer {

    /** Whether the AI can currently be asked, and if not why not. */
    CCAiDriverStatus status();

    /**
     * Judges one candidate. Never throws: any failure is a {@link CCCandidateVerdict.Kind#NO_VERDICT}.
     *
     * @param source the full text of the file the candidate is in, for context
     */
    CCCandidateVerdict confirm(CCRuleDefinition rule, CCRuleCandidate candidate, String source);
}
