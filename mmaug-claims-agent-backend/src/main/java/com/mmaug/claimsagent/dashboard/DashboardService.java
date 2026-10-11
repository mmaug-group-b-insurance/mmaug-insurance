package com.mmaug.claimsagent.dashboard;

import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.claim.ClaimStatus;
import com.mmaug.claimsagent.dashboard.dto.DashboardResponse;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.security.Role;
import com.mmaug.claimsagent.validation.ValidationFindingRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ClaimRepository claimRepository;
    private final ValidationFindingRepository findingRepository;

    public DashboardService(ClaimRepository claimRepository,
                            ValidationFindingRepository findingRepository) {
        this.claimRepository = claimRepository;
        this.findingRepository = findingRepository;
    }

    public DashboardResponse build(CurrentUser user) {
        boolean officer = user.hasRole(Role.CLAIMS_OFFICER);
        boolean auditor = user.hasRole(Role.AUDITOR);
        boolean admin = user.hasRole(Role.ADMIN);

        Specification<Claim> scope = (root, query, cb) -> {
            if (officer || auditor || admin) {
                return cb.conjunction();
            }
            return cb.equal(root.get("createdByUserId"), java.util.UUID.fromString(user.userId()));
        };

        List<Claim> all = claimRepository.findAll(scope);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (ClaimStatus status : ClaimStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }
        for (Claim c : all) {
            statusCounts.merge(c.getStatus().name(), 1L, Long::sum);
        }

        long awaitingReview = all.stream()
                .filter(c -> c.getStatus() == ClaimStatus.READY_FOR_REVIEW)
                .count();

        long escalated = all.stream()
                .filter(c -> c.getStatus() == ClaimStatus.ESCALATED)
                .count();

        long riskFlagged = all.stream()
                .filter(c -> Boolean.TRUE.equals(c.getRiskFlag()))
                .count();

        long openFindings = 0;
        for (Claim c : all) {
            openFindings += findingRepository.countByClaimIdAndResolvedFalse(c.getId());
        }

        List<DashboardResponse.ActivityItem> recent = new ArrayList<>();
        all.stream()
                .sorted((a, b) -> compareInstants(b.getUpdatedAt(), a.getUpdatedAt()))
                .limit(10)
                .forEach(c -> recent.add(DashboardResponse.ActivityItem.builder()
                        .claimId(c.getId())
                        .claimNumber(c.getClaimNumber())
                        .event("Claim updated (" + c.getStatus().name() + ")")
                        .occurredAt(c.getUpdatedAt())
                        .build()));

        return DashboardResponse.builder()
                .statusCounts(statusCounts)
                .awaitingReviewCount(awaitingReview)
                .escalatedCount(escalated)
                .riskFlaggedCount(riskFlagged)
                .openFindingsCount(openFindings)
                .recentActivity(recent)
                .build();
    }

    private int compareInstants(Instant a, Instant b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }
}