package org.jabref.logic.search.query;

import java.util.Locale;

import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

/// Evaluates a Search.g4 parse tree against a group display name string.
/// Key behavioral difference from {@link SearchQueryVisitor}:
/// {@code visitImplicitAndExpression} uses OR semantics (anyMatch) instead of AND,
/// so space-separated bare terms like "machine learning" match any group containing
/// either word. Explicit AND/OR/NOT and parentheses work as expected.

public class GroupNameFilterVisitor extends SearchBaseVisitor<Boolean> {

    private final String groupName;

    public GroupNameFilterVisitor(String groupName) {
        // Lowercase for case-insensitive matching: typing "machine" matches "Machine Learning"
        this.groupName = groupName.toLowerCase(Locale.ROOT);
    }

    @Override
    public Boolean visitStart(SearchParser.StartContext ctx) {
        if (ctx.andExpression() == null) {
            return true;
        }
        return visit(ctx.andExpression());
    }

    @Override
    public Boolean visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
        if (ctx.expression().size() == 1) {
            return visit(ctx.expression().getFirst());
        }
        boolean allSimpleTerm = ctx.expression().stream()
                                   .allMatch(e -> e instanceof SearchParser.ComparisonExpressionContext);
        if (allSimpleTerm) {
            return ctx.expression().stream().anyMatch(this::visit);
        } else {
            return ctx.expression().stream().allMatch(this::visit);
        }
    }

    @Override
    public Boolean visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        boolean left = visit(ctx.left);
        boolean right = visit(ctx.right);
        return ctx.bin_op.getType() == SearchParser.AND
               ? left && right
               : left || right;
    }

    @Override
    public Boolean visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        return !visit(ctx.expression());
    }

    @Override
    public Boolean visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public Boolean visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public Boolean visitComparison(SearchParser.ComparisonContext ctx) {
        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue()).toLowerCase(Locale.ROOT);
        return groupName.contains(term);
    }
}
