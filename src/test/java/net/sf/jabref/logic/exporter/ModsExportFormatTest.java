package net.sf.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;


public class ModsExportFormatTest {

    public Charset charset;
    private ModsExportFormat modsExportFormat;
    private BibDatabaseContext databaseContext;
    private BibtexImporter testImporter;

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExportFormat();
        testImporter = new BibtexImporter(Globals.prefs.getImportFormatPreferences());
    }

    @Test(expected = SaveException.class)
    public final void testPerformExportTrowsSaveException() throws Exception {
        String filename = "ModsExportFormatTestAllFields.bib";
        Path importFile = Paths.get(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        List<BibEntry> entries = testImporter.importDatabase(importFile, charset).getDatabase()
                .getEntries();

        modsExportFormat.performExport(databaseContext, "", charset, entries);
    }

    @Test
    public final void testPerformExportEmptyEntry() throws Exception {
        modsExportFormat.performExport(databaseContext, "", charset, Collections.emptyList());
    }
}
