package com.mmaug.claimsagent.agent.guardrails;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic prompt-injection detector.
 * Flags suspicious instruction-like text in document content.
 * Never obeys the instructions; only records the suspicion.
 */
@Component
public class PromptInjectionDetector {

    private static final Pattern[] PATTERNS = {
            Pattern.compile("ignore\\s+(all\\s+)?previous\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(all\\s+)?(previous|prior)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("approve\\s+this\\s+claim", Pattern.CASE_INSENSITIVE),
            Pattern.compile("authorise\\s+payment", Pattern.CASE_INSENSITIVE),
            Pattern.compile("authorize\\s+payment", Pattern.CASE_INSENSITIVE),
            Pattern.compile("override\\s+(the\\s+)?(rules|policy)", Pattern.CASE_INSENSITIVE)
    };

    public boolean suspected(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        for (Pattern p : PATTERNS) {
            if (p.matcher(lower).find()) {
                return true;
            }
        }
        return false;
    }

    public String firstMatch(String text) {
        if (text == null || text.isBlank()) return null;
        for (Pattern p : PATTERNS) {
            var m = p.matcher(text);
            if (m.find()) {
                String found = m.group();
                return found.length() > 200 ? found.substring(0, 200) : found;
            }
        }
        return null;
    }
}