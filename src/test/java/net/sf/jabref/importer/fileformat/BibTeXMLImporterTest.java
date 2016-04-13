package net.sf.jabref.importer.fileformat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

@RunWith(MockitoJUnitRunner.class)
public class BibTeXMLImporterTest {

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

    @Test
    public void testExceptionOnInputStream() throws IOException {
        try (InputStream is = Mockito.mock(InputStream.class)) {
            Mockito.doThrow(new IOException()).when(is).read();

            BibTeXMLImporter importer = new BibTeXMLImporter();
            List<BibEntry> entry = importer.importEntries(is, new OutputPrinterToNull());
            Assert.assertTrue(entry.isEmpty());
        }
    }

    @Test
    public void testGetFormatName() {
        BibTeXMLImporter importer = new BibTeXMLImporter();
        Assert.assertEquals("BibTeXML", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        BibTeXMLImporter importer = new BibTeXMLImporter();
        Assert.assertEquals("bibtexml", importer.getCLIId());
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        BibTeXMLImporter importer = new BibTeXMLImporter();

        List<String> list = testFiles.stream().filter(n -> !n.startsWith("BibTeXMLImporterTest"))
                .collect(Collectors.toList());

        for (String str : list) {
            try (InputStream is = BibTeXMLImporter.class.getResourceAsStream(str)) {
                Assert.assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }
}
