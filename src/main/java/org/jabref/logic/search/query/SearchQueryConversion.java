package org.jabref.logic.search.query;

import java.util.List;

import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.model.search.query.SqlQueryNode;
import org.jabref.search.SearchParser;

import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchQueryConversion {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchQueryConversion.class);

    public static SqlQueryNode searchToSql(String table, SearchQuery searchQuery) {
        LOGGER.debug("Converting search expression to SQL: {}", searchQuery.getSearchExpression());
        return new SearchToSqlVisitor(table, searchQuery.getSearchFlags()).visit(searchQuery.getContext());
    }

    public static String flagsToSearchExpression(SearchQuery searchQuery) {
        LOGGER.debug("Converting search flags to search expression: {}, flags {}", searchQuery.getSearchExpression(), searchQuery.getSearchFlags());
        return new SearchFlagsToExpressionVisitor(searchQuery.getSearchFlags()).visit(searchQuery.getContext());
    }

    public static Query searchToLucene(SearchQuery searchQuery) {
        LOGGER.debug("Converting search expression to Lucene: {}", searchQuery.getSearchExpression());
        return new SearchToLuceneVisitor().visit(searchQuery.getContext());
    }

    public static List<SearchQueryNode> extractSearchTerms(SearchQuery searchQuery) {
        LOGGER.debug("Extracting search terms from search expression: {}", searchQuery.getSearchExpression());
        return new SearchQueryExtractorVisitor(searchQuery.getSearchFlags()).visit(searchQuery.getContext());
    }

    /**
     * Unescapes search value based on the Search grammar rules.
     * <p>
     * - STRING_LITERAL: Removes enclosing quotes and unescapes {@code \"}
     * <p>
     * - TERM: Unescapes {@code \=, \!, \~, \(, \)}
     */
    public static String unescapeSearchValue(SearchParser.SearchValueContext ctx) {
        if (ctx == null) {
            return "";
        }

        String term = ctx.getText();

        if (ctx.getStart().getType() == SearchParser.STRING_LITERAL) {
            return term.substring(1, term.length() - 1)
                       .replace("\\\"", "\"");
        }

        if (ctx.getStart().getType() == SearchParser.TERM) {
            return term.replaceAll("\\\\([=!~()])", "$1");
        }

        return term;
    }
}
