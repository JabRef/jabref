package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MrDLibImporterTest {

    private MrDLibImporter importer;
    private BufferedReader inputMin;
    private BufferedReader inputMax;

    @BeforeEach
    public void setUp() {
        importer = new MrDLibImporter();

        String testMin = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mr-dlib></mr-dlib>";
        String testMax = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><mr-dlib><related_articles set_id=\"28184\" suggested_label=\"Related Articles\"><related_article document_id=\"2250539\" original_document_id=\"gesis-solis-00538797\" recommendation_id=\"204944\"><authors/><click_url>https://api-dev.mr-dlib.org/v1/recommendations/204944/original_url?access_key=99ab2fc64f3228ab839e9e3525ac37f8&format=direct_url_forward</click_url><debug_details><bibDocId>0</bibDocId><bibScore>2.0</bibScore><finalScore>2.0</finalScore><rankAfterAlgorithm>3</rankAfterAlgorithm><rankAfterReRanking>3</rankAfterReRanking><rankAfterShuffling>2</rankAfterShuffling><rankDelivered>2</rankDelivered><relevanceScoreFromAlgorithm>1.0</relevanceScoreFromAlgorithm></debug_details><fallback_url>http://sowiport.gesis.org/search/id/gesis-solis-00538797</fallback_url><published_in>Fachhochschulverl.</published_in><snippet format=\"html_plain\"><![CDATA[<a href='https://api-dev.mr-dlib.org/v1/recommendations/204944/original_url?access_key=99ab2fc64f3228ab839e9e3525ac37f8&format=direct_url_forward'>Gesundheit von Arbeitslosen fördern!: ein Handbuch für Wissenschaft und Praxis</a>. . Fachhochschulverl.. 2009.]]></snippet><snippet format=\"html_fully_formatted\"><![CDATA[<a href='https://api-dev.mr-dlib.org/v1/recommendations/204944/original_url?access_key=99ab2fc64f3228ab839e9e3525ac37f8&format=direct_url_forward'><font color='#000000' size='5' face='Arial, Helvetica, sans-serif'>Gesundheit von Arbeitslosen fördern!: ein Handbuch für Wissenschaft und Praxis.</font></a><font color='#000000' size='5' face='Arial, Helvetica, sans-serif'>. <i>Fachhochschulverl.</i>. 2009.</font>]]></snippet><snippet format=\"html_and_css\"><![CDATA[<span class='mdl-title'>Gesundheit von Arbeitslosen fördern!: ein Handbuch für Wissenschaft und Praxis</span>. <span class='mdl-authors'></span>. <span class='mdl-journal'>Fachhochschulverl.</span>. <span class='mdl-year'>2009</span>]]></snippet><suggested_rank>2</suggested_rank><title>Gesundheit von Arbeitslosen fördern!: ein Handbuch für Wissenschaft und Praxis</title><year>2009</year></related_article></related_articles></mr-dlib>";
        testMax = testMax.replaceAll("&", "");
        inputMin = new BufferedReader(new StringReader(testMin));
        inputMax = new BufferedReader(new StringReader(testMax));
    }

    @Test
    public void testGetDescription() {
        assertEquals("Takes valid xml documents. Parses from MrDLib API a BibEntry", importer.getDescription());
    }

    @Test
    public void testGetName() {
        assertEquals("MrDLibImporter", importer.getName());
    }

    @Test
    public void testGetFileExtention() {
        assertEquals(FileType.XML, importer.getFileType());
    }

    @Test
    public void testImportDatabaseIsHtmlSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(inputMax);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals(
                "<a href='https://api-dev.mr-dlib.org/v1/recommendations/204944/original_url?access_key=99ab2fc64f3228ab839e9e3525ac37f8format=direct_url_forward'><font color='#000000' size='5' face='Arial, Helvetica, sans-serif'>Gesundheit von Arbeitslosen fördern!: ein Handbuch für Wissenschaft und Praxis.</font></a><font color='#000000' size='5' face='Arial, Helvetica, sans-serif'>. <i>Fachhochschulverl.</i>. 2009.</font>",
                resultList.get(0).getField("html_representation").get());
    }

    @Test
    public void testImportDatabaseIsYearSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(inputMax);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals("2009",
                resultList.get(0).getLatexFreeField(FieldName.YEAR).get());
    }

    @Test
    public void testImportDatabaseIsTitleSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(inputMax);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals("Gesundheit von Arbeitslosen fördern!: ein Handbuch für Wissenschaft und Praxis",
                resultList.get(0).getLatexFreeField(FieldName.TITLE).get());
    }

    @Test
    public void testImportDatabaseMin() throws IOException {
        ParserResult parserResult = importer.importDatabase(inputMin);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertSame(0, resultList.size());
    }
}
