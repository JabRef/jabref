package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@FetcherTest
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

    @Test
    public void searchByIdThrowsExceptionForShortInvalidISBN() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("123456789"));
    }

    @Test
    public void searchByIdThrowsExceptionForLongInvalidISB() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("012345678910"));
    }

    @Test
    public void searchByIdThrowsExceptionForInvalidISBN() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("jabref-4-ever"));
    }

}
