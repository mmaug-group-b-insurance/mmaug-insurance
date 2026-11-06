package com.mmaug.claimsagent.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Review outcomes request human follow-up; they never adjudicate a claim.
 */
class ReviewOutcomeBoundaryTest {

    @Test
    void reviewOutcomesContainOnlyPermittedPreparationActions() {
        assertThat(ReviewOutcome.values()).extracting(Enum::name)
                .containsExactlyInAnyOrder(
                        "ACKNOWLEDGED", "INFO_REQUESTED", "ESCALATED_TO_SENIOR",
                        "ACCEPTED_FOR_INVESTIGATION", "SUMMARY_REJECTED_REGENERATE");
    }

    @Test
    void rejectingASummaryRequestsRegenerationWithoutRejectingTheClaim() {
        assertThat(ReviewOutcome.valueOf("SUMMARY_REJECTED_REGENERATE"))
                .isEqualTo(ReviewOutcome.SUMMARY_REJECTED_REGENERATE);
        assertThat(ReviewOutcome.values()).extracting(Enum::name)
                .doesNotContain("APPROVED", "REJECTED", "PAYMENT_AUTHORISED", "POLICY_MODIFIED");
    }
}
