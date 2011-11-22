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
import java.io.IOException;

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



    public URL findFullTextURL(URL url) throws IOException {
        String pageSource = FindFullText.loadPage(url);
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
                    return new URL(/*BASE_URL+*/leading.substring(index+9, endIndex));
                    
                } catch (MalformedURLException e) {
                    return null;
                }
            }
            return null;
        } else
            return null;
    }
}
