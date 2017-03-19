package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.util.FileExtensions;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CopacImporterTest {

    private CopacImporter importer;


    /**
     * Generates a List of all files in the package "/src/test/resources/org/jabref/logic/importer/fileformat"
     *
     * @return A list of Names
     * @throws IOException
     */
    public List<String> getTestFiles() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get(CopacImporterTest.class.getResource("").toURI()))) {
            return stream.filter(p -> !Files.isDirectory(p)).map(f -> f.getFileName().toString())
                    .collect(Collectors.toList());

        }
    }

    @Before
    public void setUp() throws Exception {
        importer = new CopacImporter();
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(FileExtensions.COPAC, importer.getExtensions());

    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Importer for COPAC format.", importer.getDescription());
    }

    @Test
    public void testIsNotRecognizedFormat() throws Exception {
        List<String> list = getTestFiles().stream().filter(n -> !n.startsWith("CopacImporterTest"))
                .collect(Collectors.toList());
        for (String str : list) {
            Path file = Paths.get(CopacImporterTest.class.getResource(str).toURI());
            Assert.assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testImportEmptyEntries() throws Exception {
        Path path = Paths.get(CopacImporterTest.class.getResource("Empty.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(path, StandardCharsets.UTF_8).getDatabase().getEntries();
        Assert.assertEquals(Collections.emptyList(), entries);
    }
}
