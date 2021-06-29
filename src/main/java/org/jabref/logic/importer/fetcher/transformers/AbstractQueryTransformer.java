package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.model.strings.StringUtil;

import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In case the transformer contains state for a query transformation (such as the {@link IEEEQueryTransformer}), it has to be noted at the JavaDoc.
 * Otherwise, a single instance QueryTransformer can be used.
 */
public abstract class AbstractQueryTransformer {
    public static final String NO_EXPLICIT_FIELD = "default";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractQueryTransformer.class);

    // These can be used for filtering in post processing
    protected int startYear = Integer.MAX_VALUE;
    protected int endYear = Integer.MIN_VALUE;

    /**
     * Transforms a and b and c to (a AND b AND c), where
     * a, b, and c can be complex expressions.
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
                             .flatMap(Optional::stream)
                             .collect(Collectors.joining(delimiter, "(", ")"));
        if (result.equals("()")) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    /**
     * Returns the logical AND operator used by the library
     * Note: whitespaces have to be included around the operator
     *
     * Example: <code>" AND "</code>
     */
    protected abstract String getLogicalAndOperator();

    /**
     * Returns the logical OR operator used by the library
     * Note: whitespaces have to be included around the operator
     *
     * Example: <code>" OR "</code>
     */
    protected abstract String getLogicalOrOperator();

    /**
     * Returns the logical NOT operator used by the library
     *
     * Example: <code>"!"</code>
     */
    protected abstract String getLogicalNotOperator();

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
                String s = handleYear(term);
                return s.isEmpty() ? Optional.empty() : Optional.of(s);
            }
            case "year-range" -> {
                String s = handleYearRange(term);
                return s.isEmpty() ? Optional.empty() : Optional.of(s);
            }
            case "doi" -> {
                String s = handleDoi(term);
                return s.isEmpty() ? Optional.empty() : Optional.of(s);
            }
            case NO_EXPLICIT_FIELD -> {
                return handleUnFieldedTerm(term);
            }
            default -> {
                // Just add unknown fields as default
                return handleOtherField(query.getFieldAsString(), term);
            }
        }
    }

    protected String handleDoi(String term) {
        return "doi:" + term;
    }

    /**
     * Handles the not modifier, all other cases are silently ignored
     */
    private Optional<String> transform(ModifierQueryNode query) {
        ModifierQueryNode.Modifier modifier = query.getModifier();
        if (modifier == ModifierQueryNode.Modifier.MOD_NOT) {
            return transform(query.getChild()).map(s -> getLogicalNotOperator() + s);
        } else {
            return transform(query.getChild());
        }
    }

    /**
     * Return a string representation of the author fielded term
     */
    protected abstract String handleAuthor(String author);

    /**
     * Return a string representation of the title fielded term
     */
    protected abstract String handleTitle(String title);

    /**
     * Return a string representation of the journal fielded term
     */
    protected abstract String handleJournal(String journalTitle);

    /**
     * Return a string representation of the year fielded term
     */
    protected abstract String handleYear(String year);

    /**
     * Parses the year range and fills startYear and endYear.
     * Ensures that startYear <= endYear
     */
    protected void parseYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        int parsedStartYear = Integer.parseInt(split[0]);
        startYear = parsedStartYear;
        if (split.length >= 1) {
            int parsedEndYear = Integer.parseInt(split[1]);
            if (parsedEndYear >= parsedStartYear) {
                endYear = parsedEndYear;
            } else {
                startYear = parsedEndYear;
                endYear = parsedStartYear;
            }
        }
    }

    /**
     * Return a string representation of the year-range fielded term
     * Should follow the structure yyyy-yyyy
     *
     * Example: <code>2015-2021</code>
     */
    protected String handleYearRange(String yearRange) {
        parseYearRange(yearRange);
        if (endYear == Integer.MAX_VALUE) {
            // invalid year range
            return yearRange;
        }
        StringJoiner resultBuilder = new StringJoiner(getLogicalOrOperator());
        for (int i = startYear; i <= endYear; i++) {
            resultBuilder.add(handleYear(String.valueOf(i)));
        }
        return resultBuilder.toString();
    }

    /**
     * Return a string representation of the un-fielded (default fielded) term
     *
     * Default implementation: just return the term (in quotes if a space is contained)
     */
    protected Optional<String> handleUnFieldedTerm(String term) {
        return Optional.of(StringUtil.quoteStringIfSpaceIsContained(term));
    }

    protected String createKeyValuePair(String fieldAsString, String term) {
        return createKeyValuePair(fieldAsString, term, ":");
    }

    protected String createKeyValuePair(String fieldAsString, String term, String separator) {
        return String.format("%s%s%s", fieldAsString, separator, StringUtil.quoteStringIfSpaceIsContained(term));
    }

    /**
     * Return a string representation of the provided field
     * If it is not supported return an empty optional.
     */
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return Optional.of(createKeyValuePair(fieldAsString, term));
    }

    private Optional<String> transform(QueryNode query) {
        if (query instanceof BooleanQueryNode) {
            return transform((BooleanQueryNode) query);
        } else if (query instanceof FieldQueryNode) {
            return transform((FieldQueryNode) query);
        } else if (query instanceof GroupQueryNode) {
            return transform(((GroupQueryNode) query).getChild());
        } else if (query instanceof ModifierQueryNode) {
            return transform((ModifierQueryNode) query);
        } else {
            LOGGER.error("Unsupported case when transforming the query:\n {}", query);
            return Optional.empty();
        }
    }

    /**
     * Parses the given query string into a complex query using lucene.
     * Note: For unique fields, the alphabetically and numerically first instance in the query string is used in the complex query.
     *
     * @param luceneQuery The lucene query tp transform
     * @return A query string containing all fields that are contained in the original lucene query and
     * that are expressible in the library specific query language, other information either is discarded or
     * stored as part of the state of the transformer if it can be used e.g. as a URL parameter for the query.
     */
    public Optional<String> transformLuceneQuery(QueryNode luceneQuery) {
        Optional<String> transformedQuery = transform(luceneQuery);
        transformedQuery = transformedQuery.map(this::removeOuterBraces);
        return transformedQuery;
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
