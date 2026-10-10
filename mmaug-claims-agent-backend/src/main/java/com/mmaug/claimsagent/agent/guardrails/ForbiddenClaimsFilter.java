package com.mmaug.claimsagent.agent.guardrails;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Blocks forbidden wording from AI output.
 * The AI must never approve, reject, determine liability, authorise payment,
 * or modify a policy.
 */
@Component
public class ForbiddenClaimsFilter {

    private static final List<String> FORBIDDEN_PHRASES = List.of(
            "we approve",
            "claim approved",
            "claim is approved",
            "approved the claim",
            "we reject",
            "claim rejected",
            "claim is rejected",
            "rejected the claim",
            "liable",
            "liability is",
            "we will pay",
            "authorise payment",
            "authorize payment",
            "payment authorised",
            "payment authorized",
            "modify the policy",
            "change the policy",
            "update the policy"
    );

    public boolean containsForbidden(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String phrase : FORBIDDEN_PHRASES) {
            if (lower.contains(phrase)) return true;
        }
        return false;
    }

    public String findForbidden(String text) {
        if (text == null || text.isBlank()) return null;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String phrase : FORBIDDEN_PHRASES) {
            if (lower.contains(phrase)) return phrase;
        }
        return null;
    }
}