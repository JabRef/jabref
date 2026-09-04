package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.ExternalServicesTest;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExternalServicesTest
@NullMarked
public class SwhidFetcherTest {

    private static final String PARMAP_SWHID = "swh:1:dir:2dc0f462d191524530f5612d2935851505af41dd;origin=https://github.com/rdicosmo/parmap;visit=swh:1:snp:2128ed4f25f2d7ae7c8b7950a611d69cf4429063";

    private SwhidFetcher fetcher;

    @BeforeAll
    static void setUpRateLimiter() {
        SwhidFetcher.RATE_LIMITER.setRate(1000.0);
    }

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        fetcher = new SwhidFetcher(importFormatPreferences);
    }

    @Test
    void getName_returnsCorrectName() {
        assertEquals("Software Heritage", fetcher.getName());
    }

    @Test
    void performSearchById_validParampSwhid_returnsBibEntry() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById(PARMAP_SWHID);

        assertEquals(Optional.of("Parmap"), fetchedEntry.flatMap(entry -> entry.getField(StandardField.TITLE)));
        assertEquals(Optional.of("2011"), fetchedEntry.flatMap(entry -> entry.getField(StandardField.YEAR)));
        assertEquals(Optional.of("Di Cosmo, Roberto and Danelutto, Marco"), fetchedEntry.flatMap(entry -> entry.getField(StandardField.AUTHOR)));
        assertEquals(Optional.of(PARMAP_SWHID), fetchedEntry.flatMap(entry -> entry.getField(BiblatexSoftwareField.SWHID)));
    }

    @Test
    void performSearchById_nonExistentSwhid_returnsEmpty() throws FetcherException {
        String nonExistent = "swh:1:dir:0000000000000000000000000000000000000000";
        Optional<BibEntry> result = fetcher.performSearchById(nonExistent);
        assertTrue(result.isEmpty());
    }

    @Test
    void performSearchById_malformedIdentifier_throwsFetcherClientException() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("invalid-id"));
    }
}
