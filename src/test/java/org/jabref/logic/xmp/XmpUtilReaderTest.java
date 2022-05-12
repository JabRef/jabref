package org.jabref.logic.xmp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
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
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmpUtilReaderTest {

    private XmpPreferences xmpPreferences;
    private BibtexImporter testImporter;
    private final XmpUtilReader xmpUtilReader = new XmpUtilReader();

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @BeforeEach
    void setUp() {
        xmpPreferences = mock(XmpPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        testImporter = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
    }

    /**
     * Tests reading of dublinCore metadata.
     */
    @Test
    void testReadArticleDublinCoreReadRawXmp() throws IOException, URISyntaxException {
        Path path = Path.of(XmpUtilShared.class.getResource("article_dublinCore_without_day.pdf").toURI());
        List<XMPMetadata> meta = xmpUtilReader.readRawXmp(path);

        DublinCoreSchema dcSchema = DublinCoreSchemaCustom.copyDublinCoreSchema(meta.get(0).getDublinCoreSchema());
        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
        Optional<BibEntry> entry = dcExtractor.extractBibtexEntry();

        Path bibFile = Path.of(XmpUtilShared.class.getResource("article_dublinCore_without_day.bib").toURI());
        List<BibEntry> expected = testImporter.importDatabase(bibFile).getDatabase().getEntries();

        assertEquals(expected, Collections.singletonList(entry.get()));
    }

    /**
     * Tests reading of dublinCore metadata.
     */
    @Test
    void testReadArticleDublinCoreReadXmp() throws IOException, URISyntaxException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("article_dublinCore.pdf").toURI());
        List<BibEntry> entries = xmpUtilReader.readXmp(pathPdf, xmpPreferences);
        Path bibFile = Path.of(XmpUtilShared.class.getResource("article_dublinCore.bib").toURI());
        List<BibEntry> expected = testImporter.importDatabase(bibFile).getDatabase().getEntries();

        expected.forEach(bibEntry -> bibEntry.setFiles(Arrays.asList(
                new LinkedFile("", Path.of("paper.pdf"), "PDF"),
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        ));

        assertEquals(expected, entries);
    }

    @Test
    void testReadArticleDublinCoreReadXmpPartialDate() throws IOException, URISyntaxException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("article_dublinCore_partial_date.pdf").toURI());
        List<BibEntry> entries = xmpUtilReader.readXmp(pathPdf, xmpPreferences);
        Path bibFile = Path.of(XmpUtilShared.class.getResource("article_dublinCore_partial_date.bib").toURI());
        List<BibEntry> expected = testImporter.importDatabase(bibFile).getDatabase().getEntries();

        expected.forEach(bibEntry -> bibEntry.setFiles(List.of(
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        ));

        assertEquals(expected, entries);
    }

    /**
     * Tests an pdf file with an empty metadata section.
     */
    @Test
    void testReadEmtpyMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = xmpUtilReader.readXmp(Path.of(XmpUtilShared.class.getResource("empty_metadata.pdf").toURI()), xmpPreferences);
        assertEquals(Collections.emptyList(), entries);
    }

    /**
     * Test non XMP metadata. Metadata are included in the PDInformation
     */
    @Test
    void testReadPDMetadata() throws IOException, URISyntaxException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("PD_metadata.pdf").toURI());
        List<BibEntry> entries = xmpUtilReader.readXmp(pathPdf, xmpPreferences);

        Path bibFile = Path.of(XmpUtilShared.class.getResource("PD_metadata.bib").toURI());
        List<BibEntry> expected = testImporter.importDatabase(bibFile).getDatabase().getEntries();

        expected.forEach(bibEntry -> bibEntry.setFiles(List.of(
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        ));

        assertEquals(expected, entries);
    }

    /**
     * Tests an pdf file with metadata which has no description section.
     */
    @Test
    void testReadNoDescriptionMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = xmpUtilReader.readXmp(Path.of(XmpUtilShared.class.getResource("no_description_metadata.pdf").toURI()), xmpPreferences);
        assertEquals(Collections.emptyList(), entries);
    }
}
