package org.jabref.logic.journals;

import java.util.List;
import java.util.Optional;

import javafx.util.Pair;

import org.jabref.logic.importer.FetcherException;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@FetcherTest
class JournalInformationFetcherTest {

    private final JournalInformationFetcher fetcher = new JournalInformationFetcher();

    private final JournalInformation antiOxidantsJournal = new JournalInformation(
            "Antioxidants & Redox Signaling",
            "Mary Ann Liebert Inc.",
            "",
            "",
            "Biochemistry, Genetics and Molecular Biology, Medicine",
            "United States",
            "Biochemistry (Q1), Cell Biology (Q1), Clinical Biochemistry (Q1), Medicine (miscellaneous) (Q1), Molecular Biology (Q1), Physiology (Q1)",
            "27514",
            "217",
            "15230864, 15577716",
            List.of(new Pair<>(2021, 1.832), new Pair<>(2022, 1.706)),
            List.of(),
            List.of(new Pair<>(2021, 158.0), new Pair<>(2022, 217.0)),
            List.of(new Pair<>(2021, 530.0), new Pair<>(2022, 488.0)),
            List.of(new Pair<>(2021, 530.0), new Pair<>(2022, 487.0)),
            List.of(new Pair<>(2021, 24155.0), new Pair<>(2022, 19202.0)),
            List.of(new Pair<>(2021, 152.88), new Pair<>(2022, 130.63)),
            List.of(new Pair<>(2021, 4724.0), new Pair<>(2022, 3692.0)),
            List.of(new Pair<>(2021, 7.59), new Pair<>(2022, 7.21))
    );

    @Test
    public void getsName() {
        assertEquals("Journal Information", fetcher.getName());
    }

    @Test
    public void getsJournalInfoValidISSN() throws FetcherException {
        assertEquals(Optional.of(antiOxidantsJournal), fetcher.getJournalInformation("1523-0864"));
    }

    @Test
    public void getsJournalInfoValidISSNWithoutHyphen() throws FetcherException {
        assertEquals(Optional.of(antiOxidantsJournal), fetcher.getJournalInformation("15230864"));
    }

    @Test
    public void getsJournalInfoNonTrimmedISSN() throws FetcherException {
        assertEquals(Optional.of(antiOxidantsJournal), fetcher.getJournalInformation(" 1523-0864   "));
    }

    @Test
    public void getJournalInfoExtraSpaceISSN() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("1523 - 0864"));
    }

    @Test
    public void getJournalInfoEmptyISSN() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation(""));
    }

    @Test
    public void getJournalInfoInvalidISSN() {
        assertThrows(FetcherException.class, () -> fetcher.getJournalInformation("123-123"));
    }
}
