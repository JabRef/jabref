package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.ExternalServicesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExternalServicesTest
class IssnFetcherTest {

    private IssnFetcher fetcher;
    private BibEntry bibEntry;

    @BeforeEach
    void setUp() {
        fetcher = new IssnFetcher();

        bibEntry = new BibEntry()
                .withField(StandardField.ISSN, "15454509")
                .withField(StandardField.JOURNALTITLE, "Annual Review of Biochemistry")
                .withField(StandardField.PUBLISHER, "Annual Reviews");
    }

    @Test
    void performSearchByEntry() throws FetcherException {
        List<BibEntry> fetchedEntry = fetcher.performSearch(bibEntry);
        assertEquals(List.of(bibEntry), fetchedEntry);
    }

    @Test
    void performSearchById() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("15454509");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    void getName() {
        assertEquals("ISSN", fetcher.getName());
    }
}
