package com.mmaug.claimsagent.policy;

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

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "registration_number", nullable = false, length = 15)
    private String registrationNumber;

    @Column(name = "vin", length = 17)
    private String vin;

    @Column(name = "make", length = 50)
    private String make;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "engine_cc")
    private Integer engineCc;

    @Column(name = "colour", length = 30)
    private String colour;

    @Column(name = "usage_type", length = 20)
    private String usageType;

    @Column(name = "market_value", precision = 12, scale = 2)
    private BigDecimal marketValue;
}