package net.sf.jabref.importer.fileformat;

import net.sf.jabref.*;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CopacImporterTest {

    private final List<String> testFiles = getTestFiles();


    /**
     * Generates a List of all files in the package "/src/test/resources/net/sf/jabref/importer/fileformat"
     * @return A list of Names
     */
    public List<String> getTestFiles() {
        List<String> files = new ArrayList<>();
        File d = new File(System.getProperty("user.dir") + "/src/test/resources/net/sf/jabref/importer/fileformat");
        for (File f : d.listFiles()) {
            files.add(f.getName());
        }
        return files;

    }

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test(expected = IOException.class)
    public void testImportEntriesException() throws IOException {
        CopacImporter importer = new CopacImporter();
        importer.importEntries(null, new OutputPrinterToNull());
    }

    @Test
    public void testGetFormatName() {
        CopacImporter importer = new CopacImporter();
        Assert.assertEquals("Copac", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        CopacImporter importer = new CopacImporter();
        Assert.assertEquals("cpc", importer.getCLIId());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        CopacImporter importer = new CopacImporter();

        List<String> list = testFiles.stream().filter(n -> !n.startsWith("CopacImporterTest"))
                .collect(Collectors.toList());

        for (String str : list) {
            try (InputStream is = CopacImporterTest.class.getResourceAsStream(str)) {
                Assert.assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testImportEmptyEntries() throws IOException {
        CopacImporter importer = new CopacImporter();

        try (InputStream is = CopacImporterTest.class.getResourceAsStream("Empty.txt")) {
            List<BibEntry> entries = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertEquals(0, entries.size());
            Assert.assertEquals(Collections.emptyList(), entries);
        }
    }
}