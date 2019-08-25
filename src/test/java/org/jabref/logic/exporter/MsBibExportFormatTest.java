package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MsBibExportFormatTest {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public MSBibExporter msBibExportFormat;

    @BeforeEach
    public void setUp() throws Exception {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        msBibExportFormat = new MSBibExporter();
    }

    @Test
    public final void testPerformExportWithNoEntry(@TempDir Path tempFile) throws IOException, SaveException {
        Path path = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(path);
        List<BibEntry> entries = Collections.emptyList();
        msBibExportFormat.export(databaseContext, path, charset, entries);
        assertEquals(Collections.emptyList(), Files.readAllLines(path));
    }
}
