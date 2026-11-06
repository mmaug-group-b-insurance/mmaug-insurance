package com.mmaug.claimsagent.tools;

import com.mmaug.claimsagent.agent.tools.dto.ToolEnvelope;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.policy.CoverageType;
import com.mmaug.claimsagent.policy.HistoricalClaim;
import com.mmaug.claimsagent.policy.Policy;
import com.mmaug.claimsagent.policy.PolicyCoverage;
import com.mmaug.claimsagent.policy.PolicyCoverageRepository;
import com.mmaug.claimsagent.policy.PolicyDriver;
import com.mmaug.claimsagent.policy.PolicyService;
import com.mmaug.claimsagent.policy.Vehicle;
import com.mmaug.claimsagent.policy.VehicleRepository;
import com.mmaug.claimsagent.policy.HistoricalClaimRepository;
import com.mmaug.claimsagent.tools.dto.CoverageView;
import com.mmaug.claimsagent.tools.dto.HistoryView;
import com.mmaug.claimsagent.tools.dto.PolicyView;
import com.mmaug.claimsagent.tools.dto.RulesView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Internal Tools", description = "Internal read-only tool API used by the agent (SERVICE_AGENT only)")
@RestController
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@RequestMapping("/internal/tools/v1")
public class InternalToolsController {

    private final com.mmaug.claimsagent.agent.tools.BusinessRulesTool businessRules;
    private final PolicyService policyService;
    private final VehicleRepository vehicleRepository;
    private final PolicyCoverageRepository coverageRepository;
    private final HistoricalClaimRepository historyRepository;

    public InternalToolsController(PolicyService policyService,
                                   VehicleRepository vehicleRepository,
                                   PolicyCoverageRepository coverageRepository,
                                   HistoricalClaimRepository historyRepository, com.mmaug.claimsagent.agent.tools.BusinessRulesTool businessRules) {
        this.businessRules = businessRules;
        this.policyService = policyService;
        this.vehicleRepository = vehicleRepository;
        this.coverageRepository = coverageRepository;
        this.historyRepository = historyRepository;
    }

    @Operation(summary = "Get policy")
    @GetMapping("/policies/{policyNumber}")
    public ResponseEntity<ToolEnvelope<PolicyView>> getPolicy(@PathVariable String policyNumber) {
        Policy policy = policyService.findByNumber(policyNumber)
                .orElseThrow(() -> new NotFoundException("Policy not found"));

        List<Vehicle> vehicles = vehicleRepository.findAllByPolicyId(policy.getId());

        PolicyView view = PolicyView.builder()
                .policyNumber(policy.getPolicyNumber())
                .status(policy.getStatus().name())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .productCode(policy.getProductCode())
                .standardExcess(policy.getStandardExcess())
                .paymentStatus(policy.getPaymentStatus())
                .policyholder(PolicyView.PolicyholderView.builder()
                        .customerNumber(policy.getCustomer() != null ? policy.getCustomer().getCustomerNumber() : null)
                        .name(resolveCustomerName(policy))
                        .build())
                .vehicles(vehicles.stream()
                        .map(v -> PolicyView.VehicleView.builder()
                                .registration(v.getRegistrationNumber())
                                .make(v.getMake())
                                .model(v.getModel())
                                .year(v.getManufactureYear())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(ToolEnvelope.ok(view, "tool:get_policy", "v1"));
    }

    @Operation(summary = "Get coverage")
    @GetMapping("/policies/{policyNumber}/coverage")
    public ResponseEntity<ToolEnvelope<CoverageView>> getCoverage(
            @PathVariable String policyNumber,
            @RequestParam(required = false) CoverageType coverageType) {

        Policy policy = policyService.findByNumber(policyNumber)
                .orElseThrow(() -> new NotFoundException("Policy not found"));

        List<PolicyCoverage> coverages = coverageRepository.findAllByPolicyId(policy.getId());
        if (coverageType != null) {
            coverages = coverages.stream()
                    .filter(c -> c.getCoverageType() == coverageType)
                    .collect(Collectors.toList());
        }

        CoverageView view = CoverageView.builder()
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

        return ResponseEntity.ok(ToolEnvelope.ok(view, "tool:get_coverage", "v1"));
    }

    @Operation(summary = "Get claim history")
    @GetMapping("/policies/{policyNumber}/claim-history")
    public ResponseEntity<ToolEnvelope<HistoryView>> getClaimHistory(
            @PathVariable String policyNumber,
            @RequestParam(defaultValue = "5") int yearsBack) {

        if (yearsBack < 1) yearsBack = 1;
        if (yearsBack > 10) yearsBack = 10;

        Policy policy = policyService.findByNumber(policyNumber)
                .orElseThrow(() -> new NotFoundException("Policy not found"));

        LocalDate cutoff = LocalDate.now().minusYears(yearsBack);
        List<HistoricalClaim> claims = historyRepository.findAllByPolicyIdAndLossDateAfter(policy.getId(), cutoff);

        BigDecimal totalPaid = claims.stream()
                .map(HistoricalClaim::getAmountPaid)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        HistoryView view = HistoryView.builder()
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

        return ResponseEntity.ok(ToolEnvelope.ok(view, "tool:get_claim_history", "v1"));
    }

    @Operation(summary = "Get business rules")
    @GetMapping("/business-rules")
    public ResponseEntity<ToolEnvelope<RulesView>> getBusinessRules(@RequestParam(required = false) String productCode) {
        RulesView view = businessRules.load(productCode);

        return ResponseEntity.ok(ToolEnvelope.ok(view, "tool:get_business_rules", "v1"));
    }

    private String resolveCustomerName(Policy policy) {
        if (policy.getCustomer() == null) return null;
        var c = policy.getCustomer();
        if (c.getCompanyName() != null && !c.getCompanyName().isBlank()) {
            return c.getCompanyName();
        }
        return ((c.getFirstName() != null ? c.getFirstName() : "") + " "
                + (c.getLastName() != null ? c.getLastName() : "")).trim();
    }
}
