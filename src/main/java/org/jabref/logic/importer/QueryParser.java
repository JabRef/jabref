package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;

/**
 * This class converts a query string written in lucene syntax into a complex  query.
 *
 * For simplicity this is currently limited to fielded data and the boolean AND operator.
 */
public class QueryParser {

    /**
     * Parses the given query string into a complex query using lucene.
     * Note: For unique fields, the alphabetically and numerically first instance in the query string is used in the complex query.
     *
     * @param query The given query string. E.g. <code>BPMN 2.0</code> or <code>author:"Kopp" AND title:"BPEL4Chor"</code>
     * @return A complex query containing all fields of the query string
     */
    public Optional<ComplexSearchQuery> parseQueryStringIntoComplexQuery(String query) {
        try {
            StandardSyntaxParser parser = new StandardSyntaxParser();
            QueryNode luceneQuery = parser.parse(query, "default");
            QueryToComplexSearchQueryTransformator transformator = new QueryToComplexSearchQueryTransformator();
            return Optional.of(transformator.handle(luceneQuery));
        } catch (QueryNodeException | IllegalStateException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static class QueryToComplexSearchQueryTransformator {

        ComplexSearchQuery.ComplexSearchQueryBuilder builder;

        public ComplexSearchQuery handle(QueryNode query) {
            builder = ComplexSearchQuery.builder();
            transform(query);
            return builder.build();
        }

        public void transform(QueryNode query) {
            if (query instanceof FieldQueryNode) {
                transform(((FieldQueryNode) query));
                return;
            }
            query.getChildren().forEach(this::transform);
        }

        private void transform(FieldQueryNode query) {
            final String fieldValue = query.getTextAsString();
            switch (query.getFieldAsString()) {
                case "author" -> {
                    builder.author(fieldValue);
                }
                case "journal" -> {
                    builder.journal(fieldValue);
                }
                case "title" -> {
                    builder.titlePhrase(fieldValue);
                }
                case "year" -> {
                    builder.singleYear(Integer.valueOf(fieldValue));
                }
                case "year-range" -> {
                    String[] years = fieldValue.split("-");
                    if (years.length != 2) {
                        return;
                    }
                    builder.fromYearAndToYear(Integer.valueOf(years[0]), Integer.valueOf(years[1]));
                }
                default -> {
                    builder.defaultFieldPhrase(fieldValue);
                }
            }
        }

    }
}
