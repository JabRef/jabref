package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
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

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MedlineImporterTestFiles {

    private MedlineImporter medlineImporter;

    @Parameter
    public Path importFile;


    @Before
    public void setUp() {
        medlineImporter = new MedlineImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<Path> files() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get(MedlineImporterTestFiles.class.getResource("").toURI()))) {
            return stream.filter(n -> n.getFileName().toString().startsWith("MedlineImporterTest")
                    && n.getFileName().toString().endsWith(".xml")).collect(Collectors.toList());
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(medlineImporter.isRecognizedFormat(importFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testImportEntries() throws IOException {
        List<BibEntry> medlineEntries = medlineImporter.importDatabase(importFile, StandardCharsets.UTF_8).getDatabase()
                .getEntries();
        String bibFileName = importFile.getFileName().toString().replace(".xml", ".bib");
        if (medlineEntries.isEmpty()) {
            assertEquals(Collections.emptyList(), medlineEntries);
        } else {
            BibEntryAssert.assertEquals(MedlineImporterTest.class, bibFileName, medlineEntries);
        }
    }
}
