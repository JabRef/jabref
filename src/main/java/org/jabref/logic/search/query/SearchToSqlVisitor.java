package org.jabref.logic.search.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.search.indexing.BibFieldsIndexer;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.query.SearchTermFlag;
import org.jabref.model.search.query.SqlQuery;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

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
public class SearchToSqlVisitor extends SearchBaseVisitor<SqlQuery> {

    private static final String MAIN_TABLE = "main_table";
    private static final String SPLIT_TABLE = "split_table";
    private static final String INNER_TABLE = "inner_table";
    private static final String GROUPS_FIELD = StandardField.GROUPS.getName();

    private final String mainTableName;
    private final String splitValuesTableName;
    private final List<SqlQuery> nodes = new ArrayList<>();
    private int cteCounter = 0;

    public SearchToSqlVisitor(String table) {
        this.mainTableName = PostgreConstants.getMainTableSchemaReference(table);
        this.splitValuesTableName = PostgreConstants.getSplitTableSchemaReference(table);
    }

    @Override
    public SqlQuery visitStart(SearchParser.StartContext ctx) {
        SqlQuery finalNode = visit(ctx.expression());

        StringBuilder sql = new StringBuilder("WITH\n");
        List<String> params = new ArrayList<>();

        for (SqlQuery node : nodes) {
            sql.append(node.cte()).append(",\n");
            params.addAll(node.params());
        }

        // Remove the last comma and newline
        if (!nodes.isEmpty()) {
            sql.setLength(sql.length() - 2);
        }

        sql.append("SELECT * FROM ").append(finalNode.cte()).append(" GROUP BY ").append(ENTRY_ID);
        params.addAll(finalNode.params());

        return new SqlQuery(sql.toString(), params);
    }

    @Override
    public SqlQuery visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        SqlQuery subNode = visit(ctx.expression());
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
                subNode.cte());

        SqlQuery node = new SqlQuery(cte, subNode.params());
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    @Override
    public SqlQuery visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        SqlQuery left = visit(ctx.left);
        SqlQuery right = visit(ctx.right);
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
                left.cte(),
                operator,
                ENTRY_ID,
                right.cte());

        List<String> params = new ArrayList<>(left.params());
        params.addAll(right.params());

        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    @Override
    public SqlQuery visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public SqlQuery visitAtomExpression(SearchParser.AtomExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public SqlQuery visitComparison(SearchParser.ComparisonContext context) {
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

            return getFieldQueryNode(field.toLowerCase(Locale.ROOT), right, searchFlags);
        } else {
            // Query without any field name
            return getFieldQueryNode("any", right, EnumSet.of(INEXACT_MATCH, CASE_INSENSITIVE));
        }
    }

    private SqlQuery getFieldQueryNode(String field, String term, EnumSet<SearchTermFlag> searchFlags) {
        String operator = getOperator(searchFlags);
        String prefixSuffix = searchFlags.contains(INEXACT_MATCH) ? "%" : "";

        // Pseudo-fields
        field = switch (field) {
            case "key" -> InternalField.KEY_FIELD.getName();
            case "anykeyword" -> StandardField.KEYWORDS.getName();
            default -> field;
        };

        if (ENTRY_ID.toString().equals(field)) {
            return buildEntryIdQuery(term);
        } else if ("anyfield".equals(field) || "any".equals(field)) {
            if (searchFlags.contains(EXACT_MATCH)) {
                return searchFlags.contains(NEGATION)
                        ? buildExactNegationAnyFieldQuery(operator, term)
                        : buildExactAnyFieldQuery(operator, term);
            } else {
                return searchFlags.contains(NEGATION)
                        ? buildContainsNegationAnyFieldQuery(operator, prefixSuffix, term)
                        : buildContainsAnyFieldQuery(operator, prefixSuffix, term);
            }
        } else {
            if (searchFlags.contains(EXACT_MATCH)) {
                return searchFlags.contains(NEGATION)
                        ? buildExactNegationFieldQuery(field, operator, term)
                        : buildExactFieldQuery(field, operator, term);
            } else {
                return searchFlags.contains(NEGATION)
                        ? buildContainsNegationFieldQuery(field, operator, prefixSuffix, term)
                        : buildContainsFieldQuery(field, operator, prefixSuffix, term);
            }
        }
    }

    private SqlQuery buildEntryIdQuery(String entryId) {
        String cte = """
                cte%d AS (
                    SELECT %s
                    FROM %s
                    WHERE %s = ?
                )
                """.formatted(cteCounter, ENTRY_ID, mainTableName, ENTRY_ID);
        SqlQuery node = new SqlQuery(cte, List.of(entryId));
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildContainsFieldQuery(String field, String operator, String prefixSuffix, String term) {
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE (
                        (%s.%s = '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?))
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, FIELD_NAME, field,
                MAIN_TABLE, FIELD_VALUE_LITERAL, operator,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED, operator);

        List<String> params = Collections.nCopies(2, prefixSuffix + term + prefixSuffix);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildContainsNegationFieldQuery(String field, String operator, String prefixSuffix, String term) {
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                        SELECT %s.%s
                        FROM %s AS %s
                        WHERE (
                            (%s.%s = '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?))
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
                INNER_TABLE, FIELD_NAME, field,
                INNER_TABLE, FIELD_VALUE_LITERAL,
                operator,
                INNER_TABLE, FIELD_VALUE_TRANSFORMED,
                operator);

        List<String> params = Collections.nCopies(2, prefixSuffix + term + prefixSuffix);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildExactFieldQuery(String field, String operator, String term) {
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    LEFT JOIN %s AS %s
                    ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                    WHERE (
                        ((%s.%s = '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?)))
                        OR
                        ((%s.%s = '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?)))
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
                MAIN_TABLE, FIELD_VALUE_LITERAL, operator,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED, operator,
                SPLIT_TABLE, FIELD_NAME, field,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator);

        List<String> params = Collections.nCopies(4, term);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildExactNegationFieldQuery(String field, String operator, String term) {
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                        SELECT %s.%s
                        FROM %s AS %s
                        LEFT JOIN %s AS %s
                        ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                        WHERE (
                            ((%s.%s = '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?)))
                            OR
                            ((%s.%s = '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?)))
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
                INNER_TABLE, FIELD_VALUE_LITERAL, operator,
                INNER_TABLE, FIELD_VALUE_TRANSFORMED, operator,
                SPLIT_TABLE, FIELD_NAME, field,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator);

        List<String> params = Collections.nCopies(4, term);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildContainsAnyFieldQuery(String operator, String prefixSuffix, String term) {
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE (
                        (%s.%s != '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?))
                    )
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, FIELD_NAME, GROUPS_FIELD, // https://github.com/JabRef/jabref/issues/7996
                MAIN_TABLE, FIELD_VALUE_LITERAL,
                operator,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED,
                operator);

        List<String> params = Collections.nCopies(2, prefixSuffix + term + prefixSuffix);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildExactAnyFieldQuery(String operator, String term) {
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    LEFT JOIN %s AS %s
                    ON (%s.%s = %s.%s AND %s.%s = %s.%s)
                    WHERE (
                        (%s.%s != '%s')
                        AND (
                            ((%s.%s %s ?) OR (%s.%s %s ?))
                            OR
                            ((%s.%s %s ?) OR (%s.%s %s ?))
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
                MAIN_TABLE, FIELD_VALUE_LITERAL, operator,
                MAIN_TABLE, FIELD_VALUE_TRANSFORMED, operator,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator);

        List<String> params = Collections.nCopies(4, term);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildExactNegationAnyFieldQuery(String operator, String term) {
        String cte = """
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
                                ((%s.%s %s ?) OR (%s.%s %s ?))
                                OR
                                ((%s.%s %s ?) OR (%s.%s %s ?))
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
                INNER_TABLE, FIELD_VALUE_LITERAL, operator,
                INNER_TABLE, FIELD_VALUE_TRANSFORMED, operator,
                SPLIT_TABLE, FIELD_VALUE_LITERAL, operator,
                SPLIT_TABLE, FIELD_VALUE_TRANSFORMED, operator);

        List<String> params = Collections.nCopies(4, term);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
    }

    private SqlQuery buildContainsNegationAnyFieldQuery(String operator, String prefixSuffix, String term) {
        String cte = """
                cte%d AS (
                    SELECT %s.%s
                    FROM %s AS %s
                    WHERE %s.%s NOT IN (
                        SELECT %s.%s
                        FROM %s AS %s
                        WHERE (
                            (%s.%s != '%s') AND ((%s.%s %s ?) OR (%s.%s %s ?))
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
                INNER_TABLE, FIELD_VALUE_TRANSFORMED,
                operator);

        List<String> params = Collections.nCopies(2, prefixSuffix + term + prefixSuffix);
        SqlQuery node = new SqlQuery(cte, params);
        nodes.add(node);
        return new SqlQuery("cte" + cteCounter++);
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
