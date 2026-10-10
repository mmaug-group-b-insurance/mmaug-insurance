package com.mmaug.claimsagent.agent.summary;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.claim.Claim;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "claim_summary")
@Getter
@Setter
@NoArgsConstructor
public class ClaimSummary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private AgentRun run;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 1;

    @Column(name = "headline", length = 300)
    private String headline;

    @Column(name = "overall_confidence", length = 10)
    private String overallConfidence;

    @Column(name = "escalation_recommended", nullable = false)
    private Boolean escalationRecommended = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "escalation_reasons", columnDefinition = "jsonb")
    private String escalationReasons;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "uncertainty_notes", columnDefinition = "jsonb")
    private String uncertaintyNotes;

    @Column(name = "llm_model", length = 60)
    private String llmModel;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "disclaimer", length = 500)
    private String disclaimer;
}