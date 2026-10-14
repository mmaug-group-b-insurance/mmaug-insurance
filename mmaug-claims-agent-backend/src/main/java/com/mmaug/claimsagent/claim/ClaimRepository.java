package com.mmaug.claimsagent.claim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, UUID>, JpaSpecificationExecutor<Claim> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    long countByCreatedByUserId(UUID userId);

    /**
     * Returns the highest claim_number for the given year, or null if none exist yet.
     * Claim numbers have the form CLM-YYYY-NNNNNN, so a descending lexical sort
     * returns the highest sequence first.
     */
    @Query(value = """
            select c.claim_number
            from claim c
            where c.claim_number like concat('CLM-', :year, '-%')
            order by c.claim_number desc
            limit 1
            """, nativeQuery = true)
    Optional<String> findMaxClaimNumberForYear(@Param("year") int year);
}