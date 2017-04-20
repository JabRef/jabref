package org.jabref.logic.pdf.search;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;


public class PdfContentReaderTest {
    @Test
    public void readContentFromPDFToString() throws Exception {

        File pdf = new File(Paths.get(PdfContentReaderTest.class.getResource("example.pdf").toURI()).toString());

        PdfContentReader reader = new PdfContentReader();

        //TODO create test database with pdfs
//        reader.readContentFromPDFToString(pdf, )
    }

    //TODO Test if key matches

    //TODO Test empty fields

}
