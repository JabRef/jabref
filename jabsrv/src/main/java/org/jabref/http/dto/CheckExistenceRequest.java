package org.jabref.http.dto;

/**
 * DTO for receiving the URL check request from the browser.
 * Matches JSON: { "url": "https://..." }
 */
public class CheckExistenceRequest {
    private String url;

    public CheckExistenceRequest() {}

    public CheckExistenceRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
