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

package net.sf.jabref.importer.fetcher;

import java.util.List;

import net.sf.jabref.logic.fetcher.FetcherException;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Searches web resources for bibliographic information based on a free-text query.
 * May return multiple search hits.
 */
public interface SearchBasedFetcher extends WebFetcher {

    /**
     * Looks for hits which are matched by the given free-text query.
     * This is the first step in the search procedure and we are mainly interested in the authors, title and the URL.
     *
     * @param query search string
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performShallowSearch(String query) throws FetcherException;

    /*
    /**
     * Enriches the given {@link BibEntry} (which is usually an entry found by
     * {@link #performShallowSearch(String)}) by additional information.
     *
     * @param entry the entry to be completed
     * @return a {@link BibEntry} containing the enriched bibliographic information
     */
    /*
    BibEntry performDeepSearch(BibEntry entry);
    */
}
