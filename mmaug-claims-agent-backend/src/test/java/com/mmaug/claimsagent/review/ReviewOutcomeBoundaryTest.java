package com.mmaug.claimsagent.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Responsible AI boundary in code: no APPROVED or REJECTED outcome exists.
 */
class ReviewOutcomeBoundaryTest {

    @Test
    void reviewOutcomeHasNoApproveOrRejectValue() {
        for (ReviewOutcome outcome : ReviewOutcome.values()) {
            assertThat(outcome.name()).doesNotContain("APPROVE");
            assertThat(outcome.name()).doesNotContain("REJECT");
            assertThat(outcome.name()).doesNotContain("PAY");
        }
    }
}