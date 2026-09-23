package com.leaddiscovery.dto;

public class CategoryStatDto {
    private String category;
    private long leadCount;

    public CategoryStatDto() {}

    public CategoryStatDto(String category, long leadCount) {
        this.category = category;
        this.leadCount = leadCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getLeadCount() {
        return leadCount;
    }

    public void setLeadCount(long leadCount) {
        this.leadCount = leadCount;
    }
}
