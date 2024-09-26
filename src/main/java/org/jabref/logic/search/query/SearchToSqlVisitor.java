package org.jabref.logic.search.query;

import java.util.EnumSet;
import java.util.Optional;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.PostgreConstants;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts to a query processable by the scheme created by {@link org.jabref.logic.search.indexing.PostgreIndexer}.
 *
 * @implNote Similar class: {@link org.jabref.migrations.SearchToLuceneMigration}
 */
public class SearchToSqlVisitor extends SearchBaseVisitor<String> {

    public static final String MAIN_TABLE = "main_table";
    public static final String SPLIT_TABLE = "split_table";
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchToSqlVisitor.class);

    private final String mainTableName;
    private final String splitTableName;

    public SearchToSqlVisitor(String mainTableName) {
        this.mainTableName = mainTableName;
        this.splitTableName = mainTableName + PostgreConstants.TABLE_NAME_SUFFIX;
    }

    private enum SearchTermFlag {
        REGULAR_EXPRESSION,               // mutually exclusive to exact/inexact match
        NEGATION,
        CASE_SENSITIVE, CASE_INSENSITIVE, // mutually exclusive
        EXACT_MATCH, INEXACT_MATCH        // mutually exclusive
    }

    @Override
    public String visitStart(SearchParser.StartContext ctx) {
        String whereClause = getWhereClause(ctx);
        String result = """
                SELECT %s.%s
                FROM "%s" AS %s
                LEFT JOIN "%s" AS %s
                ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                WHERE (%s)
                GROUP BY %s.%s
                """.formatted(
                PostgreConstants.ENTRY_ID, MAIN_TABLE,
                mainTableName, MAIN_TABLE,
                splitTableName, SPLIT_TABLE,
                MAIN_TABLE, PostgreConstants.ENTRY_ID,
                SPLIT_TABLE, PostgreConstants.ENTRY_ID,
                MAIN_TABLE, PostgreConstants.FIELD_NAME,
                SPLIT_TABLE, PostgreConstants.FIELD_NAME,
                whereClause,
                MAIN_TABLE, PostgreConstants.ENTRY_ID);
        LOGGER.trace("Converted search query to SQL: {}", result);
        return result;
    }

    @VisibleForTesting
    public String getWhereClause(SearchParser.StartContext ctx) {
        return visit(ctx.expression());
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
                setFlags(searchFlags, SearchTermFlag.INEXACT_MATCH, false, false);
            } else if (context.CEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.INEXACT_MATCH, true, false);
            } else if (context.EEQUAL() != null || context.MATCHES() != null) {
                setFlags(searchFlags, SearchTermFlag.EXACT_MATCH, false, false);
            } else if (context.CEEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.EXACT_MATCH, true, false);
            } else if (context.REQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.REGULAR_EXPRESSION, false, false);
            } else if (context.CREEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.REGULAR_EXPRESSION, true, false);
            } else if (context.NEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.INEXACT_MATCH, false, true);
            } else if (context.NCEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.INEXACT_MATCH, true, true);
            } else if (context.NEEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.EXACT_MATCH, false, true);
            } else if (context.NCEEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.EXACT_MATCH, true, true);
            } else if (context.NREQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.REGULAR_EXPRESSION, false, true);
            } else if (context.NCREEQUAL() != null) {
                setFlags(searchFlags, SearchTermFlag.REGULAR_EXPRESSION, true, true);
            }

            return getFieldQueryNode(field, right, searchFlags);
        } else {
            // Query without any field name
            return getFieldQueryNode("any", right, EnumSet.of(SearchTermFlag.INEXACT_MATCH, SearchTermFlag.CASE_INSENSITIVE));
        }
    }

    private String getFieldQueryNode(String field, String term, EnumSet<SearchTermFlag> searchFlags) {
        StringBuilder whereClause = new StringBuilder();
        String operator = getOperator(searchFlags);
        String prefixSuffix = searchFlags.contains(SearchTermFlag.INEXACT_MATCH) ? "%" : "";

        // Pseudo-fields
        field = switch (field) {
            case "key" -> InternalField.KEY_FIELD.getName();
            case "anykeyword" -> StandardField.KEYWORDS.getName();
            default -> field;
        };

        if ("anyfield".equals(field) || "any".equals(field)) {
            whereClause.append(buildTableQuery(MAIN_TABLE, operator, prefixSuffix, term));
            if (searchFlags.contains(SearchTermFlag.EXACT_MATCH)) {
                whereClause.append(" OR ").append(buildTableQuery(SPLIT_TABLE, operator, prefixSuffix, term));
            }
        } else {
            whereClause.append(buildFieldQuery(MAIN_TABLE, field, operator, prefixSuffix, term));
            if (searchFlags.contains(SearchTermFlag.EXACT_MATCH)) {
                whereClause.append(" OR ").append(buildFieldQuery(SPLIT_TABLE, field, operator, prefixSuffix, term));
            }
        }

        return whereClause.toString();
    }

    private String getOperator(EnumSet<SearchTermFlag> searchFlags) {
        if (searchFlags.contains(SearchTermFlag.REGULAR_EXPRESSION)) {
            return (searchFlags.contains(SearchTermFlag.NEGATION) ? "!" : "") +
                    (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE) ? "~" : "~*");
        } else {
            return (searchFlags.contains(SearchTermFlag.NEGATION) ? "NOT " : "") +
                    (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE) ? "LIKE" : "ILIKE");
        }
    }

    private String buildTableQuery(String tableName, String operator, String prefixSuffix, String term) {
        return """
                (%s.%s %s '%s%s%s') OR (%s.%s %s '%s%s%s')""".formatted(
                tableName, PostgreConstants.FIELD_VALUE_LITERAL,
                operator,
                prefixSuffix, term, prefixSuffix,
                tableName, PostgreConstants.FIELD_VALUE_TRANSFORMED,
                operator, prefixSuffix, term, prefixSuffix);
    }

    private String buildFieldQuery(String tableName, String field, String operator, String prefixSuffix, String term) {
        return """
                ((%s.%s = '%s') AND (%s))""".formatted(
                tableName, PostgreConstants.FIELD_NAME, field,
                buildTableQuery(tableName, operator, prefixSuffix, term));
    }

    private static void setFlags(EnumSet<SearchTermFlag> flags, SearchTermFlag matchType, boolean caseSensitive, boolean negation) {
        flags.add(matchType);
        flags.add(caseSensitive ? SearchTermFlag.CASE_SENSITIVE : SearchTermFlag.CASE_INSENSITIVE);
        if (negation) {
            flags.add(SearchTermFlag.NEGATION);
        }
    }
}
