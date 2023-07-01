package org.jabref.logic.importer;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.fileformat.ImporterTestEngine;

public class MarcXmlParserTest {

    private static final String FILE_ENDING = ".xml";

    private static final String MALFORMED_KEY_WORD = "Malformed";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MarcXMLParserTest") && name.endsWith(FILE_ENDING)
                && !name.contains(MALFORMED_KEY_WORD);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

        private static Stream<String> invalidFileNames() throws IOException {
            Predicate<String> fileName = name -> !name.startsWith("MarcXMLParserTest");
            return ImporterTestEngine.getTestFiles(fileName).stream();
    }
}
