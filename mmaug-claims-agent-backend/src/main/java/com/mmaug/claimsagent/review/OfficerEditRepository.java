package com.mmaug.claimsagent.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfficerEditRepository extends JpaRepository<OfficerEdit, UUID> {

    List<OfficerEdit> findAllByReviewId(UUID reviewId);
}