package org.jabref.logic.search;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSearcher.class);
    private final SearchQuery query;

    private final BibDatabase database;

    public DatabaseSearcher(SearchQuery query, BibDatabase database) {
        this.query = Objects.requireNonNull(query);
        this.database = Objects.requireNonNull(database);
    }

    public List<BibEntry> getMatches() {
        LOGGER.debug("Search term: " + query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: illegal search expression");
            return Collections.emptyList();
        }

        List<BibEntry> matchEntries = database.getEntries().stream().filter(query::isMatch).collect(Collectors.toList());
        return BibDatabases.purgeEmptyEntries(matchEntries);
    }
}
