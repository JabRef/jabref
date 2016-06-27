/*  Copyright (C) 2015 Oscar Gustafsson.
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

public class SpringerFetcher implements EntryFetcher {

    private static final String API_URL = "http://api.springer.com/metadata/json?q=";
    private static final String API_KEY = "b0c7151179b3d9c1119cf325bca8460d";
    private static final Log LOGGER = LogFactory.getLog(SpringerFetcher.class);
    private static final int MAX_PER_PAGE = 100;

    private boolean shouldContinue;


    protected static String getApiUrl() {
        return API_URL;
    }

    protected static String getApiKey() {
        return API_KEY;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    protected static int getMaxPerPage() {
        return MAX_PER_PAGE;
    }

    protected boolean shouldContinue() {
        return shouldContinue;
    }

    protected void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }



    @Override
    public String getTitle() {
        return "Springer";
    }
}
