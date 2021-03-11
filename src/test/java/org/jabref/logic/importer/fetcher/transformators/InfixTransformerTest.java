package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Interface for all transformers that use infix notation for their logical binary operators
 */
public interface InfixTransformerTest {

    AbstractQueryTransformer getTransformer();

    /* All prefixes have to include the used separator
     * Example in the case of ':': <code>"author:"</code>
     */
    String getAuthorPrefix();

    String getUnFieldedPrefix();

    String getJournalPrefix();

    String getTitlePrefix();

    @Test
    default void convertAuthorField() throws Exception {
        String queryString = "author:\"Igor Steinmacher\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void convertUnFieldedTerm() throws Exception {
        String queryString = "\"default value\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + queryString);
        assertEquals(expected, searchQuery);
    }

    @Test
    default void convertExplicitUnFieldedTerm() throws Exception {
        String queryString = "default:\"default value\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + "\"default value\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void convertJournalField() throws Exception {
        String queryString = "journal:Nature";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getJournalPrefix() + "\"Nature\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    void convertYearField() throws Exception;

    @Test
    void convertYearRangeField() throws Exception;

    @Test
    default void convertMultipleValuesWithTheSameField() throws Exception {
        String queryString = "author:\"Igor Steinmacher\" author:\"Christoph Treude\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Treude\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void groupedOperations() throws Exception {
        String queryString = "(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\" AND author:\"Christoph Freunde\") AND title:test";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalOrOperator() + "(" + getAuthorPrefix() + "\"Christoph Treude\"" + getTransformer().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Freunde\"))" + getTransformer().getLogicalAndOperator() + getTitlePrefix() + "\"test\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void notOperator() throws Exception {
        String queryString = "!(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\")";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getTransformer().getLogicalNotOperator() + "(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformer().getLogicalOrOperator() + getAuthorPrefix() + "\"Christoph Treude\")");
        assertEquals(expected, searchQuery);
    }
}
