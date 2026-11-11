package com.mmaug.claimsagent.user;

import com.mmaug.claimsagent.common.BaseEntity;
import com.mmaug.claimsagent.common.crypto.AttributeEncryptor;
import com.mmaug.claimsagent.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser extends BaseEntity {

    @Column(name = "entra_object_id", nullable = false, unique = true, length = 64)
    private String entraObjectId;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "email", columnDefinition = "TEXT")
    private String email;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "display_name", columnDefinition = "TEXT")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
