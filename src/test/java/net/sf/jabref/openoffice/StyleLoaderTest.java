package net.sf.jabref.openoffice;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;

public class StyleLoaderTest {

    private JabRefPreferences backup;

    private OpenOfficePreferences preferences;
    @Before
    public void setUp() {
        backup = JabRefPreferences.getInstance();
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
        if (Globals.journalAbbreviationLoader == null) {
            Globals.journalAbbreviationLoader = mock(JournalAbbreviationLoader.class);
        }
        preferences = new OpenOfficePreferences(Globals.prefs);
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.overwritePreferences(backup);
    }

    @Test(expected = NullPointerException.class)
    public void throwNPEWithNullPreferences() {
        StyleLoader loader = new StyleLoader(null,
                mock(JournalAbbreviationRepository.class), mock(Charset.class));
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void throwNPEWithNullRepository() {
        StyleLoader loader = new StyleLoader(mock(OpenOfficePreferences.class),
                null, mock(Charset.class));
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void throwNPEWithNullCharset() {
        StyleLoader loader = new StyleLoader(mock(OpenOfficePreferences.class),
                mock(JournalAbbreviationRepository.class), null);
        fail();
    }

    @Test
    public void testGetStylesWithEmptyExternal() {
        preferences.setExternalStyles(Collections.emptyList());
        StyleLoader loader = new StyleLoader(preferences,
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());

        assertEquals(2, loader.getStyles().size());
    }

    @Test
    public void testAddStyleLeadsToOneMoreStyle() throws URISyntaxException {
        preferences.setExternalStyles(Collections.emptyList());
        StyleLoader loader = new StyleLoader(preferences,
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        int beforeAdding = loader.getStyles().size();
        String filename = Paths.get(JabRef.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        loader.addStyle(filename);
        assertEquals(beforeAdding + 1, loader.getStyles().size());
    }

    @Test
    public void testAddInvalidStyleLeadsToNoMoreStyle() {
        preferences.setExternalStyles(Collections.emptyList());
        Globals.prefs.putStringList(JabRefPreferences.OO_EXTERNAL_STYLE_FILES, Collections.emptyList());
        StyleLoader loader = new StyleLoader(preferences,
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        int beforeAdding = loader.getStyles().size();
        loader.addStyle("DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky");
        assertEquals(beforeAdding, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFile() throws URISyntaxException {
        String filename = Paths.get(JabRef.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        preferences.setExternalStyles(Collections.singletonList(filename));
        StyleLoader loader = new StyleLoader(preferences,
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        assertEquals(3, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithIncorrectExternalFile() {
        preferences.setExternalStyles(Collections.singletonList("DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky"));

        StyleLoader loader = new StyleLoader(new OpenOfficePreferences(Globals.prefs),
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        assertEquals(2, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFileRemoveStyle() throws URISyntaxException {
        String filename = Paths.get(JabRef.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        preferences.setExternalStyles(Collections.singletonList(filename));

        StyleLoader loader = new StyleLoader(new OpenOfficePreferences(Globals.prefs),
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        List<OOBibStyle> toremove = new ArrayList<>();
        int beforeRemoving = loader.getStyles().size();
        for (OOBibStyle style : loader.getStyles()) {
            if (!style.isFromResource()) {
                toremove.add(style);
            }
        }

        for (OOBibStyle style : toremove) {
            assertTrue(loader.removeStyle(style));
        }
        assertEquals(beforeRemoving - 1, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFileRemoveStyleUpdatesPreferences() throws URISyntaxException {
        String filename = Paths.get(JabRef.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        preferences.setExternalStyles(Collections.singletonList(filename));

        StyleLoader loader = new StyleLoader(preferences,
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        List<OOBibStyle> toremove = new ArrayList<>();
        for (OOBibStyle style : loader.getStyles()) {
            if (!style.isFromResource()) {
                toremove.add(style);
            }
        }

        for (OOBibStyle style : toremove) {
            assertTrue(loader.removeStyle(style));
        }
        assertTrue(preferences.getExternalStyles().isEmpty());
    }

    @Test
    public void testAddSameStyleTwiceLeadsToOneMoreStyle() throws URISyntaxException {
        preferences.setExternalStyles(Collections.emptyList());
        StyleLoader loader = new StyleLoader(new OpenOfficePreferences(Globals.prefs),
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        int beforeAdding = loader.getStyles().size();
        String filename = Paths.get(JabRef.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        loader.addStyle(filename);
        loader.addStyle(filename);
        assertEquals(beforeAdding + 1, loader.getStyles().size());
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullStyleThrowsNPE() {
        StyleLoader loader = new StyleLoader(new OpenOfficePreferences(Globals.prefs),
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        loader.addStyle(null);
        fail();
    }


    @Test
    public void testGetDefaultUsedStyle() {
        Globals.prefs.remove(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE);
        StyleLoader loader = new StyleLoader(new OpenOfficePreferences(Globals.prefs),
                mock(JournalAbbreviationRepository.class), Globals.prefs.getDefaultEncoding());
        OOBibStyle style = loader.getUsedStyle();
        assertTrue(style.isValid());
        assertEquals(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, style.getPath());
        assertEquals(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, preferences.getCurrentStyle());
    }

}
