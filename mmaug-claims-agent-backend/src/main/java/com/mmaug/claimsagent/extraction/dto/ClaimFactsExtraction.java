package com.mmaug.claimsagent.extraction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Structured facts extracted from a claim document")
public class ClaimFactsExtraction {

    private UUID documentId;
    private FieldValue policyNumber;
    private FieldValue policyholderName;
    private FieldValue driverName;
    private FieldValue driverLicenceNumber;
    private FieldValue driverDateOfBirth;
    private FieldValue vehicleRegistration;
    private FieldValue vehicleMake;
    private FieldValue vehicleModel;
    private FieldValue vehicleYear;
    private FieldValue incidentDate;
    private FieldValue incidentTime;
    private FieldValue incidentLocation;
    private FieldValue incidentDescription;
    private FieldValue thirdPartyInvolved;
    private FieldValue policeReportNumber;
    private FieldValue policeStation;
    private FieldValue injuriesReported;
    private FieldValue injuryDescription;
    private FieldValue damageDescription;
    private List<FieldValue> damagedAreas;
    private FieldValue estimatedRepairCost;
    private FieldValue currency;
    private FieldValue garageName;
    private FieldValue estimateReference;
    private FieldValue estimateDate;
    private List<FieldValue> witnesses;

    private Boolean injectionSuspected;
    private String injectionNotes;
    private String schemaVersion;
}