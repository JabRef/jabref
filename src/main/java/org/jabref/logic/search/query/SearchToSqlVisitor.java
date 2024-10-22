package org.jabref.logic.search.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.jabref.logic.search.indexing.BibFieldsIndexer;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SqlQueryNode;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import static org.jabref.model.search.PostgreConstants.ENTRY_ID;
import static org.jabref.model.search.PostgreConstants.FIELD_NAME;
import static org.jabref.model.search.PostgreConstants.FIELD_VALUE_LITERAL;
import static org.jabref.model.search.PostgreConstants.FIELD_VALUE_TRANSFORMED;
import static org.jabref.model.search.SearchFlags.CASE_INSENSITIVE;
import static org.jabref.model.search.SearchFlags.CASE_SENSITIVE;
import static org.jabref.model.search.SearchFlags.EXACT_MATCH;
import static org.jabref.model.search.SearchFlags.INEXACT_MATCH;
import static org.jabref.model.search.SearchFlags.NEGATION;
import static org.jabref.model.search.SearchFlags.REGULAR_EXPRESSION;

/**
 * Converts to a query processable by the scheme created by {@link BibFieldsIndexer}.
 * Tests are located in {@link org.jabref.logic.search.query.SearchQuerySQLConversionTest}.
 */
public class SearchToSqlVisitor extends SearchBaseVisitor<SqlQueryNode> {

    private static final String MAIN_TABLE = "main_table";
    private static final String SPLIT_TABLE = "split_table";
    private static final String INNER_TABLE = "inner_table";
    private static final String GROUPS_FIELD = StandardField.GROUPS.getName();

    private final EnumSet<SearchFlags> searchBarFlags;
    private final String mainTableName;
    private final String splitValuesTableName;
    private final List<SqlQueryNode> nodes = new ArrayList<>();
    private int cteCounter = 0;

    public SearchToSqlVisitor(String table, EnumSet<SearchFlags> searchBarFlags) {
        this.searchBarFlags = searchBarFlags;
        this.mainTableName = PostgreConstants.getMainTableSchemaReference(table);
        this.splitValuesTableName = PostgreConstants.getSplitTableSchemaReference(table);
    }

    @Override
    public SqlQueryNode visitStart(SearchParser.StartContext ctx) {
        SqlQueryNode finalNode = visit(ctx.orExpression());

        StringBuilder sql = new StringBuilder("WITH\n");
        List<String> params = new ArrayList<>();

        for (SqlQueryNode node : nodes) {
            sql.append(node.cte()).append(",\n");
            params.addAll(node.params());
        }

        // Remove the last comma and newline
        if (!nodes.isEmpty()) {
            sql.setLength(sql.length() - 2);
        }

        sql.append("SELECT * FROM ").append(finalNode.cte()).append(" GROUP BY ").append(ENTRY_ID);
        params.addAll(finalNode.params());

        return new SqlQueryNode(sql.toString(), params);
    }

    @Override
    public SqlQueryNode visitImplicitOrExpression(SearchParser.ImplicitOrExpressionContext ctx) {
        List<SqlQueryNode> children = new ArrayList<>();
        for (SearchParser.ExpressionContext exprCtx : ctx.expression()) {
            children.add(visit(exprCtx));
        }

        if (children.size() == 1) {
            return children.getFirst();
        } else {
            String cte = """
                    cte%d AS (
                    %s
                    )
                    """.formatted(
                    cteCounter,
                    children.stream().map(node -> "    SELECT %s FROM %s".formatted(ENTRY_ID, node.cte())).collect(Collectors.joining("\n    UNION\n")));

            List<String> params = children.stream().flatMap(node -> node.params().stream()).toList();
            SqlQueryNode node = new SqlQueryNode(cte, params);
            nodes.add(node);
            return new SqlQueryNode("cte" + cteCounter++);
        }
    }

    @Override
    public SqlQueryNode visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.orExpression());
    }

    @Override
    public SqlQueryNode visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
        SqlQueryNode subNode = visit(ctx.expression());
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

        SqlQueryNode node = new SqlQueryNode(cte, subNode.params());
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    @Override
    public SqlQueryNode visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        SqlQueryNode left = visit(ctx.left);
        SqlQueryNode right = visit(ctx.right);
        String operator = ctx.bin_op.getType() == SearchParser.AND ? "INTERSECT" : "UNION";

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

        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    @Override public SqlQueryNode visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public SqlQueryNode visitComparison(SearchParser.ComparisonContext ctx) {
        EnumSet<SearchFlags> searchFlags = EnumSet.noneOf(SearchFlags.class);
        String term = ctx.searchValue().getText();

        // remove possible enclosing " symbols
        if (ctx.searchValue().STRING_LITERAL() != null) {
            term = term.substring(1, term.length() - 1);
            // TODO: should be a string literal, search in the filed_value column only
        }

        // unfielded expression
        if (ctx.operator() == null) {
            boolean isCaseSensitive = searchBarFlags.contains(CASE_SENSITIVE);
            if (searchBarFlags.contains(REGULAR_EXPRESSION)) {
                setFlags(searchFlags, REGULAR_EXPRESSION, isCaseSensitive, false);
            } else {
                setFlags(searchFlags, INEXACT_MATCH, isCaseSensitive, false);
            }
            return getFieldQueryNode("any", term, searchFlags);
        }

        // fielded expression
        String field = ctx.FIELD().getText();
        SearchParser.OperatorContext operator = ctx.operator();

        // Direct comparison does not work
        // context.CONTAINS() and others are null if absent (thus, we cannot check for getText())
        if (operator.EQUAL() != null || operator.CONTAINS() != null) {
            setFlags(searchFlags, INEXACT_MATCH, false, false);
        } else if (operator.CEQUAL() != null) {
            setFlags(searchFlags, INEXACT_MATCH, true, false);
        } else if (operator.EEQUAL() != null || operator.MATCHES() != null) {
            setFlags(searchFlags, EXACT_MATCH, false, false);
        } else if (operator.CEEQUAL() != null) {
            setFlags(searchFlags, EXACT_MATCH, true, false);
        } else if (operator.REQUAL() != null) {
            setFlags(searchFlags, REGULAR_EXPRESSION, false, false);
        } else if (operator.CREEQUAL() != null) {
            setFlags(searchFlags, REGULAR_EXPRESSION, true, false);
        } else if (operator.NEQUAL() != null) {
            setFlags(searchFlags, INEXACT_MATCH, false, true);
        } else if (operator.NCEQUAL() != null) {
            setFlags(searchFlags, INEXACT_MATCH, true, true);
        } else if (operator.NEEQUAL() != null) {
            setFlags(searchFlags, EXACT_MATCH, false, true);
        } else if (operator.NCEEQUAL() != null) {
            setFlags(searchFlags, EXACT_MATCH, true, true);
        } else if (operator.NREQUAL() != null) {
            setFlags(searchFlags, REGULAR_EXPRESSION, false, true);
        } else if (operator.NCREEQUAL() != null) {
            setFlags(searchFlags, REGULAR_EXPRESSION, true, true);
        }

        return getFieldQueryNode(field.toLowerCase(Locale.ROOT), term, searchFlags);
    }

    private SqlQueryNode getFieldQueryNode(String field, String term, EnumSet<SearchFlags> searchFlags) {
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

    private SqlQueryNode buildEntryIdQuery(String entryId) {
        String cte = """
                cte%d AS (
                    SELECT %s
                    FROM %s
                    WHERE %s = ?
                )
                """.formatted(cteCounter, ENTRY_ID, mainTableName, ENTRY_ID);
        SqlQueryNode node = new SqlQueryNode(cte, List.of(entryId));
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildContainsFieldQuery(String field, String operator, String prefixSuffix, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildContainsNegationFieldQuery(String field, String operator, String prefixSuffix, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildExactFieldQuery(String field, String operator, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildExactNegationFieldQuery(String field, String operator, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildContainsAnyFieldQuery(String operator, String prefixSuffix, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildExactAnyFieldQuery(String operator, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildExactNegationAnyFieldQuery(String operator, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private SqlQueryNode buildContainsNegationAnyFieldQuery(String operator, String prefixSuffix, String term) {
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
        SqlQueryNode node = new SqlQueryNode(cte, params);
        nodes.add(node);
        return new SqlQueryNode("cte" + cteCounter++);
    }

    private static void setFlags(EnumSet<SearchFlags> flags, SearchFlags matchType, boolean caseSensitive, boolean negation) {
        flags.add(matchType);

        flags.add(caseSensitive ? CASE_SENSITIVE : CASE_INSENSITIVE);
        if (negation) {
            flags.add(NEGATION);
        }
    }

    private static String getOperator(EnumSet<SearchFlags> searchFlags) {
        return searchFlags.contains(REGULAR_EXPRESSION)
                ? (searchFlags.contains(CASE_SENSITIVE) ? "~" : "~*")
                : (searchFlags.contains(CASE_SENSITIVE) ? "LIKE" : "ILIKE");
    }
}
