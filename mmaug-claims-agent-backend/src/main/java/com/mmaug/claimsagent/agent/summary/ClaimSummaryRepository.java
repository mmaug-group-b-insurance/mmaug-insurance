package com.mmaug.claimsagent.agent.summary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClaimSummaryRepository extends JpaRepository<ClaimSummary, UUID> {

    Optional<ClaimSummary> findFirstByClaimIdOrderByVersionNoDesc(UUID claimId);

    List<ClaimSummary> findAllByClaimIdOrderByVersionNoDesc(UUID claimId);
}