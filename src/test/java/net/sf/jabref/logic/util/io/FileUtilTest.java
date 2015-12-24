package net.sf.jabref.logic.util.io;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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

    @Test
    public void testDecodeFileFieldSingleString() {
        assertEquals("test.pdf", FileUtil.decodeFileField("test.pdf").get(0).get(1));
    }

    @Test
    public void testDecodeFileFieldSingleItem() {
        List<List<String>> fileList = FileUtil.decodeFileField("paper:test.pdf:PDF");
        assertEquals("paper", fileList.get(0).get(0));
        assertEquals("test.pdf", fileList.get(0).get(1));
    }

    @Test
    public void testDecodeFileFieldMultipleItems() {
        List<List<String>> fileList = FileUtil.decodeFileField("paper:test.pdf:PDF;presentation:test.ppt:PPT");
        assertEquals("paper", fileList.get(0).get(0));
        assertEquals("test.pdf", fileList.get(0).get(1));
        assertEquals("presentation", fileList.get(1).get(0));
        assertEquals("test.ppt", fileList.get(1).get(1));
    }

    @Test
    public void testDecodeFileFieldEscaping() {
        List<List<String>> fileList = FileUtil.decodeFileField("paper:c\\:\\\\test.pdf:PDF");
        assertEquals("paper", fileList.get(0).get(0));
        assertEquals("c:\\test.pdf", fileList.get(0).get(1));
    }

    @Test
    public void testDecodeFileFieldXMLCharacter() {
        List<List<String>> fileList = FileUtil.decodeFileField("pap&#44;er:c\\:\\\\test.pdf:PDF");
        assertEquals("pap&#44;er", fileList.get(0).get(0));
        assertEquals("c:\\test.pdf", fileList.get(0).get(1));
    }

    @Test
    public void testDecodeFileFieldEmptyString() {
        assertTrue(FileUtil.decodeFileField("").isEmpty());
    }

    @Test
    public void testDecodeFileFieldNullString() {
        assertTrue(FileUtil.decodeFileField(null).isEmpty());
    }

}