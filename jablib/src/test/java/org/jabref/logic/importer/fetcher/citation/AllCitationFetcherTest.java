package org.jabref.logic.importer.fetcher.citation;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@FetcherTest
class AllCitationFetcherTest {

    private AllCitationFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new AllCitationFetcher(
                mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(GrobidPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(AiService.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    void getReferences() throws FetcherException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1016/j.jksuci.2024.102118");
        assertNotNull(fetcher.getReferences(entry));
    }

    @Test
    void getCitations() throws FetcherException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1016/j.jksuci.2024.102118");
        assertNotNull(fetcher.getCitations(entry));
    }
}
