package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        exporter = new XmpExporter(xmpPreferences);

        databaseContext = new BibDatabaseContext();
        encoding = StandardCharsets.UTF_8;
    }

    @Test
    public void exportSingleEntry(@TempDir Path testFolder) throws Exception {
        Path file = testFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Alan Turing");

        exporter.export(databaseContext, file, encoding, Collections.singletonList(entry));
        String actual = String.join("\n", Files.readAllLines(file)); // we are using \n to join, so we need it in the expected string as well, \r\n would fail
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
        entryTuring.setField(StandardField.AUTHOR, "Alan Turing");

        BibEntry entryArmbrust = new BibEntry();
        entryArmbrust.setField(StandardField.AUTHOR, "Michael Armbrust");
        entryArmbrust.setCitationKey("Armbrust2010");

        exporter.export(databaseContext, file, encoding, Arrays.asList(entryTuring, entryArmbrust));

        String actual = String.join("\n", Files.readAllLines(file)); // we are using \n to join, so we need it in the expected string as well, \r\n would fail

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
                "          <rdf:li>bibtex/citationkey/Armbrust2010</rdf:li>\n" +
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
        // set path to the one where the exporter produces several files
        Path file = testFolder.resolve(XmpExporter.XMP_SPLIT_DIRECTORY_INDICATOR);
        Files.createFile(file);

        BibEntry entryTuring = new BibEntry()
                .withField(StandardField.AUTHOR, "Alan Turing");

        BibEntry entryArmbrust = new BibEntry()
                .withField(StandardField.AUTHOR, "Michael Armbrust")
                .withCitationKey("Armbrust2010");

        exporter.export(databaseContext, file, encoding, List.of(entryTuring, entryArmbrust));

        List<String> lines = Files.readAllLines(file);
        assertEquals(Collections.emptyList(), lines);

        Path fileTuring = Path.of(file.getParent().toString(), entryTuring.getId() + "_null.xmp");
        String actualTuring = String.join("\n", Files.readAllLines(fileTuring)); // we are using \n to join, so we need it in the expected string as well, \r\n would fail

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

        Path fileArmbrust = Path.of(file.getParent().toString(), entryArmbrust.getId() + "_Armbrust2010.xmp");
        String actualArmbrust = String.join("\n", Files.readAllLines(fileArmbrust)); // we are using \n to join, so we need it in the expected string as well, \r\n would fail

        String expectedArmbrust = "  <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                "    <rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" rdf:about=\"\">\n" +
                "      <dc:creator>\n" +
                "        <rdf:Seq>\n" +
                "          <rdf:li>Michael Armbrust</rdf:li>\n" +
                "        </rdf:Seq>\n" +
                "      </dc:creator>\n" +
                "      <dc:relation>\n" +
                "        <rdf:Bag>\n" +
                "          <rdf:li>bibtex/citationkey/Armbrust2010</rdf:li>\n" +
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
        when(xmpPreferences.getXmpPrivacyFilter()).thenReturn(Collections.singleton(StandardField.AUTHOR));
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(true);

        Path file = testFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Alan Turing");

        exporter.export(databaseContext, file, encoding, Collections.singletonList(entry));
        String actual = String.join("\n", Files.readAllLines(file));
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
