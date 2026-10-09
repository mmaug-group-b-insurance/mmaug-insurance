package com.mmaug.claimsagent.policy;

import com.mmaug.claimsagent.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public Optional<Policy> findByNumber(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber);
    }

    public Policy getByNumber(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new NotFoundException("Policy not found: " + policyNumber));
    }
}