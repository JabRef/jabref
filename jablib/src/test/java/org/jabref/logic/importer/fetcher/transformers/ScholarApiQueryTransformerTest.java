package org.jabref.logic.importer.fetcher.transformers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ScholarApiQueryTransformerTest extends YearAndYearRangeByFilteringQueryTransformerTest<ScholarApiQueryTransformer> {

    @Override
    public ScholarApiQueryTransformer getTransformer() {
        return new ScholarApiQueryTransformer();
    }

    @Test
    @Disabled("ScholarAPI has no journal scoped search")
    @Override
    public void convertJournalFieldPrefix() {
    }
}
