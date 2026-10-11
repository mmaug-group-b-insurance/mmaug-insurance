package com.mmaug.claimsagent.review;

import com.mmaug.claimsagent.agent.summary.ClaimSummary;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "officer_review")
@Getter
@Setter
@NoArgsConstructor
public class OfficerReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id")
    private ClaimSummary summary;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private ReviewOutcome outcome;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "info_requested", columnDefinition = "TEXT")
    private String infoRequested;

    @Column(name = "agreed_with_escalation")
    private Boolean agreedWithEscalation;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;
}