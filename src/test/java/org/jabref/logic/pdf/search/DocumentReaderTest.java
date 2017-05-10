package org.jabref.logic.pdf.search;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;


public class DocumentReaderTest {
    @Test
    public void readContentFromPDFToString() throws Exception {

        File pdf = new File(Paths.get(DocumentReaderTest.class.getResource("example.pdf").toURI()).toString());

        DocumentReader reader = new DocumentReader();

        //TODO create test database with pdfs
//        reader.readPDFContents(pdf, )
    }

    //TODO Test if key matches

    //TODO Test empty fields

}
