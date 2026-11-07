package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("Localization.lang")
class AbbreviationCheckerTest {

    private ConferenceAbbreviationRepository abbreviationRepository;
    private AbbreviationChecker checker;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        abbreviationRepository = new ConferenceAbbreviationRepository();
        abbreviationRepository.addCustomAbbreviation(new Abbreviation("Test Conference", "T. C."));
        entry = new BibEntry(StandardEntryType.InProceedings);
        checker = new AbbreviationChecker(abbreviationRepository);
    }

    @Test
    void checkEntryComplainsAboutAbbreviatedConferenceName() {
        entry.setField(StandardField.BOOKTITLE, "T. C.");
        assertEquals(List.of(new IntegrityMessage(Localization.lang("abbreviation detected"), entry, StandardField.BOOKTITLE)), checker.check(entry));
    }

    @Test
    void checkEntryDoesNotComplainAboutConferenceNameThatHasSameAbbreviation() {
        entry.setField(StandardField.BOOKTITLE, "Conference");
        abbreviationRepository.addCustomAbbreviation(new Abbreviation("Conference", "Conference"));
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryDoesNotComplainAboutConferenceNameThatHasNoAbbreviation() {
        entry.setField(StandardField.BOOKTITLE, "Non-Abbreviated-Title");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryDoesNotComplainAboutConferenceNameThatHasNoInput() {
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryWorksForLaTeXField() {
        entry.setField(StandardField.BOOKTITLE, "Reducing Complexity and Power of Digital Multibit Error-Feedback $\\Delta$$\\Sigma$ Modulators");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void checkEntryWorksForLaTeXFieldStillContainingIllegalChars() {
        entry.setField(StandardField.BOOKTITLE, "Proceedings of the 5\\({}^{\\mbox{th}}\\) Central-European Workshop on Services and their Composition, Rostock, Germany, February 21-22, 2013");
        assertEquals(List.of(), checker.check(entry));
    }
}
