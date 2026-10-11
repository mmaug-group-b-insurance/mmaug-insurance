package com.mmaug.claimsagent.review;

import com.mmaug.claimsagent.agent.summary.ClaimSummary;
import com.mmaug.claimsagent.agent.summary.ClaimSummaryRepository;
import com.mmaug.claimsagent.agent.summary.SummaryStatement;
import com.mmaug.claimsagent.agent.summary.SummaryStatementRepository;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.claim.ClaimStatus;
import com.mmaug.claimsagent.common.exception.BusinessValidationException;
import com.mmaug.claimsagent.common.exception.ForbiddenException;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.review.dto.CreateReviewRequest;
import com.mmaug.claimsagent.review.dto.ReviewResponse;
import com.mmaug.claimsagent.review.dto.StatementEdit;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OfficerEditRepository officerEditRepository;
    private final ClaimRepository claimRepository;
    private final ClaimSummaryRepository summaryRepository;
    private final SummaryStatementRepository statementRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         OfficerEditRepository officerEditRepository,
                         ClaimRepository claimRepository,
                         ClaimSummaryRepository summaryRepository,
                         SummaryStatementRepository statementRepository) {
        this.reviewRepository = reviewRepository;
        this.officerEditRepository = officerEditRepository;
        this.claimRepository = claimRepository;
        this.summaryRepository = summaryRepository;
        this.statementRepository = statementRepository;
    }

    public ReviewResponse create(UUID claimId, CreateReviewRequest request, CurrentUser user) {
        if (!user.hasRole(Role.CLAIMS_OFFICER)) {
            throw new ForbiddenException("Only claims officers may submit reviews");
        }
        if (request.getOutcome() == ReviewOutcome.INFO_REQUESTED
                && (request.getInfoRequested() == null || request.getInfoRequested().isBlank())) {
            throw new BusinessValidationException("infoRequested is required when outcome is INFO_REQUESTED");
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + claimId));

        ClaimSummary summary = summaryRepository.findById(request.getSummaryId())
                .orElseThrow(() -> new NotFoundException("Summary not found"));

        OfficerReview review = new OfficerReview();
        review.setClaim(claim);
        review.setSummary(summary);
        review.setReviewerId(UUID.fromString(user.userId()));
        review.setOutcome(request.getOutcome());
        review.setComment(request.getComment());
        review.setInfoRequested(request.getInfoRequested());
        review.setAgreedWithEscalation(request.getAgreedWithEscalation());
        review.setReviewedAt(Instant.now());
        review = reviewRepository.save(review);

        if (request.getEdits() != null) {
            for (StatementEdit edit : request.getEdits()) {
                SummaryStatement statement = statementRepository.findById(edit.getStatementId())
                        .orElseThrow(() -> new NotFoundException(
                                "Statement not found: " + edit.getStatementId()));
                OfficerEdit oe = new OfficerEdit();
                oe.setReview(review);
                oe.setStatement(statement);
                oe.setOriginalText(statement.getText());
                oe.setEditedText(edit.getEditedText());
                oe.setReason(edit.getReason());
                officerEditRepository.save(oe);

                statement.setText(edit.getEditedText());
                statementRepository.save(statement);
            }
        }

        // Update claim status based on review outcome.
        // No APPROVED or REJECTED state exists.
        switch (request.getOutcome()) {
            case ACKNOWLEDGED -> claim.setStatus(ClaimStatus.READY_FOR_REVIEW);
            case INFO_REQUESTED -> claim.setStatus(ClaimStatus.AWAITING_INFORMATION);
            case ESCALATED_TO_SENIOR -> {
                claim.setStatus(ClaimStatus.ESCALATED);
                claim.setRiskFlag(true);
                claim.setEscalationReason("Escalated to senior by officer");
            }
            case ACCEPTED_FOR_INVESTIGATION -> claim.setStatus(ClaimStatus.UNDER_INVESTIGATION);
            case SUMMARY_REJECTED_REGENERATE -> claim.setStatus(ClaimStatus.READY_FOR_REVIEW);
        }
        claimRepository.save(claim);

        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> list(UUID claimId, CurrentUser user) {
        if (!user.hasRole(Role.CLAIMS_OFFICER)
                && !user.hasRole(Role.AUDITOR)
                && !user.hasRole(Role.ADMIN)) {
            throw new ForbiddenException("Not permitted to view reviews");
        }
        List<ReviewResponse> out = new ArrayList<>();
        for (OfficerReview r : reviewRepository.findAllByClaimIdOrderByReviewedAtDesc(claimId)) {
            out.add(toResponse(r));
        }
        return out;
    }

    private ReviewResponse toResponse(OfficerReview r) {
        List<ReviewResponse.EditItem> edits = new ArrayList<>();
        for (OfficerEdit oe : officerEditRepository.findAllByReviewId(r.getId())) {
            edits.add(ReviewResponse.EditItem.builder()
                    .statementId(oe.getStatement().getId())
                    .originalText(oe.getOriginalText())
                    .editedText(oe.getEditedText())
                    .reason(oe.getReason())
                    .build());
        }

        return ReviewResponse.builder()
                .id(r.getId())
                .claimId(r.getClaim().getId())
                .summaryId(r.getSummary() != null ? r.getSummary().getId() : null)
                .reviewer(r.getReviewerId())
                .outcome(r.getOutcome().name())
                .comment(r.getComment())
                .infoRequested(r.getInfoRequested())
                .agreedWithEscalation(r.getAgreedWithEscalation())
                .edits(edits)
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}