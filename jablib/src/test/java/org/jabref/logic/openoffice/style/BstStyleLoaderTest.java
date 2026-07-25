package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BstStyleLoaderTest {

    private static final int NUMBER_OF_INTERNAL_STYLES = 3;

    private BstStyleLoader loader;
    private OpenOfficePreferences preferences;

    @TempDir
    private Path styleFolder;

    @BeforeEach
    void setUp() {
        preferences = mock(OpenOfficePreferences.class, Answers.RETURNS_DEEP_STUBS);
        preferences.setExternalBstStyles(List.of());
        loader = new BstStyleLoader(preferences);
    }

    private Path createBstFile(Path directory, String filename) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(filename);
        Files.writeString(file, "% dummy bst file for tests");
        return file;
    }

    @Test
    void getStylesWithEmptyExternalContainsOnlyInternalStyles() {
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }

    @Test
    void addValidStyleLeadsToOneMoreStyle() throws IOException {
        Path bstFile = createBstFile(styleFolder, "mystyle.bst");

        assertTrue(loader.addStyleIfValid(bstFile));
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
    }

    @Test
    void addNonBstFileIsRejected() throws IOException {
        Path notBst = createBstFile(styleFolder, "mystyle.txt");

        assertFalse(loader.addStyleIfValid(notBst));
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }

    @Test
    void addNonExistentFileIsRejected() {
        Path missing = styleFolder.resolve("doesNotExist.bst");

        assertFalse(loader.addStyleIfValid(missing));
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }

    @Test
    void addSamePathTwiceLeadsToOneMoreStyle() throws IOException {
        Path bstFile = createBstFile(styleFolder, "mystyle.bst");

        assertTrue(loader.addStyleIfValid(bstFile));
        assertFalse(loader.addStyleIfValid(bstFile));
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
    }

    @Test
    void addingTwoDifferentFilesWithTheSameFilenameIsRejected() throws IOException {
        Path firstDir = styleFolder.resolve("first");
        Path secondDir = styleFolder.resolve("second");
        Path firstFile = createBstFile(firstDir, "mystyle.bst");
        Path secondFile = createBstFile(secondDir, "mystyle.bst");

        assertTrue(loader.addStyleIfValid(firstFile));
        assertFalse(loader.addStyleIfValid(secondFile));
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 1, loader.getStyles().size());
    }

    @Test
    void addingFilesWithDifferentFilenamesBothSucceed() throws IOException {
        Path firstFile = createBstFile(styleFolder, "mystyle.bst");
        Path secondFile = createBstFile(styleFolder, "otherstyle.bst");

        assertTrue(loader.addStyleIfValid(firstFile));
        assertTrue(loader.addStyleIfValid(secondFile));
        assertEquals(NUMBER_OF_INTERNAL_STYLES + 2, loader.getStyles().size());
    }

    @Test
    void removeExternalStyleLeadsToOneLessStyle() throws IOException {
        Path bstFile = createBstFile(styleFolder, "mystyle.bst");
        loader.addStyleIfValid(bstFile);
        int beforeRemoving = loader.getStyles().size();

        BstStyle toRemove = loader.getStyles().stream()
                                   .filter(style -> !style.isInternalStyle())
                                   .findFirst()
                                   .orElseThrow();

        assertTrue(loader.removeStyle(toRemove));
        assertEquals(beforeRemoving - 1, loader.getStyles().size());
    }

    @Test
    void removeInternalStyleReturnsFalseAndDoesNotRemove() {
        BstStyle internalStyle = loader.getStyles().stream()
                                        .filter(BstStyle::isInternalStyle)
                                        .findFirst()
                                        .orElseThrow();

        assertFalse(loader.removeStyle(internalStyle));
        assertEquals(NUMBER_OF_INTERNAL_STYLES, loader.getStyles().size());
    }
}
