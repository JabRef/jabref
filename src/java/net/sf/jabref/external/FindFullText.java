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

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.net.URLDownload;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FindFullText {

    public final static int
        FOUND_PDF = 0,
        WRONG_MIME_TYPE = 1,
        UNKNOWN_DOMAIN = 2,
        LINK_NOT_FOUND = 3,
        IO_EXCEPTION = 4,
        NO_URLS_DEFINED = 5;

    List<FullTextFinder> finders = new ArrayList<FullTextFinder>();


    public FindFullText() {
        finders.add(new ScienceDirectPdfDownload());
        finders.add(new SpringerLinkPdfDownload());
    }

    public FindResult findFullText(BibtexEntry entry) {
        String urlText = entry.getField("url");
        String doiText = entry.getField("doi");
        // First try the DOI link, if defined:
        if ((doiText != null) && (doiText.trim().length() > 0)) {
            FindResult resDoi = lookForFullTextAtURL(Globals.DOI_LOOKUP_PREFIX+doiText);
            if (resDoi.status == FOUND_PDF)
                return resDoi;
            // The DOI link failed, try falling back on the URL link, if defined:
            else if ((urlText != null) && (urlText.trim().length() > 0)) {
                FindResult resUrl = lookForFullTextAtURL(urlText);
                if (resUrl.status == FOUND_PDF)
                    return resUrl;
                else {
                    return resDoi; // If both URL and DOI fail, we assume that the error code for DOI is
                                   // probably the most relevant.
                }
            }
            else return resDoi;
        }
        // No DOI? Try URL:
        else if ((urlText != null) && (urlText.trim().length() > 0)) {
            return lookForFullTextAtURL(urlText);
        }
        // No URL either? Return error code.
        else return new FindResult(NO_URLS_DEFINED, null);
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
                            URLDownload udl = new URLDownload(null, result, null);
                            udl.openConnectionOnly();

                            String mimeType = udl.getMimeType();
                            if ((mimeType != null) && (mimeType.toLowerCase().equals("application/pdf"))) {
                                return new FindResult(result, url);
                            }
                            else {
                                udl = new URLDownload(null, result, new File("page.html"));
                                udl.download();
                                return new FindResult(WRONG_MIME_TYPE, url);
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            return new FindResult(IO_EXCEPTION, url);
                        }
                    }

                }
            }
            if (!domainKnown)
                return new FindResult(UNKNOWN_DOMAIN, url);
            else
                return new FindResult(LINK_NOT_FOUND, url);
        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {
          e.printStackTrace();
        }

        return null;
    }

    /**
     * Follow redirects until the final location is reached. This is necessary to handle DOI links, which
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
            HttpURLConnection huc = (HttpURLConnection)uc;
            huc.setInstanceFollowRedirects(false);
            huc.connect();
            int responseCode = huc.getResponseCode();
            String location = huc.getHeaderField("location");
            huc.disconnect();
            if ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP) && (redirectCount < 5)) {
                //System.out.println(responseCode);
                //System.out.println(location);
                try {
                    URL newUrl = new URL(location);
                    return resolveRedirects(newUrl, redirectCount+1);
                } catch (MalformedURLException ex) {
                    return url; // take the previous one, since this one didn't make sense.
                    // TODO: this could be caused by location being a relative link, but this would just give
                    // the default page in the case of www.springerlink.com, not the article page. Don't know why.
                }

            }
            else return url;

        }
        else return url;
    }

    public static String loadPage(URL url) throws IOException {
        Reader in = null;
        URLConnection uc;
        HttpURLConnection huc = null;
        try {
            uc = url.openConnection();
            if (uc instanceof HttpURLConnection) {
                huc = (HttpURLConnection)uc;
                huc.setInstanceFollowRedirects(false);
                huc.connect();

                in = new InputStreamReader(huc.getInputStream());
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = in.read()) != -1)
                    sb.append((char)c);
                return sb.toString();
            }
            else
                return null; // TODO: are other types of connection (https?) relevant?
        } finally {
            try {
                if (in != null) in.close();
                if (huc != null) huc.disconnect();
            } catch (IOException ex) { ex.printStackTrace(); }
        }

    }

    public static class FindResult {
        public URL url;
        public String host = null;
        public int status;

        public FindResult(URL url, URL originalUrl) {
            this.url = url;
            this.status = FOUND_PDF;
            if (originalUrl != null)
                host = originalUrl.getHost();
        }
        public FindResult(int status, URL originalUrl) {
            this.url = null;
            this.status = status;
            if (originalUrl != null)
                this.host = originalUrl.getHost();
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
