package com.mmaug.claimsagent.claim;

import com.mmaug.claimsagent.claim.dto.ClaimListItem;
import com.mmaug.claimsagent.claim.dto.ClaimResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClaimMapper {

    public ClaimResponse toResponse(Claim claim) {
        if (claim == null) return null;
        return ClaimResponse.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber())
                .status(claim.getStatus())
                .channel(claim.getChannel())
                .policyNumberStated(claim.getPolicyNumberStated())
                .policyId(claim.getPolicy() != null ? claim.getPolicy().getId() : null)
                .claimantName(claim.getClaimantName())
                .claimantPhone(claim.getClaimantPhone())
                .claimantEmail(claim.getClaimantEmail())
                .vehicleRegistration(claim.getVehicleRegistration())
                .incidentDate(claim.getIncidentDate())
                .incidentTime(claim.getIncidentTime())
                .incidentLocation(claim.getIncidentLocation())
                .incidentDescription(claim.getIncidentDescription())
                .thirdPartyInvolved(claim.getThirdPartyInvolved())
                .policeReportNumber(claim.getPoliceReportNumber())
                .injuriesReported(claim.getInjuriesReported())
                .estimatedAmount(claim.getEstimatedAmount())
                .currency(claim.getCurrency())
                .riskFlag(claim.getRiskFlag())
                .escalationReason(claim.getEscalationReason())
                .createdAt(claim.getCreatedAt())
                .submittedAt(claim.getSubmittedAt())
                .updatedAt(claim.getUpdatedAt())
                .version(claim.getVersion())
                .build();
    }

    public List<ClaimResponse> toResponseList(List<Claim> claims) {
        if (claims == null) return List.of();
        List<ClaimResponse> out = new ArrayList<>(claims.size());
        for (Claim claim : claims) {
            out.add(toResponse(claim));
        }
        return out;
    }

    public ClaimListItem toListItem(Claim claim) {
        if (claim == null) return null;
        return ClaimListItem.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber())
                .status(claim.getStatus())
                .claimantName(claim.getClaimantName())
                .policyNumberStated(claim.getPolicyNumberStated())
                .incidentDate(claim.getIncidentDate())
                .riskFlag(claim.getRiskFlag())
                .documentCount(0)
                .openFindingCount(0)
                .createdAt(claim.getCreatedAt())
                .build();
    }
}