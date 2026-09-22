package org.jabref.logic.search.query;

import java.util.List;
import java.util.regex.Pattern;

import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.model.search.query.SqlQueryNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchQueryConversion {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchQueryConversion.class);
    private static final Pattern REGEX_META_CHARACTERS = Pattern.compile("([\\\\.\\[\\]{}()*+?^$|])");

    public static SqlQueryNode searchToSql(String table, SearchQuery searchQuery) {
        LOGGER.debug("Converting search expression to SQL: {}", searchQuery.getSearchExpression());
        return new SearchToSqlVisitor(table, searchQuery.getSearchFlags()).visit(searchQuery.getContext());
    }

    public static String flagsToSearchExpression(SearchQuery searchQuery) {
        LOGGER.debug("Converting search flags to search expression: {}, flags {}", searchQuery.getSearchExpression(), searchQuery.getSearchFlags());
        return new SearchFlagsToExpressionVisitor(searchQuery.getSearchFlags()).visit(searchQuery.getContext());
    }

    public static String searchToLucene(SearchQuery searchQuery) {
        LOGGER.debug("Converting search expression to Lucene: {}", searchQuery.getSearchExpression());
        return new SearchToLuceneVisitor(searchQuery.getSearchFlags()).visit(searchQuery.getContext());
    }

    public static List<SearchQueryNode> extractSearchTerms(SearchQuery searchQuery) {
        LOGGER.debug("Extracting search terms from search expression: {}", searchQuery.getSearchExpression());
        return new SearchQueryExtractorVisitor(searchQuery.getSearchFlags()).visit(searchQuery.getContext());
    }

    /// Escapes a literal term so it can be used as a regular expression in both Java and PostgreSQL.
    public static String escapeRegexLiteral(String term) {
        return REGEX_META_CHARACTERS.matcher(term).replaceAll("\\\\$1");
    }
}
