package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public abstract class AbstractIsbnFetcherTest {

    protected AbstractIsbnFetcher fetcher;
    protected BibEntry bibEntry;

    public abstract void testName();

    public abstract void testHelpPage();

    public abstract void authorsAreCorrectlyFormatted() throws Exception;

    public abstract void searchByIdSuccessfulWithShortISBN() throws FetcherException;

    @Test
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("978-0321356680");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void searchByIdReturnsEmptyWithEmptyISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test(expected = FetcherException.class)
    public void searchByIdThrowsExceptionForShortInvalidISBN() throws FetcherException {
        fetcher.performSearchById("123456789");
    }

    @Test(expected = FetcherException.class)
    public void searchByIdThrowsExceptionForLongInvalidISB() throws FetcherException {
        fetcher.performSearchById("012345678910");
    }

    @Test(expected = FetcherException.class)
    public void searchByIdThrowsExceptionForInvalidISBN() throws FetcherException {
        fetcher.performSearchById("jabref-4-ever");
    }


}
