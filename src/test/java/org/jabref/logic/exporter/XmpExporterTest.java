package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.collections.FXCollections;

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
    private final BibDatabaseContext databaseContext = new BibDatabaseContext();
    private final XmpPreferences xmpPreferences = mock(XmpPreferences.class);

    @BeforeEach
    public void setUp() {
        exporter = new XmpExporter(xmpPreferences);
    }

    @Test
    public void exportSingleEntry(@TempDir Path testFolder) throws Exception {
        Path file = testFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Alan Turing");

        exporter.export(databaseContext, file, Collections.singletonList(entry));
        String actual = String.join("\n", Files.readAllLines(file)); // we are using \n to join, so we need it in the expected string as well, \r\n would fail
        String expected = """
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" rdf:about="">
                      <dc:creator>
                        <rdf:Seq>
                          <rdf:li>Alan Turing</rdf:li>
                        </rdf:Seq>
                      </dc:creator>
                      <dc:format>application/pdf</dc:format>
                      <dc:type>
                        <rdf:Bag>
                          <rdf:li>Misc</rdf:li>
                        </rdf:Bag>
                      </dc:type>
                    </rdf:Description>
                  </rdf:RDF>
                """.stripTrailing();
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

        exporter.export(databaseContext, file, Arrays.asList(entryTuring, entryArmbrust));

        String actual = String.join("\n", Files.readAllLines(file)); // we are using \n to join, so we need it in the expected string as well, \r\n would fail

        String expected = """
                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                      <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" rdf:about="">
                        <dc:creator>
                          <rdf:Seq>
                            <rdf:li>Alan Turing</rdf:li>
                          </rdf:Seq>
                        </dc:creator>
                        <dc:format>application/pdf</dc:format>
                        <dc:type>
                          <rdf:Bag>
                            <rdf:li>Misc</rdf:li>
                          </rdf:Bag>
                        </dc:type>
                      </rdf:Description>
                      <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" rdf:about="">
                        <dc:creator>
                          <rdf:Seq>
                            <rdf:li>Michael Armbrust</rdf:li>
                          </rdf:Seq>
                        </dc:creator>
                        <dc:relation>
                          <rdf:Bag>
                            <rdf:li>bibtex/citationkey/Armbrust2010</rdf:li>
                          </rdf:Bag>
                        </dc:relation>
                        <dc:format>application/pdf</dc:format>
                        <dc:type>
                          <rdf:Bag>
                            <rdf:li>Misc</rdf:li>
                          </rdf:Bag>
                        </dc:type>
                      </rdf:Description>
                    </rdf:RDF>
                  """.stripTrailing();
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

        exporter.export(databaseContext, file, List.of(entryTuring, entryArmbrust));

        // Nothing written in given file
        List<String> lines = Files.readAllLines(file);
        assertEquals(Collections.emptyList(), lines);

        // turing contains the turing entry only
        Path fileTuring = Path.of(file.getParent().toString(), entryTuring.getId() + "_null.xmp");
        // we are using \n to join, so we need it in the expected string as well, \r\n would fail
        String actualTuring = String.join("\n", Files.readAllLines(fileTuring));
        String expectedTuring = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" rdf:about="">
                    <dc:creator>
                      <rdf:Seq>
                        <rdf:li>Alan Turing</rdf:li>
                      </rdf:Seq>
                    </dc:creator>
                    <dc:format>application/pdf</dc:format>
                    <dc:type>
                      <rdf:Bag>
                        <rdf:li>Misc</rdf:li>
                      </rdf:Bag>
                    </dc:type>
                  </rdf:Description>
                </rdf:RDF>
              """.stripTrailing();
        assertEquals(expectedTuring, actualTuring);

        // armbrust contains the armbrust entry only
        Path fileArmbrust = Path.of(file.getParent().toString(), entryArmbrust.getId() + "_Armbrust2010.xmp");
        // we are using \n to join, so we need it in the expected string as well, \r\n would fail
        String actualArmbrust = String.join("\n", Files.readAllLines(fileArmbrust));
        String expectedArmbrust = """
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" rdf:about="">
                      <dc:creator>
                        <rdf:Seq>
                          <rdf:li>Michael Armbrust</rdf:li>
                        </rdf:Seq>
                      </dc:creator>
                      <dc:relation>
                        <rdf:Bag>
                          <rdf:li>bibtex/citationkey/Armbrust2010</rdf:li>
                        </rdf:Bag>
                      </dc:relation>
                      <dc:format>application/pdf</dc:format>
                      <dc:type>
                        <rdf:Bag>
                          <rdf:li>Misc</rdf:li>
                        </rdf:Bag>
                      </dc:type>
                    </rdf:Description>
                  </rdf:RDF>
                """.stripTrailing();
        assertEquals(expectedArmbrust, actualArmbrust);
    }

    @Test
    public void exportSingleEntryWithPrivacyFilter(@TempDir Path testFolder) throws Exception {
        when(xmpPreferences.getXmpPrivacyFilter()).thenReturn(FXCollections.observableSet(Collections.singleton(StandardField.AUTHOR)));
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(true);

        Path file = testFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "Alan Turing");

        exporter.export(databaseContext, file, Collections.singletonList(entry));

        String actual = String.join("\n", Files.readAllLines(file));
        String expected = """
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" rdf:about="">
                      <dc:format>application/pdf</dc:format>
                      <dc:type>
                        <rdf:Bag>
                          <rdf:li>Misc</rdf:li>
                        </rdf:Bag>
                      </dc:type>
                    </rdf:Description>
                  </rdf:RDF>
                """.stripTrailing();

        assertEquals(expected, actual);
    }
}
