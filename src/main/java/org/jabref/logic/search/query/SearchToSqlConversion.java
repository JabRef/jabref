package org.jabref.logic.search.query;

import org.jabref.model.search.ThrowingErrorListener;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;

public class SearchToSqlConversion {

    public static String searchToSql(String table, String searchExpression) {
        SearchParser.StartContext context = getStartContext(searchExpression);
        SearchToSqlVisitor searchToSqlVisitor = new SearchToSqlVisitor(table);
        return searchToSqlVisitor.visit(context);
    }

    private static SearchParser.StartContext getStartContext(String searchExpression) {
        SearchLexer lexer = new SearchLexer(new ANTLRInputStream(searchExpression));
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        SearchParser parser = new SearchParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancellationException on parse errors
        return parser.start();
    }

    @VisibleForTesting
    public static String getWhereClause(String table, String searchExpression) {
        SearchParser.StartContext context = getStartContext(searchExpression);
        SearchToSqlVisitor searchToSqlVisitor = new SearchToSqlVisitor(table);
        return searchToSqlVisitor.getWhereClause(context);
    }
}
