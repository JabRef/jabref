package net.sf.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileUtilTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final Path emptyTestPath = Paths.get("noExistPath");;
    private Path emptyTestFilePath;

    @Before
    public void setUpViewModel() throws IOException {
        emptyTestFilePath = createTemporaryTestFile("emptyTestFile01.txt");
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
    public void testGetFileExtensionSimpleString() {
        assertEquals("pdf", FileUtil.getFileExtension("test.pdf").get());
    }

    @Test
    public void testGetFileExtensionLowerCaseAndTrimmingString() {
        assertEquals("pdf", FileUtil.getFileExtension("test.PdF  ").get());
    }

    @Test
    public void testGetFileExtensionMultipleDotsString() {
        assertEquals("pdf", FileUtil.getFileExtension("te.st.PdF  ").get());
    }

    @Test
    public void testGetFileExtensionNoExtensionString() {
        assertFalse(FileUtil.getFileExtension("JustTextNotASingleDot").isPresent());
    }

    @Test
    public void testGetFileExtensionNoExtension2String() {
        assertFalse(FileUtil.getFileExtension(".StartsWithADotIsNotAnExtension").isPresent());
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
    public void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithOverrideExistFile() throws Exception{
        assertFalse(FileUtil.copyFile(emptyTestPath, emptyTestPath, true));
    }

    @Test
    public void testCopyFileFromEmptySourcePathToEmptyDestinationPathWithoutOverrideExistFile() throws Exception{
        assertFalse(FileUtil.copyFile(emptyTestPath, emptyTestPath, false));
    }

    @Test
    public void testCopyFileFromEmptySourcePathToExistDestinationPathWithOverrideExistFile() throws Exception{
        assertFalse(FileUtil.copyFile(emptyTestPath, emptyTestFilePath, true));
    }

    @Test
    public void testCopyFileFromEmptySourcePathToExistDestinationPathWithoutOverrideExistFile() throws Exception{
        assertFalse(FileUtil.copyFile(emptyTestPath, emptyTestFilePath, false));
    }

    @Test
    public void testCopyFileFromExistSourcePathToExistDestinationPathWithOverrideExistFile() throws Exception{

        assertTrue(FileUtil.copyFile(emptyTestFilePath, emptyTestFilePath, true));
    }

    @Test
    public void testCopyFileFromExistSourcePathToExistDestinationPathWithoutOverrideExistFile() throws Exception{
        assertFalse(FileUtil.copyFile(emptyTestFilePath, emptyTestFilePath, false));
    }

    @Test (expected = NullPointerException.class)
    public void testRenameFiletoNull() throws Exception {
        FileUtil.renameFile(null, null);
        FileUtil.renameFile(emptyTestFilePath,null);
        FileUtil.renameFile(null, emptyTestFilePath);
    }

    @Test
    public void testRenameFileWithFromFileNotExistAndToFileNotExist() throws Exception{
        assertFalse(FileUtil.renameFile(emptyTestPath, emptyTestPath));
    }

    @Test
    public void testRenameFileWithFromFileNotExistAndToFileExist() throws Exception{
        assertFalse(FileUtil.renameFile(emptyTestPath, emptyTestFilePath));
    }

    @Test
    public void testRenameFileWithFromFileExistAndToFileNotExist() throws Exception{
        assertFalse(FileUtil.renameFile(emptyTestFilePath, emptyTestPath));
    }

    @Test
    public void testRenameFileWithFromFileExistAndToFileExist() throws Exception{
        assertTrue(FileUtil.renameFile(emptyTestFilePath, emptyTestFilePath));
    }

    private Path createTemporaryTestFile(String name) throws IOException {
        File testFile = temporaryFolder.newFile(name);
        return testFile.toPath();
    }
}
