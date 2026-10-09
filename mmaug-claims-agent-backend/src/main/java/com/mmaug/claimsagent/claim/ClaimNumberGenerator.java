package com.mmaug.claimsagent.claim;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ClaimNumberGenerator {

    private final AtomicLong counter = new AtomicLong(0);

    public String next() {
        int year = Year.now().getValue();
        long seq = counter.incrementAndGet();
        return String.format("CLM-%d-%06d", year, seq);
    }
}