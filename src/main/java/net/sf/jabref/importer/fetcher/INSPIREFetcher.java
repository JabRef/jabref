/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.importer.fetcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * This class allows to access the Slac INSPIRE database. It is just a port of the original SPIRES Fetcher.
 *
 * It can either be a GeneralFetcher to pose requests to the database or fetch individual entries.
 *
 * @author Fedor Bezrukov
 * @author Sheer El-Showk
 *
 * @version $Id$
 *
 */
public class INSPIREFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(INSPIREFetcher.class);

    private static final String INSPIRE_HOST = "inspirehep.net";


    protected static String getInspireHost() {
        return INSPIRE_HOST;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    /**
     * Constructs a INSPIRE query url from slaccitation field
     *
     * @param slaccitation
     * @return query string
     *
     *         public static String constructUrlFromSlaccitation(String slaccitation) { String cmd = "j"; String key =
     *         slaccitation.replaceAll("^%%CITATION = ", "").replaceAll( ";%%$", ""); if (key.matches("^\\w*-\\w*[ /].*"
     *         )) cmd = "eprint"; try { key = URLEncoder.encode(key, "UTF-8"); } catch (UnsupportedEncodingException e)
     *         { } StringBuffer sb = new StringBuffer("http://").append(INSPIRE_HOST) .append("/");
     *         sb.append("spires/find/hep/www").append("?"); sb.append("rawcmd=find+").append(cmd).append("+");
     *         sb.append(key); return sb.toString(); }
     *
     *         /** Construct an INSPIRE query url from eprint field
     *
     * @param eprint
     * @return query string
     *
     *         public static String constructUrlFromEprint(String eprint) { String key = eprint.replaceAll(" [.*]$",
     *         ""); try { key = URLEncoder.encode(key, "UTF-8"); } catch (UnsupportedEncodingException e) { return ""; }
     *         StringBuffer sb = new StringBuffer("http://").append(INSPIRE_HOST) .append("/");
     *         sb.append("spires/find/hep/www").append("?"); sb.append("rawcmd=find+eprint+"); sb.append(key); return
     *         sb.toString(); }
     */

    @Override
    public String getTitle() {
        return "INSPIRE";
    }

    @Override
    public void stopFetching() {
        // Do nothing
    }

}
