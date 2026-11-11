package com.mmaug.claimsagent.security;

import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.common.exception.ForbiddenException;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ClaimAccessPolicy {
    private ClaimAccessPolicy() {}

    public static Specification<Claim> visibleTo(CurrentUser user) {
        if (canReadAll(user)) return (root, query, cb) -> cb.conjunction();
        if (!user.hasRole(Role.CLAIMS_OFFICER)) return (root, query, cb) -> cb.disjunction();
        return (root, query, cb) -> cb.equal(
                cb.coalesce(root.<UUID>get("assignedOfficerId"), root.get("createdByUserId")),
                UUID.fromString(user.userId()));
    }

    public static void requireRead(Claim claim, CurrentUser user) {
        if (!canReadAll(user)) requireAssignedOfficer(claim, user);
    }

    public static void requireAssignedOfficer(Claim claim, CurrentUser user) {
        UUID assigned = claim.getAssignedOfficerId();
        // Older officer-created cases have no explicit assignment.
        UUID responsible = assigned != null ? assigned : claim.getCreatedByUserId();
        if (!user.hasRole(Role.CLAIMS_OFFICER) || !UUID.fromString(user.userId()).equals(responsible)) {
            throw new ForbiddenException("This case is not assigned to the signed-in officer");
        }
    }

    private static boolean canReadAll(CurrentUser user) {
        return user.hasRole(Role.ADMIN) || user.hasRole(Role.AUDITOR);
    }
}
