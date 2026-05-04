package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.NotNode;
import org.jabref.model.search.query.OperatorNode;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

public class SearchQueryVisitor extends SearchBaseVisitor<BaseQueryNode> {

    private final boolean searchBarRegex;

    public SearchQueryVisitor(EnumSet<SearchFlags> searchFlags) {
        searchBarRegex = searchFlags.contains(SearchFlags.REGULAR_EXPRESSION);
    }

    @Override
    public BaseQueryNode visitStart(SearchParser.StartContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public BaseQueryNode visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
        List<BaseQueryNode> children = ctx.expression().stream()
                                          .map(this::visit)
                                          .collect(Collectors.toList());
        if (children.size() == 1) {
            return children.getFirst();
        }
        return new OperatorNode(OperatorNode.Operator.AND, children);
    }

    @Override
    public BaseQueryNode visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        BaseQueryNode negated = visit(ctx.expression());
        return new NotNode(negated);
    }

    @Override
    public BaseQueryNode visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        BaseQueryNode left = visit(ctx.left);
        BaseQueryNode right = visit(ctx.right);

        // Check the actual operator token
        if (ctx.bin_op.getType() == SearchParser.AND) {
            return new OperatorNode(OperatorNode.Operator.AND, List.of(left, right));
        } else { // Assuming the only other binary op is OR
            return new OperatorNode(OperatorNode.Operator.OR, List.of(left, right));
        }
    }

    @Override
    public BaseQueryNode visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public BaseQueryNode visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public BaseQueryNode visitComparison(SearchParser.ComparisonContext ctx) {
        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue());

        // unfielded terms, check the search bar flags
        if (ctx.FIELD() == null) {
            if (!searchBarRegex) {
                term = term.replace("\\", "\\\\");
            }
            return new SearchQueryNode(Optional.empty(), term);
        }

        // TODO: Here, there is no unescaping of the term (e.g., field\=thing=value does not work as expected)
        String field = ctx.FIELD().getText().toLowerCase(Locale.ROOT);

        // Pseudo-fields
        field = switch (field) {
            case "key" ->
                    InternalField.KEY_FIELD.getName();
            case "anykeyword" ->
                    StandardField.KEYWORDS.getName();
            case "anyfield" ->
                    "any";
            default ->
                    field;
        };

        if (ctx.operator() != null) {
            int operator = ctx.operator().getStart().getType();
            if (operator != SearchParser.REQUAL && operator != SearchParser.CREEQUAL) {
                term = term.replace("\\", "\\\\");
            }
        }

        if ("any".equals(field)) {
            return new SearchQueryNode(Optional.empty(), term);
        }

        if (ctx.operator() != null) {
            int operator = ctx.operator().getStart().getType();
            if (operator == SearchParser.NEQUAL
                    || operator == SearchParser.NCEQUAL
                    || operator == SearchParser.NEEQUAL
                    || operator == SearchParser.NCEEQUAL
                    || operator == SearchParser.NREQUAL
                    || operator == SearchParser.NCREEQUAL) {
                // All of these will be treated as !=
                SearchQueryNode negatedNode = new SearchQueryNode(Optional.of(FieldFactory.parseField(field)), term);
                return new NotNode(negatedNode);
            }
        }
        return new SearchQueryNode(Optional.of(FieldFactory.parseField(field)), term);
    }
}

