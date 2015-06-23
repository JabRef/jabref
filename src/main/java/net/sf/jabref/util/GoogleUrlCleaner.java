/*  Copyright (C) 2003-2013 JabRef contributors.
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
package net.sf.jabref.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Class containing method(s) for cleaning URL returned by Google search.
 * E.g. If you search for the "The String-to-String Correction Problem", Google
 * will return a list of web pages associated with that text. If you copy any 
 * link that search returned, you will have access to the link "enriched"
 * with many meta data. 
 * E.g. instead link http://dl.acm.org/citation.cfm?id=321811
 * in your clipboard you will have this link:
 *  https://www.google.hr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&ved=0CC8QFjAA&url=http%3A%2F%2Fdl.acm.org%2Fcitation.cfm%3Fid%3D321811&ei=L2_RUcj6HsfEswa7joGwBw&usg=AFQjCNEBJPUimu-bAns6lSLe-kszz4AiGA&sig2=tj9c5x62ioFHkQTKfwkj0g&bvm=bv.48572450,d.Yms
 *
 * Using methods of this class, "dirty" link will be cleaned.
 *
 * Created by Krunoslav Zubrinic, July 2013.
 */
public class GoogleUrlCleaner {

    /*
        public static void main(String[] args) {
            System.out.println(cleanUrl("https://www.google.hr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&ved=0CC0QFjAA&url=http%3A%2F%2Fdl.acm.org%2Fcitation.cfm%3Fid%3D321811&ei=gHDRUa-IKobotQbMy4GAAg&usg=AFQjCNEBJPUimu-bAns6lSLe-kszz4AiGA&sig2=DotF0pIZD8OhjDcSHPlBbQ"));
            System.out.println(cleanUrl("http://dl.acm.org/citation.cfm?id=321811"));
            System.out.println(cleanUrl("test text"));
            System.out.println(cleanUrl(" "));
            System.out.println(cleanUrl(""));
            System.out.println(cleanUrl(null));
       }
    */

    // clean Google URL
    public static String cleanUrl(String dirty) {
        if ((dirty == null) || (dirty.length() == 0)) {
            return dirty;
        }
        try {
            URL u = new URL(dirty);
            // read URL parameters
            String query = u.getQuery();
            // if there is no parameters
            if (query == null) {
                return dirty;
            }
            // split parameters
            String[] pairs = query.split("&");
            if (pairs == null) {
                return dirty;
            }
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                // "clean" url is decoded value of "url" parameter
                if (pair.substring(0, idx).equals("url")) {
                    return URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                }
            }
        } catch (MalformedURLException e) {
            return dirty;
        } catch (UnsupportedEncodingException e) {
            return dirty;
        }
        return dirty;
    }

}
