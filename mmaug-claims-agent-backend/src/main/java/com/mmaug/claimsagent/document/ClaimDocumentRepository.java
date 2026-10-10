package com.mmaug.claimsagent.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, UUID> {

    List<ClaimDocument> findAllByClaimIdAndDeletedAtIsNull(UUID claimId);

    long countByClaimIdAndDeletedAtIsNull(UUID claimId);
}