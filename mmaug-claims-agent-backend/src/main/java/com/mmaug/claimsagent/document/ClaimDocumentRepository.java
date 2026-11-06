package com.mmaug.claimsagent.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, UUID> {
    java.util.Optional<ClaimDocument> findByBlobContainerAndBlobNameAndDeletedAtIsNull(String container, String blobName);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from ClaimDocument d where d.id = :id")
    java.util.Optional<ClaimDocument> findForExtraction(@org.springframework.data.repository.query.Param("id") UUID id);


    List<ClaimDocument> findAllByClaimIdAndDeletedAtIsNull(UUID claimId);

    long countByClaimIdAndDeletedAtIsNull(UUID claimId);

    boolean existsByClaimIdAndSha256AndDeletedAtIsNull(UUID claimId, String sha256);
}
