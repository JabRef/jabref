package org.jabref.logic.search.sqlbased;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.util.Pair;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.LibrarySearcher;
import org.jabref.logic.search.inmemory.MatchInformation;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// SQL/Lucene-backed implementation of [LibrarySearcher].
/// Boots an [IndexManager] (embedded Postgres + Lucene linked-files index) per instance.
public class SqlBasedLibrarySearcher implements LibrarySearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBasedLibrarySearcher.class);

    private final BibDatabaseContext databaseContext;
    private final IndexManager indexManager;

    public SqlBasedLibrarySearcher(BibDatabaseContext databaseContext,
                                   TaskExecutor taskExecutor,
                                   CliPreferences preferences,
                                   PostgresServer postgresServer) throws IOException {
        this.databaseContext = databaseContext;
        this.indexManager = new IndexManager(databaseContext, taskExecutor, preferences, postgresServer);
    }

    @Override
    public List<BibEntry> getMatches(SearchQuery query) {
        LOGGER.debug("Search term: {}", query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: invalid search expression");
            indexManager.closeAndWait();
            return List.of();
        }
        List<BibEntry> matchEntries = indexManager.search(query)
                                                  .getMatchedEntries()
                                                  .stream()
                                                  .map(entryId -> databaseContext.getDatabase().getEntryById(entryId))
                                                  .flatMap(Optional::stream)
                                                  .toList();
        indexManager.closeAndWait();
        return BibDatabases.purgeEmptyEntries(matchEntries);
    }

    public Map<BibEntry, Optional<MatchInformation>> getDetailedMatches(SearchQuery query) {
        LOGGER.debug("Search term: {}", query);

        if (!query.isValid()) {
            LOGGER.warn("Search failed: invalid search expression");
            indexManager.closeAndWait();
            return new HashMap<>();
        }
        Map<BibEntry, Optional<MatchInformation>> matchEntries = indexManager.search(query)
                                                                             .getDetailedMatchedEntries()
                                                                             .entrySet()
                                                                             .stream()
                                                                             .map(entry ->
                                                                                     new Pair<>(databaseContext.getDatabase().getEntryById(entry.getKey()), entry.getValue()))
                                                                             .filter(pair -> pair.getKey().isPresent())
                                                                             .collect(Collectors.toMap(entry ->
                                                                                     entry.getKey().get(), Pair::getValue));
        indexManager.closeAndWait();
        return matchEntries;
    }
}
