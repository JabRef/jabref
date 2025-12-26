package org.jabref.http.dto;

/**
 * DTO for sending the result back to the browser.
 * Matches JSON: { "exists": false, "message": "..." }
 */
public class CheckExistenceResponse {
    private boolean exists;
    private String message;

    public CheckExistenceResponse(boolean exists, String message) {
        this.exists = exists;
        this.message = message;
    }

    public boolean isExists() {
        return exists;
    }

    public String getMessage() {
        return message;
    }
}
