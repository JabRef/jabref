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

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.net.URLDownload;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FindFullText {

    private static final int
            FOUND_PDF = 0;
    public static final int WRONG_MIME_TYPE = 1;
    public static final int UNKNOWN_DOMAIN = 2;
    public static final int LINK_NOT_FOUND = 3;
    public static final int IO_EXCEPTION = 4;
    public static final int NO_URLS_DEFINED = 5;

    private final List<FullTextFinder> finders = new ArrayList<FullTextFinder>();


    public FindFullText() {
        finders.add(new ScienceDirectPdfDownload());
        finders.add(new SpringerLinkPdfDownload());
        finders.add(new ACSPdfDownload());
    }

    public FindResult findFullText(BibtexEntry entry) {
        String urlText = entry.getField("url");
        String doiText = entry.getField("doi");
        // First try the Doi link, if defined:
        if (doiText != null && !doiText.trim().isEmpty()) {
            FindResult resDoi = lookForFullTextAtURL(new DOI(doiText).getURLAsASCIIString());
            if (resDoi.status == FindFullText.FOUND_PDF) {
                return resDoi;
            } else if (urlText != null && !urlText.trim().isEmpty()) {
                FindResult resUrl = lookForFullTextAtURL(urlText);
                if (resUrl.status == FindFullText.FOUND_PDF) {
                    return resUrl;
                } else {
                    return resDoi; // If both URL and Doi fail, we assume that the error code for Doi is
                                   // probably the most relevant.
                }
            } else {
                return resDoi;
            }
        }
        // No Doi? Try URL:
        else if (urlText != null && !urlText.trim().isEmpty()) {
            return lookForFullTextAtURL(urlText);
        }
        // No URL either? Return error code.
 else {
            return new FindResult(FindFullText.NO_URLS_DEFINED, null);
        }
    }

    private FindResult lookForFullTextAtURL(String urlText) {
        try {
            URL url = new URL(urlText);
            url = resolveRedirects(url, 0);
            boolean domainKnown = false;
            for (FullTextFinder finder : finders) {
                if (finder.supportsSite(url)) {
                    domainKnown = true;
                    URL result = finder.findFullTextURL(url);
                    if (result != null) {

                        // Check the MIME type of this URL to see if it is a PDF. If not,
                        // it could be because the user doesn't have access:
                        try {
                            String mimeType = new URLDownload(result).determineMimeType();
                            if (mimeType != null && mimeType.toLowerCase().equals("application/pdf")) {
                                return new FindResult(result, url);
                            }
                            else {
                                new URLDownload(result).downloadToFile(new File("page.html"));
                                return new FindResult(FindFullText.WRONG_MIME_TYPE, url);
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            return new FindResult(FindFullText.IO_EXCEPTION, url);
                        }
                    }

                }
            }
            if (!domainKnown) {
                return new FindResult(FindFullText.UNKNOWN_DOMAIN, url);
            } else {
                return new FindResult(FindFullText.LINK_NOT_FOUND, url);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Follow redirects until the final location is reached. This is necessary to handle Doi links, which
     * redirect to publishers' web sites. We need to know the publisher's domain name in order to choose
     * which FullTextFinder to use.
     * @param url The url to start with.
     * @param redirectCount The number of previous redirects. We will follow a maximum of 5 redirects.
     * @return the final URL, or the initial one in case there is no redirect.
     * @throws IOException for connection error
     */
    private URL resolveRedirects(URL url, int redirectCount) throws IOException {
        URLConnection uc = url.openConnection();
        if (uc instanceof HttpURLConnection) {
            HttpURLConnection huc = (HttpURLConnection) uc;
            huc.setInstanceFollowRedirects(false);
            huc.connect();
            int responseCode = huc.getResponseCode();
            String location = huc.getHeaderField("location");
            huc.disconnect();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP && redirectCount < 5) {
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
            }
            else {
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


    public static void dumpToFile(String text, File f) {
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(text);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
