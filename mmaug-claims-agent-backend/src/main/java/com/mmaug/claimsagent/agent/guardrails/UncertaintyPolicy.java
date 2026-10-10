package com.mmaug.claimsagent.agent.guardrails;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UncertaintyPolicy {

    private static final BigDecimal LOW_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.75);

    public boolean isLowConfidence(BigDecimal confidence) {
        if (confidence == null) return true;
        return confidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0;
    }
}