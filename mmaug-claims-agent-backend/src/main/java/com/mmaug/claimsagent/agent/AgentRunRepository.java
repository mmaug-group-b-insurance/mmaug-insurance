package com.mmaug.claimsagent.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    List<AgentRun> findAllByClaimIdOrderByCreatedAtDesc(UUID claimId);
    java.util.Optional<AgentRun> findFirstByClaimIdAndStatusInOrderByCreatedAtDesc(UUID claimId, java.util.Collection<AgentRunStatus> statuses);
    java.util.Optional<AgentRun> findFirstByClaimIdOrderByCreatedAtDesc(UUID claimId);
}