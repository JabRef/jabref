package org.jabref.logic.xmp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.io.Resources;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XmpUtilReaderTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    private XmpPreferences xmpPreferences;

    private BibtexParser parser;

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @Before
    public void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        xmpPreferences = mock(XmpPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.isUseXMPPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        parser = new BibtexParser(importFormatPreferences, fileMonitor);
    }

    /**
     * Tests reading of dublinCore metadata.
     */
    @Test
    public void testReadArticleDublinCoreReadRawXmp() throws IOException, URISyntaxException, ParseException {
        Path path = Paths.get(XmpUtilShared.class.getResource("article_dublinCore.pdf").toURI());
        List<XMPMetadata> meta = XmpUtilReader.readRawXmp(path);

        DublinCoreSchema dcSchema = meta.get(0).getDublinCoreSchema();
        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
        Optional<BibEntry> entry = dcExtractor.extractBibtexEntry();
        String bibString = Resources.toString(XmpUtilShared.class.getResource("article_dublinCore.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);

        Assert.assertEquals(entryFromBibFile.get(), entry.get());
    }

    /**
     * Tests reading of dublinCore metadata.
     */
    @Test
    public void testReadArticleDublinCoreReadXmp() throws IOException, URISyntaxException, ParseException {
        List<BibEntry> entries = XmpUtilReader.readXmp(Paths.get(XmpUtilShared.class.getResource("article_dublinCore.pdf").toURI()), xmpPreferences);
        BibEntry entry = entries.get(0);

        String bibString = Resources.toString(XmpUtilShared.class.getResource("article_dublinCore.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);

        Assert.assertEquals(entryFromBibFile.get(), entry);
    }

    /**
     * Tests an pdf file with an empty metadata section.
     */
    @Test
    public void testReadEmtpyMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = XmpUtilReader.readXmp(Paths.get(XmpUtilShared.class.getResource("empty_metadata.pdf").toURI()), xmpPreferences);
        Assert.assertEquals(Collections.EMPTY_LIST, entries);
    }

    /**
     * Test non XMP metadata. Metadata are included in the PDInformation
     */
    @Test
    public void testReadPDMetadata() throws IOException, URISyntaxException, ParseException {
        List<BibEntry> entries = XmpUtilReader.readXmp(Paths.get(XmpUtilShared.class.getResource("PD_metadata.pdf").toURI()), xmpPreferences);

        String bibString = Resources.toString(XmpUtilShared.class.getResource("PD_metadata.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);

        Assert.assertEquals(entryFromBibFile.get(), entries.get(0));
    }

}
