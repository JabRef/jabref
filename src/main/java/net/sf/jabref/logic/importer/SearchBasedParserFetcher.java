/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.logic.importer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.model.entry.BibEntry;

import org.jsoup.helper.StringUtil;

/**
 * Provides a convenient interface for search-based fetcher, which follow the usual three-step procedure:
 * 1. Open a URL based on the search query
 * 2. Parse the response to get a list of {@link BibEntry}
 * 3. Apply some {@link Formatter}
 */
public interface SearchBasedParserFetcher extends SearchBasedFetcher {

    /**
     * Constructs a URL based on the query.
     * @param query the search query
     */
    URL getQueryURL(String query) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    /**
     * Performs a cleanup of the fetched entry.
     *
     * Only systematic errors of the fetcher should be corrected here
     * (i.e. if information is consistently contained in the wrong field or the wrong format)
     * but not cosmetic issues which may depend on the user's taste (for example, LateX code vs HTML in the abstract).
     *
     * Try to reuse existing {@link Formatter} for the cleanup. For example,
     * {@code new FieldFormatterCleanup(FieldName.TITLE, new RemoveBracesFormatter()).cleanup(entry);}
     *
     * By default, no cleanup is done.
     * @param entry the entry to be cleaned-up
     */
    default void doPostCleanup(BibEntry entry) {
        // Do nothing
    }

    @Override
    default List<BibEntry> performSearch(String query) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        try (InputStream stream = new BufferedInputStream(getQueryURL(query).openStream())) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            // Post-cleanup
            fetchedEntries.forEach(this::doPostCleanup);

            return fetchedEntries;
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("An I/O exception occurred", e);
        } catch (ParserException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }
}
