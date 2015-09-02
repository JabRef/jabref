/*  Copyright (C) 2014 Commonwealth Scientific and Industrial Research Organisation
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
 * FullTextFinder implementation that attempts to find PDF url from a ACS DOI.
 */
public class ACSPdfDownload implements FullTextFinder {

    private static final String BASE_URL = "http://pubs.acs.org/doi/pdf/";


    public ACSPdfDownload() {

    }

    @Override
    public boolean supportsSite(URL url) {
        return url.getHost().toLowerCase().contains("acs.org");
    }

    @Override
    public URL findFullTextURL(URL url) throws IOException {
        try {
            return new URL(ACSPdfDownload.BASE_URL + url.getPath().substring("/doi/abs/".length()));
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
