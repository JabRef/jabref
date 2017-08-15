package org.jabref.logic.openoffice;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StyleLoaderTest {

    private static int numberOfInternalStyles = 2;
    private StyleLoader loader;

    private OpenOfficePreferences preferences;
    private LayoutFormatterPreferences layoutPreferences;
    private Charset encoding;


    @Before
    public void setUp() {
        preferences = mock(OpenOfficePreferences.class, Answers.RETURNS_DEEP_STUBS);
        layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        encoding = StandardCharsets.UTF_8;

    }

    @Test(expected = NullPointerException.class)
    public void throwNPEWithNullPreferences() {
        loader = new StyleLoader(null, layoutPreferences, mock(Charset.class));
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void throwNPEWithNullLayoutPreferences() {
        loader = new StyleLoader(mock(OpenOfficePreferences.class), null, mock(Charset.class));
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void throwNPEWithNullCharset() {
        loader = new StyleLoader(mock(OpenOfficePreferences.class), layoutPreferences, null);
        fail();
    }

    @Test
    public void testGetStylesWithEmptyExternal() {
        preferences.setExternalStyles(Collections.emptyList());
        loader = new StyleLoader(preferences, layoutPreferences, encoding);

        assertEquals(2, loader.getStyles().size());
    }

    @Test
    public void testAddStyleLeadsToOneMoreStyle() throws URISyntaxException {
        preferences.setExternalStyles(Collections.emptyList());
        loader = new StyleLoader(preferences, layoutPreferences, encoding);

        String filename = Paths.get(StyleLoader.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        loader.addStyleIfValid(filename);
        assertEquals(numberOfInternalStyles + 1, loader.getStyles().size());
    }

    @Test
    public void testAddInvalidStyleLeadsToNoMoreStyle() {
        preferences.setExternalStyles(Collections.emptyList());
        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        int beforeAdding = loader.getStyles().size();
        loader.addStyleIfValid("DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky");
        assertEquals(beforeAdding, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFile() throws URISyntaxException {
        String filename = Paths.get(StyleLoader.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        when(preferences.getExternalStyles()).thenReturn(Collections.singletonList(filename));
        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        assertEquals(numberOfInternalStyles + 1, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithIncorrectExternalFile() {
        preferences.setExternalStyles(Collections.singletonList("DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky"));

        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        assertEquals(numberOfInternalStyles, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFileRemoveStyle() throws URISyntaxException {
        String filename = Paths.get(StyleLoader.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        when(preferences.getExternalStyles()).thenReturn(Collections.singletonList(filename));

        loader = new StyleLoader(preferences, layoutPreferences, encoding);
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
    @Ignore("This tests the preferences that are mocked away")
    public void testInitalizeWithOneExternalFileRemoveStyleUpdatesPreferences() throws URISyntaxException {
        String filename = Paths.get(StyleLoader.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        when(preferences.getExternalStyles()).thenReturn(Collections.singletonList(filename));

        loader = new StyleLoader(preferences, layoutPreferences, encoding);
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
        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        int beforeAdding = loader.getStyles().size();
        String filename = Paths.get(StyleLoader.class.getResource(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH).toURI())
                .toFile().getPath();
        loader.addStyleIfValid(filename);
        loader.addStyleIfValid(filename);
        assertEquals(beforeAdding + 1, loader.getStyles().size());
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullStyleThrowsNPE() {
        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        loader.addStyleIfValid(null);
        fail();
    }

    @Test
    public void testGetDefaultUsedStyleWhenEmpty() {
        when(preferences.getCurrentStyle()).thenReturn(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        preferences.clearCurrentStyle();
        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        OOBibStyle style = loader.getUsedStyle();
        assertTrue(style.isValid());
        assertEquals(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, style.getPath());
        assertEquals(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, preferences.getCurrentStyle());
    }

    @Test
    public void testGetStoredUsedStyle() {
        when(preferences.getCurrentStyle()).thenReturn(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH);
        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        OOBibStyle style = loader.getUsedStyle();
        assertTrue(style.isValid());
        assertEquals(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, style.getPath());
        assertEquals(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, preferences.getCurrentStyle());
    }

    @Test
    @Ignore("This tests the preferences that are mocked away")
    public void testGtDefaultUsedStyleWhenIncorrect() {
        when(preferences.getCurrentStyle()).thenReturn("ljlkjlkjnljnvdlsjniuhwelfhuewfhlkuewhfuwhelu");
        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        OOBibStyle style = loader.getUsedStyle();
        assertTrue(style.isValid());
        assertEquals(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, style.getPath());
        assertEquals(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, preferences.getCurrentStyle());
    }

    @Test
    public void testRemoveInternalStyleReturnsFalseAndDoNotRemove() {
        preferences.setExternalStyles(Collections.emptyList());

        loader = new StyleLoader(preferences, layoutPreferences, encoding);
        List<OOBibStyle> toremove = new ArrayList<>();
        for (OOBibStyle style : loader.getStyles()) {
            if (style.isFromResource()) {
                toremove.add(style);
            }
        }

        assertFalse(loader.removeStyle(toremove.get(0)));
        assertEquals(numberOfInternalStyles, loader.getStyles().size());
    }

}
