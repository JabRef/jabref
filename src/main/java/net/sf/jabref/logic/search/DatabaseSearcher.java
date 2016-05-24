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
package net.sf.jabref.logic.search;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabases;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Silberer, Zirn
 */
public class DatabaseSearcher {

    private final SearchQuery query;
    private final BibDatabase database;

    private static final Log LOGGER = LogFactory.getLog(DatabaseSearcher.class);

    public DatabaseSearcher(SearchQuery query, BibDatabase database) {
        this.query = Objects.requireNonNull(query);
        this.database = Objects.requireNonNull(database);
    }

    /**
     *
     * @return BibDatabase, never null
     */
    public BibDatabase getDatabaseFromMatches() {
        LOGGER.debug("Search term: " + query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: illegal search expression");
            return BibDatabases.createDatabase(Collections.emptyList());
        }

        List<BibEntry> matchEntries = database.getEntries().stream().filter(query::isMatch).collect(Collectors.toList());

        return BibDatabases.createDatabase(BibDatabases.purgeEmptyEntries(matchEntries));
    }

}
