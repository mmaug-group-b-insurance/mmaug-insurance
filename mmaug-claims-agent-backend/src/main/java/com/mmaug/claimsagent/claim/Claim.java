package com.mmaug.claimsagent.claim;

import com.mmaug.claimsagent.common.BaseEntity;
import com.mmaug.claimsagent.common.crypto.AttributeEncryptor;
import com.mmaug.claimsagent.customer.Customer;
import com.mmaug.claimsagent.policy.Policy;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "claim")
@Getter
@Setter
@NoArgsConstructor
public class Claim extends BaseEntity {

    @Column(name = "claim_number", nullable = false, unique = true, length = 20)
    private String claimNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private ClaimStatus status = ClaimStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private ClaimChannel channel = ClaimChannel.WEB_PORTAL;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "assigned_officer_id")
    private UUID assignedOfficerId;

    @Column(name = "policy_number_stated", nullable = false, length = 30)
    private String policyNumberStated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "claimant_name", columnDefinition = "TEXT")
    private String claimantName;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "claimant_phone", columnDefinition = "TEXT")
    private String claimantPhone;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "claimant_email", columnDefinition = "TEXT")
    private String claimantEmail;

    @Column(name = "vehicle_registration", length = 15)
    private String vehicleRegistration;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "incident_time")
    private LocalTime incidentTime;

    @Column(name = "incident_location", length = 300)
    private String incidentLocation;

    @Column(name = "incident_description", columnDefinition = "TEXT")
    private String incidentDescription;

    @Column(name = "third_party_involved")
    private Boolean thirdPartyInvolved;

    @Column(name = "police_report_number", length = 40)
    private String policeReportNumber;

    @Column(name = "injuries_reported")
    private Boolean injuriesReported;

    @Column(name = "estimated_amount", precision = 12, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "currency", length = 3)
    private String currency = "EUR";

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "risk_flag")
    private Boolean riskFlag = false;

    @Column(name = "escalation_reason", length = 500)
    private String escalationReason;

    @Column(name = "latest_run_id")
    private UUID latestRunId;
}