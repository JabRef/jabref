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

import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.logic.importer.fileformat.ModsImporter;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.xmlunit.builder.Input;
import org.xmlunit.builder.Input.Builder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

@RunWith(Parameterized.class)
public class ModsExportFormatTestFiles {

    public Charset charset;
    private BibDatabaseContext databaseContext;
    private File tempFile;
    private ModsExportFormat modsExportFormat;
    private BibtexImporter bibtexImporter;
    private ModsImporter modsImporter;
    private Path importFile;


    @Parameter
    public String filename;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get(ModsExportFormatTestFiles.class.getResource("").toURI()))) {
            //            stream.forEach(n -> System.out.println(n));
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                    .filter(n -> n.startsWith("Mods")).collect(Collectors.toList());
        }
    }

    @Before
    public void setUp() throws Exception {
        databaseContext = new BibDatabaseContext();
        importFile = Paths.get(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExportFormat();
        tempFile = testFolder.newFile();
        bibtexImporter = new BibtexImporter(JabRefPreferences.getInstance().getImportFormatPreferences());
        modsImporter = new ModsImporter();
    }

    @Test
    public final void testPerformExport() throws Exception {
        String xmlFileName = filename.replace(".bib", ".xml");
        String tempFilename = tempFile.getCanonicalPath();
        List<BibEntry> entries = bibtexImporter.importDatabase(importFile, charset).getDatabase().getEntries();
        Path xmlFile = Paths.get(ModsExportFormatTestFiles.class.getResource(xmlFileName).toURI());

        modsExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);

        Builder control = Input.from(Files.newInputStream(xmlFile));
        Builder test = Input.from(Files.newInputStream(Paths.get(tempFilename)));

        Assert.assertThat(test, CompareMatcher.isSimilarTo(control)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }

    @Test
    public final void testExportAsModsAndThenImportAsMods() throws Exception {
        List<BibEntry> entries = bibtexImporter.importDatabase(importFile, charset).getDatabase().getEntries();

        modsExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);
        BibEntryAssert.assertEquals(entries, Paths.get(tempFile.getPath()), modsImporter);
    }

    @Test
    public final void testImportAsModsAndExportAsMods() throws Exception {
        String xmlFileName = filename.replace(".bib", ".xml");
        String tempFilename = tempFile.getCanonicalPath();
        Path xmlFile = Paths.get(ModsExportFormatTestFiles.class.getResource(xmlFileName).toURI());

        List<BibEntry> entries = modsImporter.importDatabase(xmlFile, charset).getDatabase().getEntries();

        modsExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);

        Builder control = Input.from(Files.newInputStream(xmlFile));
        Builder test = Input.from(Files.newInputStream(Paths.get(tempFilename)));

        Assert.assertThat(test, CompareMatcher.isSimilarTo(control)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }

}
