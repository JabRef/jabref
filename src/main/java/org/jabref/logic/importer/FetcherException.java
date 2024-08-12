package org.jabref.logic.importer;

import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.http.dto.SimpleHttpResponse;
import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class FetcherException extends JabRefException {

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

    public FetcherException(URL url, SimpleHttpResponse httpResponse) {
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

    /**
     * @param dummy Required to distinguish of {@link FetcherException#FetcherException(String, Throwable)}, which is for a FetcherException with an error message
     */
    private FetcherException(String url, Throwable cause, Object dummy) {
        super(cause);
        httpResponse = null;
        this.url = url;
    }

    /**
     * @param dummy Required to distinguish of {@link FetcherException#FetcherException(String, String, Throwable)}, which is for a localized FetcherException
     */
    private FetcherException(String url, String errorMessage, Throwable cause, Object dummy) {
        super(errorMessage, cause);
        httpResponse = null;
        this.url = url;
    }

    public FetcherException(String errorMessage, Throwable cause) {
        // TODO: Convert IOException e to FetcherClientException
        //       Same TODO as in org.jabref.logic.importer.EntryBasedParserFetcher.performSearch.
        //       Maybe do this in org.jabref.logic.importer.FetcherException.getLocalizedMessage
        super(errorMessage, cause);
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

    public static FetcherException ofUrl(String url, Throwable cause) {
        return new FetcherException(url, cause, null);
    }

    public static FetcherException ofUrl(String url, String errorMessage) {
        return new FetcherException(url, errorMessage, null, null);
    }

    public static FetcherException ofUrl(String url, String errorMessage, Throwable cause) {
        return new FetcherException(url, errorMessage, cause, null);
    }

    public static FetcherException of(URL url, SimpleHttpResponse simpleHttpResponse) {
        if (simpleHttpResponse.statusCode() >= 500) {
            return new FetcherServerException(url, simpleHttpResponse);
        } else {
            return new FetcherClientException(url, simpleHttpResponse);
        }
    }

    @Override
    public String getLocalizedMessage() {
        // TODO: This should be moved to a separate class converting "any" exception object to a localized message
        // TODO: 5% of the "new-ers" pass a "real" localized message. See org.jabref.logic.importer.fetcher.GoogleScholar.addHitsFromQuery. We should maybe make use of this (and rewrite the whole message handling)
        // TODO: Try to convert IOException to some more meaningful information here (or at org.jabref.gui.DialogService.showErrorDialogAndWait(org.jabref.logic.importer.FetcherException)). See also org.jabref.logic.net.URLDownload.openConnection
        if (httpResponse != null) {
            return getPrefix() + Localization.lang("URL: %0\nHTTP %1 %2\n%3", getRedactedUrl(), httpResponse.statusCode(), httpResponse.responseMessage(), httpResponse.responseBody());
        } else if (url != null) {
            return getPrefix() + Localization.lang("URL: %0", getRedactedUrl());
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
}
