package com.mmaug.claimsagent.validation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ValidationFindingRepository extends JpaRepository<ValidationFinding, UUID> {

    List<ValidationFinding> findAllByClaimIdOrderByCreatedAtDesc(UUID claimId);

    List<ValidationFinding> findAllByClaimIdAndResolvedOrderByCreatedAtDesc(UUID claimId, Boolean resolved);

    long countByClaimIdAndResolvedFalse(UUID claimId);
}