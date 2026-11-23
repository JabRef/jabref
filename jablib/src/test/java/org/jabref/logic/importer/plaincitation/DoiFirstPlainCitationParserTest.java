package org.jabref.logic.importer.plaincitation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// [utest->req~import.plain-citation.doi-first~1]
class DoiFirstPlainCitationParserTest {

    private static final String CITATION = "Vendome C, Bavota G, Di Penta M, Linares-Vásquez M, German DM, Poshyvanyk D (2017) License usage and changes: A large-scale study on GitHub. Empirical Software Engineering 22(3):1537–1577. https://doi.org/10.1007/s10664-016-9438-4";
    private static final BibEntry FETCHED = new BibEntry().withField(StandardField.TITLE, "License usage and changes: a large-scale study on GitHub");
    private static final BibEntry PARSED = new BibEntry().withField(StandardField.TITLE, "parsed");

    private final PlainCitationParser delegate = mock(PlainCitationParser.class);
    private final IdBasedFetcher doiFetcher = mock(IdBasedFetcher.class);
    private final DoiFirstPlainCitationParser parser = new DoiFirstPlainCitationParser(delegate, doiFetcher);

    @Test
    void doiInTextIsFetchedWithoutParsing() throws FetcherException {
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(FETCHED));

        assertEquals(Optional.of(FETCHED), parser.parsePlainCitation(CITATION));
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
    void parsedEntriesWithDoiAreReplacedByFetchedEntries() throws FetcherException {
        BibEntry parsedWithDoi = new BibEntry().withField(StandardField.DOI, "10.1007/s10664-016-9438-4");
        when(delegate.parseMultiplePlainCitations("text")).thenReturn(List.of(parsedWithDoi, PARSED));
        when(doiFetcher.performSearchById("10.1007/s10664-016-9438-4")).thenReturn(Optional.of(FETCHED));

        assertEquals(List.of(FETCHED, PARSED), parser.parseMultiplePlainCitations("text"));
    }
}
