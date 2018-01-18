package org.jabref.logic.exporter;

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

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.xmlunit.builder.Input;
import org.xmlunit.builder.Input.Builder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class MSBibExportFormatTestFiles {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public Path tempFile;
    public MSBibExporter msBibExportFormat;
    public BibtexImporter testImporter;

    @Parameter
    public String filename;
    public Path resourceDir;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException, URISyntaxException {
        try (Stream<Path> stream = Files.list(Paths.get(MSBibExportFormatTestFiles.class.getResource("").toURI()))) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                    .filter(n -> n.startsWith("MsBib")).collect(Collectors.toList());
        }
    }

    @Before
    public void setUp() throws Exception {
        resourceDir = Paths.get(MSBibExportFormatTestFiles.class.getResource("").toURI());
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        msBibExportFormat = new MSBibExporter();
        tempFile = testFolder.newFile().toPath();
        testImporter = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    }

    @Test
    public final void testPerformExport() throws IOException, SaveException {
        String xmlFileName = filename.replace(".bib", ".xml");
        Path importFile = resourceDir.resolve(filename);

        List<BibEntry> entries = testImporter.importDatabase(importFile, StandardCharsets.UTF_8).getDatabase()
                .getEntries();

        msBibExportFormat.export(databaseContext, tempFile, charset, entries);

        Builder control = Input.from(Files.newInputStream(resourceDir.resolve(xmlFileName)));
        Builder test = Input.from(Files.newInputStream(tempFile));

        assertThat(test, CompareMatcher.isSimilarTo(control)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}
