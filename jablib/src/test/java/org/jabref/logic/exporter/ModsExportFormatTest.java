package org.jabref.logic.exporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ModsExportFormatTest {

    private ModsExporter modsExportFormat;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() throws URISyntaxException {
        databaseContext = new BibDatabaseContext.Builder().build();
        modsExportFormat = new ModsExporter();
        new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        Path.of(ModsExportFormatTest.class.getResource("ModsExportFormatTestAllFields.bib").toURI());
    }

    @Test
    final void exportForNoEntriesWritesNothing(@TempDir Path tempFile) throws IOException, SaveException {
        Path file = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        modsExportFormat.export(databaseContext, tempFile, List.of());
        assertEquals(List.of(), Files.readAllLines(file));
    }
}
