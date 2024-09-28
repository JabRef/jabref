package org.jabref.logic.search.query;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.PostgreConstants;
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
    private static final String MAIN_TABLE = "main_table";

    private final String mainTableName;
    private final List<String> ctes = new ArrayList<>();
    private int cteCounter = 0;

    public SearchToSqlVisitor(String mainTableName) {
        this.mainTableName = mainTableName;
    }

    private enum SearchTermFlag {
        REGULAR_EXPRESSION,               // mutually exclusive to exact/inexact match
        NEGATION,
        CASE_SENSITIVE, CASE_INSENSITIVE, // mutually exclusive
        EXACT_MATCH, INEXACT_MATCH        // mutually exclusive
    }

    @Override
    public String visitStart(SearchParser.StartContext ctx) {
        String query = visit(ctx.expression());

        StringBuilder sql = new StringBuilder("WITH\n");
        for (String cte : ctes) {
            sql.append(cte).append(",\n");
        }

        // Remove the last comma and newline
        if (!ctes.isEmpty()) {
            sql.setLength(sql.length() - 2);
        }

        sql.append("SELECT * FROM ").append(query);
        LOGGER.trace("Converted search query to SQL: {}", sql);
        return sql.toString();
    }

    @Override
    public String visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        String subQuery = visit(ctx.expression());
        String cte = """
                cte%d AS (
                 SELECT %s
                 FROM %s
                 EXCEPT
                 SELECT %s
                 FROM "%s"
                )
                """.formatted(
                cteCounter,
                PostgreConstants.ENTRY_ID,
                subQuery,
                PostgreConstants.ENTRY_ID,
                mainTableName);
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
                PostgreConstants.ENTRY_ID,
                left,
                operator,
                PostgreConstants.ENTRY_ID,
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

            cte = getFieldQueryNode(field, right, searchFlags);
        } else {
            // Query without any field name
            cte = getFieldQueryNode("any", right, EnumSet.of(SearchTermFlag.INEXACT_MATCH, SearchTermFlag.CASE_INSENSITIVE));
        }
        ctes.add(cte);
        return "cte" + cteCounter++;
    }

    private void setFlags(EnumSet<SearchTermFlag> flags, SearchTermFlag matchType, boolean caseSensitive, boolean negation) {
        flags.add(matchType);

        flags.add(caseSensitive ? SearchTermFlag.CASE_SENSITIVE : SearchTermFlag.CASE_INSENSITIVE);
        if (negation) {
            flags.add(SearchTermFlag.NEGATION);
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
            whereClause.append(buildAllFieldsQuery(operator, prefixSuffix, term));
        } else {
            whereClause.append(buildFieldQuery(field, operator, prefixSuffix, term));
        }

        return whereClause.toString();
    }

    private static String getOperator(EnumSet<SearchTermFlag> searchFlags) {
        return searchFlags.contains(SearchTermFlag.REGULAR_EXPRESSION)
                ? (searchFlags.contains(SearchTermFlag.NEGATION) ? "!" : "") + (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE) ? "~" : "~*")
                : (searchFlags.contains(SearchTermFlag.NEGATION) ? "NOT " : "") + (searchFlags.contains(SearchTermFlag.CASE_SENSITIVE) ? "LIKE" : "ILIKE");
    }

    private String buildFieldQuery(String field, String operator, String prefixSuffix, String term) {
        return """
                cte%d AS (
                 SELECT %s.%s
                 FROM "%s" AS %s
                 WHERE (%s.%s = '%s') AND ((%s.%s %s '%s%s%s') OR (%s.%s %s '%s%s%s'))
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, PostgreConstants.ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, PostgreConstants.FIELD_NAME,
                field,
                MAIN_TABLE, PostgreConstants.FIELD_VALUE_LITERAL,
                operator,
                prefixSuffix, term, prefixSuffix,
                MAIN_TABLE, PostgreConstants.FIELD_VALUE_TRANSFORMED,
                operator,
                prefixSuffix, term, prefixSuffix);
    }

    private String buildAllFieldsQuery(String operator, String prefixSuffix, String term) {
        return """
                cte%d AS (
                 SELECT %s.%s
                 FROM "%s" AS %s
                 WHERE ((%s.%s %s '%s%s%s') OR (%s.%s %s '%s%s%s'))
                )
                """.formatted(
                cteCounter,
                MAIN_TABLE, PostgreConstants.ENTRY_ID,
                mainTableName, MAIN_TABLE,
                MAIN_TABLE, PostgreConstants.FIELD_VALUE_LITERAL,
                operator,
                prefixSuffix, term, prefixSuffix,
                MAIN_TABLE, PostgreConstants.FIELD_VALUE_TRANSFORMED,
                operator,
                prefixSuffix, term, prefixSuffix);
    }
}
