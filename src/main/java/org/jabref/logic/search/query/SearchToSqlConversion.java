package org.jabref.logic.search.query;

import org.jabref.model.search.ThrowingErrorListener;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchToSqlConversion {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchToSqlConversion.class);

    public static String searchToSql(String table, String searchExpression) {
        LOGGER.debug("Converting search expression to SQL: {}", searchExpression);
        SearchParser.StartContext context = getStartContext(searchExpression);
        SearchToSqlVisitor searchToSqlVisitor = new SearchToSqlVisitor(table);
        return searchToSqlVisitor.visit(context);
    }

    private static SearchParser.StartContext getStartContext(String searchExpression) {
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
