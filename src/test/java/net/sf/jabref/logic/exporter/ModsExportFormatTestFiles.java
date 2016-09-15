package net.sf.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import com.google.common.base.Joiner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class ModsExportFormatTestFiles {

    public Charset charset;
    private BibDatabaseContext databaseContext;
    private File tempFile;
    private ModsExportFormat modsExportFormat;
    private BibtexImporter testImporter;

    @Parameter
    public String filename;
    public Path resourceDir;

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
        resourceDir = Paths.get(MSBibExportFormatTestFiles.class.getResource("").toURI());
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExportFormat();
        tempFile = testFolder.newFile();
        testImporter = new BibtexImporter(Globals.prefs.getImportFormatPreferences());
    }

    @Test
    public final void testPerformExport() throws Exception {
        String xmlFileName = filename.replace(".bib", ".xml");
        Path importFile = resourceDir.resolve(filename);
        String tempFilename = tempFile.getCanonicalPath();
        List<BibEntry> entries = testImporter.importDatabase(importFile, charset).getDatabase()
                .getEntries();

        modsExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);

        String control = Joiner.on("").join(Files.readAllLines(resourceDir.resolve(xmlFileName)));
        String test = Joiner.on("").join(Files.readAllLines(Paths.get(tempFilename)));
        assertThat(test, CompareMatcher.isSimilarTo(control)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}
