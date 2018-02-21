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
public class ModsImporterTestFiles {

    @Parameter
    public String fileName;
    public Path resourceDir;

    private ModsImporter testImporter;


    @Before
    public void setUp() throws Exception {
        resourceDir = Paths.get(ModsImporterTestFiles.class.getResource("").toURI());
        testImporter = new ModsImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException, URISyntaxException {
        try (Stream<Path> stream = Files.list(Paths.get(ModsImporterTestFiles.class.getResource("").toURI()))) {
            return stream.map(n -> n.getFileName().toString()).filter(n -> n.endsWith(".xml"))
                    .filter(n -> n.startsWith("MODS")).collect(Collectors.toList());
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(testImporter.isRecognizedFormat(resourceDir.resolve(fileName), StandardCharsets.UTF_8));
    }

    @Test
    public void testImportEntries() throws Exception {
        String bibFileName = fileName.replace(".xml", ".bib");
        Path xmlFile = resourceDir.resolve(fileName);

        List<BibEntry> result = testImporter.importDatabase(xmlFile, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntryAssert.assertEquals(ModsImporter.class, bibFileName, result);

    }
}
