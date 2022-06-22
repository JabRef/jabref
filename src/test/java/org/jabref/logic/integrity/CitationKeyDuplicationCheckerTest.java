package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CitationKeyDuplicationCheckerTest {

    @Test
    void emptyCitationKey() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "")
                                       .withField(StandardField.AUTHOR, "Knuth")
                                       .withField(StandardField.YEAR, "2014");
        BibDatabase bibDatabase = new BibDatabase(List.of(entry));
        CitationKeyDuplicationChecker checker = new CitationKeyDuplicationChecker(bibDatabase);

        List<IntegrityMessage> expected = Collections.emptyList();
        assertEquals(expected, checker.check(entry));
    }

    @Test
    void hasDuplicateCitationKey() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "Knuth2014")
                                       .withField(StandardField.AUTHOR, "Knuth")
                                       .withField(StandardField.YEAR, "2014");
        BibEntry entry2 = new BibEntry().withField(InternalField.KEY_FIELD, "Knuth2014")
                                        .withField(StandardField.AUTHOR, "Knuth")
                                        .withField(StandardField.YEAR, "2014");
        BibDatabase bibDatabase = new BibDatabase(List.of(entry, entry2));
        CitationKeyDuplicationChecker checker = new CitationKeyDuplicationChecker(bibDatabase);

        List<IntegrityMessage> expected = Collections.singletonList(
                new IntegrityMessage(Localization.lang("Duplicate citation key"), entry, StandardField.KEY));
        assertEquals(expected, checker.check(entry));
    }
}
