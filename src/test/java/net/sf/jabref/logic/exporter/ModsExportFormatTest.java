package net.sf.jabref.logic.exporter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;


public class ModsExportFormatTest {

    public ModsExportFormat modsExportFormat;
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public File tempFile;
    public BibtexImporter testImporter;

    public static final String PATH_TO_FILE = "src/test/resources/net/sf/jabref/logic/exporter/";

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExportFormat();
        testImporter = new BibtexImporter(ImportFormatPreferences.fromPreferences(Globals.prefs));
    }

    @Test(expected = SaveException.class)
    public final void testPerformExportTrowsSaveException() throws IOException, URISyntaxException, SaveException {
        String filename = "ModsExportFormatTestAllFields.bib";
        Path importFile = Paths.get(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        List<BibEntry> entries = testImporter.importDatabase(importFile, Charset.defaultCharset()).getDatabase()
                .getEntries();

        modsExportFormat.performExport(databaseContext, "", charset, entries);
    }
}
