package org.jabref.logic.citationstyle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

class CitationStyleBibliographyLocalTest {

    @Test
    void returnsSafeErrorMessageWhenBibliographyGenerationFails() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Smith, John");
        entry.setField(StandardField.TITLE, "Testing Bibliography Generation");
        entry.setField(StandardField.JOURNAL, "Journal of Testing");
        entry.setField(StandardField.YEAR, "2024");

        List<BibEntry> entries = List.of(entry);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

        List<String> result = CitationStyleGenerator.generateBibliography(
                entries,
                "definitely-not-a-real-style.csl",
                CitationStyleOutputFormat.HTML,
                context,
                entryTypesManager
        );

        assertFalse(result.isEmpty());
        assertTrue(result.getFirst().contains("Cannot generate bibliography"));
    }
}
