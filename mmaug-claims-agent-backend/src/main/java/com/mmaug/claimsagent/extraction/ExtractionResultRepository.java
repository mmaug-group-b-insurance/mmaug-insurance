package com.mmaug.claimsagent.extraction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionResultRepository extends JpaRepository<ExtractionResult, UUID> {

    Optional<ExtractionResult> findFirstByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}