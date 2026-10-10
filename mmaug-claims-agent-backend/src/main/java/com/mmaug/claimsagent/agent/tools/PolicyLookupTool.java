package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.policy.Policy;
import com.mmaug.claimsagent.policy.PolicyService;
import com.mmaug.claimsagent.policy.Vehicle;
import com.mmaug.claimsagent.policy.VehicleRepository;
import com.mmaug.claimsagent.tools.dto.PolicyView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PolicyLookupTool {

    private final PolicyService policyService;
    private final VehicleRepository vehicleRepository;

    public PolicyLookupTool(PolicyService policyService, VehicleRepository vehicleRepository) {
        this.policyService = policyService;
        this.vehicleRepository = vehicleRepository;
    }

    public PolicyView lookup(String policyNumber) {
        Policy policy = policyService.findByNumber(policyNumber)
                .orElseThrow(() -> new NotFoundException("Policy not found: " + policyNumber));

        List<Vehicle> vehicles = vehicleRepository.findAllByPolicyId(policy.getId());

        return PolicyView.builder()
                .policyNumber(policy.getPolicyNumber())
                .status(policy.getStatus().name())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .productCode(policy.getProductCode())
                .standardExcess(policy.getStandardExcess())
                .paymentStatus(policy.getPaymentStatus())
                .vehicles(vehicles.stream()
                        .map(v -> PolicyView.VehicleView.builder()
                                .registration(v.getRegistrationNumber())
                                .make(v.getMake())
                                .model(v.getModel())
                                .year(v.getManufactureYear())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}