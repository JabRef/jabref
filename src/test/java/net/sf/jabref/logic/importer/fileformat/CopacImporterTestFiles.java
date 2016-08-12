package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.preferences.JabRefPreferences;

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
    private final static String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/logic/importer/fileformat";

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
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Paths.get(CopacImporterTest.class.getResource(fileName).toURI());
        Assert.assertTrue(copacImporter.isRecognizedFormat(file, Charset.defaultCharset()));
    }

    @Test
    public void testImportEntries() throws IOException, URISyntaxException {
        String bibFileName = fileName.replace(".txt", ".bib");

        try (InputStream bibStream = BibtexImporterTest.class.getResourceAsStream(bibFileName)) {
            BibEntryAssert.assertEquals(bibStream, CopacImporterTest.class.getResource(fileName), copacImporter);
        }
    }
}
