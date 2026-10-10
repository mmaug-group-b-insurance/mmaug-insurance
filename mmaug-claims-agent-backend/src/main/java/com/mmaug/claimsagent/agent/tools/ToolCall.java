package com.mmaug.claimsagent.agent.tools;

import com.mmaug.claimsagent.agent.AgentRun;
import com.mmaug.claimsagent.agent.AgentStep;
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
@Table(name = "tool_call")
@Getter
@Setter
@NoArgsConstructor
public class ToolCall extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id")
    private AgentStep step;

    @Column(name = "tool_name", nullable = false, length = 60)
    private String toolName;

    @Column(name = "tool_schema_version", length = 20)
    private String toolSchemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "arguments_json", columnDefinition = "jsonb")
    private String argumentsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "jsonb")
    private String resultJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ToolCallStatus status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "called_at", nullable = false)
    private Instant calledAt;
}