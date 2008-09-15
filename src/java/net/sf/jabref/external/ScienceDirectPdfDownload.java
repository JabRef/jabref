package net.sf.jabref.external;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 * FullTextFinder implementation that attempts to find PDF url from a Sciencedirect article page.
 */
public class ScienceDirectPdfDownload implements FullTextFinder {

    //private static final String BASE_URL = "http://www.sciencedirect.com";

    public ScienceDirectPdfDownload() {

    }

    public boolean supportsSite(URL url) {
        return url.getHost().toLowerCase().indexOf("www.sciencedirect.com") != -1;
    }



    public URL findFullTextURL(URL url, String pageSource) {
        //System.out.println(pageSource);
        int index = pageSource.indexOf("PDF (");
        //System.out.println(index);
        if (index > -1) {
            String leading = pageSource.substring(0, index);
            //System.out.println(leading.toLowerCase());
            index = leading.toLowerCase().lastIndexOf("<a href=");
            //System.out.println(index);
            if ((index > -1) && (index+9 < leading.length())) {
                int endIndex = leading.indexOf("\"", index+9);

                try {
                    // System.out.println(leading.substring(index+9, endIndex));
                    URL pdfUrl = new URL(/*BASE_URL+*/leading.substring(index+9, endIndex));
                    //System.out.println(pdfUrl.toString());
                    return pdfUrl;
                } catch (MalformedURLException e) {
                    return null;
                }
            }
            return null;
        } else
            return null;
    }
}
