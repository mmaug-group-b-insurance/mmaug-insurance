package com.mmaug.claimsagent.validation;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.AgentRunRepository;
import com.mmaug.claimsagent.claim.Claim;
import com.mmaug.claimsagent.claim.ClaimFact;
import com.mmaug.claimsagent.claim.ClaimFactRepository;
import com.mmaug.claimsagent.claim.ClaimRepository;
import com.mmaug.claimsagent.common.exception.ForbiddenException;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.security.Role;
import com.mmaug.claimsagent.validation.dto.FindingResponse;
import com.mmaug.claimsagent.validation.dto.ResolveFindingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FindingService {

    private final ValidationFindingRepository findingRepository;
    private final ClaimRepository claimRepository;
    private final ClaimFactRepository claimFactRepository;
    private final AgentRunRepository agentRunRepository;
    private final CompletenessValidator completenessValidator;
    private final ConfidenceChecker confidenceChecker;
    private final ContradictionDetector contradictionDetector;

    public FindingService(ValidationFindingRepository findingRepository,
                          ClaimRepository claimRepository,
                          ClaimFactRepository claimFactRepository,
                          AgentRunRepository agentRunRepository,
                          CompletenessValidator completenessValidator,
                          ConfidenceChecker confidenceChecker,
                          ContradictionDetector contradictionDetector) {
        this.findingRepository = findingRepository;
        this.claimRepository = claimRepository;
        this.claimFactRepository = claimFactRepository;
        this.agentRunRepository = agentRunRepository;
        this.completenessValidator = completenessValidator;
        this.confidenceChecker = confidenceChecker;
        this.contradictionDetector = contradictionDetector;
    }

    /** Called by the orchestrator after validation. */
    public List<ValidationFinding> runValidation(UUID claimId, UUID agentRunId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + claimId));

        AgentRun run = agentRunId != null
                ? agentRunRepository.findById(agentRunId).orElse(null)
                : null;

        List<ClaimFact> facts = claimFactRepository.findAllByClaimId(claimId);

        List<ValidationFinding> findings = new ArrayList<>();
        findings.addAll(completenessValidator.validate(claim));
        findings.addAll(confidenceChecker.validate(claim, facts));
        findings.addAll(contradictionDetector.validate(claim, facts));

        for (ValidationFinding f : findings) {
            f.setAgentRun(run);
        }
        return findingRepository.saveAll(findings);
    }

    @Transactional(readOnly = true)
    public List<FindingResponse> list(UUID claimId, String type, String severity, Boolean resolved, CurrentUser user) {
        if (!user.hasRole(Role.CLAIMS_OFFICER) && !user.hasRole(Role.AUDITOR) && !user.hasRole(Role.ADMIN)) {
            throw new ForbiddenException("Only claims officers may view findings");
        }
        List<ValidationFinding> findings = (resolved != null)
                ? findingRepository.findAllByClaimIdAndResolvedOrderByCreatedAtDesc(claimId, resolved)
                : findingRepository.findAllByClaimIdOrderByCreatedAtDesc(claimId);

        List<FindingResponse> out = new ArrayList<>();
        for (ValidationFinding f : findings) {
            if (type != null && !type.equalsIgnoreCase(f.getFindingType().name())) continue;
            if (severity != null && !severity.equalsIgnoreCase(f.getSeverity().name())) continue;
            out.add(toResponse(f));
        }
        return out;
    }

    public FindingResponse resolve(UUID findingId, ResolveFindingRequest request, CurrentUser user) {
        if (!user.hasRole(Role.CLAIMS_OFFICER)) {
            throw new ForbiddenException("Only claims officers may resolve findings");
        }
        ValidationFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new NotFoundException("Finding not found: " + findingId));

        finding.setResolved(true);
        finding.setResolvedByUserId(UUID.fromString(user.userId()));
        finding.setResolvedAt(Instant.now());
        finding.setResolutionNote(request.getResolutionNote());

        return toResponse(findingRepository.save(finding));
    }

    private FindingResponse toResponse(ValidationFinding f) {
        return FindingResponse.builder()
                .id(f.getId())
                .claimId(f.getClaim().getId())
                .type(f.getFindingType().name())
                .severity(f.getSeverity().name())
                .origin(f.getOrigin().name())
                .fieldName(f.getFieldName())
                .message(f.getMessage())
                .evidence(List.of())
                .resolved(f.getResolved())
                .resolvedBy(f.getResolvedByUserId())
                .resolvedAt(f.getResolvedAt())
                .resolutionNote(f.getResolutionNote())
                .createdAt(f.getCreatedAt())
                .build();
    }
}