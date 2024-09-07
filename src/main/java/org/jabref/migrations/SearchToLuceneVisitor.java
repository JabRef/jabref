package org.jabref.migrations;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.RegexpQueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to a Lucene index with the assumption that the ngram analyzer is used.
 *
 * Tests are located in {@link org.jabref.migrations.SearchToLuceneMigrationTest}.
 */
public class SearchToLuceneVisitor extends SearchBaseVisitor<QueryNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchToLuceneVisitor.class);

    private final boolean isRegularExpression;

    public SearchToLuceneVisitor(boolean isRegularExpression) {
        this.isRegularExpression = isRegularExpression;
    }

    @Override
    public QueryNode visitStart(SearchParser.StartContext ctx) {
        QueryNode result = visit(ctx.expression());

        // If user searches for a single negation, Lucene also (!) interprets it as filter on the entities matched by the other terms
        // We need to add a "filter" to match all entities
        // See https://github.com/LoayGhreeb/lucene-mwe/issues/1 for more details
        if (result instanceof ModifierQueryNode modifierQueryNode) {
            if (modifierQueryNode.getModifier() == ModifierQueryNode.Modifier.MOD_NOT) {
                return new AndQueryNode(List.of(new FieldQueryNode(SearchFieldConstants.DEFAULT_FIELD.toString(), "*", 0, 0), modifierQueryNode));
            }
        }

        // User might search for NOT this AND NOT that - we also need to convert properly
        if (result instanceof AndQueryNode andQueryNode) {
            if (andQueryNode.getChildren().stream().allMatch(child -> child instanceof ModifierQueryNode modifierQueryNode && modifierQueryNode.getModifier() == ModifierQueryNode.Modifier.MOD_NOT)) {
                List<QueryNode> children = andQueryNode.getChildren().stream()
                                                       // prepend "any:* AND" to each child
                                                       .map(child -> new AndQueryNode(List.of(new FieldQueryNode(SearchFieldConstants.DEFAULT_FIELD.toString(), "*", 0, 0), child)))
                                                       .map(child -> (QueryNode) child)
                                                       .toList();
                return new AndQueryNode(children);
            }
        }

        return result;
    }

    @Override
    public QueryNode visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        return new ModifierQueryNode(visit(ctx.expression()), ModifierQueryNode.Modifier.MOD_NOT);
    }

    @Override
    public QueryNode visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return new GroupQueryNode(visit(ctx.expression()));
    }

    @Override
    public QueryNode visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        if ("AND".equalsIgnoreCase(ctx.operator.getText())) {
            return new AndQueryNode(List.of(visit(ctx.left), visit(ctx.right)));
        } else {
            return new OrQueryNode(List.of(visit(ctx.left), visit(ctx.right)));
        }
    }

    @Override
    public QueryNode visitComparison(SearchParser.ComparisonContext context) {
        // The comparison is a leaf node in the tree

        // remove possible enclosing " symbols
        String right = context.right.getText();
        if (right.startsWith("\"") && right.endsWith("\"")) {
            right = right.substring(1, right.length() - 1);
        }

        Optional<SearchParser.NameContext> fieldDescriptor = Optional.ofNullable(context.left);
        int startIndex = context.getStart().getStartIndex();
        int stopIndex = context.getStop().getStopIndex();
        if (fieldDescriptor.isPresent()) {
            String field = fieldDescriptor.get().getText();

            // Direct comparison does not work
            // context.CONTAINS() and others are null if absent (thus, we cannot check for getText())
            if (context.CONTAINS() != null ||
                    context.MATCHES() != null ||
                    context.EQUAL() != null ||
                    context.EEQUAL() != null) { // exact match
                if (LOGGER.isDebugEnabled() && context.EEQUAL() != null) {
                    LOGGER.warn("Exact match is currently not supported by Lucene, using contains instead. Term: {}", context.getText());
                }
                return getFieldQueryNode(field, right, startIndex, stopIndex, false);
            }

            assert (context.NEQUAL() != null);

            // Treating of "wrong" query field != "". This did not work in v5.x, but should work in v6.x
            boolean forceRegex;
            if (right.isEmpty()) {
                forceRegex = true;
                right = ".+";
            } else {
                forceRegex = false;
            }

            return new ModifierQueryNode(getFieldQueryNode(field, right, startIndex, stopIndex, forceRegex), ModifierQueryNode.Modifier.MOD_NOT);
        } else {
            return getFieldQueryNode(SearchFieldConstants.DEFAULT_FIELD.toString(), right, startIndex, stopIndex, false);
        }
    }

    /**
     * A search query can be either a regular expression or a normal query.
     * In Lucene, this is represented by a RegexpQueryNode or a FieldQueryNode.
     * They are created in this class accordingly.
     */
    private QueryNode getFieldQueryNode(String field, String term, int startIndex, int stopIndex, boolean forceRegex) {
        field = switch (field) {
            case "anyfield" -> SearchFieldConstants.DEFAULT_FIELD.toString();
            case "anykeyword" -> StandardField.KEYWORDS.getName();
            case "key" -> InternalField.KEY_FIELD.getName();
            default -> field;
        };

        if (isRegularExpression || forceRegex) {
            // Lucene does a sanity check on the positions, thus we provide other fake positions
            return new RegexpQueryNode(field, term, 0, term.length());
        }
        return new FieldQueryNode(field, term, startIndex, stopIndex);
    }
}
