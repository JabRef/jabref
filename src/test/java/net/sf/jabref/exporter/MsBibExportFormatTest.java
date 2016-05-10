package net.sf.jabref.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class MsBibExportFormatTest {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public File tempFile;
    public MSBibExportFormat msBibExportFormat;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        databaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
        charset = Charsets.UTF_8;
        msBibExportFormat = new MSBibExportFormat();
        tempFile = testFolder.newFile();
    }

    @Test
    public final void testPerformExportWithNoEntry() throws IOException {
        List<BibEntry> entries = Collections.emptyList();
        String tempFileName = tempFile.getCanonicalPath();
        msBibExportFormat.performExport(databaseContext, tempFileName, charset, entries);
        assertEquals(Collections.emptyList(), Files.readAllLines(tempFile.toPath()));
    }

    @Test
    public final void testPerformExportWithTestBib() throws IOException {
        try (FileInputStream stream = new FileInputStream("src/test/resources/net/sf/jabref/bibtexFiles/test.bib");
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(reader);
            BibDatabase database = result.getDatabase();
            String tempFilename = tempFile.getCanonicalPath();
            List<BibEntry> entries = database.getEntries();
            assertNotNull(entries);
            msBibExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);
            List<String> expected = Files.readAllLines(Paths.get("src/test/resources/net/sf/jabref/exporter/test.xml"));
            List<String> exported = Files.readAllLines(Paths.get(tempFilename));
            Collections.sort(expected);
            Collections.sort(exported);
            assertEquals(expected, exported);
        }
    }
}
