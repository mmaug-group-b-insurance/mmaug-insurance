package com.mmaug.claimsagent.claim;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
public class ClaimNumberGenerator {

    private static final String PREFIX = "CLM";
    private static final int SEQUENCE_WIDTH = 6;

    private final ClaimRepository claimRepository;

    public ClaimNumberGenerator(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    /**
     * Returns the next claim number in the form CLM-YYYY-NNNNNN.
     *
     * The number is derived from the highest existing claim number for the
     * current year. A fresh transaction is used so the query sees committed
     * rows even when this runs inside an outer transaction (i.e. the create
     * call). Collisions under concurrent load are handled by a retry loop
     * in ClaimService.create.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String next() {
        int year = Year.now().getValue();
        long nextSeq = claimRepository.findMaxClaimNumberForYear(year)
                .map(ClaimNumberGenerator::parseSequence)
                .map(max -> max + 1)
                .orElse(1L);

        return String.format("%s-%d-%0" + SEQUENCE_WIDTH + "d", PREFIX, year, nextSeq);
    }

    private static long parseSequence(String claimNumber) {
        if (claimNumber == null) return 0;
        int lastDash = claimNumber.lastIndexOf('-');
        if (lastDash < 0 || lastDash == claimNumber.length() - 1) return 0;
        try {
            return Long.parseLong(claimNumber.substring(lastDash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}