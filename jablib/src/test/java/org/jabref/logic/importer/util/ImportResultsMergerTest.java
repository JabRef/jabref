package org.jabref.logic.importer.util;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ImportResultsMergerTest {

    @Test
    void mergeImportResultsKeepsStringConstantsOfBibtexImport() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "#venue#");
        BibDatabase importedDatabase = new BibDatabase(List.of(entry));
        importedDatabase.addString(new BibtexString("venue", "Journal of Computer Science"));

        ParserResult result = ImportResultsMerger.mergeImportResults(
                List.of(new ImportFormatReader.ImportResult(ImportFormatReader.BIBTEX_FORMAT, new ParserResult(importedDatabase))),
                ',',
                mock(OwnerPreferences.class),
                mock(TimestampPreferences.class));

        assertEquals(Optional.of("Journal of Computer Science"),
                result.getDatabase().getStringByName("venue").map(BibtexString::getContent));
        assertEquals(Optional.of("#venue#"), entry.getField(StandardField.JOURNAL));
    }
}
