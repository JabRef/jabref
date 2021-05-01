package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JstorQueryTransformerTest extends InfixTransformerTest<JstorQueryTransformer> {

    @Override
    public JstorQueryTransformer getTransformer() {
        return new JstorQueryTransformer();
    }

    @Override
    public String getAuthorPrefix() {
        return "au:";
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "pt:";
    }

    @Override
    public String getTitlePrefix() {
        return "ti:";
    }

    @Override
    public void convertYearField() throws Exception {
        String queryString = "year:2018";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> query = getTransformer().transformLuceneQuery(luceneQuery);
        assertEquals(Optional.of("sd:2018 AND ed:2018"), query);
    }

    @Override
    public void convertYearRangeField() throws Exception {
        String queryString = "year-range:2018-2021";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> query = getTransformer().transformLuceneQuery(luceneQuery);
        assertEquals(Optional.of("sd:2018 AND ed:2021"), query);
    }
}
