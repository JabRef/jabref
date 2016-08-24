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
