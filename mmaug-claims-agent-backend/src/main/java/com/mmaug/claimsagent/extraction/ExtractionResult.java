package com.mmaug.claimsagent.extraction;

import com.mmaug.claimsagent.common.BaseEntity;
import com.mmaug.claimsagent.document.ClaimDocument;
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

@Entity
@Table(name = "extraction_result")
@Getter
@Setter
@NoArgsConstructor
public class ExtractionResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private ClaimDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ExtractionStatus status = ExtractionStatus.PENDING;

    @Column(name = "extractor", length = 30)
    private String extractor;

    @Column(name = "extractor_model", length = 60)
    private String extractorModel;

    @Column(name = "llm_model", length = 60)
    private String llmModel;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "schema_version", length = 20)
    private String schemaVersion;

    @Column(name = "raw_text_blob_name", length = 200)
    private String rawTextBlobName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structured_json", columnDefinition = "jsonb")
    private String structuredJson;

    @Column(name = "overall_confidence", precision = 4, scale = 3)
    private BigDecimal overallConfidence;

    @Column(name = "injection_suspected", nullable = false)
    private Boolean injectionSuspected = false;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}