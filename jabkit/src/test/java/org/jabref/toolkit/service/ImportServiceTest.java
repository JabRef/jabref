package org.jabref.toolkit.service;

import java.nio.file.Path;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.StandardField;
import org.jabref.toolkit.commands.AbstractJabKitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportServiceTest extends AbstractJabKitTest {

    @Test
    void importBibtexFile() throws Exception {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        ParserResult parserResult = ImportService.importBibTexFile(source, preferences, true);

        BibDatabase database = parserResult.getDatabase();
        assertTrue(database.getEntries().size() > 1);
        database.getEntries().forEach(entry -> assertTrue(entry.getField(StandardField.TITLE).isPresent()));
    }

    @Test
    void importFileWithAutoDetection() throws Exception {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        ParserResult parserResult = ImportService.importFile(source, "*", preferences, true);

        BibDatabase database = parserResult.getDatabase();
        assertTrue(database.getEntries().size() > 1);
        database.getEntries().forEach(entry -> assertTrue(entry.getField(StandardField.TITLE).isPresent()));
    }
}
