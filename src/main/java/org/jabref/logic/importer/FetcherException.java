package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.http.dto.SimpleHttpResponse;
import org.jabref.logic.JabRefException;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetcherException extends JabRefException {
    private static final Logger LOGGER = LoggerFactory.getLogger(FetcherException.class);

    Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api|key|api[-_]?key)=[^&]*");

    String REDACTED_STRING = "[REDACTED]";

    private final String url;
    private final SimpleHttpResponse httpResponse;

    public FetcherException(String url, SimpleHttpResponse httpResponse) {
        // Empty string handled at org.jabref.logic.importer.FetcherException.getPrefix.
        super("");
        this.url = url;
        this.httpResponse = httpResponse;
    }

    protected FetcherException(URL url, SimpleHttpResponse httpResponse) {
        this(url.toString(), httpResponse);
    }

    public FetcherException(URL url, Throwable cause) {
        super(cause);
        httpResponse = null;
        this.url = url.toString();
    }

    public FetcherException(URL url, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        httpResponse = null;
        this.url = url.toString();
    }

    public FetcherException(String errorMessage, Throwable cause) {
        // TODO: Convert IOException e to FetcherClientException
        //       Same TODO as in org.jabref.logic.importer.EntryBasedParserFetcher.performSearch.
        //       Maybe do this in org.jabref.logic.importer.FetcherException.getLocalizedMessage

        // Idea (from org.jabref.logic.importer.fetcher.GoogleScholar.performSearchPaged)
        // if (e.getMessage().contains("Server returned HTTP response code: 503 for URL")) {
        super(errorMessage, cause);
        if (LOGGER.isDebugEnabled() && (cause instanceof IOException)) {
            LOGGER.debug("I/O exception", cause);
        }
        httpResponse = null;
        url = null;
    }

    public FetcherException(String errorMessage) {
        super(errorMessage);
        httpResponse = null;
        url = null;
    }

    public FetcherException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
        httpResponse = null;
        url = null;
    }

    @Override
    public String getLocalizedMessage() {
        // TODO: This should be moved to a separate class converting "any" exception object to a localized message
        // TODO: 5% of the "new-ers" pass a "real" localized message. See org.jabref.logic.importer.fetcher.GoogleScholar.addHitsFromQuery. We should maybe make use of this (and rewrite the whole message handling)
        // TODO: Try to convert IOException to some more meaningful information here (or at org.jabref.gui.DialogService.showErrorDialogAndWait(org.jabref.logic.importer.FetcherException)). See also org.jabref.logic.net.URLDownload.openConnection
        if (httpResponse != null) {
            // We decided to not "translate" technical terms (URL, HTTP)
            return getPrefix() + "URL: %s\nHTTP %d %s\n%s".formatted(getRedactedUrl(), httpResponse.statusCode(), httpResponse.responseMessage(), httpResponse.responseBody());
        } else if (url != null) {
            return getPrefix() + "URL: %s".formatted(getRedactedUrl());
        } else {
            return super.getLocalizedMessage();
        }
    }

    private String getRedactedUrl() {
        return API_KEY_PATTERN.matcher(url).replaceAll(REDACTED_STRING);
    }

    private String getPrefix() {
        String superLocalizedMessage = super.getLocalizedMessage();
        if (!StringUtil.isBlank(superLocalizedMessage)) {
            return superLocalizedMessage + "\n";
        } else {
            return "";
        }
    }

    public Optional<SimpleHttpResponse> getHttpResponse() {
        return Optional.ofNullable(httpResponse);
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public static FetcherException of(URL url, SimpleHttpResponse simpleHttpResponse) {
        if (simpleHttpResponse.statusCode() >= 500) {
            return new FetcherServerException(url, simpleHttpResponse);
        } else {
            return new FetcherClientException(url, simpleHttpResponse);
        }
    }
}
