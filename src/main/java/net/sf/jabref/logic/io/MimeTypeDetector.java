package net.sf.jabref.logic.io;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
        try {
            String contentType = Unirest.head(url).asBinary().getHeaders().getFirst("content-type");
            // HEAD and GET headers might differ, try real GET request
            if(contentType == null) {
                contentType = Unirest.get(url).asBinary().getHeaders().getFirst("content-type");
            }
            return contentType;
        } catch (UnirestException e) {
            LOGGER.debug("Error getting MIME type of URL", e);
            return null;
        }
    }
}