package org.jabref.logic.util.io;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileHistoryTest {

    @Test
    public void testFileHistory() {
        FileHistory fh = new FileHistory(new ArrayList<>());

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

        assertEquals(Arrays.asList("ii", "hh", "gg"), fh.getHistory());
    }

}
