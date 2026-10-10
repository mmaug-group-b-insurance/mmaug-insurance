package com.mmaug.claimsagent.agent;

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

@Entity
@Table(name = "agent_step")
@Getter
@Setter
@NoArgsConstructor
public class AgentStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRun run;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 20)
    private AgentStepType stepType;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "status", nullable = false, length = 15)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_summary", columnDefinition = "jsonb")
    private String inputSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_summary", columnDefinition = "jsonb")
    private String outputSummary;

    @Column(name = "input_hash", length = 64)
    private String inputHash;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "error_code", length = 40)
    private String errorCode;
}