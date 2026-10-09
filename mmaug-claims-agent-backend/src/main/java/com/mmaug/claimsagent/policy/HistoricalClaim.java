package com.mmaug.claimsagent.policy;

import com.mmaug.claimsagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "historical_claim")
@Getter
@Setter
@NoArgsConstructor
public class HistoricalClaim extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "claim_reference", nullable = false, length = 25)
    private String claimReference;

    @Column(name = "loss_date", nullable = false)
    private LocalDate lossDate;

    @Column(name = "loss_type", nullable = false, length = 30)
    private String lossType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "currency", length = 3)
    private String currency = "EUR";

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "at_fault")
    private Boolean atFault;
}