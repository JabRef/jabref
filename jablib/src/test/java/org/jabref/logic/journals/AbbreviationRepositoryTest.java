package org.jabref.logic.journals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for AbbreviationRepository to verify it works with both journal and conference abbreviations
class AbbreviationRepositoryTest {

    private AbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AbbreviationRepository();
    }

    @Test
    void repositoryWorksWithJournalAbbreviations() {
        Abbreviation journalAbbr = new Abbreviation(
                "Journal of Software Engineering",
                "J. Softw. Eng.",
                "JSE"
        );

        repository.addCustomAbbreviation(journalAbbr);

        assertTrue(repository.isKnownName("Journal of Software Engineering"));
        assertEquals(Optional.of("J. Softw. Eng."), repository.getDefaultAbbreviation("Journal of Software Engineering"));
    }

    @Test
    void repositoryWorksWithConferenceAbbreviations() {
        Abbreviation conferenceAbbr = new Abbreviation(
                "International Conference on Business Process Management",
                "BPM",
                "BPM"
        );

        repository.addCustomAbbreviation(conferenceAbbr);

        assertTrue(repository.isKnownName("International Conference on Business Process Management"));
        assertEquals(Optional.of("BPM"), repository.getDefaultAbbreviation("International Conference on Business Process Management"));
    }

    @Test
    void repositoryIsTypeAgnostic() {
        Abbreviation journal = new Abbreviation("Journal A", "J. A", "JA");
        Abbreviation conference = new Abbreviation("Conference B", "Conf. B", "CB");

        repository.addCustomAbbreviation(journal);
        repository.addCustomAbbreviation(conference);

        assertEquals(2, repository.getCustomAbbreviations().size());
        assertTrue(repository.isKnownName("Journal A"));
        assertTrue(repository.isKnownName("Conference B"));
    }

    @Test
    void repositoryHandlesMixedAbbreviations() {
        List<Abbreviation> abbreviations = List.of(
                new Abbreviation("IEEE Transactions on Software Engineering", "IEEE Trans. Softw. Eng.", "TSE"),
                new Abbreviation("International Conference on Software Engineering", "ICSE", "ICSE"),
                new Abbreviation("ACM Computing Surveys", "ACM Comput. Surv.", "CSUR")
        );

        repository.addCustomAbbreviations(abbreviations);

        assertEquals(3, repository.getCustomAbbreviations().size());
        assertEquals(Optional.of("IEEE Trans. Softw. Eng."), repository.getDefaultAbbreviation("IEEE Transactions on Software Engineering"));
        assertEquals(Optional.of("ICSE"), repository.getDefaultAbbreviation("International Conference on Software Engineering"));
        assertEquals(Optional.of("ACM Comput. Surv."), repository.getDefaultAbbreviation("ACM Computing Surveys"));
    }
}
