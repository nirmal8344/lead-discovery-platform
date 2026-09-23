package com.leaddiscovery.dto;

import com.leaddiscovery.entity.*;
import com.leaddiscovery.entity.enums.ContactVerificationStatus;
import com.leaddiscovery.entity.enums.PhoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LeadExportRowDto {

    private String leadId;
    private String companyName;
    private String category;
    private String contactPerson;
    private String contactRole;
    private String location;
    private String city;
    private String state;
    private String country;
    private String address;
    private String officialWebsite;
    private String email;
    private String emailSourceUrl;
    private String emailSourceDomain;
    private String emailVerificationStatus;
    private String phone;
    private String phoneSourceUrl;
    private String whatsApp;
    private String whatsAppSourceUrl;
    private String socialLinks;
    private String confidence;
    private String verificationStatus;
    private String discoverySourceUrl;
    private String createdAt;

    public static final String[] HEADERS = new String[] {
            "Lead ID",
            "Company Name",
            "Category",
            "Contact Person",
            "Contact Role",
            "Location",
            "City",
            "State",
            "Country",
            "Address",
            "Official Website",
            "Email",
            "Email Source URL",
            "Email Source Domain",
            "Email Verification Status",
            "Phone",
            "Phone Source URL",
            "WhatsApp",
            "WhatsApp Source URL",
            "Social Links",
            "Confidence",
            "Verification Status",
            "Discovery Source URL",
            "Created At"
    };

    public static LeadExportRowDto fromEntity(Organization org) {
        LeadExportRowDto row = new LeadExportRowDto();
        row.setLeadId(org.getId() != null ? String.valueOf(org.getId()) : "");
        row.setCompanyName(org.getBusinessName() != null ? org.getBusinessName() : "");
        row.setCategory(org.getCategory() != null ? org.getCategory() : "");

        // Contacts (Contact Person & Role)
        String contactPerson = "";
        String contactRole = "";
        if (org.getContacts() != null && !org.getContacts().isEmpty()) {
            contactPerson = org.getContacts().stream()
                    .map(Contact::getContactPerson)
                    .filter(cp -> cp != null && !cp.isBlank())
                    .collect(Collectors.joining("; "));
            contactRole = org.getContacts().stream()
                    .map(c -> c.getRole() != null ? c.getRole() : "")
                    .filter(r -> !r.isBlank())
                    .collect(Collectors.joining("; "));
        }
        row.setContactPerson(contactPerson);
        row.setContactRole(contactRole);

        // Location
        List<String> locParts = new ArrayList<>();
        if (org.getCity() != null && !org.getCity().isBlank()) locParts.add(org.getCity().trim());
        if (org.getState() != null && !org.getState().isBlank()) locParts.add(org.getState().trim());
        if (org.getCountry() != null && !org.getCountry().isBlank()) locParts.add(org.getCountry().trim());
        row.setLocation(String.join(", ", locParts));
        row.setCity(org.getCity() != null ? org.getCity() : "");
        row.setState(org.getState() != null ? org.getState() : "");
        row.setCountry(org.getCountry() != null ? org.getCountry() : "");
        row.setAddress(org.getAddress() != null ? org.getAddress() : "");

        // Official Website
        String websiteUrl = "";
        if (org.getWebsites() != null && !org.getWebsites().isEmpty()) {
            for (Website w : org.getWebsites()) {
                if (Boolean.TRUE.equals(w.getIsOfficial())) {
                    websiteUrl = w.getUrl();
                    break;
                }
            }
            if (websiteUrl.isEmpty()) {
                websiteUrl = org.getWebsites().get(0).getUrl();
            }
        }
        row.setOfficialWebsite(websiteUrl);

        // Email
        String email = "";
        String emailSourceUrl = "";
        String emailSourceDomain = "";
        String emailVerificationStatus = "";
        if (org.getEmailAddresses() != null && !org.getEmailAddresses().isEmpty()) {
            EmailAddress chosenEmail = null;
            for (EmailAddress e : org.getEmailAddresses()) {
                if (e.getVerificationStatus() == ContactVerificationStatus.VERIFIED) {
                    chosenEmail = e;
                    break;
                }
            }
            if (chosenEmail == null) {
                chosenEmail = org.getEmailAddresses().get(0);
            }
            email = chosenEmail.getNormalizedValue();
            emailSourceUrl = chosenEmail.getSourcePage() != null ? chosenEmail.getSourcePage().getPageUrl() : "";
            emailSourceDomain = chosenEmail.getSourceDomain() != null ? chosenEmail.getSourceDomain() : "";
            emailVerificationStatus = chosenEmail.getVerificationStatus() != null ? chosenEmail.getVerificationStatus().name() : "";
        }
        row.setEmail(email);
        row.setEmailSourceUrl(emailSourceUrl);
        row.setEmailSourceDomain(emailSourceDomain);
        row.setEmailVerificationStatus(emailVerificationStatus);

        // Phone & WhatsApp segregation
        String phone = "";
        String phoneSourceUrl = "";
        String whatsApp = "";
        String whatsAppSourceUrl = "";

        if (org.getPhoneNumbers() != null && !org.getPhoneNumbers().isEmpty()) {
            PhoneNumber chosenPhone = null;
            PhoneNumber chosenWhatsApp = null;

            for (PhoneNumber p : org.getPhoneNumbers()) {
                if (p.getPhoneType() == PhoneType.WHATSAPP) {
                    if (chosenWhatsApp == null || p.getVerificationStatus() == ContactVerificationStatus.VERIFIED) {
                        chosenWhatsApp = p;
                    }
                } else {
                    if (chosenPhone == null || p.getVerificationStatus() == ContactVerificationStatus.VERIFIED) {
                        chosenPhone = p;
                    }
                }
            }

            if (chosenPhone != null) {
                phone = chosenPhone.getNormalizedValue();
                phoneSourceUrl = chosenPhone.getSourcePage() != null ? chosenPhone.getSourcePage().getPageUrl() : "";
            }
            if (chosenWhatsApp != null) {
                whatsApp = chosenWhatsApp.getNormalizedValue();
                whatsAppSourceUrl = chosenWhatsApp.getSourcePage() != null ? chosenWhatsApp.getSourcePage().getPageUrl() : "";
            }
        }
        row.setPhone(phone);
        row.setPhoneSourceUrl(phoneSourceUrl);
        row.setWhatsApp(whatsApp);
        row.setWhatsAppSourceUrl(whatsAppSourceUrl);

        // Social Links
        String socialLinks = org.getSocialLinks() != null && !org.getSocialLinks().isEmpty() ?
                org.getSocialLinks().stream()
                        .map(s -> s.getPlatform() + ": " + s.getUrl())
                        .collect(Collectors.joining("; ")) : "";
        row.setSocialLinks(socialLinks);

        // Confidence, Status, Provenance
        row.setConfidence(org.getConfidenceScore() != null ? org.getConfidenceScore().toString() : "0.00");
        row.setVerificationStatus(org.getVerificationStatus() != null ? org.getVerificationStatus().name() : "");
        row.setDiscoverySourceUrl(org.getSourceUrl() != null ? org.getSourceUrl() : "");
        row.setCreatedAt(org.getCreatedAt() != null ? org.getCreatedAt().toString() : "");

        return row;
    }

    public String[] toArray() {
        return new String[] {
                leadId, companyName, category, contactPerson, contactRole,
                location, city, state, country, address,
                officialWebsite, email, emailSourceUrl, emailSourceDomain, emailVerificationStatus,
                phone, phoneSourceUrl, whatsApp, whatsAppSourceUrl,
                socialLinks, confidence, verificationStatus, discoverySourceUrl, createdAt
        };
    }

    // Getters and Setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getContactRole() { return contactRole; }
    public void setContactRole(String contactRole) { this.contactRole = contactRole; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getOfficialWebsite() { return officialWebsite; }
    public void setOfficialWebsite(String officialWebsite) { this.officialWebsite = officialWebsite; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailSourceUrl() { return emailSourceUrl; }
    public void setEmailSourceUrl(String emailSourceUrl) { this.emailSourceUrl = emailSourceUrl; }
    public String getEmailSourceDomain() { return emailSourceDomain; }
    public void setEmailSourceDomain(String emailSourceDomain) { this.emailSourceDomain = emailSourceDomain; }
    public String getEmailVerificationStatus() { return emailVerificationStatus; }
    public void setEmailVerificationStatus(String emailVerificationStatus) { this.emailVerificationStatus = emailVerificationStatus; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPhoneSourceUrl() { return phoneSourceUrl; }
    public void setPhoneSourceUrl(String phoneSourceUrl) { this.phoneSourceUrl = phoneSourceUrl; }
    public String getWhatsApp() { return whatsApp; }
    public void setWhatsApp(String whatsApp) { this.whatsApp = whatsApp; }
    public String getWhatsAppSourceUrl() { return whatsAppSourceUrl; }
    public void setWhatsAppSourceUrl(String whatsAppSourceUrl) { this.whatsAppSourceUrl = whatsAppSourceUrl; }
    public String getSocialLinks() { return socialLinks; }
    public void setSocialLinks(String socialLinks) { this.socialLinks = socialLinks; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getDiscoverySourceUrl() { return discoverySourceUrl; }
    public void setDiscoverySourceUrl(String discoverySourceUrl) { this.discoverySourceUrl = discoverySourceUrl; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
