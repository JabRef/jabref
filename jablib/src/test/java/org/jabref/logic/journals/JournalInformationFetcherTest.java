package org.jabref.logic.journals;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.JournalInformationFetcher;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@FetcherTest
class JournalInformationFetcherTest {

    private final JournalInformationFetcher fetcher = new JournalInformationFetcher();

    @Test
    void getsName() {
        assertEquals("Journal Information", fetcher.getName());
    }

    @Test
    void getsJournalInfoUsingIssn() throws FetcherException {
        JournalInformation journalInformation = fetcher.getJournalInformation("1545-4509", "").orElseThrow();

        assertEquals("Annual Review of Biochemistry", journalInformation.title());
        assertEquals("Annual Reviews", journalInformation.publisher());
        assertEquals("0066-4154, 1545-4509", journalInformation.issn());
    }

    @Test
    void getsJournalInfoUsingName() throws FetcherException {
        JournalInformation journalInformation = fetcher.getJournalInformation("", "Annual Review of Biochemistry").orElseThrow();

        assertEquals("Annual Review of Biochemistry", journalInformation.title());
    }

    @Test
    void getsJournalInfoUsingIssnWithoutHyphen() throws FetcherException {
        JournalInformation journalInformation = fetcher.getJournalInformation("15454509", "").orElseThrow();

        assertEquals("Annual Review of Biochemistry", journalInformation.title());
    }

    @Test
    void getsJournalInfoUsingTrimmedIssn() throws FetcherException {
        JournalInformation journalInformation = fetcher.getJournalInformation(" 1545-4509   ", "").orElseThrow();

        assertEquals("Annual Review of Biochemistry", journalInformation.title());
    }

    @Test
    void throwsForInvalidIssnWithoutJournalName() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("123-123", ""));
    }

    @Test
    void throwsWhenNoIssnOrJournalNameIsProvided() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("", ""));
    }

    @Test
    void throwsWhenJournalNameIsNotFound() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("", "zzz"));
    }
}
