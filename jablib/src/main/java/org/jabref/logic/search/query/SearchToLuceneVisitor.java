package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.jabref.model.search.LinkedFilesConstants;
import org.jabref.model.search.SearchFlags;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.apache.lucene.queryparser.classic.QueryParser;

/// Tests are located in `org.jabref.logic.search.query.SearchQueryLuceneConversionTest`.
public class SearchToLuceneVisitor extends SearchBaseVisitor<String> {
    private final EnumSet<SearchFlags> searchFlags;

    public SearchToLuceneVisitor(EnumSet<SearchFlags> searchFlags) {
        this.searchFlags = searchFlags;
    }

    @Override
    public String visitStart(SearchParser.StartContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public String visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
        List<String> children = ctx.expression().stream().map(this::visit).toList();
        return children.size() == 1 ? children.getFirst() : String.join(" ", children);
    }

    @Override
    public String visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        String expr = visit(ctx.andExpression());
        return expr.isEmpty() ? "" : "(" + expr + ")";
    }

    @Override
    public String visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        return "NOT (" + visit(ctx.expression()) + ")";
    }

    @Override
    public String visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        String left = visit(ctx.left);
        String right = visit(ctx.right);

        if (left.isEmpty() && right.isEmpty()) {
            return "";
        }
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }

        String operator = ctx.bin_op.getType() == SearchParser.AND ? " AND " : " OR ";
        return left + operator + right;
    }

    @Override
    public String visitComparison(SearchParser.ComparisonContext ctx) {
        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue());
        boolean isQuoted = ctx.searchValue().getStart().getType() == SearchParser.STRING_LITERAL;

        // unfielded expression
        if (ctx.FIELD() == null) {
            if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
                return "/" + term + "/";
            }
            return isQuoted ? "\"" + escapeQuotes(term) + "\"" : QueryParser.escape(term);
        }

        String field = ctx.FIELD().getText().toLowerCase(Locale.ROOT);
        if (!isValidField(field)) {
            return "";
        }

        int operator = ctx.operator().getStart().getType();

        if ("content".equals(field)
                && !isQuoted
                && !searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)
                && !isRegexOperator(operator)
                && !isExactMatchOperator(operator)) {

            field = field + ":";
            return buildContentWildcardQuery(field, term, operator);
        }

        field = "any".equals(field) || "anyfield".equals(field) ? "" : field + ":";
        return buildFieldExpression(field, term, operator, isQuoted);
    }

    private boolean isValidField(String field) {
        return "any".equals(field) || "anyfield".equals(field) || LinkedFilesConstants.PDF_FIELDS.contains(field);
    }

    private String buildFieldExpression(String field, String term, int operator, boolean isQuoted) {
        boolean isRegexOp = isRegexOperator(operator);
        boolean isNegationOp = isNegationOperator(operator);

        if (isRegexOp) {
            String expression = field + "/" + term + "/";
            return isNegationOp ? "NOT " + expression : expression;
        } else {
            term = isQuoted ? "\"" + escapeQuotes(term) + "\"" : QueryParser.escape(term);
            String expression = field + term;
            return isNegationOp ? "NOT " + expression : expression;
        }
    }

    private String buildContentWildcardQuery(String field, String term, int operator) {
        boolean isNegationOp = isNegationOperator(operator);
        String escapedTerm = QueryParser.escape(term);

        if (!escapedTerm.contains("*") && !escapedTerm.contains("?")) {
            escapedTerm = "*" + escapedTerm + "*";
        }

        String expression = field + escapedTerm;
        return isNegationOp ? "NOT " + expression : expression;
    }

    private static String escapeQuotes(String term) {
        return term.replace("\"", "\\\"");
    }

    private static boolean isNegationOperator(int operator) {
        return switch (operator) {
            case SearchParser.NEQUAL,
                 SearchParser.NCEQUAL,
                 SearchParser.NEEQUAL,
                 SearchParser.NCEEQUAL,
                 SearchParser.NREQUAL,
                 SearchParser.NCREEQUAL ->
                    true;
            default ->
                    false;
        };
    }

    private static boolean isRegexOperator(int operator) {
        return switch (operator) {
            case SearchParser.REQUAL,
                 SearchParser.CREEQUAL,
                 SearchParser.NREQUAL,
                 SearchParser.NCREEQUAL ->
                    true;
            default ->
                    false;
        };
    }

    private boolean isExactMatchOperator(int operator) {
        return switch (operator) {
            case SearchParser.EEQUAL,   // ==
                 SearchParser.CEEQUAL,  // ==!
                 SearchParser.NEEQUAL,  // !==
                 SearchParser.NCEEQUAL, // !==!
                 SearchParser.MATCHES   // MATCHES
                    -> true;
            default -> false;
        };
    }
}
