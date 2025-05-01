package org.jabref.logic.search.query;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

/**
 * Tests are located in {@link org.jabref.logic.search.query.SearchQueryExtractorConversionTest}.
 */
public class SearchQueryExtractorVisitor extends SearchBaseVisitor<List<SearchQueryNode>> {

    private final boolean searchBarRegex;
    private boolean isNegated = false;

    public SearchQueryExtractorVisitor(EnumSet<SearchFlags> searchFlags) {
        searchBarRegex = searchFlags.contains(SearchFlags.REGULAR_EXPRESSION);
    }

    @Override
    public List<SearchQueryNode> visitStart(SearchParser.StartContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public List<SearchQueryNode> visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
        List<List<SearchQueryNode>> children = ctx.expression().stream().map(this::visit).toList();
        if (children.size() == 1) {
            return children.getFirst();
        } else {
            List<SearchQueryNode> terms = new ArrayList<>();
            for (List<SearchQueryNode> child : children) {
                terms.addAll(child);
            }
            return terms;
        }
    }

    @Override
    public List<SearchQueryNode> visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        isNegated = !isNegated;
        List<SearchQueryNode> terms = visit(ctx.expression());
        isNegated = !isNegated;
        return terms;
    }

    @Override
    public List<SearchQueryNode> visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        List<SearchQueryNode> terms = new ArrayList<>();
        terms.addAll(visit(ctx.left));
        terms.addAll(visit(ctx.right));
        return terms;
    }

    @Override
    public List<SearchQueryNode> visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public List<SearchQueryNode> visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public List<SearchQueryNode> visitComparison(SearchParser.ComparisonContext ctx) {
        // ignore negated comparisons
        if (isNegated) {
            return List.of();
        }
        if (ctx.operator() != null) {
            int operator = ctx.operator().getStart().getType();
            if (operator == SearchParser.NEQUAL
                    || operator == SearchParser.NCEQUAL
                    || operator == SearchParser.NEEQUAL
                    || operator == SearchParser.NCEEQUAL
                    || operator == SearchParser.NREQUAL
                    || operator == SearchParser.NCREEQUAL) {
                return List.of();
            }
        }
        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue());

        // if not regex, escape the backslashes, because the highlighter uses regex

        // unfielded terms, check the search bar flags
        if (ctx.FIELD() == null) {
            if (!searchBarRegex) {
                term = term.replace("\\", "\\\\");
            }
            return List.of(new SearchQueryNode(Optional.empty(), term));
        }

        String field = ctx.FIELD().getText().toLowerCase(Locale.ROOT);

        // Pseudo-fields
        field = switch (field) {
            case "key" -> InternalField.KEY_FIELD.getName();
            case "anykeyword" -> StandardField.KEYWORDS.getName();
            case "anyfield" -> "any";
            default -> field;
        };

        if (ctx.operator() != null) {
            int operator = ctx.operator().getStart().getType();
            if (operator != SearchParser.REQUAL && operator != SearchParser.CREEQUAL) {
                term = term.replace("\\", "\\\\");
            }
        }

        if ("any".equals(field)) {
            return List.of(new SearchQueryNode(Optional.empty(), term));
        }
        return List.of(new SearchQueryNode(Optional.of(FieldFactory.parseField(field)), term));
    }
}
