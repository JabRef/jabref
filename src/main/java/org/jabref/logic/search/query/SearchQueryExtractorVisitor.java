package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jabref.model.search.SearchFlags;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

/**
 * Tests are located in {@link org.jabref.logic.search.query.SearchQueryExtractorConversionTest}.
 */
public class SearchQueryExtractorVisitor extends SearchBaseVisitor<Set<String>> {

    private final boolean searchBarRegex;
    private boolean isNegated = false;

    public SearchQueryExtractorVisitor(EnumSet<SearchFlags> searchFlags) {
        searchBarRegex = searchFlags.contains(SearchFlags.REGULAR_EXPRESSION);
    }

    @Override
    public Set<String> visitStart(SearchParser.StartContext ctx) {
        return visit(ctx.orExpression());
    }

    @Override
    public Set<String> visitImplicitOrExpression(SearchParser.ImplicitOrExpressionContext ctx) {
        List<Set<String>> children = ctx.expression().stream().map(this::visit).toList();
        if (children.size() == 1) {
            return children.getFirst();
        } else {
            Set<String> terms = new HashSet<>();
            for (Set<String> child : children) {
                terms.addAll(child);
            }
            return terms;
        }
    }

    @Override
    public Set<String> visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        isNegated = !isNegated;
        Set<String> terms = visit(ctx.expression());
        isNegated = !isNegated;
        return terms;
    }

    @Override
    public Set<String> visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        Set<String> terms = new HashSet<>();
        terms.addAll(visit(ctx.left));
        terms.addAll(visit(ctx.right));
        return terms;
    }

    @Override
    public Set<String> visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.orExpression());
    }

    @Override
    public Set<String> visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public Set<String> visitComparison(SearchParser.ComparisonContext ctx) {
        if (isNegated) {
            return Set.of();
        }
        if (ctx.operator() != null) {
            int operator = ctx.operator().getStart().getType();
            if (operator == SearchParser.NEQUAL
                || operator == SearchParser.NCEQUAL
                || operator == SearchParser.NEEQUAL
                || operator == SearchParser.NCEEQUAL
                || operator == SearchParser.NREQUAL
                || operator == SearchParser.NCREEQUAL) {
                return Set.of();
            }
        }
        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue());

        // if not regex, escape the backslashes, because the highlighter uses regex

        // unfielded terms, check the search bar flags
        if (ctx.FIELD() == null && !searchBarRegex) {
            return Set.of(term.replace("\\", "\\\\"));
        } else if (ctx.operator() != null) {
            int operator = ctx.operator().getStart().getType();
            if (operator != SearchParser.REQUAL && operator != SearchParser.CREEQUAL) {
                return Set.of(term.replace("\\", "\\\\"));
            }
        }
        return Set.of(term);
    }
}
