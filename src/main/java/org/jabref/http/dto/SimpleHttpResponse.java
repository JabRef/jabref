package org.jabref.http.dto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public record SimpleHttpResponse(int statusCode, String responseBody, String responseMessage) {
    private static final int MAX_RESPONSE_LENGTH = 1024; // 1 KB

    public SimpleHttpResponse(int statusCode, String responseBody, String responseMessage) {
        this.statusCode = statusCode;
        this.responseBody = truncateResponseBody(responseBody);
        this.responseMessage = responseMessage;
    }

    public SimpleHttpResponse(HttpURLConnection connection) throws IOException {
        this(
                connection.getResponseCode(),
                getResponseBody(connection),
                connection.getResponseMessage()
        );
    }

    /**
     * Truncates the response body to 1 KB if it exceeds that size.
     * Appends "... (truncated)" to indicate truncation.
     *
     * @param responseBody  the original response body
     * @return  the truncated response body
     */
    private static String truncateResponseBody(String responseBody) {
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_RESPONSE_LENGTH) {
            // Truncate the response body to 1 KB and append "... (truncated)"
            // Response is in English, thus we append English text - and not localized text
            return new String(bytes, 0, MAX_RESPONSE_LENGTH, StandardCharsets.UTF_8) + "... (truncated)";
        }
        // Return the original response body if it's within the 1 KB limit
        return responseBody;
    }

    /**
     * Reads the response body from the HttpURLConnection and returns it as a string.
     * This method is used to retrieve the response body from the connection,
     * which may contain error messages or other information from the server.
     *
     * @param connection the HttpURLConnection to read the response body from
     * @return the response body as a string
     * @throws IOException if an I/O error occurs while reading the response body
     */
    private static String getResponseBody(HttpURLConnection connection) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((content.length() < MAX_RESPONSE_LENGTH) && (inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return truncateResponseBody(content.toString());
        }
    }
}
