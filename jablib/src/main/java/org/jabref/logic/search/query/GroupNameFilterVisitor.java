package org.jabref.logic.search.query;

import java.util.Locale;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.misc.ParseCancellationException;

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
        // Determine whether this is a simple term or a fielded/operator comparison.
        // If the full node text differs from the searchValue text, we assume the
        // presence of a field and/or operator (e.g., "name != learning").
        String fullText = ctx.getText();
        String valueText = ctx.searchValue().getText();

        if (!fullText.equals(valueText)) {
            // Fielded/operator comparisons are not supported by this visitor.
            // Fall back to treating the entire comparison as a plain-text term,
            // to avoid misinterpreting operators like "!=" as a positive match.
            String plain = fullText.toLowerCase(Locale.ROOT);
            return groupName.contains(plain);
        }

        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue()).toLowerCase(Locale.ROOT);
        return groupName.contains(term);
    }

    /// Implemented a static method that checks whether the group name matches the given query string.
    public static boolean matches(String groupName, String query) {
        if (StringUtil.isBlank(query)) {
            return true;
        }
        try {
            SearchParser.StartContext ctx = SearchQuery.getStartContext(query);
            return new GroupNameFilterVisitor(groupName).visit(ctx);
        } catch (ParseCancellationException e) {
            return StringUtil.containsIgnoreCase(groupName, query);
        }
    }
}
