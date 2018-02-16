package org.jabref.logic.xmp;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.io.Resources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
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

public class XMPUtilTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    private XMPPreferences xmpPreferences;

    private ImportFormatPreferences importFormatPreferences;

    private BibtexParser parser;

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @Before
    public void setUp() {


        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        xmpPreferences = mock(XMPPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.isUseXMPPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        parser = new BibtexParser(importFormatPreferences, fileMonitor);
    }

    /**
     * The month attribute in DublinCore is the complete name of the month, e.g. March.
     * In JabRef, the format is #mar# instead. To get a working unit test, the JabRef's
     * bib-entry is altered from #mar# to {March}.
     * <p/>
     * Tests the readRawXMP - method
     *
     * @throws IOException
     * @throws URISyntaxException
     * @throws ParseException
     */
    @Test
    public void testReadArticleDublinCoreReadXMP() throws IOException, URISyntaxException, ParseException {

        Path path = Paths.get(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/article_dublinCore.pdf").toURI());
        Optional<XMPMetadata> meta = XMPUtilReader.readRawXMP(path);

        DublinCoreSchema dcSchema = meta.get().getDublinCoreSchema();
        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
        Optional<BibEntry> entry = dcExtractor.extractBibtexEntry();
        String bibString = Resources.toString(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/article_dublinCore.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);

        Assert.assertEquals(entryFromBibFile.get(), entry.get());
    }

    /**
     * The month attribute in DublinCore is the complete name of the month, e.g. March.
     * In JabRef, the format is #mar# instead. To get a working unit test, the JabRef's
     * bib-entry is altered from #mar# to {March}.
     * <p/>
     * Tests the readXMP - method
     *
     * @throws IOException
     * @throws URISyntaxException
     * @throws ParseException
     */
    @Test
    public void testReadArticleDublinCoreXMP() throws IOException, URISyntaxException, ParseException {
        List<BibEntry> entries = XMPUtilReader.readXMP(Paths.get(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/article_dublinCore.pdf").toURI()), xmpPreferences);
        BibEntry entry = entries.get(0);

        String bibString = Resources.toString(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/article_dublinCore.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);

        Assert.assertEquals(entryFromBibFile.get(), entry);
    }

    /**
     * Tests an pdf file with an empty metadata section.
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testReadEmtpyMetadata() throws IOException, URISyntaxException {
        List<BibEntry> entries = XMPUtilReader.readXMP(Paths.get(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/empty_metadata.pdf").toURI()), xmpPreferences);
        Assert.assertEquals(Collections.EMPTY_LIST, entries);
    }

    /**
     * Test non XMP metadata. Metadata are included in the PDInformation
     *
     * @throws IOException
     * @throws URISyntaxException
     * @throws ParseException
     */
    @Test
    public void testReadPDMetadata() throws IOException, URISyntaxException, ParseException {
        List<BibEntry> entries = XMPUtilReader.readXMP(Paths.get(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/PD_metadata.pdf").toURI()), xmpPreferences);

        String bibString = Resources.toString(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/PD_metadata.bib"), StandardCharsets.UTF_8);
        Optional<BibEntry> entryFromBibFile = parser.parseSingleEntry(bibString);

        Assert.assertEquals(entryFromBibFile.get(), entries.get(0));
    }

    @Test
    public void testWriteXMP() throws IOException, URISyntaxException, TransformerException {

        // create a default PDF
        File pdfFile = tempFolder.newFile("JabRef.pdf");
        try (PDDocument pdf = new PDDocument()) {
            // Need a single page to open in Acrobat
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.getPath());
        }

        // read a bib entry from the tests before
        List<BibEntry> entries = XMPUtilReader.readXMP(Paths.get(XMPUtilShared.class.getResource("/org/jabref/logic/xmp/PD_metadata.pdf").toURI()), xmpPreferences);
        BibEntry entry = entries.get(0);
        entry.setCiteKey("WriteXMPTest");
        entry.setId("ID4711");

        // write the changed bib entry to the create PDF
        XMPUtilWriter.writeXMP(pdfFile.getAbsolutePath(), entry, null, xmpPreferences);

        // read entry again
        List<BibEntry> entriesWritten = XMPUtilReader.readXMP(pdfFile.getPath(), xmpPreferences);
        BibEntry entryWritten = entriesWritten.get(0);

        // compare the two entries
        Assert.assertEquals(entry, entryWritten);
    }
}
