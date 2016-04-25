package net.sf.jabref.logic.openoffice;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefMain;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class OOBibStyleTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @After
    public void tearDown() {
        Globals.prefs = null;
    }

    @Test
    public void testAuthorYear() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertTrue(style.isValid());
        assertTrue(style.isFromResource());
        assertFalse(style.isBibtexKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertFalse(style.isNumberEntries());
        assertFalse(style.isSortByPosition());
    }

    @Test
    public void testAuthorYearAsFile() throws URISyntaxException, IOException {

        File defFile = Paths.get(JabRefMain.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile();

        OOBibStyle style = new OOBibStyle(defFile, mock(JournalAbbreviationRepository.class),
                Globals.prefs.getDefaultEncoding());
        assertTrue(style.isValid());
        assertFalse(style.isFromResource());
        assertFalse(style.isBibtexKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertFalse(style.isNumberEntries());
        assertFalse(style.isSortByPosition());
    }

    @Test
    public void testNumerical() throws IOException {

        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertTrue(style.isValid());
        assertFalse(style.isBibtexKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertTrue(style.isNumberEntries());
        assertTrue(style.isSortByPosition());

    }

    @Test
    public void testGetNumCitationMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
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
    public void testGetNumCitationMarkerUndefined() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "; 2-4] ",
                style.getNumCitationMarker(Arrays.asList(4, 2, 3, 0), 1, true));

        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "] ",
                style.getNumCitationMarker(Arrays.asList(0), 1, true));

        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "; 1-3] ",
                style.getNumCitationMarker(Arrays.asList(1, 2, 3, 0), 1, true));

        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "; " + OOBibStyle.UNDEFINED_CITATION_MARKER + "; "
                + OOBibStyle.UNDEFINED_CITATION_MARKER + "] ",
                style.getNumCitationMarker(Arrays.asList(0, 0, 0), 1, true));
    }

    @Test
    public void testGetCitProperty() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertEquals(", ", style.getStringCitProperty("AuthorSeparator"));
        assertEquals(3, style.getIntCitProperty("MaxAuthors"));
        assertTrue(style.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL));
        assertEquals("Default", style.getCitationCharacterFormat());
        assertEquals("Default [number] style file.", style.getName());
        Set<String> journals = style.getJournals();
        assertTrue(journals.contains("Journal name 1"));
    }

    @Test
    public void testGetCitationMarker() throws IOException {
        File testBibtexFile = new File("src/test/resources/testbib/complex.bib");
        ParserResult result = BibtexParser.parse(ImportFormatReader.getReader(testBibtexFile, StandardCharsets.UTF_8));
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
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
        ParserResult result = BibtexParser.parse(ImportFormatReader.getReader(testBibtexFile, StandardCharsets.UTF_8));
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
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

    @Test
    public void testInstitutionAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        BibDatabase database = new BibDatabase();

        Layout l = style.getReferenceFormat("article");
        l.setPostFormatter(new OOPreFormatter());

        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "{JabRef Development Team}");
        entry.setField("title", "JabRef Manual");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        assertEquals("<b>JabRef Development Team</b> (<b>2016</b>). <i>JabRef Manual</i>,  .",
                l.doLayout(entry, database));
    }

    @Test
    public void testVonAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        BibDatabase database = new BibDatabase();

        Layout l = style.getReferenceFormat("article");
        l.setPostFormatter(new OOPreFormatter());

        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "Alpha von Beta");
        entry.setField("title", "JabRef Manual");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        assertEquals("<b>von Beta, A.</b> (<b>2016</b>). <i>JabRef Manual</i>,  .",
                l.doLayout(entry, database));
    }

    @Test
    public void testInstitutionAuthorMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "{JabRef Development Team}");
        entry.setField("title", "JabRef Manual");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[JabRef Development Team, 2016]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    public void testVonAuthorMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "Alpha von Beta");
        entry.setField("title", "JabRef Manual");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[von Beta, 2016]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    public void testNullAuthorMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[, 2016]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    public void testNullYearMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "Alpha von Beta");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[von Beta, ]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    public void testEmptyEntryMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType("article");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[, ]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    public void testGetCitationMarkerInParenthesisUniquefiers() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField("author", "Alpha Beta");
        entry1.setField("title", "Paper 1");
        entry1.setField("year", "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "Alpha Beta");
        entry3.setField("title", "Paper 2");
        entry3.setField("year", "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "Gamma Epsilon");
        entry2.setField("year", "2001");
        entries.add(entry2);
        database.insertEntry(entry2);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("[Beta, 2000; Beta, 2000; Epsilon, 2001]",
                style.getCitationMarker(entries, entryDBMap, true, null, null));
        assertEquals("[Beta, 2000a,b; Epsilon, 2001]",
                style.getCitationMarker(entries, entryDBMap, true, new String[] {"a", "b", ""}, new int[] {1, 1, 1}));
    }

    @Test
    public void testGetCitationMarkerInTextUniquefiers() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField("author", "Alpha Beta");
        entry1.setField("title", "Paper 1");
        entry1.setField("year", "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "Alpha Beta");
        entry3.setField("title", "Paper 2");
        entry3.setField("year", "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "Gamma Epsilon");
        entry2.setField("year", "2001");
        entries.add(entry2);
        database.insertEntry(entry2);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("Beta [2000]; Beta [2000]; Epsilon [2001]",
                style.getCitationMarker(entries, entryDBMap, false, null, null));
        assertEquals("Beta [2000a,b]; Epsilon [2001]",
                style.getCitationMarker(entries, entryDBMap, false, new String[] {"a", "b", ""}, new int[] {1, 1, 1}));
    }

    @Test
    public void testGetCitationMarkerInParenthesisUniquefiersThreeSameAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField("author", "Alpha Beta");
        entry1.setField("title", "Paper 1");
        entry1.setField("year", "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "Alpha Beta");
        entry2.setField("title", "Paper 2");
        entry2.setField("year", "2000");
        entries.add(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "Alpha Beta");
        entry3.setField("title", "Paper 3");
        entry3.setField("year", "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("[Beta, 2000a,b,c]",
                style.getCitationMarker(entries, entryDBMap, true, new String[] {"a", "b", "c"}, new int[] {1, 1, 1}));
    }

    @Test
    public void testGetCitationMarkerInTextUniquefiersThreeSameAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField("author", "Alpha Beta");
        entry1.setField("title", "Paper 1");
        entry1.setField("year", "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "Alpha Beta");
        entry2.setField("title", "Paper 2");
        entry2.setField("year", "2000");
        entries.add(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "Alpha Beta");
        entry3.setField("title", "Paper 3");
        entry3.setField("year", "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("Beta [2000a,b,c]",
                style.getCitationMarker(entries, entryDBMap, false, new String[] {"a", "b", "c"}, new int[] {1, 1, 1}));
    }

    @Test
    // TODO: equals only work when initialized from file, not from reader
    public void testEquals() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertEquals(style1, style2);
    }

    @Test
    // TODO: equals only work when initialized from file, not from reader
    public void testNotEquals() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertNotEquals(style1, style2);
    }

    @Test
    public void testCompareToEqual() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertEquals(0, style1.compareTo(style2));
    }

    @Test
    public void testCompareToNotEqual() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                mock(JournalAbbreviationRepository.class));
        assertTrue(style1.compareTo(style2) > 0);
        assertFalse(style2.compareTo(style1) > 0);
    }


    public void testEmptyStringPropertyAndOxfordComma() throws URISyntaxException, IOException {
        String fileName = Paths.get(OOBibStyleTest.class.getResource("test.jstyle").toURI()).toString();
        OOBibStyle style = new OOBibStyle(fileName, mock(JournalAbbreviationRepository.class));
        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "Alpha von Beta and Gamma Epsilon and Ypsilon Tau");
        entry.setField("title", "JabRef Manual");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("von Beta, Epsilon, and Tau, 2016",
                style.getCitationMarker(entries, entryDBMap, true, null, null));

    }

}