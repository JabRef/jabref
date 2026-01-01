package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.os.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AllowedToUseLogic("uses OS from logic package")
class FileUtilTest {

    @TempDir
    private static Path bibTempDir;

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
        Files.writeString(existingTestFile, "existingTestFile.txt");

        otherExistingTestFile = subDir.resolve("otherExistingTestFile.txt");
        Files.createFile(otherExistingTestFile);
        Files.writeString(otherExistingTestFile, "otherExistingTestFile.txt");
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
    void getLinkedFileNameDefaultFullTitle() {
        String fileNamePattern = "[citationkey] - [fulltitle]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern).get());
    }

    @Test
    void getLinkedFileNameDefaultWithLowercaseTitle() {
        String fileNamePattern = "[citationkey] - [title:lower]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern).get());
    }

    @Test
    void getLinkedFileNameBibTeXKey() {
        String fileNamePattern = "[citationkey]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern).get());
    }

    @Test
    void getLinkedFileNameNoPattern() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern).get());
    }

    @Test
    void getDefaultFileNameNoPatternNoBibTeXKey() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "mytitle");

        assertEquals(Optional.empty(), FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void getLinkedFileNameGetKeyIfEmptyField() {
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern).get());
    }

    @Test
    void getLinkedFileNameGetDefaultIfEmptyFieldNoKey() {
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();

        assertEquals(Optional.empty(), FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void getLinkedFileNameGetOptionalEmptyIfDashAsPattern() {
        String fileNamePattern = "-";
        BibEntry entry = new BibEntry();

        assertEquals(Optional.empty(), FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void getLinkedFileNameGetOptionalEmptyIfDefaultAsPattern() {
        String fileNamePattern = "default";
        BibEntry entry = new BibEntry();

        assertEquals(Optional.empty(), FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void getLinkedFileNameByYearAuthorFirstpage() {
        String fileNamePattern = "[year]_[auth]_[firstpage]";
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "O. Kitsune");
        entry.setField(StandardField.YEAR, "1868");
        entry.setField(StandardField.PAGES, "567-579");

        assertEquals("1868_Kitsune_567", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern).get());
    }

    @Test
    void getLinkedFileNameRemovesLatexCommands() {
        String pattern = "[citationkey] - [fulltitle]";
        BibEntry entry = new BibEntry()
                .withCitationKey("BrayBuildingCommunity")
                .withField(StandardField.TITLE, "Building \\mkbibquote{Community}");
        String expected = "BrayBuildingCommunity - Building Community";
        Optional<String> result = FileUtil.createFileNameFromPattern(null, entry, pattern);
        assertEquals(expected, result.get());
    }

    @Test
    void getFileExtensionSimpleFile() {
        assertEquals("pdf", FileUtil.getFileExtension(Path.of("test.pdf")).get());
    }

    @Test
    void getFileExtensionMultipleDotsFile() {
        assertEquals("pdf", FileUtil.getFileExtension(Path.of("te.st.PdF")).get());
    }

    @Test
    void getFileExtensionNoExtensionFile() {
        assertFalse(FileUtil.getFileExtension(Path.of("JustTextNotASingleDot")).isPresent());
    }

    @Test
    void getFileExtensionNoExtension2File() {
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
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Assumed path separator is /")
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
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Assumed path separator is /")
    void uniquePathFragmentWithSameSuffix() {
        List<String> dirs = List.of("/users/jabref/bibliography.bib", "/users/jabref/koppor-bibliograsphy.bib");
        assertEquals(Optional.of("bibliography.bib"), FileUtil.getUniquePathFragment(dirs, Path.of("/users/jabref/bibliography.bib")));
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Assumed path separator is /")
    void uniquePathFragmentWithSameSuffixAndLongerName() {
        List<String> paths = List.of("/users/jabref/bibliography.bib", "/users/jabref/koppor-bibliography.bib");
        assertEquals(Optional.of("koppor-bibliography.bib"), FileUtil.getUniquePathFragment(paths, Path.of("/users/jabref/koppor-bibliography.bib")));
    }

    @Test
    void copyFileFromEmptySourcePathToEmptyDestinationPathWithOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, true));
    }

    @Test
    void copyFileFromEmptySourcePathToEmptyDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, false));
    }

    @Test
    void copyFileFromEmptySourcePathToExistDestinationPathWithOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, true));
    }

    @Test
    void copyFileFromEmptySourcePathToExistDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, false));
    }

    @Test
    void copyFileFromExistSourcePathToExistDestinationPathWithOverrideExistFile() {
        assertTrue(FileUtil.copyFile(existingTestFile, existingTestFile, true));
    }

    @Test
    void copyFileFromExistSourcePathToExistDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(existingTestFile, existingTestFile, false));
    }

    @Test
    void copyFileFromExistSourcePathToOtherExistDestinationPathWithOverrideExistFile() {
        assertTrue(FileUtil.copyFile(existingTestFile, otherExistingTestFile, true));
    }

    @Test
    void copyFileFromExistSourcePathToOtherExistDestinationPathWithoutOverrideExistFile() {
        assertFalse(FileUtil.copyFile(existingTestFile, otherExistingTestFile, false));
    }

    @Test
    void copyFileSuccessfulWithOverrideExistFile() throws IOException {
        Path subDir = rootDir.resolve("2");
        Files.createDirectory(subDir);
        Path temp = subDir.resolve("existingTestFile.txt");
        Files.createFile(temp);
        FileUtil.copyFile(existingTestFile, temp, true);
        assertEquals(Files.readAllLines(existingTestFile, StandardCharsets.UTF_8), Files.readAllLines(temp, StandardCharsets.UTF_8));
    }

    @Test
    void copyFileSuccessfulWithoutOverrideExistFile() throws IOException {
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
    void getLinkedDirNameDefaultFullTitle() {
        String fileDirPattern = "PDF/[year]/[auth]/[citationkey] - [fulltitle]";
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");
        entry.setField(StandardField.YEAR, "1998");
        entry.setField(StandardField.AUTHOR, "A. Åuthör and Author, Bete");

        assertEquals("PDF/1998/Åuthör/1234 - mytitle", FileUtil.createDirNameFromPattern(null, entry, fileDirPattern));
    }

    @Test
    void getLinkedDirNamePatternEmpty() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("1234");
        entry.setField(StandardField.TITLE, "mytitle");
        entry.setField(StandardField.YEAR, "1998");
        entry.setField(StandardField.AUTHOR, "A. Åuthör and Author, Bete");

        assertEquals("", FileUtil.createDirNameFromPattern(null, entry, ""));
    }

    @Test
    void isBibFile() throws IOException {
        Path bibFile = Files.createFile(rootDir.resolve("test.bib"));
        assertTrue(FileUtil.isBibFile(bibFile));
    }

    @Test
    void isNotBibFile() throws IOException {
        Path bibFile = Files.createFile(rootDir.resolve("test.pdf"));
        assertFalse(FileUtil.isBibFile(bibFile));
    }

    @Test
    void findInPath() {
        Optional<Path> resultPath1 = FileUtil.findSingleFileRecursively("existingTestFile.txt", rootDir);
        assertEquals(resultPath1.get().toString(), existingTestFile.toString());
    }

    @Test
    void findInListOfPath() {
        // due to the added workaround for old JabRef behavior as both path starts with the same name they are considered equal
        List<Path> paths = List.of(existingTestFile, otherExistingTestFile, rootDir);
        List<Path> resultPaths = List.of(existingTestFile);
        List<Path> result = FileUtil.findListOfFiles("existingTestFile.txt", paths);
        assertEquals(resultPaths, result);
    }

    @Test
    void extractFileExtension() {
        final String filePath = FileUtilTest.class.getResource("pdffile.pdf").getPath();
        assertEquals(Optional.of("pdf"), FileUtil.getFileExtension(filePath));
    }

    @Test
    void fileExtensionFromUrl() {
        final String filePath = "https://link.springer.com/content/pdf/10.1007%2Fs40955-018-0121-9.pdf";
        assertEquals(Optional.of("pdf"), FileUtil.getFileExtension(filePath));
    }

    @Test
    void fileNameEmpty() {
        Path path = Path.of("/");
        assertEquals(Optional.of(path), FileUtil.find("", path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "?", ">", "\""})
    void fileNameIllegal(String fileName) {
        Path path = Path.of("/");
        assertEquals(Optional.empty(), FileUtil.find(fileName, path));
    }

    @Test
    void findsFileInDirectory(@TempDir Path temp) throws IOException {
        Path firstFilePath = temp.resolve("files");
        Files.createDirectories(firstFilePath);
        Path firstFile = Files.createFile(firstFilePath.resolve("test.pdf"));

        assertEquals(Optional.of(firstFile), FileUtil.find("test.pdf", temp.resolve("files")));
    }

    @Test
    void findsFileStartingWithTheSameDirectory(@TempDir Path temp) throws IOException {
        Path firstFilePath = temp.resolve("files");
        Files.createDirectories(firstFilePath);
        Path firstFile = Files.createFile(firstFilePath.resolve("test.pdf"));

        assertEquals(Optional.of(firstFile), FileUtil.find("files/test.pdf", temp.resolve("files")));
    }

    @Test
    void doesNotFindsFileStartingWithTheSameDirectoryHasASubdirectory(@TempDir Path temp) throws IOException {
        Path firstFilesPath = temp.resolve("files");
        Path secondFilesPath = firstFilesPath.resolve("files");
        Files.createDirectories(secondFilesPath);
        Path testFile = secondFilesPath.resolve("test.pdf");
        Files.createFile(testFile);
        assertEquals(Optional.of(testFile.toAbsolutePath()), FileUtil.find("files/test.pdf", firstFilesPath));
    }

    @Test
    public void cTemp() {
        String fileName = "c:\\temp.pdf";
        if (OS.WINDOWS) {
            assertFalse(FileUtil.detectBadFileName(fileName));
        } else {
            assertTrue(FileUtil.detectBadFileName(fileName));
        }
    }

    /// Tests for issue <https://github.com/JabRef/jabref/issues/12995>
    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Symlink behavior unreliable on windows")
    void simpleRelativizeSymlinks() throws IOException {
        Path realDir = bibTempDir.resolve("realDir_" + UUID.randomUUID());
        Files.createDirectories(realDir);

        Path simpleFile = Files.createFile(realDir.resolve("simple.pdf"));
        Path symlinkDir = bibTempDir.resolve("symlinkDir_" + UUID.randomUUID());
        Files.createSymbolicLink(symlinkDir, realDir);

        Path result = FileUtil.relativize(simpleFile, List.of(symlinkDir));
        assertEquals(Path.of("simple.pdf"), result, "Simple symlink resolves to relative");
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Symlink behavior unreliable on windows")
    void chainedRelativizeSymlinks() throws IOException {
        Path chainReal = bibTempDir.resolve("chainReal_" + UUID.randomUUID());
        Files.createDirectories(chainReal);

        Path chainedFile = Files.createFile(chainReal.resolve("chained.pdf"));
        Path chainLink2 = bibTempDir.resolve("chainLink2_" + UUID.randomUUID());
        Files.createSymbolicLink(chainLink2, chainReal);
        Path chainLink1 = bibTempDir.resolve("chainLink1_" + UUID.randomUUID());
        Files.createSymbolicLink(chainLink1, chainLink2);

        Path result = FileUtil.relativize(chainedFile, List.of(chainLink1));
        assertEquals(Path.of("chained.pdf"), result, "Chained symlink resolves to relative");
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Symlink behavior unreliable on windows")
    void nestedRelativizeSymlinks() throws IOException {
        Path realDir = bibTempDir.resolve("realDir_" + UUID.randomUUID());
        Files.createDirectories(realDir);

        Path nestedDir = realDir.resolve("nested_" + UUID.randomUUID());
        Files.createDirectories(nestedDir);
        Path nestedFile = Files.createFile(nestedDir.resolve("nested.pdf"));
        Path nestedSymlink = realDir.resolve("nestedLink_" + UUID.randomUUID());
        Files.createSymbolicLink(nestedSymlink, nestedDir);

        Path result = FileUtil.relativize(nestedFile, List.of(nestedSymlink));
        assertEquals(Path.of("nested.pdf"), result, "Nested symlink resolves to relative");
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Symlink behavior unreliable on windows")
    void unrelatedFileRemainsAbsolute() throws IOException {
        Path realDir = bibTempDir.resolve("realDir_" + UUID.randomUUID());
        Files.createDirectories(realDir);
        Path symlinkDir = bibTempDir.resolve("symlinkDir_" + UUID.randomUUID());
        Files.createSymbolicLink(symlinkDir, realDir);

        Path outsideFile = Files.createFile(bibTempDir.resolve("outside.pdf"));

        Path result = FileUtil.relativize(outsideFile, List.of(symlinkDir));
        assertEquals(outsideFile, result, "Unrelated file remains absolute");
    }

    @Test
    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Symlink behavior unreliable on windows")
    void symlinkEscapeCaseIgnored() throws IOException {
        Path veryPrivate = bibTempDir.resolve("veryprivate");
        Files.createDirectories(veryPrivate);
        Path secretFile = Files.createFile(veryPrivate.resolve("a.pdf"));

        Path expensive = bibTempDir.resolve("expensive");
        Files.createSymbolicLink(expensive, veryPrivate);
        Path things = bibTempDir.resolve("things");
        Files.createSymbolicLink(things, expensive);

        Path libDir = bibTempDir.resolve("lib");
        Files.createDirectories(libDir);
        Files.createFile(libDir.resolve("bib.bib"));

        org.junit.jupiter.api.Assumptions.assumeTrue(false, "IGNORED: Symlink chain escaping base dir, see <https://github.com/JabRef/jabref/issues/12995#issuecomment-3065149862>");
        Path result = FileUtil.relativize(secretFile, List.of(things));
        assertEquals(secretFile, result);
    }

    /**
     * @implNote Tests inspired by {@link org.jabref.model.database.BibDatabaseContextTest#getFileDirectoriesWithRelativeMetadata}
     */
    public static Stream<Arguments> relativize() {
        Path bibPath = bibTempDir.resolve("bibliography.bib");
        Path filesPath = bibTempDir.resolve("files").resolve("pdfs");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(bibPath);
        database.getMetaData().setLibrarySpecificFileDirectory(filesPath.toString());

        FilePreferences fileDirPrefs = mock(FilePreferences.class);
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(true);

        Path testPdf = filesPath.resolve("test.pdf");
        BibEntry source1 = new BibEntry().withFiles(List.of(new LinkedFile(testPdf)));
        BibEntry target1 = new BibEntry().withFiles(List.of(new LinkedFile(filesPath.relativize(testPdf))));

        testPdf = bibPath.resolve("test.pdf");
        BibEntry source2 = new BibEntry().withFiles(List.of(new LinkedFile(testPdf)));
        BibEntry target2 = new BibEntry().withFiles(List.of(new LinkedFile(bibTempDir.relativize(testPdf))));

        return Stream.of(
                Arguments.of(List.of(target1), List.of(source1), database, fileDirPrefs),
                Arguments.of(List.of(target2), List.of(source2), database, fileDirPrefs)
        );
    }

    @ParameterizedTest
    @MethodSource
    void relativize(List<BibEntry> expected, List<BibEntry> entries, BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        List<BibEntry> actual = FileUtil.relativize(entries, databaseContext, filePreferences);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mnt/tmp/test.pdf"})
    void legalPaths(String fileName) {
        assertFalse(FileUtil.detectBadFileName(fileName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"te{}mp.pdf"})
    void illegalPaths(String fileName) {
        assertTrue(FileUtil.detectBadFileName(fileName));
    }

    @ParameterizedTest
    @CsvSource({
            "''                             ,                                     ,   ",
            "''                             ,                                     , -3",
            "''                             ,                                     ,  0",
            "''                             ,                                     ,  3",
            "''                             ,                                     ,  5",
            "''                             ,                                     , 10",
            "''                             , thisisatestfile.pdf                 ,   ",
            "''                             , thisisatestfile.pdf                 , -5",
            "''                             , thisisatestfile.pdf                 ,  0",
            "...                            , thisisatestfile.pdf                 ,  3",
            "th...                          , thisisatestfile.pdf                 ,  5",
            "th...e.pdf                     , thisisatestfile.pdf                 , 10",
            "thisisatestfile.pdf            , thisisatestfile.pdf                 , 20",
            "lo...                          , longfilename.extremelylongextension ,  5",
            "longfil...                     , longfilename.extremelylongextension , 10",
            "longfilename.extr...           , longfilename.extremelylongextension , 20",
            "lo...me.extremelylongextension , longfilename.extremelylongextension , 30",
    })
    void shortenFileName(String expected, String fileName, Integer maxLength) {
        assertEquals(expected, FileUtil.shortenFileName(fileName, maxLength));
    }

    @EnabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS)
    @ParameterizedTest
    @ValueSource(strings = {"/c/Users/username/Downloads/test.bib",
            "/cygdrive/c/Users/username/Downloads/test.bib",
            "/mnt/c/Users/username/Downloads/test.bib"})
    void convertCygwinPathToWindowsShouldConvertToWindowsFormatWhenRunningOnWindows(String filePath) {
        assertEquals(Path.of("C:\\\\Users\\\\username\\\\Downloads\\\\test.bib"), FileUtil.convertCygwinPathToWindows(filePath));
    }

    @DisabledOnOs(value = org.junit.jupiter.api.condition.OS.WINDOWS, disabledReason = "Test in others operational systems")
    @ParameterizedTest
    @ValueSource(strings = {"/home/username/Downloads/test.bib"})
    void convertCygwinPathToWindowsShouldReturnOriginalFilePathWhenRunningOnWindows(String filePath) {
        assertEquals(Path.of(filePath), FileUtil.convertCygwinPathToWindows(filePath));
    }
}
