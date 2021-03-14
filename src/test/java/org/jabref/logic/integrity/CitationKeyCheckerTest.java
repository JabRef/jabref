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
    private final BibEntry entry = new BibEntry();

    @Test
    void bibTexAcceptsKeyFromAuthorAndYear() {
        entry.setField(InternalField.KEY_FIELD, "Knuth2014");
        entry.setField(StandardField.AUTHOR, "Knuth");
        entry.setField(StandardField.YEAR, "2014");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void KeyFromAuthorAndTitle() {
        entry.setField(InternalField.KEY_FIELD, "BrownTheTitle");
        entry.setField(StandardField.AUTHOR, "Brown");
        entry.setField(StandardField.TITLE, "The Title");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void KeyFromTitleAndYear() {
        entry.setField(InternalField.KEY_FIELD, "TheTitle2021");
        entry.setField(StandardField.TITLE, "The Title");
        entry.setField(StandardField.YEAR, "2021");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void emptyCitationKey() {
        entry.setField(StandardField.AUTHOR, "Brown");
        entry.setField(StandardField.TITLE, "The Title");
        entry.setField(StandardField.YEAR, "2021");
        List<IntegrityMessage> expected = Collections.singletonList(new IntegrityMessage(Localization.lang("empty citation key") + ": " + entry.getAuthorTitleYear(100), entry, InternalField.KEY_FIELD));
        assertEquals(expected, checker.check(entry));
    }
}
