package net.sf.jabref.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

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

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        ExportFormats.initAllExports();
        exportFormat = ExportFormats.getExportFormat("html");

        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader(Globals.prefs);
        databaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
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

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void emitWellFormedHtml() throws Exception {
        File tmpFile = testFolder.newFile();
        String filename = tmpFile.getCanonicalPath();
        exportFormat.performExport(databaseContext, filename, charset, entries);
        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals("</html>", lines.get(lines.size() - 1));
    }
}
