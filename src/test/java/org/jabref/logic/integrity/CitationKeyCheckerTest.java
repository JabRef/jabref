package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CitationKeyCheckerTest {

    private final CitationKeyChecker checker = new CitationKeyChecker();

    @Test
    void bibTexAcceptsKeyFromAuthorAndYear() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "Knuth2014")
                                       .withField(StandardField.AUTHOR, "Knuth")
                                       .withField(StandardField.YEAR, "2014");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void acceptsKeyFromAuthorAndTitle() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "BrownTheTitle")
                                       .withField(StandardField.AUTHOR, "Brown")
                                       .withField(StandardField.TITLE, "The Title");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void acceptsKeyFromTitleAndYear() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "TheTitle2021")
                                       .withField(StandardField.TITLE, "The Title")
                                       .withField(StandardField.YEAR, "2021");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void emptyCitationKey() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Brown")
                                       .withField(StandardField.TITLE, "The Title")
                                       .withField(StandardField.YEAR, "2021");
        List<IntegrityMessage> expected = Collections.singletonList(new IntegrityMessage(Localization.lang("empty citation key") + ": " + entry.getAuthorTitleYear(100), entry, InternalField.KEY_FIELD));
        assertEquals(expected, checker.check(entry));
    }
}
