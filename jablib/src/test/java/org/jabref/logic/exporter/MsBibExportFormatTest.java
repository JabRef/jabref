package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MsBibExportFormatTest {

    public BibDatabaseContext databaseContext;
    public MSBibExporter msBibExportFormat;

    @BeforeEach
    void setUp() {
        databaseContext = BibDatabaseContext.empty();
        msBibExportFormat = new MSBibExporter();
    }

    @Test
    final void performExportWithNoEntry(@TempDir Path tempFile) throws IOException, SaveException {
        Path path = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(path);
        List<BibEntry> entries = List.of();
        msBibExportFormat.export(databaseContext, path, entries);
        assertEquals(List.of(), Files.readAllLines(path));
    }
}
