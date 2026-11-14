package org.jabref.logic.exporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarkdownPublicationExporterFilesTest {

    static Stream<String> fileNames() throws IOException, URISyntaxException {
        // Point to one existing file, otherwise the default class path is returned
        Path resourceDir = Path.of(MarkdownPublicationExporterFilesTest.class.getResource("MarkdownPublicationExportTestEscaping.bib").toURI()).getParent();
        try (Stream<Path> stream = Files.list(resourceDir)) {
            return stream.map(path -> path.getFileName().toString())
                         .filter(name -> name.startsWith("MarkdownPublicationExportTest"))
                         .filter(name -> name.endsWith(".bib"))
                         .toList()
                         .stream();
        }
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void performExport(String bibFileName, @TempDir Path testFolder) throws URISyntaxException, IOException, SaveException {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(new BibEntryPreferences(','));
        BibtexImporter bibtexImporter = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());

        Path bibFile = Path.of(MarkdownPublicationExporterFilesTest.class.getResource(bibFileName).toURI());
        ParserResult parserResult = bibtexImporter.importDatabase(bibFile);
        Path exportFile = testFolder.resolve("publications.md");

        new MarkdownPublicationExporter(LayoutFormatterPreferences.getDefault())
                .export(parserResult.getDatabaseContext(),
                        exportFile,
                        parserResult.getDatabase().getEntries(),
                        List.of(),
                        mock(JournalAbbreviationRepository.class));

        Path expectedFile = Path.of(MarkdownPublicationExporterFilesTest.class.getResource(bibFileName.replace(".bib", ".md")).toURI());
        assertEquals(Files.readString(expectedFile).replace("\r\n", "\n"),
                Files.readString(exportFile).replace("\r\n", "\n"));
    }
}
