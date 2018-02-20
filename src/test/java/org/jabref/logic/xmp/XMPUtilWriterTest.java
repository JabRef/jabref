package org.jabref.logic.xmp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XMPUtilWriterTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    private XMPPreferences xmpPreferences;

    private ImportFormatPreferences importFormatPreferences;

    private BibtexParser parser;

    private static final String olly2018 = "@article{Olly2018,\r\n" +
            "  author       = {Olly and Johannes},\r\n" +
            "  title        = {Stefan's palace},\r\n" +
            "  journal      = {Test Journal},\r\n" +
            "  volume       = {1},\r\n" +
            "  number       = {1},\r\n" +
            "  pages        = {1-2},\r\n" +
            "  month        = mar,\r\n" +
            "  issn         = {978-123-123},\r\n" +
            "  note         = {That's a note},\r\n" +
            "  abstract     = {That's an abstract},\r\n" +
            "  comment      = {That's a comment},\r\n" +
            "  doi          = {10/3212.3123},\r\n" +
            "  file         = {:article_dublinCore.pdf:PDF},\r\n" +
            "  groups       = {NO},\r\n" +
            "  howpublished = {Online},\r\n" +
            "  keywords     = {Keyword1, Keyword2},\r\n" +
            "  owner        = {Me},\r\n" +
            "  review       = {Here are the reviews},\r\n" +
            "  timestamp    = {2018-02-15},\r\n" +
            "  url          = {https://www.olly2018.edu},\r\n" +
            "}";

    private static final String toral2006 = "@InProceedings{Toral2006,\r\n" +
            "  author     = {Toral, Antonio and Munoz, Rafael},\r\n" +
            "  title      = {A proposal to automatically build and maintain gazetteers for Named Entity Recognition by using Wikipedia},\r\n" +
            "  booktitle  = {Proceedings of EACL},\r\n" +
            "  pages      = {56--61},\r\n" +
            "  date       = {2006},\r\n" +
            "  eprinttype = {asdf},\r\n" +
            "  eventdate  = {2017-05-31},\r\n" +
            "  owner      = {Christoph Schwentker},\r\n" +
            "  timestamp  = {2016.11.07},\r\n" +
            "  url        = {asdfasdfas},\r\n" +
            "  urldate    = {2017-05-31},\r\n" +
            "}";

    private static final String vapnik2000 = "@Book{Vapnik2000,\r\n" +
            "  title     = {The Nature of Statistical Learning Theory},\r\n" +
            "  publisher = {Springer Science + Business Media},\r\n" +
            "  author    = {Vladimir N. Vapnik},\r\n" +
            "  date      = {2000},\r\n" +
            "  doi       = {10.1007/978-1-4757-3264-1},\r\n" +
            "  owner     = {Christoph Schwentker},\r\n" +
            "  timestamp = {2016.06.20},\r\n" +
            "}";

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
     * Test for writing a PDF file with a single DublinCore metadata entry.
     *
     * @throws IOException
     * @throws URISyntaxException
     * @throws TransformerException
     * @throws ParseException
     */
    //    @Test
    //    public void testWriteXMP() throws IOException, URISyntaxException, TransformerException, ParseException {
    //
    //        File pdfFile = this.createDefaultFile("JabRef_writeSingle.pdf");
    //
    //        // read a bib entry from the tests before
    //        String entryString = vapnik2000;
    //        BibEntry entry = parser.parseEntries(entryString).get(0);
    //        entry.setCiteKey("WriteXMPTest");
    //        entry.setId("ID4711");
    //
    //        // write the changed bib entry to the create PDF
    //        XMPUtilWriter.writeXMP(pdfFile.getAbsolutePath(), entry, null, xmpPreferences);
    //
    //        // read entry again
    //        List<BibEntry> entriesWritten = XMPUtilReader.readXMP(pdfFile.getPath(), xmpPreferences);
    //        BibEntry entryWritten = entriesWritten.get(0);
    //
    //        // compare the two entries
    //        Assert.assertEquals(entry, entryWritten);
    //
    //    }

    @Test
    public void testWriteMultipleBibEntries() throws IOException, ParseException, TransformerException {

        File pdfFile = this.createDefaultFile("JabRef_writeMultiple.pdf");

        List<BibEntry> entries = Arrays.asList(parser.singleFromString(vapnik2000, importFormatPreferences, fileMonitor).get(),
                parser.singleFromString(olly2018, importFormatPreferences, fileMonitor).get(),
                parser.singleFromString(toral2006, importFormatPreferences, fileMonitor).get());

        XMPUtilWriter.writeXMP(Paths.get(pdfFile.getAbsolutePath()), entries, null, xmpPreferences);
    }

    private File createDefaultFile(String fileName) throws IOException {
        // create a default PDF
        File pdfFile = tempFolder.newFile(fileName);
        try (PDDocument pdf = new PDDocument()) {
            // Need a single page to open in Acrobat
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.getPath());
        }

        return pdfFile;
    }
}
