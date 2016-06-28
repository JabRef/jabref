package net.sf.jabref.logic.util.io;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileUtilTest {
    @Before
    public void setUp() {
        Globals.prefs = mock(JabRefPreferences.class);
    }

    @After
    public void tearDown() {
        Globals.prefs = null;
        Globals.journalAbbreviationLoader = null;
    }

    @Test
    public void testGetLinkedFileNameDefault() {
        // bibkey - title
        when(Globals.prefs.get(JabRefPreferences.PREF_IMPORT_FILENAMEPATTERN)).thenReturn("\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}");

        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234 - mytitle", FileUtil.createFileNameFromPattern(null, entry, mock(JournalAbbreviationLoader.class)));
    }

    @Test
    public void testGetLinkedFileNameBibTeXKey() {
        // bibkey
        when(Globals.prefs.get(JabRefPreferences.PREF_IMPORT_FILENAMEPATTERN)).thenReturn("\\bibtexkey");

        BibEntry entry = new BibEntry();
        entry.setCiteKey("1234");
        entry.setField("title", "mytitle");

        assertEquals("1234", FileUtil.createFileNameFromPattern(null, entry, mock(JournalAbbreviationLoader.class)));
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
