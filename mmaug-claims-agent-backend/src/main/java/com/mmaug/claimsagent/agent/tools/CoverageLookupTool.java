package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.policy.Policy;
import com.mmaug.claimsagent.policy.PolicyCoverage;
import com.mmaug.claimsagent.policy.PolicyCoverageRepository;
import com.mmaug.claimsagent.policy.PolicyService;
import com.mmaug.claimsagent.tools.dto.CoverageView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CoverageLookupTool {

    private final PolicyService policyService;
    private final PolicyCoverageRepository coverageRepository;

    public CoverageLookupTool(PolicyService policyService, PolicyCoverageRepository coverageRepository) {
        this.policyService = policyService;
        this.coverageRepository = coverageRepository;
    }

    public CoverageView lookup(String policyNumber) {
        Policy policy = policyService.findByNumber(policyNumber)
                .orElseThrow(() -> new NotFoundException("Policy not found: " + policyNumber));

        List<PolicyCoverage> coverages = coverageRepository.findAllByPolicyId(policy.getId());

        return CoverageView.builder()
                .policyNumber(policy.getPolicyNumber())
                .coverages(coverages.stream()
                        .map(c -> CoverageView.CoverageItem.builder()
                                .coverageType(c.getCoverageType().name())
                                .included(c.getIncluded())
                                .limitAmount(c.getLimitAmount())
                                .excessAmount(c.getExcessAmount())
                                .exclusionsText(c.getExclusionsText())
                                .effectiveFrom(c.getEffectiveFrom())
                                .effectiveTo(c.getEffectiveTo())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}