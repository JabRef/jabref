package org.jabref.logic.exporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class MSBibExportFormatTestFiles {

    private static Path resourceDir;
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public Path tempFile;
    public MSBibExporter msBibExportFormat;
    public BibtexImporter testImporter;

    static Stream<String> fileNames() throws IOException, URISyntaxException {
        //we have to point it to one existing file, otherwise it will return the default class path
        resourceDir = Paths.get(MSBibExportFormatTestFiles.class.getResource("MsBibExportFormatTest1.bib").toURI()).getParent();
        System.out.println(resourceDir);
        try (Stream<Path> stream = Files.list(resourceDir)) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".bib"))
                         .filter(n -> n.startsWith("MsBib")).collect(Collectors.toList()).stream();
        }
    }

    @BeforeEach
    void setUp(@TempDir Path testFolder) throws Exception {
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        msBibExportFormat = new MSBibExporter();
        Path path = testFolder.resolve("ARandomlyNamedFile.tmp");
        tempFile = Files.createFile(path);
        testImporter = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void testPerformExport(String filename) throws IOException, SaveException {
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
