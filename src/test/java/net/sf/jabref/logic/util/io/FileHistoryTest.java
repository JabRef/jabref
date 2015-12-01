package net.sf.jabref.logic.util.io;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.JabRefPreferences;

public class FileHistoryTest {

    JabRefPreferences prefs;
    String[] oldFileNames;

    @Before
    public void setUp() throws Exception {
        prefs = JabRefPreferences.getInstance();
        oldFileNames = prefs.getStringArray(JabRefPreferences.RECENT_FILES);
    }

    @After
    public void restore() {
        prefs.putStringArray(JabRefPreferences.RECENT_FILES, oldFileNames);
    }
    @Test
    public void testConstructor() {
        FileHistory fh = new FileHistory(prefs);
    }


    @Test
    public void testFileHistory() {
        FileHistory fh = new FileHistory(prefs);

        fh.newFile("aa");
        assertEquals("aa", fh.getFileName(0));
        fh.newFile("bb");
        assertEquals("bb", fh.getFileName(0));

        fh.newFile("aa");
        assertEquals("aa", fh.getFileName(0));

        fh.newFile("cc");
        assertEquals("cc", fh.getFileName(0));
        assertEquals("aa", fh.getFileName(1));
        assertEquals("bb", fh.getFileName(2));

        fh.newFile("dd");
        assertEquals("dd", fh.getFileName(0));
        assertEquals("cc", fh.getFileName(1));
        assertEquals("aa", fh.getFileName(2));
        fh.newFile("ee");
        fh.newFile("ff");
        fh.newFile("gg");
        fh.newFile("hh");
        assertEquals("bb", fh.getFileName(7));
        assertEquals(8, fh.size());
        fh.newFile("ii");
        assertEquals("aa", fh.getFileName(7));
        fh.removeItem("ff");
        assertEquals(7, fh.size());
        fh.removeItem("ee");
        fh.removeItem("dd");
        fh.removeItem("cc");
        fh.removeItem("cc");
        fh.removeItem("aa");
        fh.storeHistory();
        assertArrayEquals(new String[] {"ii", "hh", "gg"}, prefs.getStringArray(JabRefPreferences.RECENT_FILES));
    }

}
