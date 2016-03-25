package net.sf.jabref.exporter;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import com.google.common.base.Charsets;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

@RunWith(Parameterized.class)
public class ExportFormatTest {

    public IExportFormat exportFormat;
    public String exportFormatName;
    public static BibDatabase database;
    public static Charset charset;
    public static MetaData metaData;
    public static List<BibEntry> entries;

    public ExportFormatTest(IExportFormat format, String name) {
        exportFormat = format;
        exportFormatName = name;
    }


    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @BeforeClass
    public static void setUp() {
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader(Globals.prefs);
        database = new BibDatabase();
        charset = Charsets.UTF_8;
        metaData = new MetaData();
        entries = Collections.emptyList();
    }

    @Test
    public void testExportingEmptyDatabaseYieldsEmptyFile() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(database, metaData, filename, charset, entries);
        try (FileInputStream stream = new FileInputStream(tmpFile);
                InputStreamReader reader = new InputStreamReader(stream, charset)) {
            char[] buffer = new char[512];
            assertEquals(-1, reader.read(buffer)); // Empty file
        }
    }

    @Test(expected = NullPointerException.class)
    public void testExportingNullDatabaseThrowsNPE() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(null, metaData, filename, charset, entries);
    }

    @Test(expected = NullPointerException.class)
    public void testExportingNullEntriesThrowsNPE() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(database, metaData, filename, charset, null);
    }

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> exportFormats() {
        Collection<Object[]> result = new ArrayList<>();
        Globals.prefs = JabRefPreferences.getInstance();
        ExportFormats.initAllExports();
        for (IExportFormat format : ExportFormats.getExportFormats().values()) {
            result.add(new Object[] {format, format.getDisplayName()});
        }
        return result;
    }
}
