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

import net.sf.jabref.importer.fileformat.JSONEntryParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DOAJFetcher implements EntryFetcher {

    private static final String SEARCH_URL = "https://doaj.org/api/v1/search/articles/";
    private static final Log LOGGER = LogFactory.getLog(DOAJFetcher.class);
    private static final int MAX_PER_PAGE = 100;
    private final JSONEntryParser jsonConverter = new JSONEntryParser();

    private boolean shouldContinue;


    protected static String getSearchUrl() {
        return SEARCH_URL;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    protected static int getMaxPerPage() {
        return MAX_PER_PAGE;
    }

    protected JSONEntryParser getJsonConverter() {
        return jsonConverter;
    }

    protected boolean isShouldContinue() {
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
        return "DOAJ";
    }

}
