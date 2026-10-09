package com.mmaug.claimsagent.policy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClaimHistoryService {

    private final HistoricalClaimRepository historicalClaimRepository;

    public ClaimHistoryService(HistoricalClaimRepository historicalClaimRepository) {
        this.historicalClaimRepository = historicalClaimRepository;
    }

    public List<HistoricalClaim> claimsForPolicy(UUID policyId, int yearsBack) {
        LocalDate cutoff = LocalDate.now().minusYears(yearsBack);
        return historicalClaimRepository.findAllByPolicyIdAndLossDateAfter(policyId, cutoff);
    }
}