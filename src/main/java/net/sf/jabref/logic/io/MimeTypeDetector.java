package net.sf.jabref.logic.io;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.logic.net.URLDownload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class MimeTypeDetector {
    private static final Log LOGGER = LogFactory.getLog(MimeTypeDetector.class);

    public static boolean isPdfContentType(String url) {
        String contentType = getMimeType(url);

        if (contentType == null) {
            return false;
        }
        return contentType.toLowerCase().startsWith("application/pdf");
    }

    public static String getMimeType(String url) {
        String contentType = null;

        try {
            contentType = new URL(url).openConnection().getContentType();
        } catch(IOException e) {
            LOGGER.debug("Error getting MIME type of URL", e);
        } finally {
            return contentType;
        }
    }
}