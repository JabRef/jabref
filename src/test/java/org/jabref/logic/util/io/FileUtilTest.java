package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FileUtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilTest.class);

    private final Path nonExistingTestPath = Path.of("nonExistingTestPath");
    private Path existingTestFile;
    private Path otherExistingTestFile;
    private LayoutFormatterPreferences layoutFormatterPreferences;
    private Path rootDir;

    @BeforeEach
    void setUpViewModel(@TempDir Path temporaryFolder) throws IOException {
        rootDir = temporaryFolder;
        Path subDir = rootDir.resolve("1");
        Files.createDirectory(subDir);

        existingTestFile = subDir.resolve("existingTestFile.txt");
        Files.createFile(existingTestFile);
        Files.write(existingTestFile, "existingTestFile.txt".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

        otherExistingTestFile = subDir.resolve("otherExistingTestFile.txt");
        Files.createFile(otherExistingTestFile);
        Files.write(otherExistingTestFile, "otherExistingTestFile.txt".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        layoutFormatterPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    void extensionBakAddedCorrectly() {
        assertEquals(Path.of("demo.bib.bak"),
                FileUtil.addExtension(Path.of("demo.bib"), ".bak"));
    }

    @Test
    void extensionBakAddedCorrectlyToAFileContainedInTmpDirectory() {
        assertEquals(Path.of("tmp", "demo.bib.bak"),
                FileUtil.addExtension(Path.of("tmp", "demo.bib"), ".bak"));
    }

    @Test
    void testGetLinkedFileNameDefaultFullTitle() {
        String fileNamePattern = "[citationkey] - [fulltitle]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameDefaultWithLowercaseTitle() {
        String fileNamePattern = "[citationkey] - [title:lower]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameBibTeXKey() {
        String fileNamePattern = "[citationkey]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameNoPattern() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetDefaultFileNameNoPatternNoBibTeXKey() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameGetKeyIfEmptyField() {
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameGetDefaultIfEmptyFieldNoKey() {
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameByYearAuthorFirstpage() {
        String fileNamePattern = "[year]_[auth]_[firstpage]";
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "O. Kitsune");
        entry.setField(StandardField.YEAR, "1868");
        entry.setField(StandardField.PAGES, "567-579");

        assertEquals("1868_Kitsune_567", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetFileExtensionSimpleFile() {
        assertEquals("pdf", FileUtil.getFileExtension(Path.of("test.pdf")).get());
    }

    @Test
    void testGetFileExtensionMultipleDotsFile() {
        assertEquals("pdf", FileUtil.getFileExtension(Path.of("te.st.PdF")).get());
    }

    @Test
    void testGetFileExtensionNoExtensionFile() {
        assertFalse(FileUtil.getFileExtension(Path.of("JustTextNotASingleDot")).isPresent());
    }

    @Test
    void testGetFileExtensionNoExtension2File() {
        assertFalse(FileUtil.getFileExtension(Path.of(".StartsWithADotIsNotAnExtension")).isPresent());
    }

    @Test
    void getFileExtensionWithSimpleString() {
        assertEquals("pdf", FileUtil.getFileExtension("test.pdf").get());
    }

    @Test
    void getFileExtensionTrimsAndReturnsInLowercase() {
        assertEquals("pdf", FileUtil.getFileExtension("test.PdF  ").get());
    }

    @Test
    void getFileExtensionWithMultipleDotsString() {
        assertEquals("pdf", FileUtil.getFileExtension("te.st.PdF  ").get());
    }

    @Test
    void getFileExtensionWithNoDotReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileUtil.getFileExtension("JustTextNotASingleDot"));
    }

    @Test
    void getFileExtensionWithDotAtStartReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileUtil.getFileExtension(".StartsWithADotIsNotAnExtension"));
    }

    @Test
    void getFileNameWithSimpleString() {
        assertEquals("test", FileUtil.getBaseName("test.pdf"));
    }

    @Test
    void getFileNameWithMultipleDotsString() {
        assertEquals("te.st", FileUtil.getBaseName("te.st.PdF  "));
    }

    @Test
    void uniquePathSubstrings() {
       List<String> paths = List.of("C:/uniquefile.bib",
               "C:/downloads/filename.bib",
               "C:/mypaper/bib/filename.bib",
               "C:/external/mypaper/bib/filename.bib",
               "");
        List<String> uniqPath = List.of("uniquefile.bib",
              "downloads/filename.bib",
              "C:/mypaper/bib/filename.bib",
              "external/mypaper/bib/filename.bib",
              "");

        List<String> result = FileUtil.uniquePathSubstrings(paths);
        assertEquals(uniqPath, result);
    }

    @Test
    void testUniquePathFragmentWithSameSuffix() {
        List<String> dirs = List.of("/users/jabref/bibliography.bib", "/users/jabref/koppor-bibliograsphy.bib");
        assertEquals(Optional.of("bibliography.bib"), FileUtil.getUniquePathFragment(dirs, Path.of("/users/jabref/bibliography.bib")));
    }

    @Test
    void testUniquePathFragmentWithSameSuffixAndLongerName() {
        List<String> dirs = List.of("/users/jabref/bibliography.bib", "/users/jabref/koppor-bibliography.bib");
        assertEquals(Optional.of("koppor-bibliography.bib"), FileUtil.getUniquePathFragment(dirs, Path.of("/users/jabref/koppor-bibliography.bib")));
    }

    @Test
    void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, true));
    }

    @Test
    void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, false));
    }

    @Test
    void testCopyFileFromEmptySourcePathToExistDestinationPathWithOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, true));
    }

    @Test
    void testCopyFileFromEmptySourcePathToExistDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, false));
    }

    @Test
    void testCopyFileFromExistSourcePathToExistDestinationPathWithOverrideExistFile() {
        assertTrue(FileUtil.copyFile(existingTestFile, existingTestFile, true));
    }

    @Test
    void testCopyFileFromExistSourcePathToExistDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(existingTestFile, existingTestFile, false));
    }

    @Test
    void testCopyFileFromExistSourcePathToOtherExistDestinationPathWithOverrideExistFile() {
        assertTrue(FileUtil.copyFile(existingTestFile, otherExistingTestFile, true));
    }

    @Test
    void testCopyFileFromExistSourcePathToOtherExistDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(existingTestFile, otherExistingTestFile, false));
    }

    @Test
    void testCopyFileSuccessfulWithOverrideExistFile() throws IOException {
        Path subDir = rootDir.resolve("2");
        Files.createDirectory(subDir);
        Path temp = subDir.resolve("existingTestFile.txt");
        Files.createFile(temp);
        FileUtil.copyFile(existingTestFile, temp, true);
        assertEquals(Files.readAllLines(existingTestFile, StandardCharsets.UTF_8), Files.readAllLines(temp, StandardCharsets.UTF_8));
    }

    @Test
    void testCopyFileSuccessfulWithoutOverrideExistFile() throws IOException {
        Path subDir = rootDir.resolve("2");
        Files.createDirectory(subDir);
        Path temp = subDir.resolve("existingTestFile.txt");
        Files.createFile(temp);
        FileUtil.copyFile(existingTestFile, temp, false);
        assertNotEquals(Files.readAllLines(existingTestFile, StandardCharsets.UTF_8), Files.readAllLines(temp, StandardCharsets.UTF_8));
    }

    @Test
    void validFilenameShouldNotAlterValidFilename() {
        assertEquals("somename.pdf", FileUtil.getValidFileName("somename.pdf"));
    }

    @Test
    void validFilenameWithoutExtension() {
        assertEquals("somename", FileUtil.getValidFileName("somename"));
    }

    @Test
    void validFilenameShouldBeMaximum255Chars() {
        String longestValidFilename = Stream.generate(() -> String.valueOf('1')).limit(FileUtil.MAXIMUM_FILE_NAME_LENGTH - 4).collect(Collectors.joining()) + ".pdf";
        String longerFilename = Stream.generate(() -> String.valueOf('1')).limit(260).collect(Collectors.joining()) + ".pdf";
        assertEquals(longestValidFilename, FileUtil.getValidFileName(longerFilename));
    }

    @Test
    void invalidFilenameWithoutExtension() {
        String longestValidFilename = Stream.generate(() -> String.valueOf('1')).limit(FileUtil.MAXIMUM_FILE_NAME_LENGTH).collect(Collectors.joining());
        String longerFilename = Stream.generate(() -> String.valueOf('1')).limit(260).collect(Collectors.joining());
        assertEquals(longestValidFilename, FileUtil.getValidFileName(longerFilename));
    }

    @Test
    void testGetLinkedDirNameDefaultFullTitle() {
        String fileDirPattern = "PDF/[year]/[auth]/[citationkey] - [fulltitle]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");
        entry.setField(StandardField.YEAR, "1998");
        entry.setField(StandardField.AUTHOR, "A. Åuthör and Author, Bete");

        assertEquals("PDF/1998/Åuthör/1234 - mytitle", FileUtil.createDirNameFromPattern(null, entry, fileDirPattern));
    }

    @Test
    void testGetLinkedDirNamePatternEmpty() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");
        entry.setField(StandardField.YEAR, "1998");
        entry.setField(StandardField.AUTHOR, "A. Åuthör and Author, Bete");

        assertEquals("", FileUtil.createDirNameFromPattern(null, entry, ""));
    }

    @Test
    void testIsBibFile() throws IOException {
        Path bibFile = Files.createFile(rootDir.resolve("test.bib"));

        assertTrue(FileUtil.isBibFile(bibFile));
    }

    @Test
    void testIsNotBibFile() throws IOException {
        Path bibFile = Files.createFile(rootDir.resolve("test.pdf"));
        assertFalse(FileUtil.isBibFile(bibFile));
    }

    @Test
    void testFindinPath() {
        Optional<Path> resultPath1 = FileUtil.find("existingTestFile.txt", rootDir);
        assertEquals(resultPath1.get().toString(), existingTestFile.toString());
    }

    @Test
    void testFindInListOfPath() {
        List<Path> paths = List.of(existingTestFile, otherExistingTestFile, rootDir);
        List<Path> resultPaths = List.of(existingTestFile, existingTestFile);
        List<Path> result = FileUtil.findListOfFiles("existingTestFile.txt", paths);
        assertEquals(resultPaths, result);
    }
}
