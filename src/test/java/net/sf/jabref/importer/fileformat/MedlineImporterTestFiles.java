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
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MedlineImporterTestFiles {

    private final static String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";

    private MedlineImporter medlineImporter;

    @Parameter
    public String fileName;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        medlineImporter = new MedlineImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(n -> files.add(n.getFileName().toString()));
        }
        return files.stream().filter(n -> n.startsWith("MedlineImporterTest")).filter(n -> n.endsWith(".xml"))
                .collect(Collectors.toList());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        try (InputStream stream = MedlineImporterTest.class.getResourceAsStream(fileName)) {
            Assert.assertTrue(medlineImporter.isRecognizedFormat(stream));
        }
    }

    @Test
    @Ignore
    public void testImportEntries() throws IOException {
        try (InputStream inputStream = MedlineImporterTest.class.getResourceAsStream(fileName)) {
            List<BibEntry> medlineEntries = medlineImporter.importEntries(inputStream, new OutputPrinterToNull());
            String bibFileName = fileName.replace(".xml", ".bib");
            if (medlineEntries.isEmpty()) {
                assertEquals(Collections.emptyList(), medlineEntries);
            } else {
                BibEntryAssert.assertEquals(MedlineImporterTest.class, bibFileName, medlineEntries);
            }
        }
    }
}