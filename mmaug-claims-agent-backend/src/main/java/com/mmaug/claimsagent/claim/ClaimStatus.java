package com.mmaug.claimsagent.claim;

/**
 * Claim status.
 * NOTE: No APPROVED, REJECTED or PAID state exists anywhere in the model.
 * Officers close or escalate; the final decision is made outside this system.
 */
public enum ClaimStatus {
    DRAFT,
    SUBMITTED,
    PROCESSING,
    AWAITING_INFORMATION,
    READY_FOR_REVIEW,
    ESCALATED,
    UNDER_INVESTIGATION,
    CLOSED_BY_OFFICER
}