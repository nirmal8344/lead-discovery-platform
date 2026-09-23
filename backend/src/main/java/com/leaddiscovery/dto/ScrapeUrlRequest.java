package com.leaddiscovery.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class ScrapeUrlRequest {

    @NotBlank(message = "URL cannot be blank")
    @URL(message = "Please provide a valid URL (including http:// or https://)")
    private String url;

    public ScrapeUrlRequest() {
    }

    public ScrapeUrlRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
