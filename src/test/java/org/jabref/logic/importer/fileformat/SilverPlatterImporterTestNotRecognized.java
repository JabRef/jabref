package org.jabref.logic.importer.fileformat;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SilverPlatterImporterTestNotRecognized {

    public SilverPlatterImporter testImporter;


    @BeforeEach
    public void setUp() throws Exception {
        testImporter = new SilverPlatterImporter();
    }

    @Test
    public final void testIsNotRecognizedFormat() throws Exception {
        List<String> notAccept = Arrays.asList("emptyFile.xml", "IsiImporterTest1.isi",
                "RisImporterTest1.ris", "InspecImportTest2.txt");
        for (String s : notAccept) {
            URL resource = SilverPlatterImporter.class.getResource(s);
            assertNotNull(resource, "resource " + s + " must be available");
            Path file = Paths.get(resource.toURI());
            assertFalse(testImporter.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

}
