package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ModsExportFormatTestFiles {

    private static Path resourceDir;
    public Charset charset;
    private BibDatabaseContext databaseContext;
    private Path exportedFile;
    private ModsExporter exporter;
    private BibtexImporter bibtexImporter;
    private ModsImporter modsImporter;
    private Path importFile;

    public static Stream<String> fileNames() throws Exception {
        resourceDir = Path.of(MSBibExportFormatTestFiles.class.getResource("ModsExportFormatTestAllFields.bib").toURI()).getParent();
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
        exporter = new ModsExporter();
        Path path = testFolder.resolve("ARandomlyNamedFile.tmp");
        Files.createFile(path);
        exportedFile = path.toAbsolutePath();
        ImportFormatPreferences mock = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        bibtexImporter = new BibtexImporter(mock, new DummyFileUpdateMonitor());
        Mockito.when(mock.getKeywordSeparator()).thenReturn(',');
        modsImporter = new ModsImporter(mock);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public final void testPerformExport(String filename) throws Exception {
        importFile = Path.of(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        String xmlFileName = filename.replace(".bib", ".xml");
        List<BibEntry> entries = bibtexImporter.importDatabase(importFile, charset).getDatabase().getEntries();
        Path expectedFile = Path.of(ModsExportFormatTestFiles.class.getResource(xmlFileName).toURI());

        exporter.export(databaseContext, exportedFile, charset, entries);

        assertEquals(
                String.join("\n", Files.readAllLines(expectedFile)),
                String.join("\n", Files.readAllLines(exportedFile)));
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public final void testExportAsModsAndThenImportAsMods(String filename) throws Exception {
        importFile = Path.of(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        List<BibEntry> entries = bibtexImporter.importDatabase(importFile, charset).getDatabase().getEntries();

        exporter.export(databaseContext, exportedFile, charset, entries);
        BibEntryAssert.assertEquals(entries, exportedFile, modsImporter);
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    public final void testImportAsModsAndExportAsMods(String filename) throws Exception {
        importFile = Path.of(ModsExportFormatTestFiles.class.getResource(filename).toURI());
        String xmlFileName = filename.replace(".bib", ".xml");
        Path xmlFile = Path.of(ModsExportFormatTestFiles.class.getResource(xmlFileName).toURI());

        List<BibEntry> entries = modsImporter.importDatabase(xmlFile, charset).getDatabase().getEntries();

        exporter.export(databaseContext, exportedFile, charset, entries);

        assertEquals(
                String.join("\n", Files.readAllLines(xmlFile)),
                String.join("\n", Files.readAllLines(exportedFile)));
    }
}
