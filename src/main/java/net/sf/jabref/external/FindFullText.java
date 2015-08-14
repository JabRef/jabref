/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.external;

import java.io.File;
import java.io.FileWriter;
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
import net.sf.jabref.logic.crawler.ACS;
import net.sf.jabref.logic.crawler.GoogleScholar;
import net.sf.jabref.logic.crawler.ScienceDirect;
import net.sf.jabref.logic.crawler.SpringerLink;
import net.sf.jabref.logic.net.URLDownload;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FindFullText {

    private static final int FOUND_PDF = 0;
    public static final int WRONG_MIME_TYPE = 1;
    public static final int LINK_NOT_FOUND = 2;
    public static final int IO_EXCEPTION = 3;

    private final List<FullTextFinder> finders = new ArrayList<FullTextFinder>();


    public FindFullText() {
        // Ordering is important, authorities first!
        // Publisher
        finders.add(new ScienceDirect());
        finders.add(new SpringerLink());
        finders.add(new ACS());
        // Meta search
        finders.add(new GoogleScholar());
    }

    public FindResult findFullText(BibtexEntry entry) {
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
                        return new FindResult(result.get(), result.get());
                    } else {
                        return new FindResult(WRONG_MIME_TYPE, result.get());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return new FindResult(IO_EXCEPTION, null);
            }
        }
        return new FindResult(LINK_NOT_FOUND, null);
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
                //System.out.println(responseCode);
                //System.out.println(location);
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


    public static class FindResult {

        public final URL url;
        public String host;
        public final int status;


        public FindResult(URL url, URL originalUrl) {
            this.url = url;
            this.status = FindFullText.FOUND_PDF;
            if (originalUrl != null) {
                host = originalUrl.getHost();
            }
        }

        public FindResult(int status, URL originalUrl) {
            this.url = null;
            this.status = status;
            if (originalUrl != null) {
                this.host = originalUrl.getHost();
            }
        }
    }
}
