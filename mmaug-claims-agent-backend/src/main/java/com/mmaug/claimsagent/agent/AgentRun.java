package com.mmaug.claimsagent.agent;

import com.mmaug.claimsagent.claim.Claim;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_run")
@Getter
@Setter
@NoArgsConstructor
public class AgentRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "triggered_by_user_id", nullable = false)
    private UUID triggeredByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private AgentRunStatus status = AgentRunStatus.QUEUED;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "tool_schema_version", length = 20)
    private String toolSchemaVersion;

    @Column(name = "rule_set_version", length = 20)
    private String ruleSetVersion;

    @Column(name = "rule_set_sha256", length = 64)
    private String ruleSetSha256;

    @Column(name = "llm_model", length = 60)
    private String llmModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "llm_parameters", columnDefinition = "jsonb")
    private String llmParameters;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "escalation_required", nullable = false)
    private Boolean escalationRequired = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warnings", columnDefinition = "jsonb")
    private String warnings;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}