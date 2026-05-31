package org.jabref.toolkit.service;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.StandardField;
import org.jabref.toolkit.commands.AbstractJabKitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportServiceTest extends AbstractJabKitTest {

    @Test
    void importBibtexFile() {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Optional<ParserResult> parserResult = ImportService.importBibTexFile(source, preferences, true);

        BibDatabase database = parserResult.get().getDatabase();
        assertTrue(database.getEntries().size() > 1);
        database.getEntries().forEach(entry -> {
            assertTrue(entry.getField(StandardField.TITLE).isPresent());
        });
    }

    @Test
    void importFileWithAutoDetection() {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Optional<ParserResult> parserResult = ImportService.importFile(source, "*", preferences, true);

        BibDatabase database = parserResult.get().getDatabase();
        assertTrue(database.getEntries().size() > 1);
        database.getEntries().forEach(entry -> {
            assertTrue(entry.getField(StandardField.TITLE).isPresent());
        });
    }
}
