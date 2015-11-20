package net.sf.jabref.logic.util.io;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.JabRefPreferences;

public class FileHistoryTest {

    JabRefPreferences prefs;


    @Before
    public void setUp() throws Exception {
        prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testConstructor() {
        FileHistory fh = new FileHistory(prefs);
    }


    @Test
    public void testFileHistory() {
        FileHistory fh = new FileHistory(prefs);
        String[] oldFileNames = prefs.getStringArray(JabRefPreferences.RECENT_FILES);
        Integer oldHistorySize = prefs.getInt(JabRefPreferences.HISTORY_SIZE);

        prefs.putInt(JabRefPreferences.HISTORY_SIZE, 1);
        fh.newFile("aa");
        assertEquals("aa", fh.getFileName(0));
        assertEquals(1, fh.size());

        prefs.putInt(JabRefPreferences.HISTORY_SIZE, 3);
        fh.newFile("bb");
        assertEquals("bb", fh.getFileName(0));
        assertEquals(2, fh.size());

        fh.newFile("aa");
        assertEquals("aa", fh.getFileName(0));
        assertEquals(2, fh.size());

        fh.newFile("cc");
        assertEquals("cc", fh.getFileName(0));
        assertEquals("aa", fh.getFileName(1));
        assertEquals("bb", fh.getFileName(2));
        assertEquals(3, fh.size());

        fh.newFile("dd");
        assertEquals("dd", fh.getFileName(0));
        assertEquals("cc", fh.getFileName(1));
        assertEquals("aa", fh.getFileName(2));
        assertEquals(3, fh.size());

        fh.removeItem("cc");
        assertEquals("dd", fh.getFileName(0));
        assertEquals("aa", fh.getFileName(1));
        assertEquals(2, fh.size());

        fh.storeHistory();
        String[] newFileNames = prefs.getStringArray(JabRefPreferences.RECENT_FILES);

        assertArrayEquals(newFileNames, new String[] {"dd", "aa"});
        prefs.putInt(JabRefPreferences.HISTORY_SIZE, oldHistorySize);
        prefs.putStringArray(JabRefPreferences.RECENT_FILES, oldFileNames);
    }

}
