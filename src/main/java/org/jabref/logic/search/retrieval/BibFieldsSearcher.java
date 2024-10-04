package org.jabref.logic.search.retrieval;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jabref.model.search.query.SearchResult;
import org.jabref.model.search.query.SearchResults;
import org.jabref.model.search.query.SqlSearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibFieldsSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibFieldsSearcher.class);

    private final Connection connection;

    public BibFieldsSearcher(Connection connection) {
        this.connection = connection;
    }

    public SearchResults search(SqlSearchQuery searchQuery) {
        LOGGER.debug("Searching in bib fields with query: {}", searchQuery.getQuery());
        SearchResults searchResults = new SearchResults();
        try (PreparedStatement preparedStatement = connection.prepareStatement(searchQuery.getQuery())) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String entryId = resultSet.getString(1);
                searchResults.addSearchResult(entryId, new SearchResult());
            }
        } catch (SQLException e) {
            LOGGER.error("Error during bib fields search execution", e);
        }
        return searchResults;
    }
}
