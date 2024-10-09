package org.jabref.logic.search.query;

import java.util.HashSet;
import java.util.Set;

import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

public class SearchQueryExtractorVisitor extends SearchBaseVisitor<Set<String>> {

    private final Set<String> searchTerms = new HashSet<>();
    private boolean isNegated = false;

    @Override
    public Set<String> visitStart(SearchParser.StartContext ctx) {
        visit(ctx.expression());
        return searchTerms;
    }

    @Override
    public Set<String> visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        isNegated = !isNegated;
        visit(ctx.expression());
        isNegated = !isNegated;
        return searchTerms;
    }

    @Override
    public Set<String> visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        visit(ctx.left);
        visit(ctx.right);
        return searchTerms;
    }

    @Override
    public Set<String> visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Set<String> visitAtomExpression(SearchParser.AtomExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public Set<String> visitComparison(SearchParser.ComparisonContext context) {
        if (isNegated ||
            context.NEQUAL() != null ||
            context.NCEQUAL() != null ||
            context.NEEQUAL() != null ||
            context.NCEEQUAL() != null ||
            context.NREQUAL() != null ||
            context.NCREEQUAL() != null) {
            return searchTerms;
        }

        String right = context.right.getText();
        if (right.startsWith("\"") && right.endsWith("\"")) {
            right = right.substring(1, right.length() - 1);
        }
        searchTerms.add(right);

        return searchTerms;
    }
}
