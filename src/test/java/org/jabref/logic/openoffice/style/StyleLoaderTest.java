package org.jabref.logic.openoffice.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.collections.FXCollections;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

public class StyleLoaderTest {

    private static final int NUMBER_OF_INTERNAL_STYLES = 2;
    private StyleLoader loader;

    private OpenOfficePreferences preferences;
    private LayoutFormatterPreferences layoutPreferences;
    private JournalAbbreviationRepository abbreviationRepository;

    @BeforeEach
    public void setUp() {
        preferences =
            mock(OpenOfficePreferences.class, Answers.RETURNS_DEEP_STUBS);
        layoutPreferences =
            mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        abbreviationRepository = mock(JournalAbbreviationRepository.class);
    }

    @Test
    public void throwNPEWithNullPreferences() {
        assertThrows(
            NullPointerException.class,
            () ->
                loader =
                    new StyleLoader(
                        null,
                        layoutPreferences,
                        abbreviationRepository
                    )
        );
    }

    @Test
    public void throwNPEWithNullLayoutPreferences() {
        assertThrows(
            NullPointerException.class,
            () ->
                loader =
                    new StyleLoader(
                        mock(OpenOfficePreferences.class),
                        null,
                        abbreviationRepository
                    )
        );
    }

    @Test
    public void throwNPEWithNullAbbreviationRepository() {
        assertThrows(
            NullPointerException.class,
            () ->
                loader =
                    new StyleLoader(
                        mock(OpenOfficePreferences.class),
                        layoutPreferences,
                        null
                    )
        );
    }

    @Test
    public void testGetStylesWithEmptyExternal() {
        preferences.setExternalStyles(Collections.emptyList());
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );

        assertEquals(2, loader.getStyles().size());
    }

    @Test
    public void testAddStyleLeadsToOneMoreStyle() throws URISyntaxException {
        preferences.setExternalStyles(Collections.emptyList());
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );

        String filename = Path
            .of(
                StyleLoader.class.getResource(
                        StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH
                    )
                    .toURI()
            )
            .toFile()
            .getPath();
        loader.addStyleIfValid(filename);
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
    }

    @Test
    public void testAddInvalidStyleLeadsToNoMoreStyle() {
        preferences.setExternalStyles(Collections.emptyList());
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        int beforeAdding = loader.getStyles().size();
        loader.addStyleIfValid(
            "DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky"
        );
        assertEquals(beforeAdding, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFile() throws URISyntaxException {
        String filename = Path
            .of(
                StyleLoader.class.getResource(
                        StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH
                    )
                    .toURI()
            )
            .toFile()
            .getPath();
        when(preferences.getExternalStyles())
            .thenReturn(FXCollections.singletonObservableList(filename));
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithIncorrectExternalFile() {
        preferences.setExternalStyles(
            Collections.singletonList(
                "DefinitelyNotAValidFileNameOrWeAreExtremelyUnlucky"
            )
        );

        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFileRemoveStyle()
        throws URISyntaxException {
        String filename = Path
            .of(
                StyleLoader.class.getResource(
                        StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH
                    )
                    .toURI()
            )
            .toFile()
            .getPath();
        when(preferences.getExternalStyles())
            .thenReturn(FXCollections.singletonObservableList(filename));

        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        List<OOBibStyle> toremove = new ArrayList<>();
        int beforeRemoving = loader.getStyles().size();
        for (OOBibStyle style : loader.getStyles()) {
            if (!style.isInternalStyle()) {
                toremove.add(style);
            }
        }

        for (OOBibStyle style : toremove) {
            assertTrue(loader.removeStyle(style));
        }
        assertEquals(beforeRemoving - 1, loader.getStyles().size());
    }

    @Test
    public void testInitalizeWithOneExternalFileRemoveStyleUpdatesPreferences()
        throws URISyntaxException {
        String filename = Path
            .of(
                StyleLoader.class.getResource(
                        StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH
                    )
                    .toURI()
            )
            .toFile()
            .getPath();
        when(preferences.getExternalStyles())
            .thenReturn(FXCollections.singletonObservableList(filename));

        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        List<OOBibStyle> toremove = new ArrayList<>();
        for (OOBibStyle style : loader.getStyles()) {
            if (!style.isInternalStyle()) {
                toremove.add(style);
            }
        }

        for (OOBibStyle style : toremove) {
            assertTrue(loader.removeStyle(style));
        }
        // As the prefs are mocked away, the getExternalStyles still returns the initial one
        assertFalse(preferences.getExternalStyles().isEmpty());
    }

    @Test
    public void testAddSameStyleTwiceLeadsToOneMoreStyle()
        throws URISyntaxException {
        preferences.setExternalStyles(Collections.emptyList());
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        int beforeAdding = loader.getStyles().size();
        String filename = Path
            .of(
                StyleLoader.class.getResource(
                        StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH
                    )
                    .toURI()
            )
            .toFile()
            .getPath();
        loader.addStyleIfValid(filename);
        loader.addStyleIfValid(filename);
        assertEquals(beforeAdding + 1, loader.getStyles().size());
    }

    @Test
    public void testAddNullStyleThrowsNPE() {
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        assertThrows(
            NullPointerException.class,
            () -> loader.addStyleIfValid(null)
        );
    }

    @Test
    public void testGetDefaultUsedStyleWhenEmpty() {
        when(preferences.getCurrentStyle())
            .thenReturn(StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        preferences.clearCurrentStyle();
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        OOBibStyle style = loader.getUsedStyle();
        assertTrue(style.isValid());
        assertEquals(
            StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
            style.getPath()
        );
        assertEquals(
            StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
            preferences.getCurrentStyle()
        );
    }

    @Test
    public void testGetStoredUsedStyle() {
        when(preferences.getCurrentStyle())
            .thenReturn(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH);
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        OOBibStyle style = loader.getUsedStyle();
        assertTrue(style.isValid());
        assertEquals(StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, style.getPath());
        assertEquals(
            StyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
            preferences.getCurrentStyle()
        );
    }

    @Test
    public void testGetDefaultUsedStyleWhenIncorrect() {
        when(preferences.getCurrentStyle())
            .thenReturn("ljlkjlkjnljnvdlsjniuhwelfhuewfhlkuewhfuwhelu");
        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        OOBibStyle style = loader.getUsedStyle();
        assertTrue(style.isValid());
        assertEquals(
            StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
            style.getPath()
        );
    }

    @Test
    public void testRemoveInternalStyleReturnsFalseAndDoNotRemove() {
        preferences.setExternalStyles(Collections.emptyList());

        loader =
            new StyleLoader(
                preferences,
                layoutPreferences,
                abbreviationRepository
            );
        List<OOBibStyle> toremove = new ArrayList<>();
        for (OOBibStyle style : loader.getStyles()) {
            if (style.isInternalStyle()) {
                toremove.add(style);
            }
        }

        assertFalse(loader.removeStyle(toremove.get(0)));
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }
}
