package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@NullMarked
class BstStyleLoaderTest {

    private static final List<String> INTERNAL_STYLE_PATHS = List.of(
            BstStyle.INTERNAL_IEEETRAN_PATH,
            BstStyle.INTERNAL_ABBRV_PATH,
            BstStyle.INTERNAL_APA_PATH
    );

    private OpenOfficePreferences preferences;
    private BstStyleLoader loader;

    @TempDir
    private Path styleFolder;

    @BeforeEach
    void setUp() {
        preferences = mock(OpenOfficePreferences.class);
        when(preferences.getExternalBstStyles()).thenReturn(FXCollections.observableArrayList());
        loader = new BstStyleLoader(preferences);
    }

    @Test
    void getStylesStartsWithBuiltinsWithoutWritingPreferences() {
        assertEquals(INTERNAL_STYLE_PATHS, loader.getStyles().stream().map(BstStyle::getPath).toList());
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void loadingExternalStylesPreservesDuplicatesAndBuiltinNameWithoutPersisting() throws IOException {
        BstStyle style = createExternalStyle("apa.bst");
        when(preferences.getExternalBstStyles()).thenReturn(FXCollections.observableArrayList(style.getPath(), style.getPath()));

        loader = new BstStyleLoader(preferences);

        List<String> expectedPaths = new ArrayList<>(INTERNAL_STYLE_PATHS);
        expectedPaths.add(style.getPath());
        expectedPaths.add(style.getPath());
        assertEquals(expectedPaths, loader.getStyles().stream().map(BstStyle::getPath).toList());
        verify(preferences, times(2)).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addExternalStyleRegistersParsedStyleAndPersistsOnce() throws IOException {
        BstStyle style = createExternalStyle("custom.bst");
        List<BstStyle> expectedStyles = new ArrayList<>(loader.getStyles());
        expectedStyles.add(style);
        Files.delete(styleFolder.resolve("custom.bst"));

        assertTrue(loader.addExternalStyle(style));

        assertEquals(expectedStyles, loader.getStyles());
        assertSame(style, loader.getStyles().getLast());
        verify(preferences).setExternalBstStyles(List.of(style.getPath()));
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addExternalStyleRejectsInternalStyleWithoutPersisting() {
        List<BstStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addExternalStyle(beforeAdding.getFirst()));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences, never()).setExternalBstStyles(anyList());
    }

    @Test
    void addExternalStyleRejectsEqualPathWithoutPersistingAgain() throws IOException {
        BstStyle style = createExternalStyle("custom.bst");
        BstStyle duplicate = BstStyle.loadExternal(styleFolder.resolve("custom.bst"));
        assertTrue(loader.addExternalStyle(style));
        List<BstStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addExternalStyle(duplicate));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences).setExternalBstStyles(List.of(style.getPath()));
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addExternalStyleRejectsSameNameAtDifferentPathWithUppercaseExtension() throws IOException {
        BstStyle style = createExternalStyle("custom.bst");
        Files.createDirectory(styleFolder.resolve("other"));
        BstStyle duplicate = createExternalStyle("other/custom.BST");
        assertTrue(loader.addExternalStyle(style));
        List<BstStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addExternalStyle(duplicate));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences).setExternalBstStyles(List.of(style.getPath()));
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IEEEtran.BST", "abbrv.bst", "apa.BsT"})
    void addExternalStyleRejectsBuiltinNameWithoutPersisting(String filename) throws IOException {
        BstStyle style = createExternalStyle(filename);
        List<BstStyle> beforeAdding = loader.getStyles();

        assertFalse(loader.addExternalStyle(style));

        assertEquals(beforeAdding, loader.getStyles());
        verify(preferences, never()).setExternalBstStyles(anyList());
    }

    @Test
    void addExternalStyleAcceptsDifferentCaseFromBuiltinName() throws IOException {
        BstStyle style = createExternalStyle("APA.bst");

        assertTrue(loader.addExternalStyle(style));

        assertSame(style, loader.getStyles().getLast());
        verify(preferences).setExternalBstStyles(List.of(style.getPath()));
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void addExternalStyleAcceptsDifferentCaseFromExternalName() throws IOException {
        BstStyle style = createExternalStyle("custom.bst");
        Files.createDirectory(styleFolder.resolve("other"));
        BstStyle otherStyle = createExternalStyle("other/CUSTOM.bst");

        assertTrue(loader.addExternalStyle(style));
        assertTrue(loader.addExternalStyle(otherStyle));

        assertEquals(List.of(style, otherStyle), loader.getStyles().subList(INTERNAL_STYLE_PATHS.size(), loader.getStyles().size()));
        verify(preferences).setExternalBstStyles(List.of(style.getPath()));
        verify(preferences).setExternalBstStyles(List.of(style.getPath(), otherStyle.getPath()));
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void removeEqualStylePersistsOnlyActualRemoval() throws IOException {
        List<BstStyle> internalStyles = loader.getStyles();
        BstStyle style = createExternalStyle("custom.bst");
        BstStyle equalStyle = BstStyle.loadExternal(styleFolder.resolve("custom.bst"));
        assertTrue(loader.addExternalStyle(style));

        assertTrue(loader.removeStyle(equalStyle));
        assertFalse(loader.removeStyle(equalStyle));

        assertEquals(internalStyles, loader.getStyles());
        verify(preferences).setExternalBstStyles(List.of(style.getPath()));
        verify(preferences).setExternalBstStyles(List.of());
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void removeInternalStyleDoesNotPersist() {
        List<BstStyle> beforeRemoving = loader.getStyles();

        assertFalse(loader.removeStyle(beforeRemoving.getFirst()));

        assertEquals(beforeRemoving, loader.getStyles());
        verify(preferences, never()).setExternalBstStyles(anyList());
    }

    @Test
    void removeUnregisteredStyleDoesNotPersist() throws IOException {
        BstStyle style = createExternalStyle("custom.bst");
        List<BstStyle> beforeRemoving = loader.getStyles();

        assertFalse(loader.removeStyle(style));

        assertEquals(beforeRemoving, loader.getStyles());
        verify(preferences, never()).setExternalBstStyles(anyList());
    }

    @Test
    void getStylesReturnsUnmodifiableList() {
        List<BstStyle> styles = loader.getStyles();

        assertThrows(UnsupportedOperationException.class, styles::clear);

        assertEquals(styles, loader.getStyles());
    }

    @Test
    void getStylesSnapshotDoesNotChangeAfterAddingStyle() throws IOException {
        List<BstStyle> styles = loader.getStyles();
        BstStyle style = createExternalStyle("custom.bst");

        assertTrue(loader.addExternalStyle(style));

        assertEquals(INTERNAL_STYLE_PATHS, styles.stream().map(BstStyle::getPath).toList());
        assertSame(style, loader.getStyles().getLast());
    }

    @Test
    void getStylesSnapshotDoesNotChangeAfterRemovingStyle() throws IOException {
        BstStyle style = createExternalStyle("custom.bst");
        assertTrue(loader.addExternalStyle(style));
        List<BstStyle> styles = loader.getStyles();

        assertTrue(loader.removeStyle(style));

        List<String> expectedPaths = new ArrayList<>(INTERNAL_STYLE_PATHS);
        expectedPaths.add(style.getPath());
        assertEquals(expectedPaths, styles.stream().map(BstStyle::getPath).toList());
        assertEquals(INTERNAL_STYLE_PATHS, loader.getStyles().stream().map(BstStyle::getPath).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"custom.bst", "UPPER.BST", "mixed.BsT", "apa.bst"})
    void loadExternalStyleParsesWithoutRegisteringOrPersisting(String filename) throws IOException {
        Path stylePath = Files.writeString(styleFolder.resolve(filename), "READ");
        List<BstStyle> beforeLoading = loader.getStyles();

        BstStyle style = loader.loadExternalStyle(stylePath).orElseThrow();

        assertEquals(stylePath.toString(), style.getPath());
        assertEquals("READ", style.getSource());
        assertFalse(style.isInternalStyle());
        assertEquals(beforeLoading, loader.getStyles());
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @ParameterizedTest
    @ValueSource(strings = {"customstyle", "style.txt", "style.bst.txt"})
    void loadExternalStyleRejectsWrongExtensionEvenWithValidContent(String filename) throws IOException {
        Path stylePath = Files.writeString(styleFolder.resolve(filename), "READ");
        assertEquals("READ", BstStyle.loadExternal(stylePath).getSource());
        List<BstStyle> beforeLoading = loader.getStyles();

        assertEquals(Optional.empty(), loader.loadExternalStyle(stylePath));

        assertEquals(beforeLoading, loader.getStyles());
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void loadExternalStyleRejectsInvalidContentWithoutPersisting() throws IOException {
        Path stylePath = Files.writeString(styleFolder.resolve("invalid.bst"), "}");
        List<BstStyle> beforeLoading = loader.getStyles();

        assertEquals(Optional.empty(), loader.loadExternalStyle(stylePath));

        assertEquals(beforeLoading, loader.getStyles());
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void loadExternalStyleRejectsMissingFileWithoutPersisting() {
        List<BstStyle> beforeLoading = loader.getStyles();

        assertEquals(Optional.empty(), loader.loadExternalStyle(styleFolder.resolve("missing.bst")));

        assertEquals(beforeLoading, loader.getStyles());
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    @Test
    void loadExternalStyleRejectsDirectoryWithoutPersisting() throws IOException {
        Path stylePath = Files.createDirectory(styleFolder.resolve("directory.bst"));
        List<BstStyle> beforeLoading = loader.getStyles();

        assertEquals(Optional.empty(), loader.loadExternalStyle(stylePath));

        assertEquals(beforeLoading, loader.getStyles());
        verify(preferences).getExternalBstStyles();
        verifyNoMoreInteractions(preferences);
    }

    private BstStyle createExternalStyle(String filename) throws IOException {
        Path stylePath = Files.writeString(styleFolder.resolve(filename), "READ");
        return BstStyle.loadExternal(stylePath);
    }
}
