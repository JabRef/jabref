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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.bibtex.BibEntryAssert;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

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
    public Path importFile;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        bibtexmlImporter = new BibTeXMLImporter();
    }

    @Parameters(name = "{0}")
    public static Collection<Path> files() throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(FILEFORMAT_PATH))) {
            stream.forEach(files::add);
        }
        return files.stream().filter(n -> n.getFileName().toString().startsWith("BibTeXMLImporterTest") && n.getFileName().toString().endsWith(".xml"))
                .collect(Collectors.toList());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(bibtexmlImporter.isRecognizedFormat(importFile, Charset.defaultCharset()));
    }

    @Test
    public void testImportEntries() throws IOException {
        List<BibEntry> bibtexmlEntries = bibtexmlImporter.importDatabase(importFile, Charset.defaultCharset()).getDatabase().getEntries();

        String bibFileName = importFile.getFileName().toString().replace(".xml", ".bib");
        while (PATTERN.matcher(bibFileName).find()) {
            bibFileName = bibFileName.replaceFirst("[0123456789]", "");
        }
        if (bibtexmlEntries.isEmpty()) {
            Assert.assertEquals(Collections.emptyList(), bibtexmlEntries);
        } else {
            BibEntryAssert.assertEquals(BibTeXMLImporterTest.class, bibFileName, bibtexmlEntries);
        }
    }
}
