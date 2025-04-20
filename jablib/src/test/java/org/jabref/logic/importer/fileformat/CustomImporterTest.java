package org.jabref.logic.importer.fileformat;

import java.nio.file.Path;
import java.util.Arrays;

import org.jabref.logic.importer.Importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomImporterTest {

    private CustomImporter importer;

    @BeforeEach
    void setUp() throws Exception {
        importer = asCustomImporter(new CopacImporter());
    }

    @Test
    void getName() {
        assertEquals("Copac", importer.getName());
    }

    @Test
    void getId() {
        assertEquals("cpc", importer.getId());
    }

    @Test
    void getClassName() {
        assertEquals("org.jabref.logic.importer.fileformat.CopacImporter", importer.getClassName());
    }

    @Test
    void getBasePath() {
        assertEquals(Path.of("src/main/java/org/jabref/logic/importer/fileformat/CopacImporter.java"),
                importer.getBasePath());
    }

    @Test
    void getAsStringList() {
        assertEquals(Arrays.asList("src/main/java/org/jabref/logic/importer/fileformat/CopacImporter.java",
                "org.jabref.logic.importer.fileformat.CopacImporter"), importer.getAsStringList());
    }

    @Test
    void equalsWithSameReference() {
        assertEquals(importer, importer);
    }

    @Test
    void equalsIsBasedOnName() {
        // noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(new CopacImporter(), importer);
    }

    @Test
    void compareToSmaller() throws Exception {
        CustomImporter ovidImporter = asCustomImporter(new OvidImporter());

        assertTrue(importer.compareTo(ovidImporter) < 0);
    }

    @Test
    void compareToEven() throws Exception {
        assertEquals(0, importer.compareTo(asCustomImporter(new CopacImporter())));
    }

    @Test
    void testToString() {
        assertEquals("Copac", importer.toString());
    }

    @Test
    void classicConstructor() throws Exception {
        CustomImporter customImporter = new CustomImporter(
                "src/main/java/org/jabref/logic/importer/fileformat/CopacImporter.java",
                "org.jabref.logic.importer.fileformat.CopacImporter");

        assertEquals(importer, customImporter);
    }

    private CustomImporter asCustomImporter(Importer importer) throws Exception {
        return new CustomImporter(
                "src/main/java/org/jabref/logic/importer/fileformat/" + importer.getName() + "Importer.java",
                importer.getClass().getName());
    }
}
