package org.jabref.logic.integrity;

import java.util.Collections;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AbbreviationCheckerTest {

    private JournalAbbreviationRepository abbreviationRepository;
    private AbbreviationChecker checker;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        abbreviationRepository = JournalAbbreviationLoader.loadBuiltInRepository();
        abbreviationRepository.addCustomAbbreviation(new Abbreviation("Test Journal", "T. J."));
        entry = new BibEntry(StandardEntryType.InProceedings);
        checker = new AbbreviationChecker(abbreviationRepository);
    }

    @Test
    void checkValueComplainsAboutAbbreviatedJournalName() {
        entry.setField(StandardField.BOOKTITLE, "T. J.");
        assertNotEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void checkValueDoesNotComplainAboutJournalNameThatHasSameAbbreviation() {
        entry.setField(StandardField.BOOKTITLE, "Journal");
        abbreviationRepository.addCustomAbbreviation(new Abbreviation("Journal", "Journal"));
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void checkValueDoesNotComplainAboutJournalNameThatHasΝοAbbreviation() {
        entry.setField(StandardField.BOOKTITLE, "IEEE Software");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void checkValueDoesNotComplainAboutJournalNameThatHasΝοInput() {
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void checkValueWorksForLaTeXFields() {
        entry.setField(StandardField.BOOKTITLE, "Subject-Oriented Business Process Management - Second International Conference, {S-BPM} {ONE} 2010, Karlsruhe, Germany, October 14, 2010. Selected Papers");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }
}
