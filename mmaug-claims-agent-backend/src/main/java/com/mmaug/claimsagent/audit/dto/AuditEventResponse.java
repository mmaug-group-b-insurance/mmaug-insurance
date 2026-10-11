package com.mmaug.claimsagent.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Immutable audit event")
public class AuditEventResponse {

    private UUID id;
    private Instant occurredAt;
    private UUID actor;
    private String actorRole;
    private String actorType;
    private String action;
    private String outcome;
    private String entityType;
    private UUID entityId;
    private UUID claimId;
    private String ipAddress;
    private String correlationId;
    private String details;
    private String hash;
    private String prevHash;
}