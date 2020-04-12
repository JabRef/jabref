package org.jabref.logic.integrity;

import java.util.Arrays;
import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

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

    @Test
    void journalNameAcceptsFullForm() {
        for (Field field : Arrays.asList(StandardField.BOOKTITLE, StandardField.JOURNAL)) {
            IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(field, "IEEE Software"));
        }
    }

    @Test
    void journalNameAcceptsEmptyInput() {
        for (Field field : Arrays.asList(StandardField.BOOKTITLE, StandardField.JOURNAL)) {
            IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(field, ""));
        }
    }

    @Test
    void journalNameDoesNotAcceptNonAbbreviatedForm() {
        for (Field field : Arrays.asList(StandardField.BOOKTITLE, StandardField.JOURNAL)) {
            IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(field, "IEEE SW"));
        }
    }
}
