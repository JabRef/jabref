package org.jabref.logic.importer.fileformat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CopacImporterTestFiles {

    private CopacImporter copacImporter;

    @Parameter
    public String fileName;


    @Before
    public void setUp() {
        copacImporter = new CopacImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws Exception {

        try (Stream<Path> stream = Files.list(Paths.get(CopacImporterTestFiles.class.getResource("").toURI()))) {
            return stream
                    .filter(n -> n.getFileName().toString().startsWith("CopacImporterTest")
                            && n.getFileName().toString().endsWith(".txt"))
                    .map(f -> f.getFileName().toString()).collect(Collectors.toList());
        }
    }

    @Test
    public void testIsRecognizedFormat() throws Exception {
        Path file = Paths.get(CopacImporterTest.class.getResource(fileName).toURI());
        Assert.assertTrue(copacImporter.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testImportEntries() throws Exception {
        String bibFileName = fileName.replace(".txt", ".bib");

        try (InputStream bibStream = BibtexImporterTest.class.getResourceAsStream(bibFileName)) {
            BibEntryAssert.assertEquals(bibStream, CopacImporterTest.class.getResource(fileName), copacImporter);
        }
    }
}
