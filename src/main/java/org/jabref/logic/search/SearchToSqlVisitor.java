package org.jabref.logic.search;

import java.util.EnumSet;
import java.util.Optional;

import org.jabref.logic.search.indexing.PostgreConstants;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similar class: {@link org.jabref.migrations.SearchToLuceneMigration}
 */
public class SearchToSqlVisitor extends SearchBaseVisitor<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchToSqlVisitor.class);
    private final String tableName;

    public SearchToSqlVisitor(String tableName) {
        this.tableName = tableName;
    }

    private enum SearchTermFlag {
        REGULAR_EXPRESSION,               // mutually exclusive to the others
        NEGATION,
        CASE_SENSITIVE, CASE_INSENSITIVE, // mutually exclusive
        EXACT_MATCH, INEXACT_MATCH        // mutually exclusive
    }

    @Override
    public String visitStart(SearchParser.StartContext ctx) {
        return "SELECT " + PostgreConstants.ENTRY_ID + " FROM " + tableName + " WHERE " + visit(ctx.expression());
    }

    @Override
    public String visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        return "NOT " + visit(ctx.expression());
    }

    @Override
    public String visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return "(" + visit(ctx.expression()) + ")";
    }

    @Override
    public String visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        if ("AND".equalsIgnoreCase(ctx.operator.getText())) {
            return visit(ctx.left) + " AND " + visit(ctx.right);
        } else {
            return visit(ctx.left) + " OR " + visit(ctx.right);
        }
    }

    @Override
    public String visitComparison(SearchParser.ComparisonContext context) {
        // The comparison is a leaf node in the tree

        // remove possible enclosing " symbols
        String right = context.right.getText();
        if (right.startsWith("\"") && right.endsWith("\"")) {
            right = right.substring(1, right.length() - 1);
        }

        Optional<SearchParser.NameContext> fieldDescriptor = Optional.ofNullable(context.left);
        if (fieldDescriptor.isPresent()) {
            String field = fieldDescriptor.get().getText();

            // Direct comparison does not work
            // context.CONTAINS() and others are null if absent (thus, we cannot check for getText())
            EnumSet<SearchTermFlag> searchFlags = EnumSet.noneOf(SearchTermFlag.class);
            if (context.REQUAL() != null) {
                searchFlags.add(SearchTermFlag.REGULAR_EXPRESSION);
            } else {
                if (context.CONTAINS() != null || context.EQUAL() != null) {
                    searchFlags.add(SearchTermFlag.INEXACT_MATCH);
                    searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
                } else if (context.CEQUAL() != null) {
                    searchFlags.add(SearchTermFlag.INEXACT_MATCH);
                    searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
                } else if (context.MATCHES() != null || context.EEQUAL() != null) {
                    searchFlags.add(SearchTermFlag.EXACT_MATCH);
                    searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
                } else if (context.CEEQUAL() != null) {
                    searchFlags.add(SearchTermFlag.EXACT_MATCH);
                    searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
                } else if (context.NEQUAL() != null) {
                    searchFlags.add(SearchTermFlag.INEXACT_MATCH);
                    searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
                    searchFlags.add(SearchTermFlag.NEGATION);
                } else if (context.NCEQUAL() != null) {
                    searchFlags.add(SearchTermFlag.INEXACT_MATCH);
                    searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
                    searchFlags.add(SearchTermFlag.NEGATION);
                } else if (context.NEEQUAL() != null) {
                    searchFlags.add(SearchTermFlag.EXACT_MATCH);
                    searchFlags.add(SearchTermFlag.NEGATION);
                } else if (context.NCEEQUAL() != null) {
                    searchFlags.add(SearchTermFlag.EXACT_MATCH);
                    searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
                    searchFlags.add(SearchTermFlag.NEGATION);
                }
            }
            return getFieldQueryNode(field, right, searchFlags);
        } else {
            // Query without any field name
            return getFieldQueryNode(SearchFieldConstants.DEFAULT_FIELD.toString(), right, EnumSet.of(SearchTermFlag.INEXACT_MATCH, SearchTermFlag.CASE_INSENSITIVE));
        }
    }

    private String getFieldQueryNode(String field, String term, EnumSet<SearchTermFlag> searchFlags) {
        /*
        field = switch (field) {
            case "anyfield" -> field(PostgreConstants.FIELD_VALUE.toString()).eq;
            case "anykeyword" -> StandardField.KEYWORDS.getName();
            case "key" -> InternalField.KEY_FIELD.getName();
            default -> field;
        };

        if (isRegularExpression || forceRegex) {
            // Lucene does a sanity check on the positions, thus we provide other fake positions
            return new RegexpQueryNode(field, term, 0, term.length());
        }
        return new FieldQueryNode(field, term, startIndex, stopIndex);
         */
        String operator = "";
        String prefixSuffix = "";

        if (searchFlags.contains(SearchTermFlag.EXACT_MATCH)) {
            prefixSuffix = "\\y";
        }

        if (searchFlags.contains(SearchTermFlag.NEGATION)) {
            operator = "!";
        }

        if (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE)) {
            operator += "~";
        } else {
            operator += "~*";
        }

        return "(" + PostgreConstants.FIELD_NAME + " = '" + field + "' AND " + PostgreConstants.FIELD_VALUE + " " + operator + " '" + prefixSuffix + term + prefixSuffix + "')";
    }
}
