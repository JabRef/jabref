package org.jabref.logic.exporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.xmlunit.builder.Input;
import org.xmlunit.builder.Input.Builder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class BibTeXMLExporterTestFiles {

    private static Path resourceDir;
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public Path tempFile;
    public BibTeXMLExporter bibtexmlExportFormat;
    public BibtexImporter testImporter;

    public static Stream<String> fileNames() throws IOException, URISyntaxException {
        resourceDir = Path.of(MSBibExportFormatTestFiles.class.getResource("BibTeXMLExporterTestArticle.bib").toURI()).getParent();

        try (Stream<Path> stream = Files.list(resourceDir)) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                         .filter(n -> n.startsWith("BibTeXML")).collect(Collectors.toList()).stream();
        }
    }

    @BeforeEach
    public void setUp(@TempDir Path testFolder) throws Exception {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        bibtexmlExportFormat = new BibTeXMLExporter();
        Files.createFile(testFolder.resolve("ARandomlyNamedFile.tmp"));
        tempFile = testFolder.resolve("ARandomlyNamedFile.tmp");
        testImporter = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public final void testPerformExport(String filename) throws IOException, SaveException {
        String xmlFileName = filename.replace(".bib", ".xml");
        Path importFile = resourceDir.resolve(filename);
        String tempFilePath = tempFile.toAbsolutePath().toString();

        List<BibEntry> entries = testImporter.importDatabase(importFile, StandardCharsets.UTF_8).getDatabase()
                                             .getEntries();

        bibtexmlExportFormat.export(databaseContext, tempFile, charset, entries);

        Builder control = Input.from(Files.newInputStream(resourceDir.resolve(xmlFileName)));
        Builder test = Input.from(Files.newInputStream(Path.of(tempFilePath)));

        assertThat(test, CompareMatcher.isSimilarTo(control)
                                       .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}
