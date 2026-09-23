package com.leaddiscovery.dto;

import com.leaddiscovery.entity.enums.IdentityValidationResult;
import java.util.ArrayList;
import java.util.List;

public class OrganizationIdentity {

    private String extractedBusinessName;
    private String officialDomain;
    private IdentityValidationResult identityStatus;
    private String extractionSource;
    private double identityConfidence;
    private String rejectionReason;
    private List<String> warnings = new ArrayList<>();

    public OrganizationIdentity() {
    }

    public OrganizationIdentity(String extractedBusinessName, String officialDomain,
                                IdentityValidationResult identityStatus, String extractionSource,
                                double identityConfidence, String rejectionReason, List<String> warnings) {
        this.extractedBusinessName = extractedBusinessName;
        this.officialDomain = officialDomain;
        this.identityStatus = identityStatus;
        this.extractionSource = extractionSource;
        this.identityConfidence = identityConfidence;
        this.rejectionReason = rejectionReason;
        if (warnings != null) {
            this.warnings = warnings;
        }
    }

    public static OrganizationIdentity confirmed(String businessName, String domain, String extractionSource, double confidence) {
        return new OrganizationIdentity(businessName, domain, IdentityValidationResult.IDENTITY_CONFIRMED,
                extractionSource, confidence, null, new ArrayList<>());
    }

    public static OrganizationIdentity uncertain(String businessName, String domain, String extractionSource, double confidence, String warning) {
        List<String> warns = new ArrayList<>();
        if (warning != null && !warning.isBlank()) {
            warns.add(warning);
        }
        return new OrganizationIdentity(businessName, domain, IdentityValidationResult.IDENTITY_UNCERTAIN,
                extractionSource, confidence, null, warns);
    }

    public static OrganizationIdentity rejected(String rejectionReason, String domain) {
        return new OrganizationIdentity(null, domain, IdentityValidationResult.IDENTITY_REJECTED,
                "NONE", 0.0, rejectionReason, List.of(rejectionReason));
    }

    public String getExtractedBusinessName() {
        return extractedBusinessName;
    }

    public void setExtractedBusinessName(String extractedBusinessName) {
        this.extractedBusinessName = extractedBusinessName;
    }

    public String getOfficialDomain() {
        return officialDomain;
    }

    public void setOfficialDomain(String officialDomain) {
        this.officialDomain = officialDomain;
    }

    public IdentityValidationResult getIdentityStatus() {
        return identityStatus;
    }

    public void setIdentityStatus(IdentityValidationResult identityStatus) {
        this.identityStatus = identityStatus;
    }

    public String getExtractionSource() {
        return extractionSource;
    }

    public void setExtractionSource(String extractionSource) {
        this.extractionSource = extractionSource;
    }

    public double getIdentityConfidence() {
        return identityConfidence;
    }

    public void setIdentityConfidence(double identityConfidence) {
        this.identityConfidence = identityConfidence;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
