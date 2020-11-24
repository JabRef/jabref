package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
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
     * @param query The given query string
     * @return A complex query containing all fields of the query string
     */
    public Optional<ComplexSearchQuery> parseQueryStringIntoComplexQuery(String query) {
        try {
            StandardQueryParser parser = new StandardQueryParser();
            Query luceneQuery = parser.parse(query, "default");
            Set<Term> terms = new HashSet<>();
            // This implementation collects all terms from the leaves of the query tree independent of the internal boolean structure
            // If further capabilities are required in the future the visitor and ComplexSearchQuery has to be adapted accordingly.
            QueryVisitor visitor = QueryVisitor.termCollector(terms);
            luceneQuery.visit(visitor);

            List<Term> sortedTerms = new ArrayList<>(terms);
            sortedTerms.sort(Comparator.comparing(Term::text).reversed());
            return Optional.of(ComplexSearchQuery.fromTerms(sortedTerms));
        } catch (QueryNodeException | IllegalStateException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
