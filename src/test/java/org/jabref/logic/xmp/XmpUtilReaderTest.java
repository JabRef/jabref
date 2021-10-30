package org.jabref.logic.xmp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.google.common.io.Resources;
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
    private BibtexParser parser;

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        xmpPreferences = mock(XmpPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    /**
     * Tests reading of dublinCore metadata.
     */
    @Test
    void testReadArticleDublinCoreReadRawXmp() throws IOException, URISyntaxException, ParseException {
        Path path = Path.of(XmpUtilShared.class.getResource("article_dublinCore.pdf").toURI());
        List<XMPMetadata> meta = XmpUtilReader.readRawXmp(path);

        DublinCoreSchema dcSchema = meta.get(0).getDublinCoreSchema();
        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
        Optional<BibEntry> entry = dcExtractor.extractBibtexEntry();
        String bibString = Resources.toString(XmpUtilShared.class.getResource("article_dublinCore.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);

        assertEquals(entryFromBibFile.get(), entry.get());
    }

    /**
     * Tests reading of dublinCore metadata.
     */
    @Test
    void testReadArticleDublinCoreReadXmp() throws IOException, URISyntaxException, ParseException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("article_dublinCore.pdf").toURI());
        List<BibEntry> entries = XmpUtilReader.readXmp(pathPdf, xmpPreferences);
        BibEntry entry = entries.get(0);

        String bibString = Resources.toString(XmpUtilShared.class.getResource("article_dublinCore.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);
        entryFromBibFile.get().setFiles(Arrays.asList(
                new LinkedFile("", Path.of("paper.pdf"), "PDF"),
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        );

        assertEquals(entryFromBibFile.get(), entry);
    }

    /**
     * Tests an pdf file with an empty metadata section.
     */
    @Test
    void testReadEmtpyMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = XmpUtilReader.readXmp(Path.of(XmpUtilShared.class.getResource("empty_metadata.pdf").toURI()), xmpPreferences);
        assertEquals(Collections.emptyList(), entries);
    }

    /**
     * Test non XMP metadata. Metadata are included in the PDInformation
     */
    @Test
    void testReadPDMetadata() throws IOException, URISyntaxException, ParseException {
        Path pathPdf = Path.of(XmpUtilShared.class.getResource("PD_metadata.pdf").toURI());
        List<BibEntry> entries = XmpUtilReader.readXmp(pathPdf, xmpPreferences);

        String bibString = Resources.toString(XmpUtilShared.class.getResource("PD_metadata.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);
        entryFromBibFile.get().setFiles(Collections.singletonList(
                new LinkedFile("", pathPdf.toAbsolutePath(), "PDF"))
        );

        assertEquals(entryFromBibFile.get(), entries.get(0));
    }

    /**
     * Tests an pdf file with metadata which has no description section.
     */
    @Test
    void testReadNoDescriptionMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = XmpUtilReader.readXmp(Path.of(XmpUtilShared.class.getResource("no_description_metadata.pdf").toURI()), xmpPreferences);
        assertEquals(Collections.emptyList(), entries);
    }
}
