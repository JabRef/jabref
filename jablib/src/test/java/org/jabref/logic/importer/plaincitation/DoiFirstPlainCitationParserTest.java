package org.jabref.logic.importer.plaincitation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.fileformat.pdf.RuleBasedBibliographyPdfImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// [utest->req~import.plain-citation.doi-first~2]
class DoiFirstPlainCitationParserTest {

    private static final String CITATION = "Vendome C, Bavota G, Di Penta M, Linares-Vásquez M, German DM, Poshyvanyk D (2017) License usage and changes: A large-scale study on GitHub. Empirical Software Engineering 22(3):1537–1577. https://doi.org/10.1007/s10664-016-9438-4";
    private static final BibEntry PARSED = new BibEntry().withField(StandardField.TITLE, "parsed");

    private final BibEntry fetched = new BibEntry().withField(StandardField.TITLE, "License usage and changes: a large-scale study on GitHub");
    private final PlainCitationParser delegate = mock(PlainCitationParser.class);
    private final IdBasedFetcher doiFetcher = mock(IdBasedFetcher.class);
    private final DoiFirstPlainCitationParser parser = new DoiFirstPlainCitationParser(delegate, doiFetcher);

    @Test
    void doiInTextIsFetchedWithoutParsing() throws FetcherException {
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(fetched));

        assertEquals(Optional.of(fetched), parser.parsePlainCitation(CITATION));
        verifyNoInteractions(delegate);
    }

    @Test
    void failedLookupFallsBackToParser() throws FetcherException {
        when(doiFetcher.performSearchById(anyString())).thenThrow(new FetcherException("offline"));
        when(delegate.parsePlainCitation(CITATION)).thenReturn(Optional.of(PARSED));

        assertEquals(Optional.of(PARSED), parser.parsePlainCitation(CITATION));
    }

    @Test
    void textWithoutDoiIsParsed() throws FetcherException {
        when(delegate.parsePlainCitation("no identifier here")).thenReturn(Optional.of(PARSED));

        assertEquals(Optional.of(PARSED), parser.parsePlainCitation("no identifier here"));
        verifyNoInteractions(doiFetcher);
    }

    @Test
    void standaloneDoiInBatchResolvesBeforeDelegate() throws FetcherException {
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(fetched));

        assertEquals(List.of(fetched), parser.parseMultiplePlainCitations("10.1007/s10664-016-9438-4"));
        verifyNoInteractions(delegate);
    }

    @Test
    void standaloneDoiInBatchFallsBackWhenLookupFails() throws FetcherException {
        when(doiFetcher.performSearchById(anyString())).thenThrow(new FetcherException("offline"));
        when(delegate.parseMultiplePlainCitations("10.1007/s10664-016-9438-4")).thenReturn(List.of(PARSED));

        assertEquals(List.of(PARSED), parser.parseMultiplePlainCitations("10.1007/s10664-016-9438-4"));
    }

    @Test
    void parsedEntriesWithDoiAreReplacedByFetchedEntries() throws FetcherException {
        BibEntry parsedWithDoi = new BibEntry().withField(StandardField.DOI, "10.1007/s10664-016-9438-4");
        when(delegate.parseMultiplePlainCitations("text")).thenReturn(List.of(parsedWithDoi, PARSED));
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(fetched));

        assertEquals(List.of(fetched, PARSED), parser.parseMultiplePlainCitations("text"));
    }

    @Test
    void doiLinkInUrlFieldIsFetched() throws FetcherException {
        BibEntry parsedWithDoiUrl = new BibEntry().withField(StandardField.URL, "https://doi.org/10.1007/s10664-016-9438-4");
        when(delegate.parseMultiplePlainCitations("text")).thenReturn(List.of(parsedWithDoiUrl));
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(fetched));

        assertEquals(List.of(fetched), parser.parseMultiplePlainCitations("text"));
    }

    @Test
    void fetchedSingleCitationRetainsRequestedDoi() throws FetcherException {
        BibEntry fetched = new BibEntry().withField(StandardField.TITLE, "fetched");
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(fetched));

        BibEntry expected = new BibEntry().withField(StandardField.TITLE, "fetched")
                                          .withField(StandardField.DOI, "10.1007/s10664-016-9438-4");
        assertEquals(Optional.of(expected), parser.parsePlainCitation(CITATION));
    }

    @Test
    void batchRetainsParsedKeyAndMissingFieldsWithoutOverwritingFetchedMetadata() throws FetcherException {
        BibEntry parsed = new BibEntry().withCitationKey("parsedKey")
                                        .withField(StandardField.DOI, "10.1007/s10664-016-9438-4")
                                        .withField(StandardField.TITLE, "guessed")
                                        .withField(StandardField.COMMENT, "original reference");
        BibEntry fetched = new BibEntry().withField(StandardField.TITLE, "fetched");
        when(delegate.parseMultiplePlainCitations("text")).thenReturn(List.of(parsed));
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(fetched));

        BibEntry expected = new BibEntry().withCitationKey("parsedKey")
                                          .withField(StandardField.DOI, "10.1007/s10664-016-9438-4")
                                          .withField(StandardField.TITLE, "fetched")
                                          .withField(StandardField.COMMENT, "original reference");
        assertEquals(List.of(expected), parser.parseMultiplePlainCitations("text"));
    }

    @Test
    void batchResolvesUppercaseAndBareDoiInIeeeCitations() throws FetcherException {
        DoiFirstPlainCitationParser ieeeParser = new DoiFirstPlainCitationParser(new RuleBasedBibliographyPdfImporter(), doiFetcher);
        BibEntry first = new BibEntry().withField(StandardField.TITLE, "first fetched");
        BibEntry second = new BibEntry().withField(StandardField.TITLE, "second fetched");
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(first));
        when(doiFetcher.performSearchById("10.1109/ICWS.2007.59")).thenReturn(Optional.of(second));

        List<BibEntry> entries = ieeeParser.parseMultiplePlainCitations(
                "First citation DOI:10.1007/s10664-016-9438-4\n\nSecond citation 10.1109/ICWS.2007.59");

        assertEquals(List.of(Optional.of("first fetched"), Optional.of("second fetched")),
                entries.stream().map(entry -> entry.getField(StandardField.TITLE)).toList());
    }

    @Test
    void resolvesTrackedDoiLink() throws FetcherException {
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(fetched));

        assertEquals(Optional.of(fetched), parser.parsePlainCitation("https://doi.org/10.1007/s10664-016-9438-4?utm_source=test"));
    }

    @Test
    void resolvesParenthesizedCitationWithoutDroppingDoiParentheses() throws FetcherException {
        String doi = "10.1002/(sici)1099-1476(20000125)23:2<147::aid-mma108>3.0.co;2-j";
        when(doiFetcher.performSearchById(doi)).thenReturn(Optional.of(fetched));

        assertEquals(Optional.of(fetched), parser.parsePlainCitation("Citation (doi:" + doi + ")."));
    }

    @Test
    void resolvesDoiEndingInBalancedParenthesis() throws FetcherException {
        String doi = "10.1000/example(test)";
        when(doiFetcher.performSearchById(doi)).thenReturn(Optional.of(fetched));

        assertEquals(Optional.of(fetched), parser.parsePlainCitation("Citation (doi:" + doi + ")."));
    }
}
