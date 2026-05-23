package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.logic.citationkeypattern.CitationKeyGeneratorTestUtils;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("Localization.lang")
class CitationKeyDeviationCheckerTest {

    private final CitationKeyPatternPreferences citationKeyPatternPreferences = CitationKeyGeneratorTestUtils.getInstanceForTesting();

    @Test
    void citationKeyDeviatesFromGeneratedKey() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "WrongKey")
                                       .withField(StandardField.AUTHOR, "Knuth")
                                       .withField(StandardField.YEAR, "2014");
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(bibDatabase);
        CitationKeyDeviationChecker checker = new CitationKeyDeviationChecker(bibDatabaseContext, citationKeyPatternPreferences);

        List<IntegrityMessage> expected = List.of(new IntegrityMessage(
                Localization.lang("Citation key deviates from generated key %0", "Knuth2014"), entry, InternalField.KEY_FIELD));
        assertEquals(expected, checker.check(entry));
    }
}
