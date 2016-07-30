package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.charset.Charset;
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
import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.logic.importer.fileformat.MedlineImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

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

    private final static String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";

    private MedlineImporter medlineImporter;

    @Parameter
    public Path importFile;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        medlineImporter = new MedlineImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<Path> files() throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(files::add);
        }
        return files.stream().filter(n -> n.getFileName().toString().startsWith("MedlineImporterTest")
                && n.getFileName().toString().endsWith(".xml"))
                .collect(Collectors.toList());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(medlineImporter.isRecognizedFormat(importFile, Charset.defaultCharset()));
    }

    @Test
    public void testImportEntries() throws IOException {
            List<BibEntry> medlineEntries = medlineImporter.importDatabase(importFile, Charset.defaultCharset()).getDatabase().getEntries();
            String bibFileName = importFile.getFileName().toString().replace(".xml", ".bib");
            if (medlineEntries.isEmpty()) {
                assertEquals(Collections.emptyList(), medlineEntries);
            } else {
                BibEntryAssert.assertEquals(MedlineImporterTest.class, bibFileName, medlineEntries);
            }
    }
}
