package net.sf.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ExportFormatTest {

    public IExportFormat exportFormat;
    public String exportFormatName;
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;

    public ExportFormatTest(IExportFormat format, String name) {
        exportFormat = format;
        exportFormatName = name;
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Before
    public void setUp() {
        databaseContext = new BibDatabaseContext();
        charset = Charsets.UTF_8;
        entries = Collections.emptyList();
    }

    @Test
    public void testExportingEmptyDatabaseYieldsEmptyFile() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(databaseContext, filename, charset, entries);
        assertEquals(Collections.emptyList(), Files.readAllLines(tmpFile.toPath()));
    }

    @Test(expected = NullPointerException.class)
    public void testExportingNullDatabaseThrowsNPE() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(null, filename, charset, entries);
    }

    @Test(expected = NullPointerException.class)
    public void testExportingNullEntriesThrowsNPE() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(databaseContext, filename, charset, null);
    }

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> exportFormats() {
        Collection<Object[]> result = new ArrayList<>();
        JabRefPreferences prefs = JabRefPreferences.getInstance();
        JournalAbbreviationLoader journalAbbreviationLoader = new JournalAbbreviationLoader();

        Map<String, ExportFormat> customFormats = prefs.customExports.getCustomExportFormats(prefs,
                journalAbbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = prefs
                .getLayoutFormatterPreferences(journalAbbreviationLoader);
        SavePreferences savePreferences = SavePreferences.loadForExportFromPreferences(prefs);
        ExportFormats.initAllExports(customFormats, layoutPreferences, savePreferences);

        for (IExportFormat format : ExportFormats.getExportFormats().values()) {
            result.add(new Object[] {format, format.getDisplayName()});
        }
        return result;
    }
}
