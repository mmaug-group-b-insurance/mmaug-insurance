package com.mmaug.claimsagent.security;

import org.springframework.stereotype.Component;

@Component
public class ClaimAccessPolicy {

    public boolean canReadClaim(CurrentUser user, String ownerUserId, String assignedOfficerId) {
        if (user == null) return false;
        if (user.hasRole(Role.CLAIMS_OFFICER) || user.hasRole(Role.AUDITOR) || user.hasRole(Role.ADMIN)) {
            return true;
        }
        if (user.hasRole(Role.CLAIMANT)) {
            return user.userId().equals(ownerUserId);
        }
        return false;
    }

    public boolean canEditClaim(CurrentUser user, String ownerUserId, String claimStatus) {
        if (user == null) return false;
        if (user.hasRole(Role.CLAIMS_OFFICER)) return true;
        if (user.hasRole(Role.CLAIMANT)) {
            return user.userId().equals(ownerUserId) && "DRAFT".equals(claimStatus);
        }
        return false;
    }

    public boolean canTriggerAnalyse(CurrentUser user) {
        return user != null && user.hasRole(Role.CLAIMS_OFFICER);
    }

    public boolean canSubmitReview(CurrentUser user) {
        return user != null && user.hasRole(Role.CLAIMS_OFFICER);
    }
}