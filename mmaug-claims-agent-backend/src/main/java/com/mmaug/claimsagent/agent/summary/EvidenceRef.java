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

import java.util.UUID;

@Entity
@Table(name = "evidence_ref")
@Getter
@Setter
@NoArgsConstructor
public class EvidenceRef extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private SummaryStatement statement;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 15)
    private SourceType sourceType;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "snippet", length = 500)
    private String snippet;

    @Column(name = "tool_call_id")
    private UUID toolCallId;

    @Column(name = "rule_id", length = 40)
    private String ruleId;

    @Column(name = "field_name", length = 60)
    private String fieldName;
}