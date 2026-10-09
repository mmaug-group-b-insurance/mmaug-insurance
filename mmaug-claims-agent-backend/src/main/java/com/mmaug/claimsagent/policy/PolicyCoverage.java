package com.mmaug.claimsagent.policy;

import com.mmaug.claimsagent.common.BaseEntity;
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
@Table(name = "policy_coverage")
@Getter
@Setter
@NoArgsConstructor
public class PolicyCoverage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_type", nullable = false, length = 30)
    private CoverageType coverageType;

    @Column(name = "included", nullable = false)
    private Boolean included = false;

    @Column(name = "limit_amount", precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "excess_amount", precision = 12, scale = 2)
    private BigDecimal excessAmount;

    @Column(name = "exclusions_text", columnDefinition = "TEXT")
    private String exclusionsText;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}