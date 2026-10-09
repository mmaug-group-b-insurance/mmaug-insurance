package com.mmaug.claimsagent.customer;

import com.mmaug.claimsagent.common.BaseEntity;
import com.mmaug.claimsagent.common.crypto.AttributeEncryptor;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer extends BaseEntity {

    @Column(name = "customer_number", nullable = false, unique = true, length = 20)
    private String customerNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 15)
    private CustomerType customerType;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "first_name", columnDefinition = "TEXT")
    private String firstName;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "last_name", columnDefinition = "TEXT")
    private String lastName;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "company_reg_no", length = 30)
    private String companyRegNo;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "national_id_no", columnDefinition = "TEXT")
    private String nationalIdNo;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "email", columnDefinition = "TEXT")
    private String email;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "phone", columnDefinition = "TEXT")
    private String phone;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "address_line1", columnDefinition = "TEXT")
    private String addressLine1;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "address_line2", columnDefinition = "TEXT")
    private String addressLine2;

    @Column(name = "locality", length = 100)
    private String locality;

    @Column(name = "postal_code", length = 15)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 2)
    private String country = "MT";
}