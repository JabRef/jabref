package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JournalInAbbreviationListCheckerTest {

    private JournalInAbbreviationListChecker checker;
    private JournalInAbbreviationListChecker checkerb;
    private JournalAbbreviationRepository abbreviationRepository;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        abbreviationRepository = JournalAbbreviationLoader.loadBuiltInRepository();
        abbreviationRepository.addCustomAbbreviation(new Abbreviation("IEEE Software", "IEEE SW"));
        checker = new JournalInAbbreviationListChecker(StandardField.JOURNAL, abbreviationRepository);
        checkerb = new JournalInAbbreviationListChecker(StandardField.JOURNALTITLE, abbreviationRepository);
        entry = new BibEntry();
    }

    @Test
    void journalAcceptsNameInTheList() {
        entry.setField(StandardField.JOURNAL, "IEEE Software");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void journalDoesNotAcceptNameNotInList() {
        entry.setField(StandardField.JOURNAL, "IEEE Whocares");
        assertEquals(List.of(new IntegrityMessage("journal not found in abbreviation list", entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void journalTitleDoesNotAcceptRandomInputInTitle() {
        entry.setField(StandardField.JOURNALTITLE, "A journal");
        assertEquals(List.of(new IntegrityMessage("journal not found in abbreviation list", entry, StandardField.JOURNALTITLE)), checkerb.check(entry));
    }

    @Test
    void journalDoesNotAcceptRandomInputInTitle() {
        entry.setField(StandardField.JOURNAL, "A journal");
        assertEquals(List.of(new IntegrityMessage("journal not found in abbreviation list", entry, StandardField.JOURNAL)), checker.check(entry));
    }
}
