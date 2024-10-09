package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.Set;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.ThrowingErrorListener;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchQueryConversion {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchQueryConversion.class);

    public static String searchToSql(String table, String searchExpression) {
        LOGGER.debug("Converting search expression to SQL: {}", searchExpression);
        SearchParser.StartContext context = getStartContext(searchExpression);
        SearchToSqlVisitor searchToSqlVisitor = new SearchToSqlVisitor(table);
        return searchToSqlVisitor.visit(context);
    }

    public static String flagsToSearchExpression(String searchExpression, EnumSet<SearchFlags> searchFlags) {
        LOGGER.debug("Converting search flags to search expression: {}, flags {}", searchExpression, searchFlags);
        SearchParser.StartContext context = getStartContext(searchExpression);
        return new SearchFlagsToExpressionVisitor(searchFlags).visit(context);
    }

    public static Query searchToLucene(String searchExpression) {
        LOGGER.debug("Converting search expression to Lucene: {}", searchExpression);
        SearchParser.StartContext context = getStartContext(searchExpression);
        return new SearchToLuceneVisitor().visit(context);
    }

    public static Set<String> extractSearchTerms(String searchExpression) {
        LOGGER.debug("Extracting search terms from search expression: {}", searchExpression);
        SearchParser.StartContext context = getStartContext(searchExpression);
        return new SearchQueryExtractorVisitor().visit(context);
    }

    public static SearchParser.StartContext getStartContext(String searchExpression) {
        SearchLexer lexer = new SearchLexer(CharStreams.fromString(searchExpression));
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        SearchParser parser = new SearchParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancellationException on parse errors
        return parser.start();
    }
}
