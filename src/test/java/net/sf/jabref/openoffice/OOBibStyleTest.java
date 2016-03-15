package net.sf.jabref.openoffice;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import static org.mockito.Mockito.mock;

public class OOBibStyleTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs = null;
    }

    @Test
    public void testAuthorYear() {
        try {
            URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_AUTHORYEAR_STYLE_PATH);
            Reader r = new InputStreamReader(defPath.openStream());
            OOBibStyle style = new OOBibStyle(r, mock(JournalAbbreviationRepository.class));
            assertTrue(style.isValid());
            assertFalse(style.isBibtexKeyCiteMarkers());
            assertFalse(style.isBoldCitations());
            assertFalse(style.isFormatCitations());
            assertFalse(style.isItalicCitations());
            assertFalse(style.isNumberEntries());
            assertFalse(style.isSortByPosition());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testAuthorYearAsFile() {
        try {
            URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_AUTHORYEAR_STYLE_PATH);
            OOBibStyle style = new OOBibStyle(new File(defPath.getFile()), mock(JournalAbbreviationRepository.class),
                    Globals.prefs.getDefaultEncoding());
            assertTrue(style.isValid());
            assertFalse(style.isBibtexKeyCiteMarkers());
            assertFalse(style.isBoldCitations());
            assertFalse(style.isFormatCitations());
            assertFalse(style.isItalicCitations());
            assertFalse(style.isNumberEntries());
            assertFalse(style.isSortByPosition());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testNumerical() {
        try {
            URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_NUMERICAL_STYLE_PATH);
            Reader r = new InputStreamReader(defPath.openStream());
            OOBibStyle style = new OOBibStyle(r, mock(JournalAbbreviationRepository.class));
            assertTrue(style.isValid());
            assertFalse(style.isBibtexKeyCiteMarkers());
            assertFalse(style.isBoldCitations());
            assertFalse(style.isFormatCitations());
            assertFalse(style.isItalicCitations());
            assertTrue(style.isNumberEntries());
            assertTrue(style.isSortByPosition());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testGetNumCitationMarker() throws IOException {
        URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_NUMERICAL_STYLE_PATH);
        Reader r = new InputStreamReader(defPath.openStream());
        OOBibStyle style = new OOBibStyle(r, mock(JournalAbbreviationRepository.class));
        assertEquals("[1] ", style.getNumCitationMarker(Arrays.asList(1), -1, true));
        assertEquals("[1]", style.getNumCitationMarker(Arrays.asList(1), -1, false));
        assertEquals("[1] ", style.getNumCitationMarker(Arrays.asList(1), 0, true));
        assertEquals("[1-3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), 1, true));
        assertEquals("[1; 2; 3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), 5, true));
        assertEquals("[1; 2; 3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), -1, true));
        assertEquals("[1; 3; 12] ", style.getNumCitationMarker(Arrays.asList(1, 12, 3), 1, true));
        assertEquals("[3-5; 7; 10-12] ", style.getNumCitationMarker(Arrays.asList(12, 7, 3, 4, 11, 10, 5), 1, true));

        String citation = style.getNumCitationMarker(Arrays.asList(1), -1, false);
        assertEquals("[1; pp. 55-56]", style.insertPageInfo(citation, "pp. 55-56"));
    }

    @Test
    public void testGetCitProperty() throws IOException {
        URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_NUMERICAL_STYLE_PATH);
        Reader r = new InputStreamReader(defPath.openStream());
        OOBibStyle style = new OOBibStyle(r, mock(JournalAbbreviationRepository.class));
        assertEquals(", ", style.getStringCitProperty("AuthorSeparator"));
        assertEquals(3, style.getIntCitProperty("MaxAuthors"));
        assertTrue(style.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL));
        assertEquals("Default", style.getCitationCharacterFormat());
        assertEquals("Example style file for JabRef-OO connection.", style.getName());
        Set<String> journals = style.getJournals();
        assertTrue(journals.contains("Journal name 1"));
    }

    @Test
    public void testGetCitationMarker() throws IOException {
        File testBibtexFile = new File("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormatReader.getReader(testBibtexFile, encoding));
        URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_NUMERICAL_STYLE_PATH);
        Reader r = new InputStreamReader(defPath.openStream());
        OOBibStyle style = new OOBibStyle(r, mock(JournalAbbreviationRepository.class));
        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        BibDatabase db = result.getDatabase();
        for (BibEntry entry : db.getEntries()) {
            entryDBMap.put(entry, db);
        }

        BibEntry entry = db.getEntryByKey("1137631");
        assertEquals("[Boström et al., 2006]",
                style.getCitationMarker(Arrays.asList(entry), entryDBMap, true, null, null));
        assertEquals("Boström et al. [2006]",
                style.getCitationMarker(Arrays.asList(entry), entryDBMap, false, null, new int[] {3}));
        assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006]",
                style.getCitationMarker(Arrays.asList(entry), entryDBMap, true, null, new int[] {5}));
    }

    @Test
    public void testLayout() throws IOException {
        File testBibtexFile = new File("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormatReader.getReader(testBibtexFile, encoding));
        URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_NUMERICAL_STYLE_PATH);
        Reader r = new InputStreamReader(defPath.openStream());
        OOBibStyle style = new OOBibStyle(r, mock(JournalAbbreviationRepository.class));
        BibDatabase db = result.getDatabase();

        Layout l = style.getReferenceFormat("default");
        l.setPostFormatter(new OOPreFormatter());
        BibEntry entry = db.getEntryByKey("1137631");
        assertEquals(
                "Boström, G.; Wäyrynen, J.; Bodén, M.; Beznosov, K. and Kruchten, P. (<b>2006</b>). <i>Extending XP practices to support security requirements engineering</i>,   : 11-18.",
                l.doLayout(entry, db));

        l = style.getReferenceFormat("incollection");
        l.setPostFormatter(new OOPreFormatter());
        assertEquals(
                "Boström, G.; Wäyrynen, J.; Bodén, M.; Beznosov, K. and Kruchten, P. (<b>2006</b>). <i>Extending XP practices to support security requirements engineering</i>. In:  (Ed.), <i>SESS '06: Proceedings of the 2006 international workshop on Software engineering for secure systems</i>, ACM.",
                l.doLayout(entry, db));
}
}
