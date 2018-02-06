package org.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileUtilTest {
    private final Path nonExistingTestPath = Paths.get("nonExistingTestPath");
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    public TemporaryFolder otherTemporaryFolder = new TemporaryFolder();
    private Path existingTestFile;
    private Path otherExistingTestFile;
    private LayoutFormatterPreferences layoutFormatterPreferences;

    @Before
    public void setUpViewModel() throws IOException {
        existingTestFile = createTemporaryTestFile("existingTestFile.txt");
        otherExistingTestFile = createTemporaryTestFile("otherExistingTestFile.txt");
        otherTemporaryFolder.create();
        layoutFormatterPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    public void extensionBakAddedCorrectly() {
        assertEquals(Paths.get("demo.bib.bak"),
                FileUtil.addExtension(Paths.get("demo.bib"), ".bak"));
    }

    @Test
    public void extensionBakAddedCorrectlyToAFileContainedInTmpDirectory() {
        assertEquals(Paths.get("tmp", "demo.bib.bak"),
                FileUtil.addExtension(Paths.get("tmp", "demo.bib"), ".bak"));
    }

    @Test
    public void testGetLinkedFileNameDefaultWithLayout() {
        // bibkey - title
        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern, layoutFormatterPreferences));
    }

    @Test
    public void testGetLinkedFileNameDefaultFullTitle() {
        // bibkey - title
        String fileNamePattern = "[bibtexkey] - [fulltitle]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetLinkedFileNameDefaultWithLowercaseTitle() {
        // bibkey - title
        String fileNamePattern = "[bibtexkey] - [title:lower]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetLinkedFileNameBibTeXKeyWithLayout() {
        // bibkey
        String fileNamePattern = "\\bibtexkey";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                        layoutFormatterPreferences));
    }

    @Test
    public void testGetLinkedFileNameBibTeXKey() {
        // bibkey
        String fileNamePattern = "[bibtexkey]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetLinkedFileNameNoPatternWithLayout() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                layoutFormatterPreferences));
    }

    @Test
    public void testGetLinkedFileNameNoPattern() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetDefaultFileNameNoPatternNoBibTeXKeyWithLayout() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setField("title", "mytitle");

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                layoutFormatterPreferences));
    }

    @Test
    public void testGetDefaultFileNameNoPatternNoBibTeXKey() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setField("title", "mytitle");

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetLinkedFileNameGetKeyIfEmptyFieldWithLayout() {
        // bibkey - title
        String fileNamePattern = "\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                layoutFormatterPreferences));
    }

    @Test
    public void testGetLinkedFileNameGetKeyIfEmptyField() {
        // bibkey - title
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetLinkedFileNameGetDefaultIfEmptyFieldNoKeyWithLayout() {
        // bibkey - title
        String fileNamePattern = "\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        BibEntry entry = new BibEntry();

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                layoutFormatterPreferences));
    }

    @Test
    public void testGetLinkedFileNameGetDefaultIfEmptyFieldNoKey() {
        // bibkey - title
        String fileNamePattern = "[title]";
        BibEntry entry = new BibEntry();

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetLinkedFileNameByYearAuthorFirstpage() {
        // bibkey - title
        String fileNamePattern = "[year]_[auth]_[firstpage]";
        BibEntry entry = new BibEntry();
        entry.setField( "author", "O. Kitsune" );
        entry.setField( "year", "1868" );
        entry.setField( "pages", "567-579" );

        assertEquals("1868_Kitsune_567", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern));
    }

    @Test
    public void testGetFileExtensionSimpleFile() {
        assertEquals("pdf", FileHelper.getFileExtension(Paths.get("test.pdf")).get());
    }

    @Test
    public void testGetFileExtensionLowerCaseAndTrimmingFile() {
        assertEquals("pdf", FileHelper.getFileExtension(Paths.get("test.PdF  ")).get());
    }

    @Test
    public void testGetFileExtensionMultipleDotsFile() {
        assertEquals("pdf", FileHelper.getFileExtension(Paths.get("te.st.PdF  ")).get());
    }

    @Test
    public void testGetFileExtensionNoExtensionFile() {
        assertFalse(FileHelper.getFileExtension(Paths.get("JustTextNotASingleDot")).isPresent());
    }

    @Test
    public void testGetFileExtensionNoExtension2File() {
        assertFalse(FileHelper.getFileExtension(Paths.get(".StartsWithADotIsNotAnExtension")).isPresent());
    }

    @Test
    public void getFileExtensionWithSimpleString() {
        assertEquals("pdf", FileHelper.getFileExtension("test.pdf").get());
    }

    @Test
    public void getFileExtensionTrimsAndReturnsInLowercase() {
        assertEquals("pdf", FileHelper.getFileExtension("test.PdF  ").get());
    }

    @Test
    public void getFileExtensionWithMultipleDotsString() {
        assertEquals("pdf", FileHelper.getFileExtension("te.st.PdF  ").get());
    }

    @Test
    public void getFileExtensionWithNoDotReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileHelper.getFileExtension("JustTextNotASingleDot"));
    }

    @Test
    public void getFileExtensionWithDotAtStartReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileHelper.getFileExtension(".StartsWithADotIsNotAnExtension"));
    }

    @Test
    public void getFileNameWithSimpleString() {
        assertEquals("test", FileUtil.getBaseName("test.pdf"));
    }

    @Test
    public void getFileNameWithMultipleDotsString() {
        assertEquals("te.st", FileUtil.getBaseName("te.st.PdF  "));
    }

    @Test
    public void uniquePathSubstrings() {
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
    public void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, true));
    }

    @Test
    public void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithoutOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, nonExistingTestPath, false));
    }

    @Test
    public void testCopyFileFromEmptySourcePathToExistDestinationPathWithOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, true));
    }

    @Test
    public void testCopyFileFromEmptySourcePathToExistDestinationPathWithoutOverrideExistFile(){
        assertFalse(FileUtil.copyFile(nonExistingTestPath, existingTestFile, false));
    }

    @Test
    public void testCopyFileFromExistSourcePathToExistDestinationPathWithOverrideExistFile(){
        assertTrue(FileUtil.copyFile(existingTestFile, existingTestFile, true));
    }

    @Test
    public void testCopyFileFromExistSourcePathToExistDestinationPathWithoutOverrideExistFile(){
        assertFalse(FileUtil.copyFile(existingTestFile, existingTestFile, false));
    }

    @Test
    public void testCopyFileFromExistSourcePathToOtherExistDestinationPathWithOverrideExistFile(){
        assertTrue(FileUtil.copyFile(existingTestFile, otherExistingTestFile, true));
    }

    @Test
    public void testCopyFileFromExistSourcePathToOtherExistDestinationPathWithoutOverrideExistFile(){
        assertFalse(FileUtil.copyFile(existingTestFile, otherExistingTestFile, false));
    }

    @Test
    public void testCopyFileSuccessfulWithOverrideExistFile() throws IOException {
        Path temp = otherTemporaryFolder.newFile("existingTestFile.txt").toPath();
        FileUtil.copyFile(existingTestFile, temp, true);
        assertEquals(Files.readAllLines(existingTestFile, StandardCharsets.UTF_8),Files.readAllLines(temp, StandardCharsets.UTF_8));
    }

    @Test
    public void testCopyFileSuccessfulWithoutOverrideExistFile() throws IOException {
        Path temp = otherTemporaryFolder.newFile("existingTestFile.txt").toPath();
        FileUtil.copyFile(existingTestFile, temp, false);
        assertNotEquals(Files.readAllLines(existingTestFile, StandardCharsets.UTF_8),Files.readAllLines(temp, StandardCharsets.UTF_8));
    }

    @Test (expected = NullPointerException.class)
    public void testRenameFileWithFromFileIsNullAndToFileIsNull() {
        FileUtil.renameFile(null, null);
    }

    @Test (expected = NullPointerException.class)
    public void testRenameFileWithFromFileExistAndToFileIsNull() {
        FileUtil.renameFile(existingTestFile, null);
    }

    @Test (expected = NullPointerException.class)
    public void testRenameFileWithFromFileIsNullAndToFileExist() {
        FileUtil.renameFile(null, existingTestFile);
    }

    @Test
    public void testRenameFileWithFromFileNotExistAndToFileNotExist(){
        assertFalse(FileUtil.renameFile(nonExistingTestPath, nonExistingTestPath));
    }

    @Test
    public void testRenameFileWithFromFileNotExistAndToFileExist(){
        assertFalse(FileUtil.renameFile(nonExistingTestPath, existingTestFile));
    }

    @Test
    public void testRenameFileWithFromFileExistAndToFileNotExist(){
        assertTrue(FileUtil.renameFile(existingTestFile, nonExistingTestPath));
    }

    @Test
    public void testRenameFileWithFromFileExistAndToFileExist(){
        assertTrue(FileUtil.renameFile(existingTestFile, existingTestFile));
    }

    @Test
    public void testRenameFileWithFromFileExistAndOtherToFileExist(){
        assertFalse(FileUtil.renameFile(existingTestFile, otherExistingTestFile));
    }

    @Test
    public void testRenameFileSuccessful() {
        Path temp = Paths.get(otherTemporaryFolder.toString());

        System.out.println(temp);
        FileUtil.renameFile(existingTestFile, temp);
        assertFalse(Files.exists(existingTestFile));
    }

    @Test
    public void validFilenameShouldNotAlterValidFilename() {
        assertEquals("somename.pdf", FileUtil.getValidFileName("somename.pdf"));
    }

    @Test
    public void validFilenameWithoutExtension() {
        assertEquals("somename", FileUtil.getValidFileName("somename"));
    }

    @Test
    public void validFilenameShouldBeMaximum255Chars() {
        String longestValidFilename = Stream.generate(() -> String.valueOf('1')).limit(FileUtil.MAXIMUM_FILE_NAME_LENGTH).collect(Collectors.joining()) + ".pdf";
        String longerFilename = Stream.generate(() -> String.valueOf('1')).limit(260).collect(Collectors.joining()) + ".pdf";
        assertEquals(longestValidFilename, FileUtil.getValidFileName(longerFilename));
    }

    @Test
    public void invalidFilenameWithoutExtension() {
        String longestValidFilename = Stream.generate(() -> String.valueOf('1')).limit(FileUtil.MAXIMUM_FILE_NAME_LENGTH).collect(Collectors.joining());
        String longerFilename = Stream.generate(() -> String.valueOf('1')).limit(260).collect(Collectors.joining());
        assertEquals(longestValidFilename, FileUtil.getValidFileName(longerFilename));
    }

    @Test
    public void testGetLinkedDirNameDefaultFullTitle() {
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

    private Path createTemporaryTestFile(String name) throws IOException {
        File testFile = temporaryFolder.newFile(name);
        Files.write(testFile.toPath(), name.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        return testFile.toPath();
    }
}
