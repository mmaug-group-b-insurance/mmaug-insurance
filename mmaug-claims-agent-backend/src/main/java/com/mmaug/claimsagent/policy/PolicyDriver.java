package com.mmaug.claimsagent.policy;

import com.mmaug.claimsagent.common.BaseEntity;
import com.mmaug.claimsagent.common.crypto.AttributeEncryptor;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "policy_driver")
@Getter
@Setter
@NoArgsConstructor
public class PolicyDriver extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "full_name", columnDefinition = "TEXT")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "licence_number", columnDefinition = "TEXT")
    private String licenceNumber;

    @Column(name = "licence_issue_date")
    private LocalDate licenceIssueDate;

    @Column(name = "is_policyholder", nullable = false)
    private Boolean isPolicyholder = false;

    @Column(name = "is_named_driver", nullable = false)
    private Boolean isNamedDriver = false;
}