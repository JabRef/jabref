package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Interface for all transformers that use suffix notation for their logical binary operators
 */
public abstract class SuffixTransformerTest<T extends AbstractQueryTransformer> {

    protected abstract T getTransformer();

    protected abstract String getAuthorSuffix();

    protected abstract String getUnFieldedSuffix();

    protected abstract String getJournalSuffix();

    protected abstract String getTitleSuffix();

    @Test
    public void convertAuthorFieldSuffix() throws QueryNodeParseException {
        String queryString = "author=\"Igor Steinmacher\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("\"Igor Steinmacher\"" + getAuthorSuffix());
        assertEquals(expected, query);
    }

    @Test
    public void convertUnFieldedTermSuffix() throws QueryNodeParseException {
        String queryString = "\"default value\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(queryString + getUnFieldedSuffix());
        assertEquals(expected, query);
    }

    @Test
    public void convertExplicitUnFieldedTermSuffix() throws QueryNodeParseException {
        String queryString = "default=\"default value\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("\"default value\"" + getUnFieldedSuffix());
        assertEquals(expected, query);
    }

    @Test
    public void convertJournalFieldSuffix() throws QueryNodeParseException {
        String queryString = "journal=Nature";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("Nature" + getJournalSuffix());
        assertEquals(expected, query);
    }

    @Test
    public abstract void convertYearField();

    @Test
    public abstract void convertYearRangeField();

    @Test
    public void convertMultipleValuesWithTheSameSuffix() throws QueryNodeParseException {
        String queryString = "author=\"Igor Steinmacher\" author=\"Christoph Treude\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("\"Igor Steinmacher\"" + getAuthorSuffix() + getTransformer().getLogicalAndOperator() + "\"Christoph Treude\"" + getAuthorSuffix());
        assertEquals(expected, query);
    }

    @Test
    public void groupedOperationsSuffix() throws QueryNodeParseException {
        String queryString = "(author=\"Igor Steinmacher\" OR author=\"Christoph Treude\" AND author=\"Christoph Freunde\") AND title=test";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("(" + "\"Igor Steinmacher\"" + getAuthorSuffix() + getTransformer().getLogicalOrOperator() + "(" + "\"Christoph Treude\"" + getAuthorSuffix() + getTransformer().getLogicalAndOperator() + "\"Christoph Freunde\"" + getAuthorSuffix() + "))" + getTransformer().getLogicalAndOperator() + "test" + getTitleSuffix());
        assertEquals(expected, query);
    }

    @Test
    public void notOperatorSufix() throws QueryNodeParseException {
        String queryString = "!(author=\"Igor Steinmacher\" OR author=\"Christoph Treude\")";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(getTransformer().getLogicalNotOperator() + "(" + "\"Igor Steinmacher\"" + getAuthorSuffix() + getTransformer().getLogicalOrOperator() + "\"Christoph Treude\")" + getAuthorSuffix());
        assertEquals(expected, query);
    }
}
