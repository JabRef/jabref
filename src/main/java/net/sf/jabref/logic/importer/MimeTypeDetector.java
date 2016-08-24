package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import com.mashape.unirest.http.Unirest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MimeTypeDetector {
    private static final Log LOGGER = LogFactory.getLog(MimeTypeDetector.class);

    public static boolean isPdfContentType(String url) {
        Optional<String> contentType = getMimeType(url);

        return contentType.isPresent() && contentType.get().toLowerCase().startsWith("application/pdf");
    }

    private static Optional<String> getMimeType(String url) {
        Unirest.setDefaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

        // Try to use HEAD request to avoid donloading the whole file
        String contentType;
        try {
            contentType = Unirest.head(url).asString().getHeaders().get("Content-Type").get(0);

            if (contentType != null) {
                return Optional.of(contentType);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via HEAD request", e);
        }

        // Use GET request as alternative if no HEAD request is available
        try {
            contentType = Unirest.get(url).asString().getHeaders().get("Content-Type").get(0);

            if (contentType != null) {
                return Optional.of(contentType);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via GET request", e);
        }

        // Try to resolve local URIs
        try {
            URLConnection connection = new URL(url).openConnection();

            return Optional.ofNullable(connection.getContentType());
        } catch (IOException e) {
            LOGGER.debug("Error trying to get MIME type of local URI", e);
        }

        return Optional.empty();
    }
}
