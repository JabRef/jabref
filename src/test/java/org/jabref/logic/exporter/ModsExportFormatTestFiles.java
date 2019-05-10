package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.xmlunit.builder.Input;
import org.xmlunit.builder.Input.Builder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ModsExportFormatTestFiles {

    private static Path resourceDir;
    public Charset charset;
    private BibDatabaseContext databaseContext;
    private Path tempFile;
    private ModsExporter modsExportFormat;
    private BibtexImporter bibtexImporter;
    private ModsImporter modsImporter;
    private Path importFile;

    public static Stream<String> fileNames() throws Exception {
        resourceDir = Paths.get(MSBibExportFormatTestFiles.class.getResource("ModsExportFormatTestAllFields.bib").toURI()).getParent();
        System.out.println(resourceDir);

        try (Stream<Path> stream = Files.list(resourceDir)) {
            //            stream.forEach(n -> System.out.println(n));
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                         .filter(n -> n.startsWith("Mods")).collect(Collectors.toList()).stream();
        }
    }

    @BeforeEach
    public void setUp(@TempDir Path testFolder) throws Exception {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        modsExportFormat = new ModsExporter();
        Path path = testFolder.resolve("ARandomlyNamedFile.tmp");
        Files.createFile(path);
        tempFile = path.toAbsolutePath();
        ImportFormatPreferences mock = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        bibtexImporter = new BibtexImporter(mock, new DummyFileUpdateMonitor());
        Mockito.when(mock.getKeywordSeparator()).thenReturn(',');
        modsImporter = new ModsImporter(mock);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public final void testPerformExport(String filename) throws Exception {
        importFile = Paths.get(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        String xmlFileName = filename.replace(".bib", ".xml");
        Path tempFilename = tempFile.toAbsolutePath();
        List<BibEntry> entries = bibtexImporter.importDatabase(importFile, charset).getDatabase().getEntries();
        Path xmlFile = Paths.get(ModsExportFormatTestFiles.class.getResource(xmlFileName).toURI());

        modsExportFormat.export(databaseContext, tempFile, charset, entries);

        Builder control = Input.from(Files.newInputStream(xmlFile));
        Builder test = Input.from(Files.newInputStream(tempFilename));
        assertThat(test, CompareMatcher.isSimilarTo(control)
                                       .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public final void testExportAsModsAndThenImportAsMods(String filename) throws Exception {
        importFile = Paths.get(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        List<BibEntry> entries = bibtexImporter.importDatabase(importFile, charset).getDatabase().getEntries();

        modsExportFormat.export(databaseContext, tempFile, charset, entries);
        BibEntryAssert.assertEquals(entries, tempFile, modsImporter);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public final void testImportAsModsAndExportAsMods(String filename) throws Exception {
        importFile = Paths.get(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        String xmlFileName = filename.replace(".bib", ".xml");
        Path tempFilename = tempFile.toAbsolutePath();
        Path xmlFile = Paths.get(ModsExportFormatTestFiles.class.getResource(xmlFileName).toURI());

        List<BibEntry> entries = modsImporter.importDatabase(xmlFile, charset).getDatabase().getEntries();

        modsExportFormat.export(databaseContext, tempFile, charset, entries);

        Builder control = Input.from(Files.newInputStream(xmlFile));
        Builder test = Input.from(Files.newInputStream(tempFilename));

        assertThat(test, CompareMatcher.isSimilarTo(control)
                                       .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}
