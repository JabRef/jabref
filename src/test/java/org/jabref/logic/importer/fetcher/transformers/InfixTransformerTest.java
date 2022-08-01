package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
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
    public void convertAuthorFieldPrefix() throws Exception {
        String queryString = "author:\"Igor Steinmacher\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    public void convertUnFieldedTermPrefix() throws Exception {
        String queryString = "\"default value\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + queryString);
        assertEquals(expected, searchQuery);
    }

    @Test
    public void convertExplicitUnFieldedTermPrefix() throws Exception {
        String queryString = "default:\"default value\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + "\"default value\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    public void convertJournalFieldPrefix() throws Exception {
        String queryString = "journal:Nature";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getJournalPrefix() + "Nature");
        assertEquals(expected, searchQuery);
    }

    @Test
    public abstract void convertYearField() throws Exception;

    @Test
    public abstract void convertYearRangeField() throws Exception;

    @Test
    public void convertMultipleValuesWithTheSameFieldPrefix() throws Exception {
        String queryString = "author:\"Igor Steinmacher\" author:\"Christoph Treude\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Treude\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    public void groupedOperationsPrefix() throws Exception {
        String queryString = "(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\" AND author:\"Christoph Freunde\") AND title:test";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalOrOperator() + "(" + getAuthorPrefix() + "\"Christoph Treude\"" + getTransformer().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Freunde\"))" + getTransformer().getLogicalAndOperator() + getTitlePrefix() + "test");
        assertEquals(expected, searchQuery);
    }

    @Test
    public void notOperatorPrefix() throws Exception {
        String queryString = "!(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\")";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getTransformer().getLogicalNotOperator() + "(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalOrOperator() + getAuthorPrefix() + "\"Christoph Treude\")");
        assertEquals(expected, searchQuery);
    }
}
