package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GVKQueryTransformerTest extends InfixTransformerTest<GVKQueryTransformer> {

    @Override
    public GVKQueryTransformer getTransformer() {
        return new GVKQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "pica.per=";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "pica.all=";
    }

    @Override
    public String getJournalPrefix() {
        return "pica.zti=";
    }

    @Override
    public String getTitlePrefix() {
        return "pica.tit=";
    }

    @Override
    @Test
    public void convertYearField() throws Exception {
        String queryString = "year:2018";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> query = getTransformer().transformLuceneQuery(luceneQuery);

        Optional<String> expected = Optional.of("pica.erj=2018");
        assertEquals(expected, query);
    }

    @Disabled("Not supported by GVK")
    @Override
    @Test
    public void convertYearRangeField() throws Exception {
    }
}
