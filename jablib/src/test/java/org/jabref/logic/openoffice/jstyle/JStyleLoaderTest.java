package org.jabref.logic.openoffice.jstyle;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.observableArrayList());
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
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);

        assertEquals(2, loader.getStyles().size());
    }

    @Test
    void addStyleLeadsToOneMoreStyle() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);

        assertTrue(loader.addStyleIfValid(jStyleFile));
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString()));
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addInvalidStyleLeadsToNoMoreStyle() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        int beforeAdding = loader.getStyles().size();
        assertFalse(loader.addStyleIfValid(styleFolder.resolve("missing.jstyle")));
        assertEquals(beforeAdding, loader.getStyles().size());
        verify(preferences, never()).setExternalJStyles(anyList());
    }

    @Test
    void initializeWithOneExternalFile() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.singletonObservableList(jStyleFile.toString()));
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void initializeWithIncorrectExternalFile() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.singletonObservableList(styleFolder.resolve("missing.jstyle").toString()));

        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }

    @Test
    void initializeWithOneExternalFileRemoveStyle() {
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
    void initializeWithOneExternalFileRemoveStyleUpdatesPreferences() {
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
        verify(preferences).setExternalJStyles(List.of());
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addSameStyleTwiceLeadsToOneMoreStyle() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        int beforeAdding = loader.getStyles().size();
        assertTrue(loader.addStyleIfValid(jStyleFile));
        assertFalse(loader.addStyleIfValid(jStyleFile));
        assertEquals(beforeAdding + 1, loader.getStyles().size());
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString()));
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addNullStyleThrowsNPE() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        assertThrows(NullPointerException.class, () -> loader.addStyleIfValid(null));
    }

    @Test
    void loadingExternalStylesPreservesCurrentStyleSelection() {
        preferences = OpenOfficePreferences.getDefault();
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        JStyle selectedStyle = loader.getStyles().getLast();
        preferences.setCurrentStyle(selectedStyle);
        loader.addStyleIfValid(jStyleFile);

        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);

        assertEquals(selectedStyle, preferences.getCurrentStyle());
        assertEquals(List.of(jStyleFile.toString()), preferences.getExternalJStyles());
        assertEquals(List.of(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, jStyleFile.toString()),
                loader.getStyles().stream().map(JStyle::getPath).toList());
    }

    @Test
    void removeInternalStyleReturnsFalseAndDoNotRemove() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> toremove = new ArrayList<>();
        for (JStyle style : loader.getStyles()) {
            if (style.isInternalStyle()) {
                toremove.add(style);
            }
        }

        assertFalse(loader.removeStyle(toremove.getFirst()));
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
        verify(preferences, never()).setExternalJStyles(anyList());
    }

    @Test
    void addExternalStyleRegistersParsedStyleAndPersistsOnce() throws IOException {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        JStyle style = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);
        Files.delete(jStyleFile);

        assertTrue(loader.addExternalStyle(style));

        assertSame(style, loader.getStyles().getLast());
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString()));
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addExternalStyleRejectsInternalStyleWithoutPersisting() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addExternalStyle(beforeAdding.getFirst()));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences, never()).setExternalJStyles(anyList());
    }

    @Test
    void addExternalStyleRejectsInvalidStyleWithoutPersisting() throws IOException {
        Files.writeString(jStyleFile, "Not a JStyle");
        JStyle style = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addExternalStyle(style));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences, never()).setExternalJStyles(anyList());
    }

    @Test
    void addStyleIfValidRejectsInvalidContentWithoutPersisting() throws IOException {
        Files.writeString(jStyleFile, "Not a JStyle");
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addStyleIfValid(jStyleFile));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences, never()).setExternalJStyles(anyList());
    }

    @Test
    void addExternalStyleRejectsEqualStyleWithoutPersistingAgain() throws IOException {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        JStyle style = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);
        JStyle duplicate = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);
        assertTrue(loader.addExternalStyle(style));
        List<JStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addExternalStyle(duplicate));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString()));
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addExternalStyleAcceptsSameNameAtDifferentPaths() throws IOException {
        Path otherFile = Files.copy(jStyleFile, styleFolder.resolve("other.jstyle"));
        JStyle style = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);
        JStyle otherStyle = new JStyle(otherFile, layoutPreferences, abbreviationRepository);
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        assertEquals(loader.getStyles().getFirst().getName(), style.getName());
        assertEquals(style.getName(), otherStyle.getName());

        assertTrue(loader.addExternalStyle(style));
        assertTrue(loader.addExternalStyle(otherStyle));

        assertEquals(List.of(style, otherStyle), loader.getStyles().subList(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size()));
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString()));
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString(), otherFile.toString()));
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addExternalStyleAcceptsDifferentPropertiesAtSamePath() throws IOException {
        JStyle style = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);
        JStyle changedStyle = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);
        changedStyle.getCitProperties().put("BibTeXKeyCitations", true);
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);

        assertTrue(loader.addExternalStyle(style));
        assertTrue(loader.addExternalStyle(changedStyle));

        assertEquals(List.of(style, changedStyle), loader.getStyles().subList(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size()));
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString()));
        verify(preferences).setExternalJStyles(List.of(jStyleFile.toString(), jStyleFile.toString()));
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void removeUnregisteredStyleDoesNotPersist() throws IOException {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> beforeRemoving = loader.getStyles();
        JStyle style = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);

        assertFalse(loader.removeStyle(style));

        assertEquals(beforeRemoving, loader.getStyles());
        verify(preferences, never()).setExternalJStyles(anyList());
    }

    @Test
    void removeSameStyleTwicePersistsOnlyOnce() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.singletonObservableList(jStyleFile.toString()));
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        JStyle style = loader.getStyles().getLast();

        assertTrue(loader.removeStyle(style));
        assertFalse(loader.removeStyle(style));

        assertEquals(List.of(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH),
                loader.getStyles().stream().map(JStyle::getPath).toList());
        verify(preferences).setExternalJStyles(List.of());
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void getStylesReturnsUnmodifiableList() {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> styles = loader.getStyles();

        assertThrows(UnsupportedOperationException.class, styles::clear);

        assertEquals(styles, loader.getStyles());
    }

    @Test
    void getStylesSnapshotDoesNotChangeAfterAddingStyle() throws IOException {
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> styles = loader.getStyles();
        JStyle style = new JStyle(jStyleFile, layoutPreferences, abbreviationRepository);

        assertTrue(loader.addExternalStyle(style));

        assertEquals(List.of(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH),
                styles.stream().map(JStyle::getPath).toList());
        assertSame(style, loader.getStyles().getLast());
    }

    @Test
    void getStylesSnapshotDoesNotChangeAfterRemovingStyle() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.singletonObservableList(jStyleFile.toString()));
        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);
        List<JStyle> styles = loader.getStyles();

        assertTrue(loader.removeStyle(styles.getLast()));

        assertEquals(List.of(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, jStyleFile.toString()),
                styles.stream().map(JStyle::getPath).toList());
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }

    @Test
    void loadingExternalStylesPreservesStoredDuplicatesWithoutPersisting() {
        when(preferences.getExternalJStyles()).thenReturn(FXCollections.observableArrayList(jStyleFile.toString(), jStyleFile.toString()));

        loader = new JStyleLoader(preferences, layoutPreferences, abbreviationRepository);

        assertEquals(List.of(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, jStyleFile.toString(), jStyleFile.toString()),
                loader.getStyles().stream().map(JStyle::getPath).toList());
        verify(preferences).getExternalJStyles();
        verifyNoMoreInteractions(preferences);
    }
}
