package net.sf.jabref.external;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.fetcher.*;
import net.sf.jabref.logic.net.URLDownload;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FindFullText {
    private final List<FullTextFinder> finders = new ArrayList<FullTextFinder>();

    public FindFullText() {
        // Ordering is important, authorities first!
        // Publisher
        finders.add(new ScienceDirect());
        finders.add(new SpringerLink());
        finders.add(new ACS());
        finders.add(new ArXiv());
        // Meta search
        finders.add(new GoogleScholar());
    }

    public Optional<URL> findFullText(BibtexEntry entry) {
        for (FullTextFinder finder : finders) {
            try {
                Optional<URL> result = finder.findFullText(entry);

                if (result.isPresent()) {
                    // TODO: recheck this!
                    // Check the MIME type of this URL to see if it is a PDF. If not,
                    // it could be because the user doesn't have access:
                    // FIXME: redirection break this!
                    // Property-based software engineering measurement
                    // http://drum.lib.umd.edu/bitstream/1903/19/2/CS-TR-3368.pdf
                    // FIXME:
                    // INFO: Fulltext PDF found @ Google: https://www.uni-bamberg.de/fileadmin/uni/fakultaeten/wiai_lehrstuehle/praktische_informatik/Dateien/Publikationen/sose14-towards-application-portability-in-paas.pdf
                    // javax.net.ssl.SSLProtocolException: handshake alert:  unrecognized_name
                    // http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
                    String mimeType = new URLDownload(result.get()).determineMimeType();
                    if (mimeType != null && mimeType.toLowerCase().equals("application/pdf")) {
                        return Optional.of(result.get());
                    } else {
                        // TODO log
                    }
                }
            } catch (IOException ex) {
                // TODO log
                continue;
            }
        }
        return Optional.empty();
    }

    /**
     * Follow redirects until the final location is reached. This is necessary to handle Doi links, which
     * redirect to publishers' web sites. We need to know the publisher's domain name in order to choose
     * which FullTextFinder to use.
     *
     * @param url           The url to start with.
     * @param redirectCount The number of previous redirects. We will follow a maximum of 5 redirects.
     * @return the final URL, or the initial one in case there is no redirect.
     * @throws IOException for connection error
     */
    private static URL resolveRedirects(URL url, int redirectCount) throws IOException {
        URLConnection uc = url.openConnection();
        if (uc instanceof HttpURLConnection) {
            HttpURLConnection huc = (HttpURLConnection) uc;
            huc.setInstanceFollowRedirects(false);
            huc.connect();
            int responseCode = huc.getResponseCode();
            String location = huc.getHeaderField("location");
            huc.disconnect();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM && redirectCount < 5) {
                try {
                    URL newUrl = new URL(location);
                    return resolveRedirects(newUrl, redirectCount + 1);
                } catch (MalformedURLException ex) {
                    return url; // take the previous one, since this one didn't make sense.
                    // TODO: this could be caused by location being a relative link, but this would just give
                    // the default page in the case of www.springerlink.com, not the article page. Don't know why.
                }

            } else {
                return url;
            }

        } else {
            return url;
        }
    }

    public static String loadPage(URL url) throws IOException {
        Reader in = null;
        URLConnection uc;
        HttpURLConnection huc = null;
        try {
            uc = url.openConnection();
            if (uc instanceof HttpURLConnection) {
                huc = (HttpURLConnection) uc;
                huc.setInstanceFollowRedirects(false);
                huc.connect();

                in = new InputStreamReader(huc.getInputStream());
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = in.read()) != -1) {
                    sb.append((char) c);
                }
                return sb.toString();
            } else {
                return null; // TODO: are other types of connection (https?) relevant?
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (huc != null) {
                    huc.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
}
