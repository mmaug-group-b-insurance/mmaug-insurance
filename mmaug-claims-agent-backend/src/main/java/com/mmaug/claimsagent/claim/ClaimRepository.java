package com.mmaug.claimsagent.claim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, UUID>, JpaSpecificationExecutor<Claim> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    long countByCreatedByUserId(UUID userId);
}