package net.sf.jabref.logic.exporter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
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
public class BibTeXMLExporterTestFiles {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public File tempFile;
    public BibTeXMLExportFormat bibtexmlExportFormat;
    public BibtexImporter testImporter;

    @Parameter
    public String filename;
    public Path resourceDir;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException, URISyntaxException {
        try (Stream<Path> stream = Files.list(Paths.get(BibTeXMLExporterTestFiles.class.getResource("").toURI()))) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                    .filter(n -> n.startsWith("BibTeXML")).collect(Collectors.toList());
        }
    }

    @Before
    public void setUp() throws Exception {
        resourceDir = Paths.get(BibTeXMLExporterTestFiles.class.getResource("").toURI());
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        bibtexmlExportFormat = new BibTeXMLExportFormat();
        tempFile = testFolder.newFile();
        testImporter = new BibtexImporter(JabRefPreferences.getInstance().getImportFormatPreferences());
    }

    @Test
    public final void testPerformExport() throws IOException, SaveException {
        String xmlFileName = filename.replace(".bib", ".xml");
        Path importFile = resourceDir.resolve(filename);
        String tempFilename = tempFile.getCanonicalPath();

        List<BibEntry> entries = testImporter.importDatabase(importFile, StandardCharsets.UTF_8).getDatabase()
                .getEntries();

        bibtexmlExportFormat.performExport(databaseContext, tempFile.getPath(), charset, entries);

        Builder control = Input.from(Files.newInputStream(resourceDir.resolve(xmlFileName)));
        Builder test = Input.from(Files.newInputStream(Paths.get(tempFilename)));

        Assert.assertThat(test, CompareMatcher.isSimilarTo(control)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}