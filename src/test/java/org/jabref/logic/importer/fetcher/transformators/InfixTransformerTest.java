package org.jabref.logic.importer.fetcher.transformators;

import java.util.Optional;

import org.jabref.logic.importer.fetcher.transformators.AbstractQueryTransformer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Interface for all transformers that use infix notation for their logical binary operators
 */
public interface InfixTransformerTest {


    AbstractQueryTransformer getTransformator();

    /* All prefixes have to include the used separator
     * Example in the case of ':': <code>"author:"</code>
     */
    String getAuthorPrefix();

    String getUnFieldedPrefix();

    String getJournalPrefix();

    String getTitlePrefix();

    @Test
    default void convertAuthorField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\"");
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void convertUnFieldedTerm() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("\"default value\"");
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + "\"default value\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void convertExplicitUnFieldedTerm() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("default:\"default value\"");
        Optional<String> expected = Optional.of(getUnFieldedPrefix() + "\"default value\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void convertJournalField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("journal:Nature");
        Optional<String> expected = Optional.of(getJournalPrefix() + "\"Nature\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void convertAlphabeticallyFirstJournalField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("journal:Nature journal:\"Complex Networks\"");
        Optional<String> expected = Optional.of(getJournalPrefix() + "\"Nature\"" + getTransformator().getLogicalAndOperator() + getJournalPrefix() + "\"Complex Networks\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    void convertYearField() throws Exception;

    @Test
    void convertYearRangeField() throws Exception;

    @Test
    default void convertMultipleValuesWithTheSameField() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("author:\"Igor Steinmacher\" author:\"Christoph Treude\"");
        Optional<String> expected = Optional.of(getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformator().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Treude\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void groupedOperations() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\" AND author:\"Christoph Freunde\") AND title:test");
        Optional<String> expected = Optional.of("(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformator().getLogicalOrOperator() + "(" + getAuthorPrefix() + "\"Christoph Treude\"" + getTransformator().getLogicalAndOperator() + getAuthorPrefix() + "\"Christoph Freunde\"))" + getTransformator().getLogicalAndOperator() + getTitlePrefix() + "\"test\"");
        assertEquals(expected, searchQuery);
    }

    @Test
    default void notOperator() throws Exception {
        Optional<String> searchQuery = getTransformator().parseQueryStringIntoComplexQuery("!(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\")");
        Optional<String> expected = Optional.of(getTransformator().getLogicalNotOperator() + "(" + getAuthorPrefix() + "\"Igor Steinmacher\"" + getTransformator().getLogicalOrOperator() + getAuthorPrefix() + "\"Christoph Treude\")");
        assertEquals(expected, searchQuery);
    }
}
