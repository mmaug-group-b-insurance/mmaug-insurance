package com.mmaug.claimsagent.review;

/**
 * Review outcomes available to claims officers.
 * NOTE: There is deliberately no APPROVED or REJECTED value.
 * The AI never approves or rejects a claim. Officers record their
 * acknowledgement, escalation, or request for information. The final
 * claim decision is made outside this system.
 */
public enum ReviewOutcome {
    ACKNOWLEDGED,
    INFO_REQUESTED,
    ESCALATED_TO_SENIOR,
    ACCEPTED_FOR_INVESTIGATION,
    SUMMARY_REJECTED_REGENERATE
}