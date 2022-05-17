package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Interface for all transformers that use infix notation for their logical binary operators
 */
public abstract class SuffixTransformerTest<T extends AbstractQueryTransformer> {

    protected abstract T getTransformer();

    protected abstract String getAuthorSuffix();

    protected abstract String getUnFieldedSuffix();

    protected abstract String getJournalSuffix();

    protected abstract String getTitleSuffix();

    @Test
    public void convertAuthorFieldSuffix() throws Exception {
        String queryString = "author:\"Igor Steinmacher\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("\"Igor Steinmacher\"" + getAuthorSuffix());
        assertEquals(expected, searchQuery);
    }

    @Test
    public void convertUnFieldedTermSuffix() throws Exception {
        String queryString = "\"default value\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(queryString + getUnFieldedSuffix());
        assertEquals(expected, searchQuery);
    }

    @Test
    public void convertExplicitUnFieldedTermSuffix() throws Exception {
        String queryString = "default:\"default value\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("\"default value\"" + getUnFieldedSuffix());
        assertEquals(expected, searchQuery);
    }

    @Test
    public void convertJournalFieldSuffix() throws Exception {
        String queryString = "journal:Nature";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("Nature" + getJournalSuffix());
        assertEquals(expected, searchQuery);
    }

    @Test
    public abstract void convertYearField() throws Exception;

    @Test
    public abstract void convertYearRangeField() throws Exception;

    @Test
    public void convertMultipleValuesWithTheSameSuffix() throws Exception {
        String queryString = "author:\"Igor Steinmacher\" author:\"Christoph Treude\"";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("\"Igor Steinmacher\"" + getAuthorSuffix() + getTransformer().getLogicalAndOperator() + "\"Christoph Treude\"" + getAuthorSuffix());
        assertEquals(expected, searchQuery);
    }

    @Test
    public void groupedOperationsSuffix() throws Exception {
        String queryString = "(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\" AND author:\"Christoph Freunde\") AND title:test";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of("(" + "\"Igor Steinmacher\"" + getAuthorSuffix() + getTransformer().getLogicalOrOperator() + "(" + "\"Christoph Treude\"" + getAuthorSuffix() + getTransformer().getLogicalAndOperator() + "\"Christoph Freunde\"" + getAuthorSuffix() + "))" + getTransformer().getLogicalAndOperator() + "test" + getTitleSuffix());
        assertEquals(expected, searchQuery);
    }

    @Test
    public void notOperatorSufix() throws Exception {
        String queryString = "!(author:\"Igor Steinmacher\" OR author:\"Christoph Treude\")";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);
        Optional<String> expected = Optional.of(getTransformer().getLogicalNotOperator() + "(" + "\"Igor Steinmacher\"" + getAuthorSuffix() + getTransformer().getLogicalOrOperator() + "\"Christoph Treude\")" + getAuthorSuffix());
        assertEquals(expected, searchQuery);
    }
}
