package com.mmaug.claimsagent.tools.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Policy view exposed to the internal tools API")
public class PolicyView {

    private String policyNumber;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String productCode;
    private BigDecimal standardExcess;
    private String paymentStatus;
    private PolicyholderView policyholder;
    private List<VehicleView> vehicles;
    private List<DriverView> drivers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyholderView {
        private String customerNumber;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleView {
        private String registration;
        private String make;
        private String model;
        private Integer year;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriverView {
        private String name;
        private String licenceNumber;
        private Boolean isNamedDriver;
    }
}