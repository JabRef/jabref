package org.jabref.logic.importer;

import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.JabRefException;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractQueryTransformator {
    public static final Logger LOGGER = LoggerFactory.getLogger(SpringerQueryTransformator.class);
    public static final String NO_EXPLICIT_FIELD = "default";

    /**
     * Transforms a and b and c to (a AND b AND c), where
     * a, b, and c can be complex expressions
     */
    private Optional<String> transform(BooleanQueryNode query) {
        String delimiter;
        if (query instanceof OrQueryNode) {
            delimiter = getLogicalOrOperator();
        } else {
            // We define the logical AND operator as the default implementation
            delimiter = getLogicalAndOperator();
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

    /**
     * Returns the logical AND operator used by the library
     * Note: whitespaces have to be included around the operator, e.g. " AND "
     */
    public abstract String getLogicalAndOperator();

    /**
     * Returns the logial OR operator used by the library
     * Note: whitespaces have to be included around the operator, e.g. " OR "
     */
    public abstract String getLogicalOrOperator();

    private Optional<String> transform(FieldQueryNode query) {
        String term = query.getTextAsString();
        switch (query.getFieldAsString()) {
            case "author" -> {
                return Optional.of(handleAuthor(term));
            }
            case "title" -> {
                return Optional.of(handleTitle(term));
            }
            case "journal" -> {
                return Optional.of(handleJournal(term));
            }
            case "year" -> {
                return Optional.of(handleYear(term));
            }
            case "year-range" -> {
                return Optional.of(handleYearRange(term));
            }
            case NO_EXPLICIT_FIELD -> {
                return Optional.of(handleUnFieldedTerm(term));
            }
            default -> {
                return handleOtherField(query.getFieldAsString(), term);
            } // Just add unkown fields as default
        }
    }

    /**
     * Return a string representation of the author fielded term
     */
    protected abstract String handleAuthor(String textAsString);

    /**
     * Return a string representation of the title fielded term
     */
    protected abstract String handleTitle(String textAsString);

    /**
     * Return a string representation of the journal fielded term
     */
    protected abstract String handleJournal(String textAsString);

    /**
     * Return a string representation of the year fielded term
     */
    protected abstract String handleYear(String textAsString);

    /**
     * Return a string representation of the year-range fielded term
     */
    protected abstract String handleYearRange(String textAsString);

    /**
     * Return a string representation of the author fielded term
     */
    protected abstract String handleUnFieldedTerm(String term);

    /**
     * Return a string representation of the provided field
     * If it is not supported return an empty optional.
     */
    protected Optional<String> handleOtherField(String fieldAsString, String term){
        return Optional.of(String.format("%s:\"%s\"", fieldAsString, term));
    }

    private Optional<String> transform(QueryNode query) {
        if (query instanceof BooleanQueryNode) {
            return transform((BooleanQueryNode) query);
        } else if (query instanceof FieldQueryNode) {
            return transform((FieldQueryNode) query);
        } else if (query instanceof GroupQueryNode) {
            return transform(((GroupQueryNode) query).getChild());
        } else {
            LOGGER.error("Unsupported case when transforming the query:\n {}", query);
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
            QueryNode luceneQuery = parser.parse(query, NO_EXPLICIT_FIELD);
            System.out.println(luceneQuery);
            Optional<String> transformedQuery = transform(luceneQuery);
            transformedQuery = transformedQuery.map(this::removeOuterBraces);
            return transformedQuery;
        } catch (QueryNodeParseException e) {
            throw new JabRefException("Error parsing query", e);
        }
    }

    /**
     * Removes the outer braces as they are unnecessary
     */
    private String removeOuterBraces(String query) {
        if (query.startsWith("(") && query.endsWith(")")) {
            return query.substring(1, query.length() - 1);
        }
        return query;
    }
}
