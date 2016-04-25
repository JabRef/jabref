package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibtexEntryAssert;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CopacImporterTestFiles {

    private CopacImporter copacImporter;
    private final static String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/importer/fileformat";

    @Parameter
    public String fileName;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        copacImporter = new CopacImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<String> fileNames() throws IOException {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(n -> files.add(n.getFileName().toString()));
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
        String bibFileName = fileName.replace(".txt", ".bib");

        try (InputStream copacStream = CopacImporterTest.class.getResourceAsStream(fileName);
                InputStream bibStream = BibtexImporterTest.class.getResourceAsStream(bibFileName)) {
            BibtexEntryAssert.assertEquals(copacImporter, bibStream, copacStream);
        }

    }

}