package org.jabref;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutomaticRelinkTest {

    /** Set up Directorys
     *
     */
    public void setUpD() {
        String filepath = "src/test/TestDirectory";
        File main = new File(filepath);
        Boolean makeM = main.mkdir();

        String filepath2 = "src/test/TestDirectory/A";
        File a = new File(filepath2);
        Boolean makeA = a.mkdir();

        String filepath3 = "src/test/TestDirectory/B";
        File b = new File(filepath3);
        Boolean makeB = b.mkdir();

        assertTrue(makeM, "Error Creating Main");
        assertTrue(makeA, "Error Creating A");
        assertTrue(makeB, "Error Creating B");
    }

    @Test
    void setUp() {
     //   setUpD();
    }
    /**
     * File Doesn't do anything new
     */

    public void copy(String source, String destination) throws IOException {
        Path src = Path.of(source);
        Path dst = Path.of(destination);
        Files.copy(src, dst.resolve(src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

   @Test
   void copyTest() throws IOException {
        copy("src/test/resources/pdfs/minimal.pdf", "src/test/TestDirectory/A");
       File file = new File("src/test/TestDirectory/A/minimal.pdf");
       assertTrue(file.exists(), "Copy did not work!");
    }


    @Test
    void checkEntry() {
        BibEntry bib = new BibEntry();
        bib.addFile(new LinkedFile("description", "src/test/TestDirectory/A/minimal.pdf",".pdf"));
        List<LinkedFile> list = new List<LinkedFile>() ;
        list.add(new LinkedFile("description", "src/test/TestDirectory/A/minimal.pdf",".pdf"));
        assertEquals(list, bib.getFiles());
    }
}
