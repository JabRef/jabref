package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AbbreviationCheckerTest {

    private JournalAbbreviationRepository abbreviationRepository;
    private AbbreviationChecker checker;

    @BeforeEach
    void setUp() {
        abbreviationRepository = new JournalAbbreviationRepository(new Abbreviation("Test Journal", "T. J."));
        checker = new AbbreviationChecker(abbreviationRepository);
    }

    @Test
    void checkValueComplainsAboutAbbreviatedJournalName() {
        assertNotEquals(Optional.empty(), checker.checkValue("T. J."));
    }

    @Test
    void checkValueDoesNotComplainAboutJournalNameThatHasSameAbbreviation() {
        abbreviationRepository.addEntry(new Abbreviation("Journal", "Journal"));
        assertEquals(Optional.empty(), checker.checkValue("Journal"));
    }
}
