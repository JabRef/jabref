package org.jabref.logic.importer;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.JabRefException;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class converts a query string written in lucene syntax into a complex  query.
 *
 * For simplicity this is currently limited to fielded data and the boolean AND operator.
 */
public class SpringerQueryTransformator {
    public static final Logger LOGGER = LoggerFactory.getLogger(SpringerQueryTransformator.class);

    /**
     * Transforms a and b and c to (a AND b AND c), where
     * a, b, and c can be complex expressions
     */
    private Optional<String> transform(BooleanQueryNode query) {
        String delimiter;
        if (query instanceof OrQueryNode) {
            delimiter = " OR ";
        } else {
            delimiter = " AND ";
        }

        String result = query.getChildren().stream()
                             .map(this::transform)
                             .filter(Optional::isPresent)
                             .map(Optional::get)
                             .collect(Collectors.joining(delimiter, "(", ")"));
        if (result.equals("()")) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private Optional<String> transform(FieldQueryNode query) {
        String convertedField;
        String convertedValue;
        switch (query.getFieldAsString()) {
            case "author" -> {
                convertedField = "name";
                convertedValue = "\"" + query.getTextAsString() + "\"";
            }
            case "journal", "title" -> {
                convertedField = query.getFieldAsString();
                convertedValue = "\"" + query.getTextAsString() + "\"";
            }
            case "year" -> {
                convertedField = "date";
                convertedValue = query.getTextAsString() + "*";
            }
            case "year-range" -> {
                convertedField = "";
                String range = query.getTextAsString();
                String[] split = range.split("-");
                StringJoiner resultBuilder = new StringJoiner("* OR date:", "(date:", "*)");
                for (int i = Integer.parseInt(split[0]); i <= Integer.parseInt(split[1]); i++) {
                    resultBuilder.add(String.valueOf(i));
                }
                convertedValue = resultBuilder.toString();
            }
            default -> {
                convertedField = "";
                convertedValue = "\"" + query.getTextAsString() + "\"";
            } // Just add unkown fields as default
        }
        if (convertedField.isEmpty()) {
            return Optional.of(convertedValue);
        } else {
            return Optional.of(convertedField + ":" + convertedValue);
        }
    }

    private Optional<String> transform(QueryNode query) {
        if (query instanceof BooleanQueryNode) {
            return transform((BooleanQueryNode) query);
        } else if (query instanceof FieldQueryNode) {
            return transform((FieldQueryNode) query);
        } else {
            LOGGER.error("Unsupported case when transforming the query");
            return Optional.empty();
        }
    }

    /**
     * Parses the given query string into a complex query using lucene.
     * Note: For unique fields, the alphabetically and numerically first instance in the query string is used in the complex query.
     *
     * @param query The given query string
     * @return A complex query containing all fields of the query string
     */
    public Optional<String> parseQueryStringIntoComplexQuery(String query) throws JabRefException {
        StandardSyntaxParser parser = new StandardSyntaxParser();
        try {
            QueryNode luceneQuery = parser.parse(query, "default");
            return transform(luceneQuery);
        } catch (QueryNodeParseException e) {
            throw new JabRefException("Error parsing query", e);
        }
    }
}
