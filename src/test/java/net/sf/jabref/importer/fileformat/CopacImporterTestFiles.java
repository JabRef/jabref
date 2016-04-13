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
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class CopacImporterTestFiles {

    private CopacImporter copacImporter;

    @Parameter
    public String fileName;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        copacImporter = new CopacImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() {
        List<String> files = new ArrayList<>();
        File d = new File(System.getProperty("user.dir") + "/src/test/resources/net/sf/jabref/importer/fileformat");
        for (File f : d.listFiles()) {
            files.add(f.getName());
        }
        return files.stream().filter(n -> n.startsWith("CopacImporterTest")).filter(n -> n.endsWith(".txt"))
                .collect(Collectors.toList());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        try (InputStream stream = CopacImporterTest.class.getResourceAsStream(fileName)) {
            Assert.assertTrue(copacImporter.isRecognizedFormat(stream));
        }
    }

    @Test
    public void testImportEntries() throws IOException {
        try (InputStream copacStream = CopacImporterTest.class.getResourceAsStream(fileName)) {

            List<BibEntry> copacEntries = copacImporter.importEntries(copacStream, new OutputPrinterToNull());
            fileName = fileName.replace(".txt", ".bib");

            Assert.assertFalse(copacEntries.isEmpty());

            int size = copacEntries.size();

            // workaround because BibtexEntryAssert can only test 1 entry
            if (size != 1) {
                for (int i = 1; i <= size; i++) {
                    fileName = fileName.replaceAll(".bib", "-" + i + ".bib");
                    BibtexEntryAssert.assertEquals(CopacImporterTest.class, fileName, copacEntries.get(i - 1));
                    fileName = fileName.replaceAll("-" + i + ".bib", ".bib");
                }
            } else {
                BibtexEntryAssert.assertEquals(CopacImporterTest.class, fileName, copacEntries);
            }
        }
    }

}