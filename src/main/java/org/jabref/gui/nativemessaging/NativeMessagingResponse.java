package org.jabref.gui.nativemessaging;

import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

public class NativeMessagingResponse {

    private final Optional<JSONObject> jsonResponse;
    private final Optional<String> errorMessage;
    private final Optional<Exception> errorCause;
    private final boolean successful;

    private NativeMessagingResponse(JSONObject jsonResponse) {
        this.jsonResponse = Optional.of(jsonResponse);
        this.errorMessage = Optional.empty();
        this.errorCause = Optional.empty();
        this.successful = true;
    }

    private NativeMessagingResponse(String message, Exception cause) {
        this.errorMessage = Optional.of(message);
        this.errorCause = Optional.ofNullable(cause);
        this.jsonResponse = Optional.empty();
        this.successful = false;
    }

    public static NativeMessagingResponse fromContent(String content) {
        try {
            return new NativeMessagingResponse(new JSONObject(content));
        } catch (JSONException exception) {
            return fromException("Failed to parse response.", exception);
        }
    }

    public static NativeMessagingResponse fromException(String message, Exception cause) {
        return new NativeMessagingResponse(message, cause);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public Optional<JSONObject> getJsonResponse() {
        return jsonResponse;
    }

    public Optional<Exception> getErrorCause() {
        return errorCause;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
    }
}
