package com.mmaug.claimsagent.claim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimFactRepository extends JpaRepository<ClaimFact, UUID> {

    List<ClaimFact> findAllByClaimId(UUID claimId);
}