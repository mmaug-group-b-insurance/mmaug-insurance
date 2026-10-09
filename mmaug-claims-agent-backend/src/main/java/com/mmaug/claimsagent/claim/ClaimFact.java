package com.mmaug.claimsagent.claim;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "claim_fact")
@Getter
@Setter
@NoArgsConstructor
public class ClaimFact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "field_name", nullable = false, length = 60)
    private String fieldName;

    @Column(name = "value_text", length = 1000)
    private String valueText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private FactStatus status;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "source_type", length = 15)
    private String sourceType;

    @Column(name = "source_document_id")
    private UUID sourceDocumentId;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "source_snippet", length = 500)
    private String sourceSnippet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alternate_values", columnDefinition = "jsonb")
    private String alternateValues;
}