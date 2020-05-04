package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

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

public class ModsExportFormatTest {

    public Charset charset;
    private ModsExporter modsExportFormat;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    public void setUp() throws Exception {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExporter();
        new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        Path.of(ModsExportFormatTest.class.getResource("ModsExportFormatTestAllFields.bib").toURI());
    }

    @Test
    public final void exportForNoEntriesWritesNothing(@TempDir Path tempFile) throws Exception {
        Path file = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        modsExportFormat.export(databaseContext, tempFile, charset, Collections.emptyList());
        assertEquals(Collections.emptyList(), Files.readAllLines(file));
    }
}
