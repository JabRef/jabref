package net.sf.jabref.openoffice;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;


public class OOBibStyleTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();

        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader(Globals.prefs);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAuthorYear() {
        try {
            URL defPath = JabRef.class.getResource(OpenOfficePanel.DEFAULT_AUTHORYEAR_STYLE_PATH);
            Reader r = new InputStreamReader(defPath.openStream());
            OOBibStyle style = new OOBibStyle(r, Globals.journalAbbreviationLoader.getRepository());
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
            OOBibStyle style = new OOBibStyle(new File(defPath.getFile()),
                    Globals.journalAbbreviationLoader.getRepository(), Globals.prefs.getDefaultEncoding());
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
            OOBibStyle style = new OOBibStyle(r, Globals.journalAbbreviationLoader.getRepository());
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
        OOBibStyle style = new OOBibStyle(r, Globals.journalAbbreviationLoader.getRepository());
        assertEquals("[1] ", style.getNumCitationMarker(Arrays.asList(1), -1, true));
        assertEquals("[1]", style.getNumCitationMarker(Arrays.asList(1), -1, false));
        assertEquals("[1] ", style.getNumCitationMarker(Arrays.asList(1), 0, true));
        assertEquals("[1-3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), 1, true));
        assertEquals("[1; 2; 3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), 5, true));
        assertEquals("[1; 2; 3] ", style.getNumCitationMarker(Arrays.asList(1, 2, 3), -1, true));
        assertEquals("[1; 3; 12] ", style.getNumCitationMarker(Arrays.asList(1, 12, 3), 1, true));
        assertEquals("[3-5; 7; 10-12] ", style.getNumCitationMarker(Arrays.asList(12, 7, 3, 4, 11, 10, 5), 1, true));
    }
}
