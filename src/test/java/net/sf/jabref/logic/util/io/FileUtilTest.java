package net.sf.jabref.logic.util.io;

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

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileUtilTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    public TemporaryFolder otherTemporaryFolder = new TemporaryFolder();

    private final Path nonExistingTestPath = Paths.get("nonExistingTestPath");
    private Path existingTestFile;
    private Path otherExistingTestFile;

    @Before
    public void setUpViewModel() throws IOException {
        existingTestFile = createTemporaryTestFile("existingTestFile.txt");
        otherExistingTestFile = createTemporaryTestFile("otherExistingTestFile.txt");
        otherTemporaryFolder.create();
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
    public void testGetLinkedFileNameDefault() {
        // bibkey - title
        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234 - mytitle",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern, JabRefPreferences.getInstance()
                        .getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class))));
    }

    @Test
    public void testGetLinkedFileNameBibTeXKey() {
        // bibkey
        String fileNamePattern = "\\bibtexkey";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234",
                FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                        mock(LayoutFormatterPreferences.class)));
    }

    @Test
    public void testGetLinkedFileNameNoPattern() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                mock(LayoutFormatterPreferences.class)));
    }

    @Test
    public void testGetDefaultFileNameNoPatternNoBibTeXKey() {
        String fileNamePattern = "";
        BibEntry entry = new BibEntry();
        entry.setField("title", "mytitle");

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                mock(LayoutFormatterPreferences.class)));
    }

    @Test
    public void testGetLinkedFileNameGetKeyIfEmptyField() {
        // bibkey - title
        String fileNamePattern = "\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                JabRefPreferences.getInstance().getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class))));
    }

    @Test
    public void testGetLinkedFileNameGetDefaultIfEmptyFieldNoKey() {
        // bibkey - title
        String fileNamePattern = "\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        BibEntry entry = new BibEntry();

        assertEquals("default", FileUtil.createFileNameFromPattern(null, entry, fileNamePattern,
                JabRefPreferences.getInstance().getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class))));
    }

    @Test
    public void testGetFileExtensionSimpleFile() {
        assertEquals("pdf", FileUtil.getFileExtension(new File("test.pdf")).get());
    }

    @Test
    public void testGetFileExtensionLowerCaseAndTrimmingFile() {
        assertEquals("pdf", FileUtil.getFileExtension(new File("test.PdF  ")).get());
    }

    @Test
    public void testGetFileExtensionMultipleDotsFile() {
        assertEquals("pdf", FileUtil.getFileExtension(new File("te.st.PdF  ")).get());
    }

    @Test
    public void testGetFileExtensionNoExtensionFile() {
        assertFalse(FileUtil.getFileExtension(new File("JustTextNotASingleDot")).isPresent());
    }

    @Test
    public void testGetFileExtensionNoExtension2File() {
        assertFalse(FileUtil.getFileExtension(new File(".StartsWithADotIsNotAnExtension")).isPresent());
    }

    @Test
    public void getFileExtensionWithSimpleString() {
        assertEquals("pdf", FileUtil.getFileExtension("test.pdf").get());
    }

    @Test
    public void getFileExtensionTrimsAndReturnsInLowercase() {
        assertEquals("pdf", FileUtil.getFileExtension("test.PdF  ").get());
    }

    @Test
    public void getFileExtensionWithMultipleDotsString() {
        assertEquals("pdf", FileUtil.getFileExtension("te.st.PdF  ").get());
    }

    @Test
    public void getFileExtensionWithNoDotReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileUtil.getFileExtension("JustTextNotASingleDot"));
    }

    @Test
    public void getFileExtensionWithDotAtStartReturnsEmptyExtension() {
        assertEquals(Optional.empty(), FileUtil.getFileExtension(".StartsWithADotIsNotAnExtension"));
    }

    @Test
    public void getFileNameWithSimpleString() {
        assertEquals("test", FileUtil.getFileName("test.pdf"));
    }

    @Test
    public void getFileNameWithMultipleDotsString() {
        assertEquals("te.st", FileUtil.getFileName("te.st.PdF  "));
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
        FileUtil.renameFile(existingTestFile.toString(),null);
    }

    @Test (expected = NullPointerException.class)
    public void testRenameFileWithFromFileIsNullAndToFileExist() {
        FileUtil.renameFile(null, existingTestFile.toString());
    }

    @Test
    public void testRenameFileWithFromFileNotExistAndToFileNotExist(){
        assertFalse(FileUtil.renameFile(nonExistingTestPath.toString(), nonExistingTestPath.toString()));
    }

    @Test
    public void testRenameFileWithFromFileNotExistAndToFileExist(){
        assertFalse(FileUtil.renameFile(nonExistingTestPath.toString(), existingTestFile.toString()));
    }

    @Test
    public void testRenameFileWithFromFileExistAndToFileNotExist(){
        assertTrue(FileUtil.renameFile(existingTestFile.toString(), nonExistingTestPath.toString()));
    }

    @Test
    public void testRenameFileWithFromFileExistAndToFileExist(){
        assertTrue(FileUtil.renameFile(existingTestFile.toString(), existingTestFile.toString()));
    }

    @Test
    public void testRenameFileWithFromFileExistAndOtherToFileExist(){
        assertFalse(FileUtil.renameFile(existingTestFile.toString(), otherExistingTestFile.toString()));
    }

    @Test
    public void testRenameFileSuccessful () throws IOException {
        String temp = otherTemporaryFolder.toString();
        System.out.println(temp);
        FileUtil.renameFile(existingTestFile.toString(), temp);
        assertFalse(Files.exists(existingTestFile));
    }

    private Path createTemporaryTestFile(String name) throws IOException {
        File testFile = temporaryFolder.newFile(name);
        Files.write(testFile.toPath(), name.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        return testFile.toPath();
    }
}
