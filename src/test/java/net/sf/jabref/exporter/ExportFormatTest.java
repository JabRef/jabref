package net.sf.jabref.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class ExportFormatTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        ExportFormats.initAllExports();
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader(Globals.prefs);

    }

    @Test
    public void testExportingEmptyDatabaseLayoutBasedFormat() throws Exception {
        BibDatabase db = new BibDatabase();
        Map<String, IExportFormat> exportFormats = ExportFormats.getExportFormats();
        IExportFormat exportFormat = exportFormats.get("html");
        try {
            File tmpFile = File.createTempFile("exporttest", "html");
            tmpFile.deleteOnExit();
            String filename = tmpFile.getCanonicalPath();
            List<BibEntry> entries = Collections.emptyList();
            Charset charset = Charsets.UTF_8;
            MetaData metaData = new MetaData();
            exportFormat.performExport(db, metaData, filename, charset, entries);
        } catch (IOException e) {
            Assert.fail("Exception caught: " + e.toString() + e.getMessage());
        }
    }

    @Test
    public void testExportingEmptyDatabaseClassBasedFormat() throws Exception {
        BibDatabase db = new BibDatabase();
        Map<String, IExportFormat> exportFormats = ExportFormats.getExportFormats();
        IExportFormat exportFormat = exportFormats.get("oocalc");
        try {
            File tmpFile = File.createTempFile("exporttest", "oocalc");
            tmpFile.deleteOnExit();
            String filename = tmpFile.getCanonicalPath();
            List<BibEntry> entries = Collections.emptyList();
            Charset charset = Charsets.UTF_8;
            MetaData metaData = new MetaData();
            exportFormat.performExport(db, metaData, filename, charset, entries);
        } catch (IOException e) {
            Assert.fail("Exception caught: " + e.toString() + e.getMessage());
        }
    }

}
