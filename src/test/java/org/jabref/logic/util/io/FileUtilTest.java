package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(TempDirectory.class)
class FileUtilTest {
    private final Path nonExistingTestPath = Paths.get("nonExistingTestPath");
    private Path existingTestFile;
    private Path otherExistingTestFile;
    private LayoutFormatterPreferences layoutFormatterPreferences;
    private Path rootDir;

    @BeforeEach
    void setUpViewModel(@TempDirectory.TempDir Path temporaryFolder) throws IOException {
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
        assertEquals(Paths.get("demo.bib.bak"),
                FileUtil.addExtension(Paths.get("demo.bib"), ".bak"));
    }

    @Test
    void extensionBakAddedCorrectlyToAFileContainedInTmpDirectory() {
        assertEquals(Paths.get("tmp", "demo.bib.bak"),
                FileUtil.addExtension(Paths.get("tmp", "demo.bib"), ".bak"));
    }

    @Test
    void testGetLinkedFileNameDefaultFullTitle() {
        // bibkey - title
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameDefaultWithLowercaseTitle() {
        // bibkey - title
        String fileNamePattern = "[bibtexkey] - [title:lower]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameBibTeXKey() {
        // bibkey
        String fileNamePattern = "[bibtexkey]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameNoPattern() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetDefaultFileNameNoPatternNoBibTeXKey() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setField("title", "mytitle");

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameGetKeyIfEmptyField() {
        // bibkey - title
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameGetDefaultIfEmptyFieldNoKey() {
        // bibkey - title
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetLinkedFileNameByYearAuthorFirstpage() {
        // bibkey - title
        String fileNamePattern = "[year]_[auth]_[firstpage]";
        BibEntry entry = new BibEntry();
        entry.setField( "author", "O. Kitsune" );
        entry.setField( "year", "1868" );
        entry.setField( "pages", "567-579" );

        assertEquals("1868_Kitsune_567", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    void testGetFileExtensionSimpleFile() {
        assertEquals("pdf", FileHelper.getFileExtension(Paths.get("test.pdf")).get());
    }

    @Test
    void testGetFileExtensionMultipleDotsFile() {
        assertEquals("pdf", FileHelper.getFileExtension(Paths.get("te.st.PdF")).get());
    }

    @Test
    void testGetFileExtensionNoExtensionFile() {
        assertFalse(FileHelper.getFileExtension(Paths.get("JustTextNotASingleDot")).isPresent());
    }

    @Test
    void testGetFileExtensionNoExtension2File() {
        assertFalse(FileHelper.getFileExtension(Paths.get(".StartsWithADotIsNotAnExtension")).isPresent());
    }

    @Test
    void getFileExtensionWithSimpleString() {
        assertEquals("pdf", FileHelper.getFileExtension("test.pdf").get());
    }

    @Test
    void getFileExtensionTrimsAndReturnsInLowercase() {
        assertEquals("pdf", FileHelper.getFileExtension("test.PdF  ").get());
    }

    @Test
    void getFileExtensionWithMultipleDotsString() {
        assertEquals("pdf", FileHelper.getFileExtension("te.st.PdF  ").get());
    }

    @Test
    void getFileExtensionWithNoDotReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileHelper.getFileExtension("JustTextNotASingleDot"));
    }

    @Test
    void getFileExtensionWithDotAtStartReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileHelper.getFileExtension(".StartsWithADotIsNotAnExtension"));
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
        String[] pathArr = {Paths.get("C:/uniquefile.bib").toString(),
                Paths.get("C:/downloads/filename.bib").toString(), Paths.get("C:/mypaper/bib/filename.bib").toString(),
                Paths.get("C:/external/mypaper/bib/filename.bib").toString(), ""};
        String[] uniqArr = {Paths.get("uniquefile.bib").toString(), Paths.get("downloads/filename.bib").toString(),
                Paths.get("C:/mypaper/bib/filename.bib").toString(),
                Paths.get("external/mypaper/bib/filename.bib").toString(), ""};
        List<String> paths = Arrays.asList(pathArr);
        List<String> uniqPath = Arrays.asList(uniqArr);

        List<String> result = FileUtil.uniquePathSubstrings(paths);
        assertEquals(uniqPath, result);
    }

    @Test
    void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, true));
    }

    @Test
    void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithoutOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, false));
    }

    @Test
    void testCopyFileFromEmptySourcePathToExistDestinationPathWithOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, true));
    }

    @Test
    void testCopyFileFromEmptySourcePathToExistDestinationPathWithoutOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, false));
    }

    @Test
    void testCopyFileFromExistSourcePathToExistDestinationPathWithOverrideExistFile(){
        assertTrue(FileUtil.copyFile(existingTestFile, existingTestFile, true));
    }

    @Test
    void testCopyFileFromExistSourcePathToExistDestinationPathWithoutOverrideExistFile(){
        assertFalse(FileUtil.copyFile(existingTestFile, existingTestFile, false));
    }

    @Test
    void testCopyFileFromExistSourcePathToOtherExistDestinationPathWithOverrideExistFile(){
        assertTrue(FileUtil.copyFile(existingTestFile, otherExistingTestFile, true));
    }

    @Test
    void testCopyFileFromExistSourcePathToOtherExistDestinationPathWithoutOverrideExistFile(){
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
    void testRenameFileWithFromFileIsNullAndToFileIsNull() {
        assertThrows(NullPointerException.class, () -> FileUtil.renameFile(null, null));
    }

    @Test
    void testRenameFileWithFromFileExistAndToFileIsNull() {
        assertThrows(NullPointerException.class, () -> FileUtil.renameFile(existingTestFile, null));
    }

    @Test
    void testRenameFileWithFromFileIsNullAndToFileExist() {
        assertThrows(NullPointerException.class, () -> FileUtil.renameFile(null, existingTestFile));
    }

    @Test
    void testRenameFileWithFromFileNotExistAndToFileNotExist(){
        assertFalse(FileUtil.renameFile(nonExistingTestPath, nonExistingTestPath));
    }

    @Test
    void testRenameFileWithFromFileNotExistAndToFileExist(){
        assertFalse(FileUtil.renameFile(nonExistingTestPath, existingTestFile));
    }

    @Test
    void testRenameFileWithFromFileExistAndToFileNotExist(){
        assertTrue(FileUtil.renameFile(existingTestFile, nonExistingTestPath));
    }

    @Test
    void testRenameFileWithFromFileExistAndToFileExist(){
        assertTrue(FileUtil.renameFile(existingTestFile, existingTestFile));
    }

    @Test
    void testRenameFileWithFromFileExistAndOtherToFileExist(){
        assertFalse(FileUtil.renameFile(existingTestFile, otherExistingTestFile));
    }

    @Test
    void testRenameFileSuccessful(@TempDirectory.TempDir Path otherTemporaryFolder) {
        // Be careful. This "otherTemporaryFolder" is the same as the "temporaryFolder"
        // in the @BeforeEach method.
        Path temp = Paths.get(otherTemporaryFolder.resolve("123").toString());

        System.out.println(temp);
        FileUtil.renameFile(existingTestFile, temp);
        assertFalse(Files.exists(existingTestFile));
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
        String longestValidFilename = Stream.generate(() -> String.valueOf('1')).limit(FileUtil.MAXIMUM_FILE_NAME_LENGTH).collect(Collectors.joining()) + ".pdf";
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
        // bibkey - title
        String fileNamePattern = "PDF/[year]/[auth]/[bibtexkey] - [fulltitle]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");
        entry.setField("year", "1998");
        entry.setField("author", "A. Åuthör and Author, Bete");

        assertEquals("PDF/1998/Åuthör/1234 - mytitle",
                FileUtil.createDirNameFromPattern(null, entry, fileNamePattern));
    }
    @Test
    public void testIsBibFile() throws IOException
    {
        Path bibFile = Files.createFile(rootDir.resolve("test.bib"));

        assertTrue(FileUtil.isBibFile(bibFile));
    }

    @Test
    public void testIsNotBibFile() throws IOException {
        Path bibFile = Files.createFile(rootDir.resolve("test.pdf"));
        assertFalse(FileUtil.isBibFile(bibFile));
    }
}
