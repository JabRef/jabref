package net.sf.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CopacImporterTest {

    private static final String FILEFORMAT_PATH = "src/test/resources/net/sf/jabref/logic/importer/fileformat";
    private CopacImporter importer;


    /**
     * Generates a List of all files in the package "/src/test/resources/net/sf/jabref/logic/importer/fileformat"
     *
     * @return A list of Names
     * @throws IOException
     */
    public List<String> getTestFiles() throws IOException {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(n -> files.add(n.getFileName().toString()));
        }
        return files;
    }

    @Before
    public void setUp() throws Exception {
        importer = new CopacImporter();
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(FileExtensions.COPAC, importer.getExtensions());

    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Importer for COPAC format.", importer.getDescription());
    }

    @Test
    public void testIsNotRecognizedFormat() throws IOException, URISyntaxException {
        List<String> list = getTestFiles().stream().filter(n -> !n.startsWith("CopacImporterTest"))
                .collect(Collectors.toList());
        for (String str : list) {
            Path file = Paths.get(CopacImporterTest.class.getResource(str).toURI());
            Assert.assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testImportEmptyEntries() throws IOException, URISyntaxException {
        Path path = Paths.get(CopacImporterTest.class.getResource("Empty.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(path, Charset.defaultCharset()).getDatabase().getEntries();
        Assert.assertEquals(Collections.emptyList(), entries);
    }
}
