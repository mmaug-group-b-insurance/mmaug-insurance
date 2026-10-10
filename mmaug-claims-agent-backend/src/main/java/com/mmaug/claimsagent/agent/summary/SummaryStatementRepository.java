package com.mmaug.claimsagent.agent.summary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SummaryStatementRepository extends JpaRepository<SummaryStatement, UUID> {

    List<SummaryStatement> findAllBySummaryIdOrderByOrderNoAsc(UUID summaryId);
}