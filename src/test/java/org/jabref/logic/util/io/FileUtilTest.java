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

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseLogic("uses OS from logic package")
class FileUtilTest {
    private final Path nonExistingTestPath = Path.of("nonExistingTestPath");
    private Path existingTestFile;
    private Path otherExistingTestFile;
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
        Optional<Path> resultPath1 = FileUtil.findSingleFileRecursively("existingTestFile.txt", rootDir);
        assertEquals(resultPath1.get().toString(), existingTestFile.toString());
    }

    @Test
    void testFindInListOfPath() {
        // due to the added workaround for old JabRef behavior as both path starts with the same name they are considered equal
        List<Path> paths = List.of(existingTestFile, otherExistingTestFile, rootDir);
        List<Path> resultPaths = List.of(existingTestFile);
        List<Path> result = FileUtil.findListOfFiles("existingTestFile.txt", paths);
        assertEquals(resultPaths, result);
    }

    @Test
    public void extractFileExtension() {
        final String filePath = FileUtilTest.class.getResource("pdffile.pdf").getPath();
        assertEquals(Optional.of("pdf"), FileUtil.getFileExtension(filePath));
    }

    @Test
    public void fileExtensionFromUrl() {
        final String filePath = "https://link.springer.com/content/pdf/10.1007%2Fs40955-018-0121-9.pdf";
        assertEquals(Optional.of("pdf"), FileUtil.getFileExtension(filePath));
    }

    @Test
    public void testFileNameEmpty() {
        Path path = Path.of("/");
        assertEquals(Optional.of(path), FileUtil.find("", path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "?", ">", "\""})
    public void testFileNameIllegal(String fileName) {
        Path path = Path.of("/");
        assertEquals(Optional.empty(), FileUtil.find(fileName, path));
    }

    @Test
    public void testFindsFileInDirectory(@TempDir Path temp) throws Exception {
        Path firstFilePath = temp.resolve("files");
        Files.createDirectories(firstFilePath);
        Path firstFile = Files.createFile(firstFilePath.resolve("test.pdf"));

        assertEquals(Optional.of(firstFile), FileUtil.find("test.pdf", temp.resolve("files")));
    }

    @Test
    public void testFindsFileStartingWithTheSameDirectory(@TempDir Path temp) throws Exception {
        Path firstFilePath = temp.resolve("files");
        Files.createDirectories(firstFilePath);
        Path firstFile = Files.createFile(firstFilePath.resolve("test.pdf"));

        assertEquals(Optional.of(firstFile), FileUtil.find("files/test.pdf", temp.resolve("files")));
    }

    @Test
    public void testDoesNotFindsFileStartingWithTheSameDirectoryHasASubdirectory(@TempDir Path temp) throws Exception {
        Path firstFilesPath = temp.resolve("files");
        Path secondFilesPath = firstFilesPath.resolve("files");
        Files.createDirectories(secondFilesPath);
        Path testFile = secondFilesPath.resolve("test.pdf");
        Files.createFile(testFile);
        assertEquals(Optional.of(testFile.toAbsolutePath()), FileUtil.find("files/test.pdf", firstFilesPath));
    }

    public void testCTemp() {
        String fileName = "c:\\temp.pdf";
        if (OS.WINDOWS) {
            assertFalse(FileUtil.detectBadFileName(fileName));
        } else {
            assertTrue(FileUtil.detectBadFileName(fileName));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mnt/tmp/test.pdf"})
    public void legalPaths(String fileName) {
        assertFalse(FileUtil.detectBadFileName(fileName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"te{}mp.pdf"})
    public void illegalPaths(String fileName) {
        assertTrue(FileUtil.detectBadFileName(fileName));
    }
}
