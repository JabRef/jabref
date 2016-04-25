package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

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
    private final static String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";

    private BibTeXMLImporter bibtexmlImporter;

    @Parameter
    public String fileName;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        bibtexmlImporter = new BibTeXMLImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(n -> files.add(n.getFileName().toString()));
        }
        return files.stream().filter(n -> n.startsWith("BibTeXMLImporterTest")).filter(n -> n.endsWith(".xml"))
                .collect(Collectors.toList());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        try (InputStream stream = BibTeXMLImporterTest.class.getResourceAsStream(fileName)) {
            Assert.assertTrue(bibtexmlImporter.isRecognizedFormat(stream));
        }
    }

    @Test
    public void testImportEntries() throws IOException {
        try (InputStream bitexmlStream = BibTeXMLImporterTest.class.getResourceAsStream(fileName)) {
            List<BibEntry> bibtexmlEntries = bibtexmlImporter.importEntries(bitexmlStream, new OutputPrinterToNull());

            String bibFileName = fileName.replace(".xml", ".bib");
            while (PATTERN.matcher(bibFileName).find()) {
                bibFileName = bibFileName.replaceFirst("[0123456789]", "");
            }
            if (bibtexmlEntries.isEmpty()) {
                Assert.assertEquals(Collections.emptyList(), bibtexmlEntries);
            } else {
                BibtexEntryAssert.assertEquals(BibTeXMLImporterTest.class, bibFileName, bibtexmlEntries);
            }
        }
    }
}