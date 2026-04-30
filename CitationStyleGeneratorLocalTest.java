package org.jabref.logic.citationstyle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

class CitationStyleGeneratorLocalTest {

    @Test
    void returnsCitationTextWhenValidStyleIsProvided() {
        Optional<CitationStyle> style = CSLStyleUtils.createCitationStyleFromFile("ieee.csl");
        assertTrue(style.isPresent());

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Smith, John");
        entry.setField(StandardField.TITLE, "Testing Citation Generation");
        entry.setField(StandardField.JOURNAL, "Journal of Testing");
        entry.setField(StandardField.YEAR, "2024");

        List<BibEntry> entries = List.of(entry);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

        String result = CitationStyleGenerator.generateCitation(
                entries,
                style.get().getSource(),
                CitationStyleOutputFormat.TEXT,
                context,
                entryTypesManager
        );

        assertFalse(result.isBlank());
    }
}
