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

import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;

/**
 * FullTextFinder implementation that attempts to find PDF url from a Sciencedirect article page.
 */
public class SpringerLinkPdfDownload implements FullTextFinder {

    private static final String BASE_URL = "http://www.springerlink.com";
    private static final String CONTENT_BASE_URL = "http://www.springerlink.com/content/";

    public SpringerLinkPdfDownload() {

    }

    public boolean supportsSite(URL url) {
        return url.getHost().toLowerCase().indexOf("www.springerlink.com") != -1;
    }



    public URL findFullTextURL(URL url) throws IOException {
        // If the url contains a 'id=' component, we will try to
        int idIndex = url.toString().indexOf("id=");
        if (idIndex > -1) {
            url = new URL(CONTENT_BASE_URL+url.toString().substring(idIndex+3));
        }
        //System.out.println("URL NOW: "+url);
        String pageSource = FindFullText.loadPage(url);
        FindFullText.dumpToFile(pageSource, new File("page.html"));
        int index = pageSource.indexOf("PDF (");
        if (index > -1) {
            String leading = pageSource.substring(0, index);
            String marker = "href=";
            index = leading.toLowerCase().lastIndexOf(marker);
            if ((index > -1) && (index+marker.length()+1 < leading.length())) {
                int endIndex = leading.indexOf("\"", index+marker.length()+1);

                try {
                    URL pdfUrl = new URL(BASE_URL+leading.substring(index+marker.length()+1, endIndex));
                    System.out.println(pdfUrl.toString());
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
