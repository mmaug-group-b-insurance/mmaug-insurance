package com.mmaug.claimsagent.retention;

import com.mmaug.claimsagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "retention_policy")
@Getter
@Setter
@NoArgsConstructor
public class RetentionPolicy extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays;

    @Column(name = "action_on_expiry", nullable = false, length = 15)
    private String actionOnExpiry;

    @Column(name = "legal_basis", length = 200)
    private String legalBasis;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}