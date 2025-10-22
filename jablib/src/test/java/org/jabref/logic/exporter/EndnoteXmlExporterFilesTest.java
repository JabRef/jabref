package org.jabref.logic.exporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.EndnoteXmlImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.support.BibEntryAssert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

class EndnoteXmlExporterFilesTest {

    private Exporter exporter;
    private BibDatabaseContext databaseContext;
    private Path exportFile;
    private Path bibFileToExport;
    private BibtexImporter bibtexImporter;
    private EndnoteXmlImporter endnoteXmlImporter;

    @BeforeEach
    void setUp(@TempDir Path testFolder) {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(mock(BibEntryPreferences.class));
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        databaseContext = BibDatabaseContext.empty();
        exporter = new EndnoteXmlExporter(new BibEntryPreferences(','));
        endnoteXmlImporter = new EndnoteXmlImporter(importFormatPreferences);
        bibtexImporter = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());
        exportFile = testFolder.resolve("exported-endnote.xml").toAbsolutePath();
    }

    static Stream<String> fileNames() throws IOException, URISyntaxException {
        // we have to point it to one existing file, otherwise it will return the default class path
        Path resourceDir = Path.of(EndnoteXmlExporterFilesTest.class.getResource("EndnoteXmlExportTestSingleBookEntry.bib").toURI()).getParent();
        try (Stream<Path> stream = Files.list(resourceDir)) {
            return stream.map(n -> n.getFileName().toString())
                         .filter(n -> n.endsWith(".bib"))
                         .filter(n -> n.startsWith("EndnoteXml"))
                         // mapping required, because we get "source already consumed or closed" otherwise
                         .toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    final void performExport(String filename) throws URISyntaxException, IOException, TransformerException, SaveException, ParserConfigurationException {
        bibFileToExport = Path.of(EndnoteXmlExporterFilesTest.class.getResource(filename).toURI());
        List<BibEntry> entries = bibtexImporter.importDatabase(bibFileToExport).getDatabase().getEntries();
        exporter.export(databaseContext, exportFile, entries);
        String actual = String.join("\n", Files.readAllLines(exportFile));

        String xmlFileName = filename.replace(".bib", ".xml");
        Path expectedFile = Path.of(ModsExportFormatFilesTest.class.getResource(xmlFileName).toURI());
        String expected = String.join("\n", Files.readAllLines(expectedFile));

        // The order of the XML elements changes
        // The order does not really matter, so we ignore it.
        // Source: https://stackoverflow.com/a/16540679/873282
        assertThat(actual, isSimilarTo(expected)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    final void exportAsEndnoteAndThenImportAsEndnote(String filename) throws IOException, TransformerException, URISyntaxException, SaveException, ParserConfigurationException {
        bibFileToExport = Path.of(EndnoteXmlExporterFilesTest.class.getResource(filename).toURI());
        List<BibEntry> entries = bibtexImporter.importDatabase(bibFileToExport).getDatabase().getEntries();

        exporter.export(databaseContext, exportFile, entries);
        BibEntryAssert.assertEquals(entries, exportFile, endnoteXmlImporter);
    }
}
