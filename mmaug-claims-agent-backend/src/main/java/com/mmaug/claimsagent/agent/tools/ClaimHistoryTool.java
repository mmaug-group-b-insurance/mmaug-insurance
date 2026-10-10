package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.policy.HistoricalClaim;
import com.mmaug.claimsagent.policy.HistoricalClaimRepository;
import com.mmaug.claimsagent.policy.Policy;
import com.mmaug.claimsagent.policy.PolicyService;
import com.mmaug.claimsagent.tools.dto.HistoryView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ClaimHistoryTool {

    private final PolicyService policyService;
    private final HistoricalClaimRepository historyRepository;

    public ClaimHistoryTool(PolicyService policyService, HistoricalClaimRepository historyRepository) {
        this.policyService = policyService;
        this.historyRepository = historyRepository;
    }

    public HistoryView lookup(String policyNumber, int yearsBack) {
        Policy policy = policyService.findByNumber(policyNumber)
                .orElseThrow(() -> new NotFoundException("Policy not found: " + policyNumber));

        int years = Math.max(1, Math.min(10, yearsBack));
        LocalDate cutoff = LocalDate.now().minusYears(years);

        List<HistoricalClaim> claims = historyRepository.findAllByPolicyIdAndLossDateAfter(policy.getId(), cutoff);
        BigDecimal totalPaid = claims.stream()
                .map(HistoricalClaim::getAmountPaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return HistoryView.builder()
                .policyNumber(policy.getPolicyNumber())
                .totalClaims(claims.size())
                .totalPaid(totalPaid)
                .claims(claims.stream()
                        .map(c -> HistoryView.HistoryItem.builder()
                                .claimReference(c.getClaimReference())
                                .lossDate(c.getLossDate())
                                .lossType(c.getLossType())
                                .amountPaid(c.getAmountPaid())
                                .outcome(c.getOutcome())
                                .atFault(c.getAtFault())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}