package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JStyleLoaderTest {

    private static final int NUMBER_OF_INTERNAL_STYLES = 2;
    private static final String JSTYLE_NAME = "test.jstyle";

    private JStyleLoader loader;

    private OpenOfficePreferences preferences;
    private LayoutFormatterPreferences layoutPreferences;
    private JournalAbbreviationRepository abbreviationRepository;

    @TempDir
    private Path styleFolder;

    private Path jStyleFile;

    @BeforeEach
    void setUp() throws IOException {
        preferences = mock(OpenOfficePreferences.class, Answers.RETURNS_DEEP_STUBS);
        layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        abbreviationRepository = mock(JournalAbbreviationRepository.class);
        try (InputStream stream = JStyleLoader.class.getResourceAsStream(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH)) {
            jStyleFile = styleFolder.resolve(JSTYLE_NAME);
            Files.copy(stream, jStyleFile);
        }
    }

    @Test
    void throwNPEWithNullPreferences() {
        assertThrows(NullPointerException.class, () -> loader = new JStyleLoader(null, layoutPreferences, abbreviationRepository));
    }

    @Test
    void throwNPEWithNullLayoutPreferences() {
        assertThrows(NullPointerException.class, () -> loader = new JStyleLoader(mock(OpenOfficePreferences.class), null, abbreviationRepository));
    }

    @Test
    void throwNPEWithNullAbbreviationRepository() {
        assertThrows(NullPointerException.class, () -> loader = new JStyleLoader(mock(OpenOfficePreferences.class), layoutPreferences, null));
    }

    @Test
    void getStylesWithEmptyExternal() {
        preferences.setExternalJStyles(List.of());
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);

        assertEquals(2, loader.getStyles().size());
    }

    @Test
    void addStyleLeadsToOneMoreStyle() {
        preferences.setExternalJStyles(List.of());
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);

        loader.addStyleIfValid(jStyleFile);
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
    }

    @Test
    void addInvalidStyleLeadsToNoMoreStyle() {
        preferences.setExternalJStyles(List.of());
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        int beforeAdding = loader.getStyles().size();
        loader.addStyleIfValid(Path.of("DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky"));
        assertEquals(beforeAdding, loader.getStyles().size());
    }

    @Test
    void initalizeWithOneExternalFile() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.singletonObservableList(jStyleFile.toString()));
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
    }

    @Test
    void initalizeWithIncorrectExternalFile() {
        preferences.setExternalJStyles(List.of("DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky"));

        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }

    @Test
    void initalizeWithOneExternalFileRemoveStyle() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.singletonObservableList(jStyleFile.toString()));

        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> toremove = new ArrayList<>();
        int beforeRemoving = loader.getStyles().size();
        for (JStyle style : loader.getStyles()) {
            if (!style.isInternalStyle()) {
                toremove.add(style);
            }
        }

        for (JStyle style : toremove) {
            assertTrue(loader.removeStyle(style));
        }
        assertEquals(beforeRemoving - 1, loader.getStyles().size());
    }

    @Test
    void initalizeWithOneExternalFileRemoveStyleUpdatesPreferences() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.singletonObservableList(jStyleFile.toString()));

        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> toremove = new ArrayList<>();
        for (JStyle style : loader.getStyles()) {
            if (!style.isInternalStyle()) {
                toremove.add(style);
            }
        }

        for (JStyle style : toremove) {
            assertTrue(loader.removeStyle(style));
        }
        // As the prefs are mocked away, the getExternalStyles still returns the initial one
        assertFalse(preferences.getExternalJStyles().isEmpty());
    }

    @Test
    void addSameStyleTwiceLeadsToOneMoreStyle() {
        preferences.setExternalJStyles(List.of());
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        int beforeAdding = loader.getStyles().size();
        loader.addStyleIfValid(jStyleFile);
        loader.addStyleIfValid(jStyleFile);
        assertEquals(beforeAdding + 1, loader.getStyles().size());
    }

    @Test
    void addNullStyleThrowsNPE() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        assertThrows(NullPointerException.class, () -> loader.addStyleIfValid(null));
    }

    @Test
    void getDefaultUsedStyleWhenEmpty() {
        when(preferences.getCurrentJStyle()).thenReturn(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        preferences.clearCurrentJStyle();
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        JStyle style = loader.getUsedJstyle();
        assertTrue(style.isValid());
        assertEquals(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, style.getPath());
        assertEquals(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, preferences.getCurrentJStyle());
    }

    @Test
    void getStoredUsedStyle() {
        when(preferences.getCurrentJStyle()).thenReturn(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH);
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        JStyle style = loader.getUsedJstyle();
        assertTrue(style.isValid());
        assertEquals(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, style.getPath());
        assertEquals(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, preferences.getCurrentJStyle());
    }

    @Test
    void getDefaultUsedStyleWhenIncorrect() {
        when(preferences.getCurrentJStyle()).thenReturn("ljlkjlkjnljnvdlsjniuhwelfhuewfhlkuewhfuwhelu");
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        JStyle style = loader.getUsedJstyle();
        assertTrue(style.isValid());
        assertEquals(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, style.getPath());
    }

    @Test
    void removeInternalStyleReturnsFalseAndDoNotRemove() {
        preferences.setExternalJStyles(List.of());

        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> toremove = new ArrayList<>();
        for (JStyle style : loader.getStyles()) {
            if (style.isInternalStyle()) {
                toremove.add(style);
            }
        }

        assertFalse(loader.removeStyle(toremove.getFirst()));
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }
}
