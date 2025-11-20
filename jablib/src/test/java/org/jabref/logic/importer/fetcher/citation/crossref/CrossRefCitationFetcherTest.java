package org.jabref.logic.importer.fetcher.citation.crossref;

import java.util.List;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class CrossRefCitationFetcherTest {

    /// Test for <https://api.crossref.org/works/10.47397/tb/44-3/tb138kopp-jabref>
    @Test
    void getReferences() throws FetcherException {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getDefaultPlainCitationParser()).thenReturn(PlainCitationParserChoice.RULE_BASED_GENERAL);
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class);
        GrobidPreferences grobidPreferences = mock(GrobidPreferences.class);
        AiService aiService = mock(AiService.class);
        CrossRefCitationFetcher fetcher = new CrossRefCitationFetcher(
                importerPreferences, importFormatPreferences, citationKeyPatternPreferences, grobidPreferences, aiService);
        List<BibEntry> references = fetcher.getReferences(new BibEntry().withField(StandardField.DOI, "10.47397/tb/44-3/tb138kopp-jabref"));
        assertNotEquals(List.of(), references);
    }
}
