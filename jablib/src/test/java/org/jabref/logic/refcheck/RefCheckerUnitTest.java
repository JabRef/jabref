package org.jabref.logic.refcheck;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Unit tests for RefChecker using mocked fetchers.
/// These tests are fully deterministic and require no network access.
class RefCheckerUnitTest {

    private DoiFetcher doiFetcher;
    private ArXivFetcher arXivFetcher;
    private CrossRef crossRef;
    private RefChecker refChecker;

    private BibEntry localEntry;

    private BibEntry matchingAuthoritativeEntry;

    private BibEntry nonMatchingAuthoritativeEntry;

    @BeforeEach
    void setUp() throws FetcherException {
        doiFetcher = mock(DoiFetcher.class);
        arXivFetcher = mock(ArXivFetcher.class);
        crossRef = mock(CrossRef.class);
        refChecker = new RefChecker(doiFetcher, arXivFetcher, crossRef);

        localEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish and Shazeer, Noam")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.DOI, "10.48550/arXiv.1706.03762");

        matchingAuthoritativeEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish and Shazeer, Noam")
                .withField(StandardField.YEAR, "2017");

        nonMatchingAuthoritativeEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "A Completely Different Paper")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.YEAR, "2000");

        when(doiFetcher.performSearchById(anyString())).thenReturn(Optional.empty());
        when(crossRef.findIdentifier(any())).thenReturn(Optional.empty());
        when(arXivFetcher.findIdentifier(any())).thenReturn(Optional.empty());
    }

    @Test
    void doiMatchReturnsReal() throws FetcherException {
        when(doiFetcher.performSearchById(anyString()))
                .thenReturn(Optional.of(matchingAuthoritativeEntry));

        RefCheckResult result = refChecker.check(localEntry);

        assertEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void doiStrongMismatchReturnsFake() throws FetcherException {
        when(doiFetcher.performSearchById(anyString()))
                .thenReturn(Optional.of(nonMatchingAuthoritativeEntry));

        RefCheckResult result = refChecker.check(localEntry);

        assertEquals(RefValidity.FAKE, result.validity());
        assertNotNull(result.otherEntry());
    }

    @Test
    void doiNotFoundReturnsFakeWithNullMatch() throws FetcherException {
        when(doiFetcher.performSearchById(anyString())).thenReturn(Optional.empty());

        RefCheckResult result = refChecker.check(localEntry);

        assertEquals(RefValidity.FAKE, result.validity());
        assertNull(result.otherEntry());
    }

    @Test
    void doiExceptionReturnsFakeAndTriesNextSource() throws FetcherException {
        when(doiFetcher.performSearchById(anyString()))
                .thenThrow(new FetcherException("network error"));
        when(crossRef.findIdentifier(any())).thenReturn(Optional.empty());

        RefCheckResult result = refChecker.check(localEntry);

        assertEquals(RefValidity.FAKE, result.validity());
        assertNull(result.otherEntry());
    }

    @Test
    void crossRefDiscoversDOIAndReturnsReal() throws FetcherException {
        BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish and Shazeer, Noam")
                .withField(StandardField.YEAR, "2017");

        when(crossRef.findIdentifier(any()))
                .thenReturn(DOI.parse("10.48550/arXiv.1706.03762"));
        when(doiFetcher.performSearchById(anyString()))
                .thenReturn(Optional.of(matchingAuthoritativeEntry));

        RefCheckResult result = refChecker.check(entryWithoutDoi);

        assertEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void crossRefNotFoundReturnsFake() throws FetcherException {
        BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish")
                .withField(StandardField.YEAR, "2017");

        when(crossRef.findIdentifier(any())).thenReturn(Optional.empty());

        RefCheckResult result = refChecker.check(entryWithoutDoi);

        assertEquals(RefValidity.FAKE, result.validity());
    }

    @Test
    void crossRefExceptionReturnsFake() throws FetcherException {
        BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish")
                .withField(StandardField.YEAR, "2017");

        when(crossRef.findIdentifier(any()))
                .thenThrow(new FetcherException("network error"));

        RefCheckResult result = refChecker.check(entryWithoutDoi);

        assertEquals(RefValidity.FAKE, result.validity());
    }

    @Test
    void allSourcesFailReturnsFakeWithNullMatch() throws FetcherException {
        when(doiFetcher.performSearchById(anyString()))
                .thenThrow(new FetcherException("network error"));
        when(crossRef.findIdentifier(any()))
                .thenThrow(new FetcherException("network error"));
        when(arXivFetcher.findIdentifier(any()))
                .thenThrow(new FetcherException("network error"));

        RefCheckResult result = refChecker.check(localEntry);

        verify(doiFetcher).performSearchById(anyString());
        verify(crossRef).findIdentifier(any());
        verify(arXivFetcher).findIdentifier(any());

        assertEquals(RefValidity.FAKE, result.validity());
        assertNull(result.otherEntry());
    }

    @Test
    void notFoundVsFoundButMismatchDistinguishedByOtherEntry() throws FetcherException {
        when(doiFetcher.performSearchById(anyString())).thenReturn(Optional.empty());
        RefCheckResult notFound = refChecker.check(localEntry);
        assertNull(notFound.otherEntry());

        when(doiFetcher.performSearchById(anyString()))
                .thenReturn(Optional.of(nonMatchingAuthoritativeEntry));
        RefCheckResult foundButWrong = refChecker.check(localEntry);
        assertNotNull(foundButWrong.otherEntry());
    }

    @Test
    void doiPartialMismatchReturnsUnsure() throws FetcherException {
        BibEntry partialMatch = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish")
                .withField(StandardField.YEAR, "1990");

        when(doiFetcher.performSearchById(anyString())).thenReturn(Optional.of(partialMatch));

        RefCheckResult result = refChecker.check(localEntry);

        assertEquals(RefValidity.UNSURE, result.validity());
        assertNotNull(result.otherEntry());
    }

    @Test
    void crossRefMismatchReturnsFakeWithOtherEntry() throws FetcherException {
        BibEntry local = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Local Paper");

        when(crossRef.findIdentifier(any())).thenReturn(DOI.parse("10.1000/mismatch"));
        when(doiFetcher.performSearchById("10.1000/mismatch"))
                .thenReturn(Optional.of(nonMatchingAuthoritativeEntry));

        RefCheckResult result = refChecker.check(local);

        assertEquals(RefValidity.FAKE, result.validity());
        assertNotNull(result.otherEntry());
    }

    @Test
    void arXivMismatchReturnsFake() throws FetcherException {
        BibEntry entryWithNoIdentifiers = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Wrong Title")
                .withField(StandardField.AUTHOR, "Wrong Author")
                .withField(StandardField.YEAR, "2020");

        ArXivIdentifier arxivId = ArXivIdentifier.parse("2005.14165").orElseThrow();

        when(arXivFetcher.findIdentifier(any())).thenReturn(Optional.of(arxivId));
        when(arXivFetcher.performSearchById("2005.14165"))
                .thenReturn(Optional.of(nonMatchingAuthoritativeEntry));

        RefCheckResult result = refChecker.check(entryWithNoIdentifiers);

        verify(arXivFetcher).performSearchById("2005.14165");

        assertEquals(RefValidity.FAKE, result.validity());
        assertNotNull(result.otherEntry());
    }

    @Test
    void entryWithNoArXivIdReturnsFakeViaArXiv() throws FetcherException {
        when(arXivFetcher.findIdentifier(any())).thenReturn(Optional.empty());

        RefCheckResult result = refChecker.check(localEntry);
        assertEquals(RefValidity.FAKE, result.validity());
    }

    @Test
    void bestOfPrefersUnsureOverFake() throws FetcherException {
        when(doiFetcher.performSearchById(anyString())).thenReturn(Optional.of(nonMatchingAuthoritativeEntry));

        BibEntry partialMatch = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish")
                .withField(StandardField.YEAR, "1990");

        when(crossRef.findIdentifier(any())).thenReturn(DOI.parse("10.1000/unsure"));
        when(doiFetcher.performSearchById("10.1000/unsure")).thenReturn(Optional.of(partialMatch));

        RefCheckResult result = refChecker.check(localEntry);

        assertEquals(RefValidity.UNSURE, result.validity());
        assertEquals(partialMatch, result.otherEntry());
    }

    @Test
    void bestOfSameLevelUnsurePrefersHigherScore() throws FetcherException {
        BibEntry testLocal = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish")
                .withField(StandardField.YEAR, "2017");

        BibEntry weakUnsure = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Wrong Author")
                .withField(StandardField.YEAR, "1980");

        BibEntry strongerUnsure = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "V, Anish")
                .withField(StandardField.YEAR, "2010");

        when(crossRef.findIdentifier(any())).thenReturn(DOI.parse("10.1000/weak"));
        when(doiFetcher.performSearchById("10.1000/weak")).thenReturn(Optional.of(weakUnsure));

        ArXivIdentifier arxivId = ArXivIdentifier.parse("1706.03762").get();
        when(arXivFetcher.findIdentifier(any())).thenReturn(Optional.of(arxivId));
        when(arXivFetcher.performSearchById("1706.03762")).thenReturn(Optional.of(strongerUnsure));

        RefCheckResult result = refChecker.check(testLocal);

        assertEquals(RefValidity.UNSURE, result.validity());
        assertEquals(strongerUnsure, result.otherEntry());
    }

    @Test
    void entryWithLargerAuthorListThanAuthoritativeEntry() throws FetcherException {
        BibEntry authoritativeEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish")
                .withField(StandardField.YEAR, "2017");

        BibEntry testLocal = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish and Shazeer, Noam and Parmar, Niki and Uszkoreit, Jakob and Jones, Llion and Gomez, Aidan N and Kaiser, Lukasz and Polosukhin, Illia")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.DOI, "10.1000/123");

        when(doiFetcher.performSearchById("10.1000/123")).thenReturn(Optional.of(authoritativeEntry));

        RefCheckResult result = refChecker.check(testLocal);

        assertEquals(RefValidity.UNSURE, result.validity());
    }

    @Test
    void miscEntryIsClassifiedAsUnsureWithoutCallingAnyFetcher() throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Implement RefChecker in JabKit")
                .withField(StandardField.AUTHOR, "Oliver Kopp")
                .withField(StandardField.URL, "https://github.com/JabRef/jabref/issues/13604");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.UNSURE, result.validity());
        assertNull(result.otherEntry());
        verify(doiFetcher, never()).performSearchById(anyString());
        verify(crossRef, never()).findIdentifier(any());
        verify(arXivFetcher, never()).findIdentifier(any());
    }

    @Test
    void onlineEntryIsClassifiedAsUnsure() throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Online)
                .withField(StandardField.TITLE, "Some web page")
                .withField(StandardField.URL, "https://example.com");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.UNSURE, result.validity());
        verify(doiFetcher, never()).performSearchById(anyString());
    }

    @Test
    void unpublishedEntryIsClassifiedAsUnsure() throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Unpublished)
                .withField(StandardField.TITLE, "Draft paper")
                .withField(StandardField.AUTHOR, "Someone");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.UNSURE, result.validity());
        verify(doiFetcher, never()).performSearchById(anyString());
    }
}
