package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQuery;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Interface for all transformers that use infix notation for their logical binary operators
 */
public abstract class InfixTransformerTest<T extends AbstractQueryTransformer> {

    protected abstract T getTransformer();

    /* All prefixes have to include the used separator
     * Example in the case of ':': <code>"author:"</code>
     */

    protected String getAuthorPrefix() {
        return "";
    }

    protected String getUnFieldedPrefix() {
        return "";
    }

    protected String getJournalPrefix() {
        return "";
    }

    protected String getTitlePrefix() {
        return "";
    }

    @Test
    public void convertAuthorFieldPrefix() throws QueryNodeParseException {
        String queryString = "author:\"Igor Steinmacher\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"");
        assertEquals(expected, query);
    }

    @Test
    public void convertUnFieldedTermPrefix() throws QueryNodeParseException {
        String queryString = "\"default value\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + queryString);
        assertEquals(expected, query);
    }

    @Test
    public void convertExplicitUnFieldedTermPrefix() throws QueryNodeParseException {
        String queryString = "default:\"default value\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + "\"default value\"");
        assertEquals(expected, query);
    }

    @Test
    public void convertJournalFieldPrefix() throws QueryNodeParseException {
        String queryString = "journal:Nature";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(getJournalPrefix() + "Nature");
        assertEquals(expected, query);
    }

    @Test
    public abstract void convertYearField() throws QueryNodeParseException;

    @Test
    public abstract void convertYearRangeField() throws QueryNodeParseException;

    @Test
    public void convertMultipleValuesWithTheSameFieldPrefix() throws QueryNodeParseException {
        String queryString = "author:\"Igor Steinmacher\" author:\"Christoph Treude\"";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Treude\"");
        assertEquals(expected, query);
    }

    @Test
    public void groupedOperationsPrefix() throws QueryNodeParseException {
        String queryString = "(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\" AND author:\"Christoph Freunde\") AND title:test";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of("(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalOrOperator() + "(" + getAuthorPrefix() + "\"Christoph Treude\"" + getTransformer().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Freunde\"))" + getTransformer().getLogicalAndOperator() + getTitlePrefix() + "test");
        assertEquals(expected, query);
    }

    @Test
    public void notOperatorPrefix() throws QueryNodeParseException {
        String queryString = "!(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\")";
        SearchQuery searchQuery = new SearchQuery(queryString);
        BaseQueryNode searchQueryList = new SearchQueryVisitor(searchQuery.getSearchFlags()).visitStart(searchQuery.getContext());
        Optional<String> query = getTransformer().transformSearchQuery(searchQueryList);
        Optional<String> expected = Optional.of(getTransformer().getLogicalNotOperator() + "(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalOrOperator() + getAuthorPrefix() + "\"Christoph Treude\")");
        assertEquals(expected, query);
    }
}
