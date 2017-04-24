package org.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;


public class ModsExportFormatTest {

    public Charset charset;
    private ModsExportFormat modsExportFormat;
    private BibDatabaseContext databaseContext;
    private BibtexImporter bibtexImporter;
    private File tempFile;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private Path importFile;


    @Before
    public void setUp() throws Exception {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExportFormat();
        bibtexImporter = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
        tempFile = testFolder.newFile();
        importFile = Paths.get(ModsExportFormatTest.class.getResource("ModsExportFormatTestAllFields.bib").toURI());
    }

    @Test(expected = SaveException.class)
    public final void testPerformExportTrowsSaveException() throws Exception {
        List<BibEntry> entries = bibtexImporter.importDatabase(importFile, charset).getDatabase().getEntries();

        modsExportFormat.performExport(databaseContext, "", charset, entries);
    }

    @Test
    public final void testPerformExportEmptyEntry() throws Exception {
        String canonicalPath = tempFile.getCanonicalPath();
        modsExportFormat.performExport(databaseContext, canonicalPath, charset, Collections.emptyList());
        Assert.assertEquals(Collections.emptyList(), Files.readAllLines(Paths.get(canonicalPath)));
    }
}
