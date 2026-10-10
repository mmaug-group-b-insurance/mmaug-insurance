package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.common.BaseEntity;
import com.mmaug.claimsagent.document.ClaimDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "extracted_field")
@Getter
@Setter
@NoArgsConstructor
public class ExtractedField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_id", nullable = false)
    private ExtractionResult extraction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private ClaimDocument document;

    @Column(name = "field_name", nullable = false, length = 60)
    private String fieldName;

    @Column(name = "field_value", length = 1000)
    private String fieldValue;

    @Column(name = "normalised_value", length = 1000)
    private String normalisedValue;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "page_number")
    private Integer pageNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bounding_box", columnDefinition = "jsonb")
    private String boundingBox;

    @Column(name = "snippet", length = 500)
    private String snippet;
}