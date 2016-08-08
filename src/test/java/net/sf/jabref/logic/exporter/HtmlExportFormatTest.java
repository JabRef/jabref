package net.sf.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import com.google.common.base.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class HtmlExportFormatTest {
    private IExportFormat exportFormat;
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader();
        ExportFormats.initAllExports(
                Globals.prefs.customExports.getCustomExportFormats(Globals.prefs, Globals.journalAbbreviationLoader),
                LayoutFormatterPreferences.fromPreferences(Globals.prefs, Globals.journalAbbreviationLoader));
        exportFormat = ExportFormats.getExportFormat("html");

        databaseContext = new BibDatabaseContext();
        charset = Charsets.UTF_8;
        BibEntry entry = new BibEntry();
        entry.setField("title", "my paper title");
        entry.setField("author", "Stefan Kolb");
        entry.setCiteKey("mykey");
        entries = Arrays.asList(entry);
    }

    @After
    public void tearDown() {
        exportFormat = null;
    }

    @Test
    public void emitWellFormedHtml() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(databaseContext, filename, charset, entries);
        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals("</html>", lines.get(lines.size() - 1));
    }
}
