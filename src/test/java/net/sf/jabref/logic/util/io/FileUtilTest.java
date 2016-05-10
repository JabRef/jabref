package net.sf.jabref.logic.util.io;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FileUtilTest {

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
        String[] pathArr = {
                Paths.get("C:/uniquefile.bib").toString(),
                Paths.get("C:/downloads/filename.bib").toString(),
                Paths.get("C:/mypaper/bib/filename.bib").toString(),
                Paths.get("C:/external/mypaper/bib/filename.bib").toString(),
                ""
        };
        String[] uniqArr = {
                Paths.get("uniquefile.bib").toString(),
                Paths.get("downloads/filename.bib").toString(),
                Paths.get("C:/mypaper/bib/filename.bib").toString(),
                Paths.get("external/mypaper/bib/filename.bib").toString(),
                ""
        };
        List<String> paths = Arrays.asList(pathArr);
        List<String> uniqPath = Arrays.asList(uniqArr);

        List<String> result = FileUtil.uniquePathSubstrings(paths);
        assertEquals(uniqPath, result);
    }

    
}