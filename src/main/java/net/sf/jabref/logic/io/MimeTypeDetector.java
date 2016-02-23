package net.sf.jabref.logic.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class MimeTypeDetector {
    private static final Log LOGGER = LogFactory.getLog(MimeTypeDetector.class);

    public static boolean isPdfContentType(String url) {
        String contentType = getMimeType(url);

        return contentType != null && contentType.toLowerCase().startsWith("application/pdf");
    }

    private static String getMimeType(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();

            return connection.getContentType();
        } catch (IOException e) {
            LOGGER.debug("Error getting MIME type of URL", e);
            return null;
        }
    }
}