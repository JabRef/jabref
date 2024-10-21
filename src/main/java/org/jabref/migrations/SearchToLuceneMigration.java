package org.jabref.migrations;

import java.io.IOException;

import org.jabref.model.search.ThrowingErrorListener;
import org.jabref.search.SearchLexer;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;

public class SearchToLuceneMigration {
    public static String migrateToLuceneSyntax(String searchExpression, boolean isRegularExpression) throws IOException {
        SearchParser.StartContext context = getStartContext(searchExpression);
        SearchToLuceneVisitor searchToLuceneVisitor = new SearchToLuceneVisitor(isRegularExpression);
        QueryNode luceneQueryNode = searchToLuceneVisitor.visit(context);
        return luceneQueryNode.toQueryString(new EscapeQuerySyntaxImpl()).toString();
    }

    private static SearchParser.StartContext getStartContext(String searchExpression) {
        SearchLexer lexer;
        try {
            lexer = new SearchLexer(CharStreams.fromFileName(searchExpression));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lexer.removeErrorListeners(); // no infos on file system
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        SearchParser parser = new SearchParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); // no infos on file system
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy()); // ParseCancellationException on parse errors
        return parser.start();
    }
}
