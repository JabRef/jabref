package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XmpExporterTest {

    private Exporter exporter;
    private BibDatabaseContext databaseContext;
    private Charset encoding;
    private final XmpPreferences xmpPreferences = mock(XmpPreferences.class);

    @BeforeEach
    public void setUp() {
        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);

        exporter = exporterFactory.getExporterByName("xmp").get();

        databaseContext = new BibDatabaseContext();
        encoding = StandardCharsets.UTF_8;
    }

    @Test
    public void exportSingleEntry(@TempDir Path testFolder) throws Exception {
        Path file = testFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        BibEntry entry = new BibEntry();
        entry.setField("author", "Alan Turing");

        exporter.export(databaseContext, file, encoding, Collections.singletonList(entry));
        String actual = Files.readAllLines(file).stream().collect(Collectors.joining("\n")); //we are using \n to join, so we need it in the expected string as well, \r\n would fail
        String expected = "  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                "    <rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" rdf:about=\"\">\n" +
                "      <dc:creator>\n" +
                "        <rdf:Seq>\n" +
                "          <rdf:li>Alan Turing</rdf:li>\n" +
                "        </rdf:Seq>\n" +
                "      </dc:creator>\n" +
                "      <dc:format>application/pdf</dc:format>\n" +
                "      <dc:type>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>Misc</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:type>\n" +
                "    </rdf:Description>\n" +
                "  </rdf:RDF>";
        assertEquals(expected, actual);
    }

    @Test
    public void writeMultipleEntriesInASingleFile(@TempDir Path testFolder) throws Exception {
        Path file = testFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        BibEntry entryTuring = new BibEntry();
        entryTuring.setField("author", "Alan Turing");

        BibEntry entryArmbrust = new BibEntry();
        entryArmbrust.setField("author", "Michael Armbrust");
        entryArmbrust.setCiteKey("Armbrust2010");

        exporter.export(databaseContext, file, encoding, Arrays.asList(entryTuring, entryArmbrust));

        String actual = Files.readAllLines(file).stream().collect(Collectors.joining("\n")); //we are using \n to join, so we need it in the expected string as well, \r\n would fail

        String expected = "  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                "    <rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" rdf:about=\"\">\n" +
                "      <dc:creator>\n" +
                "        <rdf:Seq>\n" +
                "          <rdf:li>Alan Turing</rdf:li>\n" +
                "        </rdf:Seq>\n" +
                "      </dc:creator>\n" +
                "      <dc:format>application/pdf</dc:format>\n" +
                "      <dc:type>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>Misc</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:type>\n" +
                "    </rdf:Description>\n" +
                "    <rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" rdf:about=\"\">\n" +
                "      <dc:creator>\n" +
                "        <rdf:Seq>\n" +
                "          <rdf:li>Michael Armbrust</rdf:li>\n" +
                "        </rdf:Seq>\n" +
                "      </dc:creator>\n" +
                "      <dc:relation>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>bibtex/bibtexkey/Armbrust2010</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:relation>\n" +
                "      <dc:format>application/pdf</dc:format>\n" +
                "      <dc:type>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>Misc</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:type>\n" +
                "    </rdf:Description>\n" +
                "  </rdf:RDF>";
        assertEquals(expected, actual);
    }

    @Test
    public void writeMultipleEntriesInDifferentFiles(@TempDir Path testFolder) throws Exception {
        Path file = testFolder.resolve("split");
        Files.createFile(file);

        BibEntry entryTuring = new BibEntry();
        entryTuring.setField("author", "Alan Turing");

        BibEntry entryArmbrust = new BibEntry();
        entryArmbrust.setField("author", "Michael Armbrust");
        entryArmbrust.setCiteKey("Armbrust2010");

        exporter.export(databaseContext, file, encoding, Arrays.asList(entryTuring, entryArmbrust));

        List<String> lines = Files.readAllLines(file);
        assertEquals(Collections.emptyList(), lines);

        Path fileTuring = Paths.get(file.getParent().toString() + "/" + entryTuring.getId() + "_null.xmp");
        List<String> linesTuring = Files.readAllLines(fileTuring);
        String actualTuring = Files.readAllLines(fileTuring).stream().collect(Collectors.joining("\n")); //we are using \n to join, so we need it in the expected string as well, \r\n would fail

        String expectedTuring = "  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                "    <rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" rdf:about=\"\">\n" +
                "      <dc:creator>\n" +
                "        <rdf:Seq>\n" +
                "          <rdf:li>Alan Turing</rdf:li>\n" +
                "        </rdf:Seq>\n" +
                "      </dc:creator>\n" +
                "      <dc:format>application/pdf</dc:format>\n" +
                "      <dc:type>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>Misc</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:type>\n" +
                "    </rdf:Description>\n" +
                "  </rdf:RDF>";

        assertEquals(expectedTuring, actualTuring);

        Path fileArmbrust = Paths.get(file.getParent().toString() + "/" + entryArmbrust.getId() + "_Armbrust2010.xmp");
        String actualArmbrust = Files.readAllLines(fileArmbrust).stream().collect(Collectors.joining("\n")); //we are using \n to join, so we need it in the expected string as well, \r\n would fail

        String expectedArmbrust = "  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                "    <rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" rdf:about=\"\">\n" +
                "      <dc:creator>\n" +
                "        <rdf:Seq>\n" +
                "          <rdf:li>Michael Armbrust</rdf:li>\n" +
                "        </rdf:Seq>\n" +
                "      </dc:creator>\n" +
                "      <dc:relation>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>bibtex/bibtexkey/Armbrust2010</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:relation>\n" +
                "      <dc:format>application/pdf</dc:format>\n" +
                "      <dc:type>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>Misc</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:type>\n" +
                "    </rdf:Description>\n" +
                "  </rdf:RDF>";

        assertEquals(expectedArmbrust, actualArmbrust);
    }

    @Test
    public void exportSingleEntryWithPrivacyFilter(@TempDir Path testFolder) throws Exception {
        when(xmpPreferences.getXmpPrivacyFilter()).thenReturn(Arrays.asList("author"));
        when(xmpPreferences.isUseXMPPrivacyFilter()).thenReturn(true);

        Path file = testFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        BibEntry entry = new BibEntry();
        entry.setField("author", "Alan Turing");

        exporter.export(databaseContext, file, encoding, Collections.singletonList(entry));
        String actual = Files.readAllLines(file).stream().collect(Collectors.joining("\n"));
        String expected = "  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                "    <rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" rdf:about=\"\">\n" +
                "      <dc:format>application/pdf</dc:format>\n" +
                "      <dc:type>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>Misc</rdf:li>\n" +
                "        </rdf:Bag>\n" +
                "      </dc:type>\n" +
                "    </rdf:Description>\n" +
                "  </rdf:RDF>";
        assertEquals(expected, actual);
    }
}
