package com.mmaug.claimsagent.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricalClaimRepository extends JpaRepository<HistoricalClaim, UUID> {

    List<HistoricalClaim> findAllByPolicyIdAndLossDateAfter(UUID policyId, LocalDate after);
}