package org.jabref.logic.search.retrieval;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResult;
import org.jabref.model.search.query.SearchResults;
import org.jabref.model.search.query.SqlQueryNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgreConstants.ENTRY_ID;

public class BibFieldsSearcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibFieldsSearcher.class);

    private final Connection connection;
    private final String tableName;

    public BibFieldsSearcher(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    public boolean isMatched(BibEntry entry, SearchQuery searchQuery) {
        SearchQuery newSearchQuery = createBooleanQueryForEntry(entry, searchQuery);
        return search(newSearchQuery).isMatched(entry);
    }

    private static SearchQuery createBooleanQueryForEntry(BibEntry entry, SearchQuery oldSearchQuery) {
        String newSearchExpression = "( " + ENTRY_ID + "= " + entry.getId() + ") AND (" + oldSearchQuery.getSearchExpression() + " )";
        return new SearchQuery(newSearchExpression, oldSearchQuery.getSearchFlags());
    }

    public SearchResults search(SearchQuery searchQuery) {
        if (!searchQuery.isValid()) {
            return new SearchResults();
        }
        SqlQueryNode sqlQueryNode = SearchQueryConversion.searchToSql(tableName, searchQuery);
        SearchResults searchResults = new SearchResults();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQueryNode.cte())) {
            for (int i = 0; i < sqlQueryNode.params().size(); i++) {
                preparedStatement.setString(i + 1, sqlQueryNode.params().get(i));
            }
            LOGGER.debug("Executing search query: {}", preparedStatement);
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
