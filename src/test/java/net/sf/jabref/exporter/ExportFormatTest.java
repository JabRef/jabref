package net.sf.jabref.exporter;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class ExportFormatTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        ExportFormats.initAllExports();
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader(Globals.prefs);

    }

    @Test
    public void testExportingEmptyDatabaseLayoutBasedFormat() throws Exception {
        BibDatabase database = new BibDatabase();
        Map<String, IExportFormat> exportFormats = ExportFormats.getExportFormats();
        IExportFormat exportFormat = exportFormats.get("ris");
        File tmpFile = testFolder.newFile();
        tmpFile.deleteOnExit();
        String filename = tmpFile.getCanonicalPath();
        List<BibEntry> entries = Collections.emptyList();
        Charset charset = Charsets.UTF_8;
        MetaData metaData = new MetaData();
        exportFormat.performExport(database, metaData, filename, charset, entries);
        assertTrue(tmpFile.exists());
        try (FileReader fileReader = new FileReader(tmpFile)) {
            char[] buffer = new char[512];
            assertEquals(-1, fileReader.read(buffer)); // Empty file
        }
    }

    @Test
    public void testExportingEmptyDatabaseClassBasedFormat() throws Exception {
        BibDatabase database = new BibDatabase();
        Map<String, IExportFormat> exportFormats = ExportFormats.getExportFormats();
        IExportFormat exportFormat = exportFormats.get("oocalc");
        File tmpFile = File.createTempFile("exporttest", "oocalc");
        tmpFile.deleteOnExit();
        String filename = tmpFile.getCanonicalPath();
        List<BibEntry> entries = Collections.emptyList();
        Charset charset = Charsets.UTF_8;
        MetaData metaData = new MetaData();
        exportFormat.performExport(database, metaData, filename, charset, entries);
        assertTrue(tmpFile.exists());
        try (FileReader fileReader = new FileReader(tmpFile)) {
            char[] buffer = new char[512];
            assertEquals(-1, fileReader.read(buffer)); // Empty file
        }
    }

}
