package com.mmaug.claimsagent.policy;

import com.mmaug.claimsagent.common.BaseEntity;
import com.mmaug.claimsagent.customer.Customer;
import jakarta.persistence.Column;
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
import java.time.LocalDate;

@Entity
@Table(name = "policy")
@Getter
@Setter
@NoArgsConstructor
public class Policy extends BaseEntity {

    @Column(name = "policy_number", nullable = false, unique = true, length = 20)
    private String policyNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private PolicyStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "annual_premium", precision = 12, scale = 2)
    private BigDecimal annualPremium;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "standard_excess", precision = 12, scale = 2)
    private BigDecimal standardExcess;

    @Column(name = "no_claims_bonus_years")
    private Integer noClaimsBonusYears;

    @Column(name = "payment_status", length = 15)
    private String paymentStatus;
}