package org.jabref.logic.importer.fetcher.transformators;

class JstorQueryTransformerTest implements InfixTransformerTest {

    @Override
    public AbstractQueryTransformer getTransformator() {
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

    }

    @Override
    public void convertYearRangeField() throws Exception {

    }
}
