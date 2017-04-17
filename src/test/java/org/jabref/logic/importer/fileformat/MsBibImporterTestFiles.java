package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MsBibImporterTestFiles {

    @Parameter
    public String fileName;
    public Path resourceDir;


    @Before
    public void setUp() throws Exception {
        resourceDir = Paths.get(MsBibImporterTestFiles.class.getResource("").toURI());
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException, URISyntaxException {
        try (Stream<Path> stream = Files.list(Paths.get(MsBibImporterTestFiles.class.getResource("").toURI()))) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".xml"))
                    .filter(n -> n.startsWith("MsBib")).collect(Collectors.toList());
        }
    }

    @Test
    public final void testIsRecognizedFormat() throws Exception {
        MsBibImporter testImporter = new MsBibImporter();
        Path xmlFile = resourceDir.resolve(fileName);

        Assert.assertTrue(testImporter.isRecognizedFormat(xmlFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testImportEntries() throws Exception {

        String bibFileName = fileName.replace(".xml", ".bib");
        MsBibImporter testImporter = new MsBibImporter();

        Path xmlFile = resourceDir.resolve(fileName);

        List<BibEntry> result = testImporter.importDatabase(xmlFile, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntryAssert.assertEquals(MsBibImporterTest.class, bibFileName, result);
    }

}
