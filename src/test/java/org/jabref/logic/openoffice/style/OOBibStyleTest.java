package org.jabref.logic.openoffice.style;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationMarkerEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericBibEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OOBibStyleTest {
    private LayoutFormatterPreferences layoutFormatterPreferences;

    @BeforeEach
    void setUp() {
        layoutFormatterPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    void testAuthorYear() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, layoutFormatterPreferences);
        assertTrue(style.isValid());
        assertTrue(style.isInternalStyle());
        assertFalse(style.isCitationKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertFalse(style.isNumberEntries());
        assertFalse(style.isSortByPosition());
    }

    @Test
    void testAuthorYearAsFile() throws URISyntaxException, IOException {
        File defFile = Path.of(OOBibStyleTest.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                           .toFile();
        OOBibStyle style = new OOBibStyle(defFile, layoutFormatterPreferences, StandardCharsets.UTF_8);
        assertTrue(style.isValid());
        assertFalse(style.isInternalStyle());
        assertFalse(style.isCitationKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertFalse(style.isNumberEntries());
        assertFalse(style.isSortByPosition());
    }

    @Test
    void testNumerical() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        assertTrue(style.isValid());
        assertFalse(style.isCitationKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertTrue(style.isNumberEntries());
        assertTrue(style.isSortByPosition());
    }

    /*
     * begin helpers
     */
    static String runGetNumCitationMarker2a(OOBibStyle style,
                                            List<Integer> num, int minGroupingCount, boolean inList) {
        return OOBibStyleTestHelper.runGetNumCitationMarker2a(style, num, minGroupingCount, inList);
    }

    static CitationMarkerNumericEntry numEntry(String key, int num, String pageInfoOrNull) {
        return OOBibStyleTestHelper.numEntry(key, num, pageInfoOrNull);
    }

    static CitationMarkerNumericBibEntry numBibEntry(String key, Optional<Integer> num) {
        return OOBibStyleTestHelper.numBibEntry(key, num);
    }

    static String runGetNumCitationMarker2b(OOBibStyle style,
                                            int minGroupingCount,
                                            CitationMarkerNumericEntry... s) {
        List<CitationMarkerNumericEntry> input = Stream.of(s).collect(Collectors.toList());
        OOText res = style.getNumCitationMarker2(input, minGroupingCount);
        return res.toString();
    }

    static CitationMarkerEntry makeCitationMarkerEntry(BibEntry entry,
                                                       BibDatabase database,
                                                       String uniqueLetterQ,
                                                       String pageInfoQ,
                                                       boolean isFirstAppearanceOfSource) {
        return OOBibStyleTestHelper.makeCitationMarkerEntry(entry,
                                                            database,
                                                            uniqueLetterQ,
                                                            pageInfoQ,
                                                            isFirstAppearanceOfSource);
    }

    /*
     * Similar to old API. pageInfo is new, and unlimAuthors is
     * replaced with isFirstAppearanceOfSource
     */
    static String getCitationMarker2(OOBibStyle style,
                                     List<BibEntry> entries,
                                     Map<BibEntry, BibDatabase> entryDBMap,
                                     boolean inParenthesis,
                                     String[] uniquefiers,
                                     Boolean[] isFirstAppearanceOfSource,
                                     String[] pageInfo) {
        return OOBibStyleTestHelper.getCitationMarker2(style,
                                                       entries,
                                                       entryDBMap,
                                                       inParenthesis,
                                                       uniquefiers,
                                                       isFirstAppearanceOfSource,
                                                       pageInfo);
    }

    /*
     * end helpers
     */

    @Test
    void testGetNumCitationMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        assertEquals("[1] ", style.getNumCitationMarker(Arrays.asList(1), -1, true));
        assertEquals("[1] ", runGetNumCitationMarker2a(style, Arrays.asList(1), -1, true));

        assertEquals("[1]", style.getNumCitationMarker(Arrays.asList(1), -1, false));
        assertEquals("[1]", runGetNumCitationMarker2a(style, Arrays.asList(1), -1, false));
        assertEquals("[1]", runGetNumCitationMarker2b(style, -1, numEntry("key", 1, null)));

        assertEquals("[1] ", style.getNumCitationMarker(Arrays.asList(1), 0, true));
        assertEquals("[1] ", runGetNumCitationMarker2a(style, Arrays.asList(1), 0, true));

        /*
         * The following tests as for a numeric label for a
         * bibliography entry containing more than one numbers.
         * We do not need this, not reproduced.
         */
        assertEquals("[1-3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), 1, true));
        assertEquals("[1; 2; 3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), 5, true));
        assertEquals("[1; 2; 3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), -1, true));
        assertEquals("[1; 3; 12] ", style.getNumCitationMarker(Arrays.asList(1, 12, 3), 1, true));
        assertEquals("[3-5; 7; 10-12] ", style.getNumCitationMarker(Arrays.asList(12, 7, 3, 4, 11, 10, 5), 1, true));

        String citation = style.getNumCitationMarker(Arrays.asList(1), -1, false);
        assertEquals("[1; pp. 55-56]", style.insertPageInfo(citation, "pp. 55-56"));

        CitationMarkerNumericEntry e2 = numEntry("key", 1, "pp. 55-56");
        assertEquals(true, e2.getPageInfo().isPresent());
        assertEquals("pp. 55-56", e2.getPageInfo().get().toString());
        citation = runGetNumCitationMarker2b(style, -1, e2);
        assertEquals("[1; pp. 55-56]", citation);

        OOBibStyleTestHelper.testGetNumCitationMarkerExtra(style);
    }

    @Test
    void testGetNumCitationMarkerUndefined() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        /*
         * Testing bibliography labels with multiple numbers again.
         * Not reproduced.
         */
        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "; 2-4] ",
                style.getNumCitationMarker(Arrays.asList(4, 2, 3, 0), 1, true));

        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "] ",
                style.getNumCitationMarker(Arrays.asList(0), 1, true));

        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "; 1-3] ",
                style.getNumCitationMarker(Arrays.asList(1, 2, 3, 0), 1, true));

        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "; " + OOBibStyle.UNDEFINED_CITATION_MARKER + "; "
                        + OOBibStyle.UNDEFINED_CITATION_MARKER + "] ",
                style.getNumCitationMarker(Arrays.asList(0, 0, 0), 1, true));

        /*
         * We have these instead:
         */

        // unresolved citations look like [??key]
        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "key" + "]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("key", 0, null)));

        // pageInfo is shown for unresolved citations
        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "key" + "; p1]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("key", 0, "p1")));

        // unresolved citations sorted to the front
        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "key" + "; 2-4]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x4", 4, ""),
                                               numEntry("x2", 2, ""),
                                               numEntry("x3", 3, ""),
                                               numEntry("key", 0, "")));

        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "key" + "; 1-3]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x1", 1, ""),
                                               numEntry("x2", 2, ""),
                                               numEntry("y3", 3, ""),
                                               numEntry("key", 0, "")));

        // multiple unresolved citations are not collapsed
        assertEquals("["
                     + OOBibStyle.UNDEFINED_CITATION_MARKER + "x1" + "; "
                     + OOBibStyle.UNDEFINED_CITATION_MARKER + "x2" + "; "
                     + OOBibStyle.UNDEFINED_CITATION_MARKER + "x3" + "]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x1", 0, ""),
                                               numEntry("x2", 0, ""),
                                               numEntry("x3", 0, "")));

        /*
         * BIBLIOGRAPHY
         */
        CitationMarkerNumericBibEntry x = numBibEntry("key", Optional.empty());
        assertEquals("[" + OOBibStyle.UNDEFINED_CITATION_MARKER + "key" + "] ",
                     style.getNumCitationMarkerForBibliography(x).toString());
    }

    @Test
    void testGetCitProperty() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        assertEquals(", ", style.getStringCitProperty("AuthorSeparator"));

        // old
        assertEquals(3, style.getIntCitProperty("MaxAuthors"));
        assertTrue(style.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL));
        // new
        assertEquals(3, style.getMaxAuthors());
        assertTrue(style.getMultiCiteChronological());

        assertEquals("Default", style.getCitationCharacterFormat());
        assertEquals("Default [number] style file.", style.getName());
        Set<String> journals = style.getJournals();
        assertTrue(journals.contains("Journal name 1"));
    }

    @Test
    void testGetCitationMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, layoutFormatterPreferences);
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "Gustav Bostr\\\"{o}m and Jaana W\\\"{a}yrynen and Marine Bod\\'{e}n and Konstantin Beznosov and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "SESS '06: Proceedings of the 2006 international workshop on Software engineering for secure systems")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "Extending XP practices to support security requirements engineering")
                .withField(StandardField.PAGES, "11--18");
        entry.setCitationKey("Bostrom2006"); // citation key is not optional now
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        entryDBMap.put(entry, database);

        // Check what unlimAuthors values correspond to isFirstAppearanceOfSource false/true
        assertEquals(3, style.getMaxAuthors());
        assertEquals(-1, style.getMaxAuthorsFirst());

        assertEquals("[Boström et al., 2006]",
                style.getCitationMarker(Collections.singletonList(entry), entryDBMap, true, null, null));
        assertEquals("[Boström et al., 2006]",
                     getCitationMarker2(style,
                                        Collections.singletonList(entry), entryDBMap,
                                        true, null, null, null));

        assertEquals("Boström et al. [2006]",
                style.getCitationMarker(Collections.singletonList(entry), entryDBMap, false, null, new int[]{3}));
        assertEquals("Boström et al. [2006]",
                     getCitationMarker2(style,
                                        Collections.singletonList(entry), entryDBMap,
                                        false, null, new Boolean[]{false}, null));

        assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006]",
                style.getCitationMarker(Collections.singletonList(entry), entryDBMap, true, null, new int[]{5}));
        assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006]",
                     getCitationMarker2(style,
                                        Collections.singletonList(entry), entryDBMap,
                                        true,
                                        null,
                                        new Boolean[]{true}  /* corresponds to -1, not 5 */,
                                        null));
    }

    @Test
    void testLayout() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, layoutFormatterPreferences);

        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "Gustav Bostr\\\"{o}m and Jaana W\\\"{a}yrynen and Marine Bod\\'{e}n and Konstantin Beznosov and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "SESS '06: Proceedings of the 2006 international workshop on Software engineering for secure systems")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "Extending XP practices to support security requirements engineering")
                .withField(StandardField.PAGES, "11--18");
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);

        Layout l = style.getReferenceFormat(new UnknownEntryType("default"));
        l.setPostFormatter(new OOPreFormatter());
        assertEquals(
                "Boström, G.; Wäyrynen, J.; Bodén, M.; Beznosov, K. and Kruchten, P. (<b>2006</b>). <i>Extending XP practices to support security requirements engineering</i>,   : 11-18.",
                l.doLayout(entry, database));

        l = style.getReferenceFormat(StandardEntryType.InCollection);
        l.setPostFormatter(new OOPreFormatter());
        assertEquals(
                "Boström, G.; Wäyrynen, J.; Bodén, M.; Beznosov, K. and Kruchten, P. (<b>2006</b>). <i>Extending XP practices to support security requirements engineering</i>. In:  (Ed.), <i>SESS '06: Proceedings of the 2006 international workshop on Software engineering for secure systems</i>, ACM.",
                l.doLayout(entry, database));
    }

    @Test
    void testInstitutionAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, layoutFormatterPreferences);
        BibDatabase database = new BibDatabase();

        Layout l = style.getReferenceFormat(StandardEntryType.Article);
        l.setPostFormatter(new OOPreFormatter());

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "{JabRef Development Team}");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        assertEquals("<b>JabRef Development Team</b> (<b>2016</b>). <i>JabRef Manual</i>,  .",
                l.doLayout(entry, database));
    }

    @Test
    void testVonAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        BibDatabase database = new BibDatabase();

        Layout l = style.getReferenceFormat(StandardEntryType.Article);
        l.setPostFormatter(new OOPreFormatter());

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        assertEquals("<b>von Beta, A.</b> (<b>2016</b>). <i>JabRef Manual</i>,  .",
                l.doLayout(entry, database));
    }

    @Test
    void testInstitutionAuthorMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setCitationKey("JabRef2016");
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "{JabRef Development Team}");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[JabRef Development Team, 2016]", style.getCitationMarker(entries, entryDBMap, true, null, null));

        assertEquals("[JabRef Development Team, 2016]",
                     getCitationMarker2(style,
                                        entries, entryDBMap, true, null, null, null));
    }

    @Test
    void testVonAuthorMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[von Beta, 2016]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    void testNullAuthorMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[, 2016]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    void testNullYearMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[von Beta, ]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    void testEmptyEntryMarker() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[, ]", style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    void testGetCitationMarkerInParenthesisUniquefiers() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 2");
        entry3.setField(StandardField.YEAR, "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Gamma Epsilon");
        entry2.setField(StandardField.YEAR, "2001");
        entries.add(entry2);
        database.insertEntry(entry2);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("[Beta, 2000; Beta, 2000; Epsilon, 2001]",
                style.getCitationMarker(entries, entryDBMap, true, null, null));
        assertEquals("[Beta, 2000a,b; Epsilon, 2001]",
                style.getCitationMarker(entries, entryDBMap, true, new String[]{"a", "b", ""}, new int[]{1, 1, 1}));
    }

    @Test
    void testGetCitationMarkerInTextUniquefiers() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 2");
        entry3.setField(StandardField.YEAR, "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Gamma Epsilon");
        entry2.setField(StandardField.YEAR, "2001");
        entries.add(entry2);
        database.insertEntry(entry2);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("Beta [2000]; Beta [2000]; Epsilon [2001]",
                style.getCitationMarker(entries, entryDBMap, false, null, null));
        assertEquals("Beta [2000a,b]; Epsilon [2001]",
                style.getCitationMarker(entries, entryDBMap, false, new String[]{"a", "b", ""}, new int[]{1, 1, 1}));
    }

    @Test
    void testGetCitationMarkerInParenthesisUniquefiersThreeSameAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Alpha Beta");
        entry2.setField(StandardField.TITLE, "Paper 2");
        entry2.setField(StandardField.YEAR, "2000");
        entries.add(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 3");
        entry3.setField(StandardField.YEAR, "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("[Beta, 2000a,b,c]",
                style.getCitationMarker(entries, entryDBMap, true, new String[]{"a", "b", "c"}, new int[]{1, 1, 1}));
    }

    @Test
    void testGetCitationMarkerInTextUniquefiersThreeSameAuthor() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Alpha Beta");
        entry2.setField(StandardField.TITLE, "Paper 2");
        entry2.setField(StandardField.YEAR, "2000");
        entries.add(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 3");
        entry3.setField(StandardField.YEAR, "2000");
        entries.add(entry3);
        database.insertEntry(entry3);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("Beta [2000a,b,c]",
                style.getCitationMarker(entries, entryDBMap, false, new String[]{"a", "b", "c"}, new int[]{1, 1, 1}));
    }

    @Test
    // TODO: equals only work when initialized from file, not from reader
    void testEquals() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        assertEquals(style1, style2);
    }

    @Test
    // TODO: equals only work when initialized from file, not from reader
    void testNotEquals() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                layoutFormatterPreferences);
        assertNotEquals(style1, style2);
    }

    @Test
    void testCompareToEqual() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        assertEquals(0, style1.compareTo(style2));
    }

    @Test
    void testCompareToNotEqual() throws IOException {
        OOBibStyle style1 = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences);
        OOBibStyle style2 = new OOBibStyle(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                layoutFormatterPreferences);
        assertTrue(style1.compareTo(style2) > 0);
        assertFalse(style2.compareTo(style1) > 0);
    }

    @Test
    void testEmptyStringPropertyAndOxfordComma() throws Exception {
        OOBibStyle style = new OOBibStyle("test.jstyle", layoutFormatterPreferences);
        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta and Gamma Epsilon and Ypsilon Tau");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("von Beta, Epsilon, & Tau, 2016",
                style.getCitationMarker(entries, entryDBMap, true, null, null));
    }

    @Test
    void testIsValidWithDefaultSectionAtTheStart() throws Exception {
        OOBibStyle style = new OOBibStyle("testWithDefaultAtFirstLIne.jstyle", layoutFormatterPreferences);
        assertTrue(style.isValid());
    }

    @Test
    void testGetCitationMarkerJoinFirst() throws IOException {
        OOBibStyle style = new OOBibStyle(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                                          layoutFormatterPreferences);

        // Question: What should happen if some of the sources is
        // marked as isFirstAppearanceOfSource?
        // This test documents what is happening now.

        // Two entries with identical normalizedMarkers and many authors.
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.AUTHOR,
                           "Gustav Bostr\\\"{o}m"
                           + " and Jaana W\\\"{a}yrynen"
                           + " and Marine Bod\\'{e}n"
                           + " and Konstantin Beznosov"
                           + " and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "A book 1")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "Title 1")
                .withField(StandardField.PAGES, "11--18");
        entry1.setCitationKey("b1");

        BibEntry entry2 = new BibEntry()
                .withField(StandardField.AUTHOR,
                           "Gustav Bostr\\\"{o}m"
                           + " and Jaana W\\\"{a}yrynen"
                           + " and Marine Bod\\'{e}n"
                           + " and Konstantin Beznosov"
                           + " and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "A book 2")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "title2")
                .withField(StandardField.PAGES, "11--18");
        entry2.setCitationKey("b2");

        // Last Author differs.
        BibEntry entry3 = new BibEntry()
                .withField(StandardField.AUTHOR,
                           "Gustav Bostr\\\"{o}m"
                           + " and Jaana W\\\"{a}yrynen"
                           + " and Marine Bod\\'{e}n"
                           + " and Konstantin Beznosov"
                           + " and Philippe NotKruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "A book 3")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "title3")
                .withField(StandardField.PAGES, "11--18");
        entry3.setCitationKey("b3");

        BibDatabase database = new BibDatabase();
        database.insertEntry(entry1);
        database.insertEntry(entry2);
        database.insertEntry(entry3);

        // Without pageInfo, two isFirstAppearanceOfSource may be joined.
        // The third is NotKruchten, should not be joined.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                makeCitationMarkerEntry(entry1, database, "a", null, true);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                makeCitationMarkerEntry(entry2, database, "b", null, true);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                makeCitationMarkerEntry(entry3, database, "c", null, true);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006a,b"
                         + "; Boström, Wäyrynen, Bodén, Beznosov & NotKruchten, 2006c]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    true,
                                                    NonUniqueCitationMarker.THROWS).toString());

            assertEquals("Boström, Wäyrynen, Bodén, Beznosov & Kruchten [2006a,b]"
                         + "; Boström, Wäyrynen, Bodén, Beznosov & NotKruchten [2006c]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    false,
                                                    NonUniqueCitationMarker.THROWS).toString());
        }

        // Without pageInfo, only the first is isFirstAppearanceOfSource.
        // The second may be joined, based on expanded normalizedMarkers.
        // The third is NotKruchten, should not be joined.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                makeCitationMarkerEntry(entry1, database, "a", null, true);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                makeCitationMarkerEntry(entry2, database, "b", null, false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                makeCitationMarkerEntry(entry3, database, "c", null, false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006a,b"
                         + "; Boström et al., 2006c]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    true,
                                                    NonUniqueCitationMarker.THROWS).toString());

        }
        // Without pageInfo, only the second is isFirstAppearanceOfSource.
        // The second is not joined, because it is a first appearance, thus
        // requires more names to be shown.
        // The third is NotKruchten, should not be joined.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                makeCitationMarkerEntry(entry1, database, "a", null, false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                makeCitationMarkerEntry(entry2, database, "b", null, true);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                makeCitationMarkerEntry(entry3, database, "c", null, false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a"
                         + "; Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006b"
                         + "; Boström et al., 2006c]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    true,
                                                    NonUniqueCitationMarker.THROWS).toString());
        }

        // Without pageInfo, neither is isFirstAppearanceOfSource.
        // The second is joined.
        // The third is NotKruchten, but is joined because NotKruchten is not among the names shown.
        // Is this the correct behaviour?
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                makeCitationMarkerEntry(entry1, database, "a", null, false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                makeCitationMarkerEntry(entry2, database, "b", null, false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                makeCitationMarkerEntry(entry3, database, "c", null, false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a,b,c]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    true,
                                                    NonUniqueCitationMarker.THROWS).toString());
        }

        // With pageInfo: different entries with identical non-null pageInfo: not joined.
        // XY [2000a,b,c; p1] whould be confusing.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                makeCitationMarkerEntry(entry2, database, "b", "p1", false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                makeCitationMarkerEntry(entry3, database, "c", "p1", false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a; p1"
                         + "; Boström et al., 2006b; p1"
                         + "; Boström et al., 2006c; p1]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    true,
                                                    NonUniqueCitationMarker.THROWS).toString());
        }

        // With pageInfo: same entries with identical non-null pageInfo: collapsed.
        // Note: "same" here looks at the visible parts and citation key only,
        //       but ignores the rest. Normally the citation key should distinguish.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a; p1]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    true,
                                                    NonUniqueCitationMarker.THROWS).toString());
        }
        // With pageInfo: same entries with different pageInfo: kept separate.
        // Empty ("") and missing pageInfos considered equal, thus collapsed.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                makeCitationMarkerEntry(entry1, database, "a", "p2", false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                makeCitationMarkerEntry(entry1, database, "a", "", false);
            citationMarkerEntries.add(cm3);
            CitationMarkerEntry cm4 =
                makeCitationMarkerEntry(entry1, database, "a", null, false);
            citationMarkerEntries.add(cm4);

            assertEquals("[Boström et al., 2006a; p1"
                         + "; Boström et al., 2006a; p2"
                         + "; Boström et al., 2006a]",
                         style.createCitationMarker(citationMarkerEntries,
                                                    true,
                                                    NonUniqueCitationMarker.THROWS).toString());
        }
    }
}
