package org.jabref.model.entry;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrossrefTest {

    private static BibDatabase database;

    @BeforeAll
    static void SetUp() throws ImportException, URISyntaxException {
        ImportFormatReader reader = new ImportFormatReader();
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        reader.resetImportFormats(importFormatPreferences, mock(XmpPreferences.class), new DummyFileUpdateMonitor());

        Path file = Paths.get(CrossrefTest.class.getResource("crossref.bib").toURI());
        database = reader.importFromFile("bibtex", file).getDatabase();
    }

    private BibEntry getEntry(String key) {
        var entries = database.getEntriesByKey(key);
        assertEquals(1, entries.size());
        return entries.get(0);
    }

    @Test
    void inproceedings_proceedings_inheritance() {
        var source = getEntry("pr_001");
        var target = getEntry("inpr_001");

        assertEquals(
            source.getResolvedFieldOrAlias(StandardField.YEAR, database),
            target.getResolvedFieldOrAlias(StandardField.YEAR, database)
        );

        assertEquals(
            source.getResolvedFieldOrAlias(StandardField.TITLE, database),
            target.getResolvedFieldOrAlias(StandardField.BOOKTITLE, database)
        );
    }

    @Test
    void inproceedings_proceedings_no_inheritance() {
        var target = getEntry("inpr_001");

        assertFalse(target.getResolvedFieldOrAlias(StandardField.TITLE, database).isPresent());
    }

    @Test
    void inproceedings_proceedings_no_overwrite() {
        var source = getEntry("pr_001");
        var target = getEntry("inpr_002");

        assertNotEquals(
                source.getResolvedFieldOrAlias(StandardField.YEAR, database),
                target.getResolvedFieldOrAlias(StandardField.YEAR, database)
        );

        assertNotEquals(
                source.getResolvedFieldOrAlias(StandardField.TITLE, database),
                target.getResolvedFieldOrAlias(StandardField.BOOKTITLE, database)
        );
    }
}
