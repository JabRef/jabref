package org.jabref.preferences;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilePreferenceTest {

    private FilePreferences filePreferences;
    private final String testUser = "test";
    private final String testMainFileDirectory = "test";
    private final boolean shouldStoreFilesRelativeToBibFile = false;
    private final String testFileNamePattern = "test";
    private final String testFileDirPattern = "test";
    private final boolean shouldDownloadLinkedFiles = false;
    private final boolean shouldSearchFilesOnOpen = false;
    private final boolean shouldOpenBrowseOnCreate = false;

    @BeforeEach
    public void setup() {
        filePreferences = new FilePreferences(testUser, testMainFileDirectory, shouldStoreFilesRelativeToBibFile,
                testFileNamePattern, testFileDirPattern, shouldDownloadLinkedFiles, shouldSearchFilesOnOpen,
                shouldOpenBrowseOnCreate);
    }

    @Test
    public void getUserTest() {
        assertEquals(testUser, filePreferences.getUser());
    }

    @Test
    public void getFileDirectoryNonEmptyTest() {
        assertEquals(Optional.of(Path.of(testMainFileDirectory)), filePreferences.getFileDirectory());
    }

    @Test
    public void getFileDirectoryEmptyTest() {
        filePreferences = new FilePreferences(testUser, "", shouldStoreFilesRelativeToBibFile,
                testFileNamePattern, testFileDirPattern, shouldDownloadLinkedFiles, shouldSearchFilesOnOpen,
                shouldOpenBrowseOnCreate);

        assertEquals(Optional.empty(), filePreferences.getFileDirectory());
    }

    @Test
    public void shouldStoreFilesRelativeToBibTest() {
        assertEquals(shouldStoreFilesRelativeToBibFile, filePreferences.shouldStoreFilesRelativeToBib());
    }

    @Test
    public void getFileNamePatternTest() {
        assertEquals(testFileNamePattern, filePreferences.getFileNamePattern());
    }

    @Test
    public void getFileDirectoryPatternTest() {
        assertEquals(testFileDirPattern, filePreferences.getFileDirectoryPattern());
    }

    @Test
    public void shouldDownloadLinkedFilesTest() {
        assertEquals(shouldDownloadLinkedFiles, filePreferences.shouldDownloadLinkedFiles());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void withShouldDownloadLinkedFilesTest(boolean newShouldDownloadLinkedFiles) {
        FilePreferences expected = new FilePreferences(testUser, testMainFileDirectory, shouldStoreFilesRelativeToBibFile,
                testFileNamePattern, testFileDirPattern, newShouldDownloadLinkedFiles, shouldSearchFilesOnOpen,
                shouldOpenBrowseOnCreate);

        assertEquals(expected.shouldDownloadLinkedFiles(),
                filePreferences.withShouldDownloadLinkedFiles(newShouldDownloadLinkedFiles).shouldDownloadLinkedFiles());
    }

    @Test
    public void shouldSearchFilesOnOpenTest() {
        assertEquals(shouldSearchFilesOnOpen, filePreferences.shouldSearchFilesOnOpen());
    }

    @Test
    public void shouldOpenBrowseOnCreateTest() {
        assertEquals(shouldOpenBrowseOnCreate, filePreferences.shouldOpenBrowseOnCreate());
    }
}
