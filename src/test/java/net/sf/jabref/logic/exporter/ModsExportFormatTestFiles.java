package net.sf.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

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
public class ModsExportFormatTestFiles {

    public Charset charset;
    private BibDatabaseContext databaseContext;
    private File tempFile;
    private ModsExportFormat modsExportFormat;
    private BibtexImporter testImporter;

    @Parameter
    public String filename;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get(ModsExportFormatTestFiles.class.getResource("").toURI()))) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                    .filter(n -> n.startsWith("Mods")).collect(Collectors.toList());
        }
    }

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExportFormat();
        tempFile = testFolder.newFile();
        testImporter = new BibtexImporter(Globals.prefs.getImportFormatPreferences());
    }

    @Test
    public final void testPerformExport() throws Exception {
        String xmlFileName = filename.replace(".bib", ".xml");
        Path importFile = Paths.get(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        String tempFilename = tempFile.getCanonicalPath();
        List<BibEntry> entries = testImporter.importDatabase(importFile, Charset.defaultCharset()).getDatabase()
                .getEntries();

        modsExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);

        List<String> expected = Files.readAllLines(Paths.get(ModsExportFormatTestFiles.class.getResource("").toURI() + xmlFileName));
        List<String> exported = Files.readAllLines(Paths.get(tempFilename));
        Collections.sort(expected);
        Collections.sort(exported);
        assertEquals(expected, exported);
    }
}
