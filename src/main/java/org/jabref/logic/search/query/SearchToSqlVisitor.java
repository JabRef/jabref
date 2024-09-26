package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.Optional;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to a query processable by the scheme created by {@link org.jabref.logic.search.indexing.PostgreIndexer}.
 *
 * @implNote Similar class: {@link org.jabref.migrations.SearchToLuceneMigration}
 */
public class SearchToSqlVisitor extends SearchBaseVisitor<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchToSqlVisitor.class);

    private final String tableName;

    public SearchToSqlVisitor(String tableName) {
        this.tableName = tableName;
    }

    private enum SearchTermFlag {
        REGULAR_EXPRESSION,               // mutually exclusive to exact/inexact match
        NEGATION,
        CASE_SENSITIVE, CASE_INSENSITIVE, // mutually exclusive
        EXACT_MATCH, INEXACT_MATCH        // mutually exclusive
    }

    @Override
    public String visitStart(SearchParser.StartContext ctx) {
        String whereClause = visit(ctx.expression());
        return """
                SELECT %s
                FROM "%s" AS main_table
                LEFT JOIN "%s_split_values" AS split_table
                ON main_table.%s = split_table.%s
                AND main_table.%s = split_table.%s
                WHERE %s
                GROUP BY %s
                """.formatted(PostgreConstants.ENTRY_ID,
                tableName,
                tableName,
                PostgreConstants.ENTRY_ID,
                PostgreConstants.ENTRY_ID,
                PostgreConstants.FIELD_NAME,
                PostgreConstants.FIELD_NAME,
                whereClause,
                PostgreConstants.ENTRY_ID);
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
            if (context.EQUAL() != null || context.CONTAINS() != null) {
                searchFlags.add(SearchTermFlag.INEXACT_MATCH);
                searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
            } else if (context.CEQUAL() != null) {
                searchFlags.add(SearchTermFlag.INEXACT_MATCH);
                searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
            } else if (context.EEQUAL() != null || context.MATCHES() != null) {
                searchFlags.add(SearchTermFlag.EXACT_MATCH);
                searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
            } else if (context.CEEQUAL() != null) {
                searchFlags.add(SearchTermFlag.EXACT_MATCH);
                searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
            } else if (context.REQUAL() != null) {
                searchFlags.add(SearchTermFlag.REGULAR_EXPRESSION);
                searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
            } else if (context.CREEQUAL() != null) {
                searchFlags.add(SearchTermFlag.REGULAR_EXPRESSION);
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
                searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
                searchFlags.add(SearchTermFlag.NEGATION);
            } else if (context.NCEEQUAL() != null) {
                searchFlags.add(SearchTermFlag.EXACT_MATCH);
                searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
                searchFlags.add(SearchTermFlag.NEGATION);
            } else if (context.NREQUAL() != null) {
                searchFlags.add(SearchTermFlag.REGULAR_EXPRESSION);
                searchFlags.add(SearchTermFlag.CASE_INSENSITIVE);
                searchFlags.add(SearchTermFlag.NEGATION);
            } else if (context.NCREEQUAL() != null) {
                searchFlags.add(SearchTermFlag.REGULAR_EXPRESSION);
                searchFlags.add(SearchTermFlag.CASE_SENSITIVE);
                searchFlags.add(SearchTermFlag.NEGATION);
            }

            return getFieldQueryNode(field, right, searchFlags);
        } else {
            // Query without any field name
            return getFieldQueryNode(SearchFieldConstants.DEFAULT_FIELD.toString(), right, EnumSet.of(SearchTermFlag.INEXACT_MATCH, SearchTermFlag.CASE_INSENSITIVE));
        }
    }

    private String getFieldQueryNode(String field, String term, EnumSet<SearchTermFlag> searchFlags) {
        String additionalCondition = null;
        if (searchFlags.contains((SearchTermFlag.EXACT_MATCH))) {
            // additionally search in second table

            String operator = "";
            if (searchFlags.contains(SearchTermFlag.NEGATION)) {
                operator = "NOT ";
            }

            if (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE)) {
                operator += "LIKE";
            } else {
                operator += "ILIKE";
            }
            additionalCondition = "(split_table.field_name = '\" + field + \"' AND split_table.field_value " + operator + " '" + term + "')";
        }

        String operator = "";
        String prefixSuffix = "";

        if (searchFlags.contains(SearchTermFlag.REGULAR_EXPRESSION)) {
            if (searchFlags.contains(SearchTermFlag.NEGATION)) {
                operator = "!";
            }

            if (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE)) {
                operator += "~";
            } else {
                operator += "~*";
            }
        } else {
            if (searchFlags.contains(SearchTermFlag.INEXACT_MATCH)) {
                prefixSuffix = "%";
            }

            if (searchFlags.contains(SearchTermFlag.NEGATION)) {
                operator = "NOT ";
            }

            if (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE)) {
                operator += "LIKE";
            } else {
                operator += "ILIKE";
            }
        }

        // Pseudo-fields
        if ("anyfield".equals(field) || "any".equals(field)) {
            return "(" + PostgreConstants.FIELD_VALUE_LITERAL + " " + operator + " '" + prefixSuffix + term + prefixSuffix + "')";
        }

        field = switch (field) {
            case "key" ->
                    InternalField.KEY_FIELD.getName();
            case "anykeyword" ->
                    StandardField.KEYWORDS.getName();
            default ->
                    field;
        };

        String resultMainTable = "(" + PostgreConstants.FIELD_NAME + " = '" + field + "' AND " + PostgreConstants.FIELD_VALUE_LITERAL + " " + operator + " '" + prefixSuffix + term + prefixSuffix + "')";
        if (additionalCondition != null) {
            return "(" + resultMainTable + " OR " + additionalCondition + ")";
        } else {
            return resultMainTable;
        }
    }
}
