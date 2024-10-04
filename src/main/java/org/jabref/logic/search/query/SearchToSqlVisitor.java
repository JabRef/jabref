package org.jabref.logic.search.query;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.search.indexing.BibFieldsIndexer;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.query.SearchTermFlag;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgreConstants.ENTRY_ID;
import static org.jabref.model.search.PostgreConstants.FIELD_NAME;
import static org.jabref.model.search.PostgreConstants.FIELD_VALUE_LITERAL;
import static org.jabref.model.search.PostgreConstants.FIELD_VALUE_TRANSFORMED;
import static org.jabref.model.search.query.SearchTermFlag.CASE_INSENSITIVE;
import static org.jabref.model.search.query.SearchTermFlag.CASE_SENSITIVE;
import static org.jabref.model.search.query.SearchTermFlag.EXACT_MATCH;
import static org.jabref.model.search.query.SearchTermFlag.INEXACT_MATCH;
import static org.jabref.model.search.query.SearchTermFlag.NEGATION;
import static org.jabref.model.search.query.SearchTermFlag.REGULAR_EXPRESSION;

/**
 * Converts to a query processable by the scheme created by {@link BibFieldsIndexer}.
 * Tests are located in {@link org.jabref.logic.search.query.SearchQuerySQLConversionTest}.
 */
public class SearchToSqlVisitor extends SearchBaseVisitor<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchToSqlVisitor.class);
    private static final String MAIN_TABLE = "main_table";
    private static final String SPLIT_TABLE = "split_table";
    private static final String INNER_TABLE = "inner_table";
    private static final String GROUPS_FIELD = StandardField.GROUPS.getName();

    private final String mainTableName;
    private final String splitValuesTableName;

    private final List<String> ctes = new ArrayList<>();
    private int cteCounter = 0;

    public SearchToSqlVisitor(String table) {
        this.mainTableName = PostgreConstants.getMainTableSchemaReference(table);
        this.splitValuesTableName = PostgreConstants.getSplitTableSchemaReference(table);
    }

    @Override
    public String visitStart(SearchParser.StartContext ctx) {
        String query = visit(ctx.expression());

        StringBuilder sql = new StringBuilder("WITH\n");
        ctes.forEach(cte -> sql.append(cte).append(",\n"));

        // Remove the last comma and newline
        if (!ctes.isEmpty()) {
            sql.setLength(sql.length() - 2);
        }

        sql.append("SELECT * FROM ").append(query).append(" GROUP BY ").append(ENTRY_ID);
        LOGGER.trace("Converted search query to SQL: {}", sql);
        return sql.toString();
    }

    @Override
    public String visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        String subQuery = visit(ctx.expression());
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                       SELECT %s
                       FROM %s
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, ENTRY_ID,
                ENTRY_ID,
                subQuery);
        ctes.add(cte);
        return "cte" + cteCounter++;
    }

    @Override
    public String visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        String left = visit(ctx.left);
        String right = visit(ctx.right);
        String operator = "AND".equalsIgnoreCase(ctx.operator.getText()) ? "INTERSECT" : "UNION";

        String cte = """
                cte%d AS (
                    SELECT %s
                    FROM %s
                    %s
                    SELECT %s
                    FROM %s
                )
                """.formatted(
                cteCounter,
                ENTRY_ID,
                left,
                operator,
                ENTRY_ID,
                right);
        ctes.add(cte);
        return "cte" + cteCounter++;
    }

    @Override
    public String visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public String visitAtomExpression(SearchParser.AtomExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public String visitName(SearchParser.NameContext ctx) {
        return ctx.getText();
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
        String cte;
        if (fieldDescriptor.isPresent()) {
            String field = fieldDescriptor.get().getText();

            // Direct comparison does not work
            // context.CONTAINS() and others are null if absent (thus, we cannot check for getText())
            EnumSet<SearchTermFlag> searchFlags = EnumSet.noneOf(SearchTermFlag.class);
            if (context.EQUAL() != null || context.CONTAINS() != null) {
                setFlags(searchFlags, INEXACT_MATCH, false, false);
            } else if (context.CEQUAL() != null) {
                setFlags(searchFlags, INEXACT_MATCH, true, false);
            } else if (context.EEQUAL() != null || context.MATCHES() != null) {
                setFlags(searchFlags, EXACT_MATCH, false, false);
            } else if (context.CEEQUAL() != null) {
                setFlags(searchFlags, EXACT_MATCH, true, false);
            } else if (context.REQUAL() != null) {
                setFlags(searchFlags, REGULAR_EXPRESSION, false, false);
            } else if (context.CREEQUAL() != null) {
                setFlags(searchFlags, REGULAR_EXPRESSION, true, false);
            } else if (context.NEQUAL() != null) {
                setFlags(searchFlags, INEXACT_MATCH, false, true);
            } else if (context.NCEQUAL() != null) {
                setFlags(searchFlags, INEXACT_MATCH, true, true);
            } else if (context.NEEQUAL() != null) {
                setFlags(searchFlags, EXACT_MATCH, false, true);
            } else if (context.NCEEQUAL() != null) {
                setFlags(searchFlags, EXACT_MATCH, true, true);
            } else if (context.NREQUAL() != null) {
                setFlags(searchFlags, REGULAR_EXPRESSION, false, true);
            } else if (context.NCREEQUAL() != null) {
                setFlags(searchFlags, REGULAR_EXPRESSION, true, true);
            }

            cte = getFieldQueryNode(field, right, searchFlags);
        } else {
            // Query without any field name
            cte = getFieldQueryNode("any", right, EnumSet.of(INEXACT_MATCH, CASE_INSENSITIVE));
        }
        ctes.add(cte);
        return "cte" + cteCounter++;
    }

    private String getFieldQueryNode(String field, String term, EnumSet<SearchTermFlag> searchFlags) {
        String cte;
        String operator = getOperator(searchFlags);
        String prefixSuffix = searchFlags.contains(INEXACT_MATCH) ? "%" : "";

        // Pseudo-fields
        field = switch (field) {
            case "key" -> InternalField.KEY_FIELD.getName();
            case "anykeyword" -> StandardField.KEYWORDS.getName();
            default -> field;
        };

        if (ENTRY_ID.toString().equals(field)) {
            cte = """
                    cte%d AS (
                        SELECT %s
                        FROM %s
                        WHERE %s = '%s'
                    )
                    """.formatted(
                    cteCounter,
                    ENTRY_ID,
                    mainTableName,
                    ENTRY_ID, term);
        } else if ("anyfield".equals(field) || "any".equals(field)) {
            if (searchFlags.contains(EXACT_MATCH)) {
                cte = searchFlags.contains(NEGATION)
                        ? buildExactNegationAnyFieldQuery(operator, term)
                        : buildExactAnyFieldQuery(operator, term);
            } else {
                cte = searchFlags.contains(NEGATION)
                        ? buildContainsNegationAnyFieldQuery(operator, prefixSuffix, term)
                        : buildContainsAnyFieldQuery(operator, prefixSuffix, term);
            }
        } else {
            if (searchFlags.contains(EXACT_MATCH)) {
                cte = searchFlags.contains(NEGATION)
                        ? buildExactNegationFieldQuery(field, operator, term)
                        : buildExactFieldQuery(field, operator, term);
            } else {
                cte = searchFlags.contains(NEGATION)
                        ? buildContainsNegationFieldQuery(field, operator, prefixSuffix, term)
                        : buildContainsFieldQuery(field, operator, prefixSuffix, term);
            }
        }
        return cte;
    }

    private String buildContainsFieldQuery(String field, String operator, String prefixSuffix, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE (
                        (%s.%s = '%s') AND ((%s.%s %s '%s%s%s') OR (%s.%s %s '%s%s%s'))
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, FIELD_NAME,
                field,
                MAIN_TABLE, FIELD_VALUE_LITERAL,
                operator,
                prefixSuffix, term, prefixSuffix,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED,
                operator,
                prefixSuffix, term, prefixSuffix);
    }

    private String buildContainsNegationFieldQuery(String field, String operator, String prefixSuffix, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                        SELECT %s.%s
                        FROM %s AS %s
                        WHERE (
                            (%s.%s = '%s') AND ((%s.%s %s '%s%s%s') OR (%s.%s %s '%s%s%s'))
                        )
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, ENTRY_ID,
                INNER_TABLE, ENTRY_ID,
                mainTableName, INNER_TABLE,
                INNER_TABLE, FIELD_NAME,
                field,
                INNER_TABLE, FIELD_VALUE_LITERAL,
                operator,
                prefixSuffix, term, prefixSuffix,
                INNER_TABLE, FIELD_VALUE_TRANSFORMED,
                operator,
                prefixSuffix, term, prefixSuffix);
    }

    private String buildExactFieldQuery(String field, String operator, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    LEFT JOIN %s AS %s
                    ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                    WHERE (
                        ((%s.%s = '%s') AND ((%s.%s %s '%s') OR (%s.%s %s '%s')))
                        OR
                        ((%s.%s = '%s') AND ((%s.%s %s '%s') OR (%s.%s %s '%s')))
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                splitValuesTableName, SPLIT_TABLE,
                MAIN_TABLE, ENTRY_ID, SPLIT_TABLE, ENTRY_ID,
                MAIN_TABLE, FIELD_NAME, SPLIT_TABLE, FIELD_NAME,
                MAIN_TABLE, FIELD_NAME, field,
                MAIN_TABLE, FIELD_VALUE_LITERAL, operator, term,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED, operator, term,
                SPLIT_TABLE, FIELD_NAME, field,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator, term,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator, term);
    }

    private String buildExactNegationFieldQuery(String field, String operator, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                        SELECT %s.%s
                        FROM %s AS %s
                        LEFT JOIN %s AS %s
                        ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                        WHERE (
                            ((%s.%s = '%s') AND ((%s.%s %s '%s') OR (%s.%s %s '%s')))
                            OR
                            ((%s.%s = '%s') AND ((%s.%s %s '%s') OR (%s.%s %s '%s')))
                        )
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, ENTRY_ID,
                INNER_TABLE, ENTRY_ID,
                mainTableName, INNER_TABLE,
                splitValuesTableName, SPLIT_TABLE,
                INNER_TABLE, ENTRY_ID, SPLIT_TABLE, ENTRY_ID,
                INNER_TABLE, FIELD_NAME, SPLIT_TABLE, FIELD_NAME,
                INNER_TABLE, FIELD_NAME, field,
                INNER_TABLE, FIELD_VALUE_LITERAL, operator, term,
                INNER_TABLE, FIELD_VALUE_TRANSFORMED, operator, term,
                SPLIT_TABLE, FIELD_NAME, field,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator, term,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator, term);
    }

    private String buildContainsAnyFieldQuery(String operator, String prefixSuffix, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE (
                        (%s.%s != '%s') AND ((%s.%s %s '%s%s%s') OR (%s.%s %s '%s%s%s'))
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, FIELD_NAME, GROUPS_FIELD, // https://github.com/JabRef/jabref/issues/7996
                MAIN_TABLE, FIELD_VALUE_LITERAL,
                operator,
                prefixSuffix, term, prefixSuffix,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED,
                operator,
                prefixSuffix, term, prefixSuffix);
    }

    private String buildExactAnyFieldQuery(String operator, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    LEFT JOIN %s AS %s
                    ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                    WHERE (
                        (%s.%s != '%s')
                        AND (
                            ((%s.%s %s '%s') OR (%s.%s %s '%s'))
                            OR
                            ((%s.%s %s '%s') OR (%s.%s %s '%s'))
                        )
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                splitValuesTableName, SPLIT_TABLE,
                MAIN_TABLE, ENTRY_ID, SPLIT_TABLE, ENTRY_ID,
                MAIN_TABLE, FIELD_NAME, SPLIT_TABLE, FIELD_NAME,
                MAIN_TABLE, FIELD_NAME, GROUPS_FIELD, // https://github.com/JabRef/jabref/issues/7996
                MAIN_TABLE, FIELD_VALUE_LITERAL, operator, term,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED, operator, term,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator, term,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator, term);
    }

    private String buildExactNegationAnyFieldQuery(String operator, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                        SELECT %s.%s
                        FROM %s AS %s
                        LEFT JOIN %s AS %s
                        ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                        WHERE (
                            (%s.%s != '%s')
                            AND (
                                ((%s.%s %s '%s') OR (%s.%s %s '%s'))
                                OR
                                ((%s.%s %s '%s') OR (%s.%s %s '%s'))
                            )
                        )
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, ENTRY_ID,
                INNER_TABLE, ENTRY_ID,
                mainTableName, INNER_TABLE,
                splitValuesTableName, SPLIT_TABLE,
                INNER_TABLE, FIELD_NAME, SPLIT_TABLE, FIELD_NAME,
                INNER_TABLE, ENTRY_ID, SPLIT_TABLE, ENTRY_ID,
                INNER_TABLE, FIELD_NAME, GROUPS_FIELD, // https://github.com/JabRef/jabref/issues/7996
                INNER_TABLE, FIELD_VALUE_LITERAL, operator, term,
                INNER_TABLE, FIELD_VALUE_TRANSFORMED, operator, term,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator, term,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator, term);
    }

    private String buildContainsNegationAnyFieldQuery(String operator, String prefixSuffix, String term) {
        return """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                        SELECT %s.%s
                        FROM %s AS %s
                        WHERE (
                            (%s.%s != '%s') AND ((%s.%s %s '%s%s%s') OR (%s.%s %s '%s%s%s'))
                        )
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, ENTRY_ID,
                INNER_TABLE, ENTRY_ID,
                mainTableName, INNER_TABLE,
                INNER_TABLE, FIELD_NAME, GROUPS_FIELD, // https://github.com/JabRef/jabref/issues/7996
                INNER_TABLE, FIELD_VALUE_LITERAL,
                operator,
                prefixSuffix, term, prefixSuffix,
                INNER_TABLE, FIELD_VALUE_TRANSFORMED,
                operator,
                prefixSuffix, term, prefixSuffix);
    }

    private static void setFlags(EnumSet<SearchTermFlag> flags, SearchTermFlag matchType, boolean caseSensitive, boolean negation) {
        flags.add(matchType);

        flags.add(caseSensitive ? CASE_SENSITIVE : CASE_INSENSITIVE);
        if (negation) {
            flags.add(NEGATION);
        }
    }

    private static String getOperator(EnumSet<SearchTermFlag> searchFlags) {
        return searchFlags.contains(REGULAR_EXPRESSION)
                ? (searchFlags.contains(CASE_SENSITIVE) ? "~" : "~*")
                : (searchFlags.contains(CASE_SENSITIVE) ? "LIKE" : "ILIKE");
    }
}
