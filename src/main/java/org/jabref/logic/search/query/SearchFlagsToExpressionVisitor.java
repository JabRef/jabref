package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.Optional;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchTermFlag;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.query.SearchTermFlag.CASE_INSENSITIVE;
import static org.jabref.model.search.query.SearchTermFlag.CASE_SENSITIVE;
import static org.jabref.model.search.query.SearchTermFlag.EXACT_MATCH;
import static org.jabref.model.search.query.SearchTermFlag.INEXACT_MATCH;
import static org.jabref.model.search.query.SearchTermFlag.NEGATION;
import static org.jabref.model.search.query.SearchTermFlag.REGULAR_EXPRESSION;

public class SearchFlagsToExpressionVisitor extends SearchBaseVisitor<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchFlagsToExpressionVisitor.class);

    private final boolean isCaseSensitive;
    private final boolean isRegularExpression;

    public SearchFlagsToExpressionVisitor(EnumSet<SearchFlags> searchFlags) {
        LOGGER.debug("Converting search flags to search expression: {}", searchFlags);
        this.isCaseSensitive = searchFlags.contains(SearchFlags.CASE_SENSITIVE);
        this.isRegularExpression = searchFlags.contains(SearchFlags.REGULAR_EXPRESSION);
    }

    @Override
    public String visitStart(SearchParser.StartContext context) {
        return visit(context.expression());
    }

    @Override
    public String visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return "(" + visit(ctx.expression()) + ")";
    }

    @Override
    public String visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        return "NOT " + visit(ctx.expression());
    }

    @Override
    public String visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        return visit(ctx.left) + " " + ctx.operator.getText() + " " + visit(ctx.right);
    }

    public String visitComparison(SearchParser.ComparisonContext context) {
        String right = context.right.getText();

        Optional<SearchParser.NameContext> fieldDescriptor = Optional.ofNullable(context.left);
        EnumSet<SearchTermFlag> termFlags = EnumSet.noneOf(SearchTermFlag.class);

        if (fieldDescriptor.isPresent()) {
            String field = fieldDescriptor.get().getText();

            termFlags.add(isCaseSensitive ? CASE_SENSITIVE : CASE_INSENSITIVE);
            if (context.NEQUAL() != null) {
                termFlags.add(NEGATION);
            }

            if (isRegularExpression) {
                termFlags.add(REGULAR_EXPRESSION);
            } else {
                if (context.EQUAL() != null || context.CONTAINS() != null || context.NEQUAL() != null) {
                    termFlags.add(INEXACT_MATCH);
                } else if (context.EEQUAL() != null || context.MATCHES() != null) {
                    termFlags.add(EXACT_MATCH);
                }
            }
            return getFieldQueryNode(field, right, termFlags);
        } else {
            termFlags.add(isCaseSensitive ? CASE_SENSITIVE : CASE_INSENSITIVE);
            if (isRegularExpression) {
                termFlags.add(REGULAR_EXPRESSION);
            } else {
                termFlags.add(INEXACT_MATCH);
            }
            return getFieldQueryNode("any", right, termFlags);
        }
    }

    private String getFieldQueryNode(String field, String term, EnumSet<SearchTermFlag> searchFlags) {
        String operator = getOperator(searchFlags);
        return field + " " + operator + " " + term;
    }

    private static String getOperator(EnumSet<SearchTermFlag> searchFlags) {
        StringBuilder operator = new StringBuilder();

        if (searchFlags.contains(NEGATION)) {
            operator.append("!");
        }

        if (searchFlags.contains(INEXACT_MATCH)) {
            operator.append("=");
        } else if (searchFlags.contains(EXACT_MATCH)) {
            operator.append("==");
        } else if (searchFlags.contains(REGULAR_EXPRESSION)) {
            operator.append("=~");
        }

        if (searchFlags.contains(CASE_SENSITIVE)) {
            operator.append("!");
        }

        return operator.toString();
    }
}
