package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;


public class ModsExportFormatTest {

    public Charset charset;
    private ModsExporter modsExportFormat;
    private BibDatabaseContext databaseContext;
    private BibtexImporter bibtexImporter;
    private Path tempFile;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private Path importFile;

    @Before
    public void setUp() throws Exception {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExporter();
        bibtexImporter = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        tempFile = testFolder.newFile().toPath();
        importFile = Paths.get(ModsExportFormatTest.class.getResource("ModsExportFormatTestAllFields.bib").toURI());
    }

    @Test
    public final void exportForNoEntriesWritesNothing() throws Exception {
        modsExportFormat.export(databaseContext, tempFile, charset, Collections.emptyList());
        Assert.assertEquals(Collections.emptyList(), Files.readAllLines(tempFile));
    }
}
