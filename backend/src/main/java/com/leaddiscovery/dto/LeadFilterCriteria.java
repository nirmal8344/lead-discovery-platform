package com.leaddiscovery.dto;

import com.leaddiscovery.entity.enums.VerificationStatus;

public class LeadFilterCriteria {

    private String city;
    private String category;
    private VerificationStatus verificationStatus;
    private String search;
    private Long taskId;
    private Long userId;

    public LeadFilterCriteria() {
    }

    public LeadFilterCriteria(String city, String category, VerificationStatus verificationStatus, String search, Long taskId) {
        this.city = city;
        this.category = category;
        this.verificationStatus = verificationStatus;
        this.search = search;
        this.taskId = taskId;
    }

    public LeadFilterCriteria(String city, String category, VerificationStatus verificationStatus, String search, Long taskId, Long userId) {
        this.city = city;
        this.category = category;
        this.verificationStatus = verificationStatus;
        this.search = search;
        this.taskId = taskId;
        this.userId = userId;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
