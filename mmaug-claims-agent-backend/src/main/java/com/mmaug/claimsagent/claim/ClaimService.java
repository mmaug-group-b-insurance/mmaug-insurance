package com.mmaug.claimsagent.claim;

import com.mmaug.claimsagent.claim.dto.ClaimDetailResponse;
import com.mmaug.claimsagent.claim.dto.ClaimListItem;
import com.mmaug.claimsagent.claim.dto.ClaimResponse;
import com.mmaug.claimsagent.claim.dto.ClaimSearchCriteria;
import com.mmaug.claimsagent.claim.dto.CreateClaimRequest;
import com.mmaug.claimsagent.claim.dto.UpdateClaimRequest;
import com.mmaug.claimsagent.common.exception.BusinessValidationException;
import com.mmaug.claimsagent.common.exception.ForbiddenException;
import com.mmaug.claimsagent.common.exception.NotFoundException;
import com.mmaug.claimsagent.customer.Customer;
import com.mmaug.claimsagent.customer.CustomerService;
import com.mmaug.claimsagent.policy.Policy;
import com.mmaug.claimsagent.policy.PolicyService;
import com.mmaug.claimsagent.security.CurrentUser;
import com.mmaug.claimsagent.security.Role;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimFactRepository claimFactRepository;
    private final ClaimNumberGenerator numberGenerator;
    private final ClaimMapper claimMapper;
    private final PolicyService policyService;
    private final CustomerService customerService;

    public ClaimService(ClaimRepository claimRepository,
                        ClaimFactRepository claimFactRepository,
                        ClaimNumberGenerator numberGenerator,
                        ClaimMapper claimMapper,
                        PolicyService policyService,
                        CustomerService customerService) {
        this.claimRepository = claimRepository;
        this.claimFactRepository = claimFactRepository;
        this.numberGenerator = numberGenerator;
        this.claimMapper = claimMapper;
        this.policyService = policyService;
        this.customerService = customerService;
    }

    public ClaimResponse create(CreateClaimRequest request, CurrentUser user) {
        if (user == null) throw new ForbiddenException("Authentication required");
        if (!user.hasRole(Role.CLAIMANT) && !user.hasRole(Role.CLAIMS_OFFICER)) {
            throw new ForbiddenException("Only claimants or officers may create claims");
        }

        Claim claim = new Claim();
        claim.setClaimNumber(numberGenerator.next());
        claim.setStatus(ClaimStatus.DRAFT);
        claim.setChannel(request.getChannel() != null ? request.getChannel() : ClaimChannel.WEB_PORTAL);
        claim.setCreatedByUserId(UUID.fromString(user.userId()));
        claim.setPolicyNumberStated(request.getPolicyNumber());
        claim.setClaimantName(request.getClaimantName());
        claim.setClaimantPhone(request.getClaimantPhone());
        claim.setClaimantEmail(request.getClaimantEmail());
        claim.setVehicleRegistration(request.getVehicleRegistration());
        claim.setIncidentDate(request.getIncidentDate());
        claim.setIncidentTime(request.getIncidentTime());
        claim.setIncidentLocation(request.getIncidentLocation());
        claim.setIncidentDescription(request.getIncidentDescription());
        claim.setThirdPartyInvolved(request.getThirdPartyInvolved());
        claim.setPoliceReportNumber(request.getPoliceReportNumber());
        claim.setInjuriesReported(request.getInjuriesReported());
        claim.setEstimatedAmount(request.getEstimatedAmount());
        claim.setReportedAt(Instant.now());

        // Attempt to link to a policy — do not fail if not found; validation happens later.
        policyService.findByNumber(request.getPolicyNumber()).ifPresent(policy -> {
            claim.setPolicy(policy);
            Customer customer = policy.getCustomer();
            if (customer != null) {
                claim.setCustomer(customer);
            }
        });

        Claim saved = claimRepository.save(claim);
        return claimMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClaimResponse get(UUID id, CurrentUser user) {
        Claim claim = loadAndCheckRead(id, user);
        return claimMapper.toResponse(claim);
    }

    @Transactional(readOnly = true)
    public ClaimDetailResponse getDetail(UUID id, CurrentUser user) {
        Claim claim = loadAndCheckRead(id, user);
        ClaimResponse response = claimMapper.toResponse(claim);
        return ClaimDetailResponse.builder()
                .claim(response)
                .latestRunId(claim.getLatestRunId())
                .build();
    }

    public ClaimResponse update(UUID id, UpdateClaimRequest request, CurrentUser user) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + id));

        boolean isOfficer = user.hasRole(Role.CLAIMS_OFFICER);
        boolean isOwner = user.hasRole(Role.CLAIMANT) && user.userId().equals(claim.getCreatedByUserId().toString());

        if (!isOfficer && !isOwner) {
            throw new ForbiddenException("Not permitted to update this claim");
        }
        if (isOwner && !isOfficer && claim.getStatus() != ClaimStatus.DRAFT) {
            throw new BusinessValidationException("Only draft claims can be updated by the owner");
        }
        if (request.getVersion() != null && !request.getVersion().equals(claim.getVersion())) {
            throw new BusinessValidationException("Claim has been modified by another user");
        }

        if (request.getPolicyNumber() != null) claim.setPolicyNumberStated(request.getPolicyNumber());
        if (request.getClaimantName() != null) claim.setClaimantName(request.getClaimantName());
        if (request.getClaimantPhone() != null) claim.setClaimantPhone(request.getClaimantPhone());
        if (request.getClaimantEmail() != null) claim.setClaimantEmail(request.getClaimantEmail());
        if (request.getVehicleRegistration() != null) claim.setVehicleRegistration(request.getVehicleRegistration());
        if (request.getIncidentDate() != null) claim.setIncidentDate(request.getIncidentDate());
        if (request.getIncidentTime() != null) claim.setIncidentTime(request.getIncidentTime());
        if (request.getIncidentLocation() != null) claim.setIncidentLocation(request.getIncidentLocation());
        if (request.getIncidentDescription() != null) claim.setIncidentDescription(request.getIncidentDescription());
        if (request.getThirdPartyInvolved() != null) claim.setThirdPartyInvolved(request.getThirdPartyInvolved());
        if (request.getPoliceReportNumber() != null) claim.setPoliceReportNumber(request.getPoliceReportNumber());
        if (request.getInjuriesReported() != null) claim.setInjuriesReported(request.getInjuriesReported());
        if (request.getEstimatedAmount() != null) claim.setEstimatedAmount(request.getEstimatedAmount());

        Claim saved = claimRepository.save(claim);
        return claimMapper.toResponse(saved);
    }

    public ClaimResponse submit(UUID id, CurrentUser user) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + id));

        boolean isOwner = user.userId().equals(claim.getCreatedByUserId().toString());
        if (!isOwner && !user.hasRole(Role.CLAIMS_OFFICER)) {
            throw new ForbiddenException("Not permitted to submit this claim");
        }
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new BusinessValidationException("Only draft claims can be submitted");
        }

        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setSubmittedAt(Instant.now());
        Claim saved = claimRepository.save(claim);
        return claimMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ClaimListItem> search(ClaimSearchCriteria criteria, CurrentUser user) {
        Pageable pageable = toPageable(criteria);
        Specification<Claim> spec = buildSpec(criteria, user);
        Page<Claim> page = claimRepository.findAll(spec, pageable);
        return page.map(claimMapper::toListItem);
    }

    private Pageable toPageable(ClaimSearchCriteria criteria) {
        int page = criteria.getPage() != null ? Math.max(0, criteria.getPage()) : 0;
        int size = criteria.getSize() != null ? Math.min(50, Math.max(1, criteria.getSize())) : 20;
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (criteria.getSort() != null && criteria.getSort().contains(",")) {
            String[] parts = criteria.getSort().split(",", 2);
            Sort.Direction dir = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, parts[0]);
        }
        return PageRequest.of(page, size, sort);
    }

    private Specification<Claim> buildSpec(ClaimSearchCriteria criteria, CurrentUser user) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!user.hasRole(Role.CLAIMS_OFFICER)
                    && !user.hasRole(Role.AUDITOR)
                    && !user.hasRole(Role.ADMIN)) {
                predicates.add(cb.equal(root.get("createdByUserId"), UUID.fromString(user.userId())));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getClaimNumber() != null && !criteria.getClaimNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("claimNumber")),
                        "%" + criteria.getClaimNumber().toLowerCase() + "%"));
            }
            if (criteria.getPolicyNumber() != null && !criteria.getPolicyNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("policyNumberStated")),
                        "%" + criteria.getPolicyNumber().toLowerCase() + "%"));
            }
            if (criteria.getRiskFlag() != null) {
                predicates.add(cb.equal(root.get("riskFlag"), criteria.getRiskFlag()));
            }
            if (criteria.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("incidentDate"), criteria.getDateFrom()));
            }
            if (criteria.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("incidentDate"), criteria.getDateTo()));
            }
            if (Boolean.TRUE.equals(criteria.getAssignedToMe())) {
                predicates.add(cb.equal(root.get("assignedOfficerId"), UUID.fromString(user.userId())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Claim loadAndCheckRead(UUID id, CurrentUser user) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Claim not found: " + id));
        boolean isOfficer = user.hasRole(Role.CLAIMS_OFFICER);
        boolean isAuditor = user.hasRole(Role.AUDITOR);
        boolean isAdmin = user.hasRole(Role.ADMIN);
        boolean isOwner = user.userId().equals(claim.getCreatedByUserId().toString());
        if (!isOfficer && !isAuditor && !isAdmin && !isOwner) {
            throw new ForbiddenException("Not permitted to access this claim");
        }
        return claim;
    }
}