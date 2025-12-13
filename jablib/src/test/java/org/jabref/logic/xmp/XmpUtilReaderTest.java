package org.jabref.logic.xmp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.schema.DublinCoreSchemaCustom;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
class XmpUtilReaderTest {

    private XmpPreferences xmpPreferences;
    private BibtexImporter bibtexImporter;
    private final XmpUtilReader xmpUtilReader = new XmpUtilReader();

    @BeforeEach
    void setUp() {
        xmpPreferences = mock(XmpPreferences.class);

        // The test code assumes privacy filters to be off
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        bibtexImporter = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    }

    @Test
    void readArticleDublinCoreReadRawXmp() throws IOException, URISyntaxException {
        Path path = Path.of(XmpUtilShared.class.getResource("article_dublinCore_without_day.pdf").toURI());
        List<XMPMetadata> meta = xmpUtilReader.readRawXmp(path);

        DublinCoreSchema dcSchema = DublinCoreSchemaCustom.copyDublinCoreSchema(meta.getFirst().getDublinCoreSchema());
        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
        Optional<BibEntry> entry = dcExtractor.extractBibtexEntry();

        Path bibFile = Path.of(XmpUtilShared.class.getResource("article_dublinCore_without_day.bib").toURI());
        List<BibEntry> expected = bibtexImporter.importDatabase(bibFile).getDatabase().getEntries();

        assertEquals(expected, List.of(entry.get()));
    }

    @Test
    void readArticleDublinCoreReadXmp() throws IOException, URISyntaxException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("article_dublinCore.pdf").toURI());
        List<BibEntry> entries = xmpUtilReader.readXmp(pathPdf, xmpPreferences);

        Path bibFile = Path.of(XmpUtilShared.class.getResource("article_dublinCore.bib").toURI());
        List<BibEntry> expected = bibtexImporter.importDatabase(bibFile).getDatabase().getEntries();
        expected.forEach(bibEntry -> bibEntry.setFiles(Arrays.asList(
                new LinkedFile("", Path.of("paper.pdf"), "PDF"),
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        ));

        assertEquals(expected, entries);
    }

    @Test
    void readArticleDublinCoreReadXmpPartialDate() throws IOException, URISyntaxException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("article_dublinCore_partial_date.pdf").toURI());
        List<BibEntry> entries = xmpUtilReader.readXmp(pathPdf, xmpPreferences);

        Path bibFile = Path.of(XmpUtilShared.class.getResource("article_dublinCore_partial_date.bib").toURI());
        List<BibEntry> expected = bibtexImporter.importDatabase(bibFile).getDatabase().getEntries();
        expected.forEach(bibEntry -> bibEntry.setFiles(List.of(
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        ));

        assertEquals(expected, entries);
    }

    @Test
    void readEmtpyMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = xmpUtilReader.readXmp(Path.of(XmpUtilShared.class.getResource("empty_metadata.pdf").toURI()), xmpPreferences);
        assertEquals(List.of(), entries);
    }

    /**
     * Test non XMP metadata. Metadata are included in the PDInformation
     */
    @Test
    void readPDMetadataNonXmp() throws IOException, URISyntaxException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("PD_metadata.pdf").toURI());
        List<BibEntry> entries = xmpUtilReader.readXmp(pathPdf, xmpPreferences);

        Path bibFile = Path.of(XmpUtilShared.class.getResource("PD_metadata.bib").toURI());
        List<BibEntry> expected = bibtexImporter.importDatabase(bibFile).getDatabase().getEntries();

        expected.forEach(bibEntry -> bibEntry.setFiles(List.of(
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        ));

        assertEquals(expected, entries);
    }

    /**
     * Tests an pdf file with metadata which has no description section.
     */
    @Test
    void readNoDescriptionMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = xmpUtilReader.readXmp(Path.of(XmpUtilShared.class.getResource("no_description_metadata.pdf").toURI()), xmpPreferences);
        assertEquals(List.of(), entries);
    }
}
