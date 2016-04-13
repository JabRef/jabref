package net.sf.jabref.importer.fileformat;

import net.sf.jabref.*;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class BibTeXMLImporterTestFiles {

    private static final Pattern PATTERN = Pattern.compile("\\D*[0123456789]");

    private BibTeXMLImporter bibtexmlImporter;

    @Parameter
    public String fileName;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        bibtexmlImporter = new BibTeXMLImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() {
        List<String> files = new ArrayList<>();
        File d = new File(System.getProperty("user.dir") + "/src/test/resources/net/sf/jabref/importer/fileformat");
        for (File f : d.listFiles()) {
            files.add(f.getName());
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

            fileName = fileName.replace(".xml", ".bib");

            while (PATTERN.matcher(fileName).find()) {
                fileName = fileName.replaceFirst("[0123456789]", "");
            }

            if (bibtexmlEntries.isEmpty()) {
                Assert.assertEquals(Collections.emptyList(), bibtexmlEntries);
            } else {
                BibtexEntryAssert.assertEquals(BibTeXMLImporterTest.class, fileName, bibtexmlEntries);
            }
        }
    }
}