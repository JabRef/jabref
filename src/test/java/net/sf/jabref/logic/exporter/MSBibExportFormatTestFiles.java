package net.sf.jabref.logic.exporter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
<<<<<<< ad6b5cc1ff518ac2366c3f82810b7a7f24cc7ed6
import net.sf.jabref.importer.fileformat.BibtexImporter;
=======
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.model.database.BibDatabase;
>>>>>>> Refactored importer and split into logic and gui
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MSBibExportFormatTestFiles {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public File tempFile;
    public MSBibExportFormat msBibExportFormat;
    public BibtexImporter testImporter;

    public static final String PATH_TO_FILE = "src/test/resources/net/sf/jabref/logic/exporter/";

    @Parameter
    public String filename;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(PATH_TO_FILE))) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                    .filter(n -> n.startsWith("MsBib")).collect(Collectors.toList());
        }
    }

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        databaseContext = new BibDatabaseContext();
        charset = Charsets.UTF_8;
        msBibExportFormat = new MSBibExportFormat();
        tempFile = testFolder.newFile();
        testImporter = new BibtexImporter(ImportFormatPreferences.fromPreferences(Globals.prefs));
    }

    @Test
    public final void testPerformExport() throws IOException, URISyntaxException, SaveException {
        String xmlFileName = filename.replace(".bib", ".xml");
        Path importFile = Paths.get(MSBibExportFormatTestFiles.class.getResource(filename).toURI());
        String tempFilename = tempFile.getCanonicalPath();
        List<BibEntry> entries = testImporter.importDatabase(importFile, Charset.defaultCharset()).getDatabase()
                .getEntries();

        msBibExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);

        List<String> expected = Files.readAllLines(Paths.get(PATH_TO_FILE + xmlFileName));
        List<String> exported = Files.readAllLines(Paths.get(tempFilename));
        Collections.sort(expected);
        Collections.sort(exported);
        assertEquals(expected, exported);
    }
}
