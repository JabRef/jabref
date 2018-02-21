package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
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
public class BibTeXMLImporterTestFiles {

    private static final Pattern PATTERN = Pattern.compile("\\D*[0123456789]");

    private BibTeXMLImporter bibtexmlImporter;

    @Parameter
    public Path importFile;


    @Before
    public void setUp() {
        bibtexmlImporter = new BibTeXMLImporter();
    }

    @Parameters(name = "{0}")
    public static List<Path> files() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get(BibTeXMLImporterTest.class.getResource("").toURI()))) {
            return stream.filter(n -> n.getFileName().toString().startsWith("BibTeXMLImporterTest")
                    && n.getFileName().toString().endsWith(".xml")).collect(Collectors.toList());
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(bibtexmlImporter.isRecognizedFormat(importFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testImportEntries() throws IOException {
        List<BibEntry> bibtexmlEntries = bibtexmlImporter.importDatabase(importFile, StandardCharsets.UTF_8)
                .getDatabase().getEntries();

        String bibFileName = importFile.getFileName().toString().replace(".xml", ".bib");
        while (PATTERN.matcher(bibFileName).find()) {
            bibFileName = bibFileName.replaceFirst("[0123456789]", "");
        }
        if (bibtexmlEntries.isEmpty()) {
            Assert.assertEquals(Collections.emptyList(), bibtexmlEntries);
        } else {
            BibEntryAssert.assertEquals(BibTeXMLImporterTest.class, bibFileName, bibtexmlEntries);
        }
    }
}
