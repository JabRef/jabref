package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.List;

import org.jabref.model.search.SearchFlags;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.SearchFlags.CASE_INSENSITIVE;
import static org.jabref.model.search.SearchFlags.CASE_SENSITIVE;
import static org.jabref.model.search.SearchFlags.EXACT_MATCH;
import static org.jabref.model.search.SearchFlags.INEXACT_MATCH;
import static org.jabref.model.search.SearchFlags.NEGATION;
import static org.jabref.model.search.SearchFlags.REGULAR_EXPRESSION;

/**
 * Tests are located in {@link org.jabref.logic.search.query.SearchQueryFlagsConversionTest}.
 */
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
    public String visitStart(SearchParser.StartContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public String visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
        List<String> children = ctx.expression().stream().map(this::visit).toList();
        if (children.size() == 1) {
            return children.getFirst();
        }
        return String.join(" ", children);
    }

    @Override
    public String visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return "(" + visit(ctx.andExpression()) + ")";
    }

    @Override
    public String visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        return "NOT " + visit(ctx.expression());
    }

    @Override
    public String visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        return visit(ctx.left) + " " + ctx.bin_op.getText() + " " + visit(ctx.right);
    }

    @Override
    public String visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public String visitComparison(SearchParser.ComparisonContext ctx) {
        String term = ctx.searchValue().getText();

        // unfielded expression
        if (ctx.FIELD() == null) {
            return term;
        }

        // fielded expression
        EnumSet<SearchFlags> searchFlags = EnumSet.noneOf(SearchFlags.class);
        String field = ctx.FIELD().getText();
        int operator = ctx.operator().getStart().getType();

        searchFlags.add(isCaseSensitive ? CASE_SENSITIVE : CASE_INSENSITIVE);
        if (operator == SearchParser.NEQUAL) {
            searchFlags.add(NEGATION);
        }

        if (isRegularExpression) {
            searchFlags.add(REGULAR_EXPRESSION);
        } else {
            if (operator == SearchParser.EQUAL || operator == SearchParser.CONTAINS || operator == SearchParser.NEQUAL) {
                searchFlags.add(INEXACT_MATCH);
            } else if (operator == SearchParser.EEQUAL || operator == SearchParser.MATCHES) {
                searchFlags.add(EXACT_MATCH);
            }
        }
        return getFieldQueryNode(field, term, searchFlags);
    }

    private String getFieldQueryNode(String field, String term, EnumSet<SearchFlags> searchFlags) {
        String operator = getOperator(searchFlags);
        return field + " " + operator + " " + term;
    }

    private static String getOperator(EnumSet<SearchFlags> searchFlags) {
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
