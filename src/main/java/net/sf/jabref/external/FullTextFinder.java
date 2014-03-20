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
import java.io.IOException;

/**
 * This interface is used for classes that try to resolve a full-text PDF url from an article
 * web page. Implementing classes should specialize on specific article sites.
 *  */
public interface FullTextFinder {

    /**
     * Report whether this FullTextFinder works for the site providing the given URL.
     *
     * @param url The url to check.
     * @return true if the site is supported, false otherwise. If the site might be supported,
     *   it is best to return true.
     */
    public boolean supportsSite(URL url);

       /**
     * Take the source HTML for an article page, and try to find the URL to the
     * full text for this article.
     *
     * @param url The URL to the article's web page.
     * @return The fulltext PDF URL, if found, or null if not found.
     * @throws java.io.IOException
     */
    public URL findFullTextURL(URL url) throws IOException;
}
