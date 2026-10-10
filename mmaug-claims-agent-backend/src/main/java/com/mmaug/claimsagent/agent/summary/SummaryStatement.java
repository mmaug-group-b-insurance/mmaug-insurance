package com.mmaug.claimsagent.agent.summary;

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

@Entity
@Table(name = "summary_statement")
@Getter
@Setter
@NoArgsConstructor
public class SummaryStatement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "summary_id", nullable = false)
    private ClaimSummary summary;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(name = "section", nullable = false, length = 30)
    private String section;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 20)
    private StatementType statementType;

    @Column(name = "text", length = 1500)
    private String text;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;
}