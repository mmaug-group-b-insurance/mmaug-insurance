package com.mmaug.claimsagent.agent.guardrails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForbiddenClaimsFilterTest {

    private final ForbiddenClaimsFilter filter = new ForbiddenClaimsFilter();

    @Test
    void blocksApproveWording() {
        assertThat(filter.containsForbidden("We approve this claim")).isTrue();
        assertThat(filter.containsForbidden("claim approved")).isTrue();
    }

    @Test
    void blocksRejectWording() {
        assertThat(filter.containsForbidden("We reject the claim")).isTrue();
        assertThat(filter.containsForbidden("claim is rejected")).isTrue();
    }

    @Test
    void blocksLiabilityWording() {
        assertThat(filter.containsForbidden("Driver is liable for the damage")).isTrue();
    }

    @Test
    void blocksPaymentWording() {
        assertThat(filter.containsForbidden("We will pay the claimant")).isTrue();
        assertThat(filter.containsForbidden("authorise payment")).isTrue();
    }

    @Test
    void allowsNeutralWording() {
        assertThat(filter.containsForbidden("The policy was active on the incident date")).isFalse();
        assertThat(filter.containsForbidden("Officer should review the estimate")).isFalse();
    }
}