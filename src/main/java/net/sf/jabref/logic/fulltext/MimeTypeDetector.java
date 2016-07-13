package net.sf.jabref.logic.fulltext;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MimeTypeDetector {
    private static final Log LOGGER = LogFactory.getLog(MimeTypeDetector.class);

    public static boolean isPdfContentType(String url) {
        Optional<String> contentType = getMimeType(url);

        return contentType.isPresent() && contentType.get().toLowerCase().startsWith("application/pdf");
    }

    private static Optional<String> getMimeType(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();

            return Optional.ofNullable(connection.getContentType());
        } catch (IOException e) {
            LOGGER.debug("Error getting MIME type of URL", e);
            return Optional.empty();
        }
    }
}