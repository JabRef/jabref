package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EndnoteXmlExporterTest {
    private Exporter exporter;
    private final BibDatabaseContext databaseContext = new BibDatabaseContext();
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        exporter = new EndnoteXmlExporter();

        entry = new BibEntry(StandardEntryType.Conference);
        entry.setCitationKey("Dai2018");
        entry.setField(StandardField.AUTHOR, "Dai, H. K.");
        entry.setField(StandardField.TITLE, "Episode-Rule Mining with Minimal Occurrences via First Local Maximization in Confidence");
        entry.setField(StandardField.BOOKTITLE, "Proceedings of the 9th International Symposium on Information and Communication Technology");
        entry.setField(StandardField.YEAR, "2018");
        entry.setField(StandardField.MONTH, "12");
        entry.setField(StandardField.DAY, "6");
        entry.setField(StandardField.SERIES, "SoICT '18");
        entry.setField(StandardField.PAGES, "130--136");
        entry.setField(StandardField.ADDRESS, "New York, NY, USA");
        entry.setField(StandardField.PUBLISHER, "Association for Computing Machinery");
        entry.setField(StandardField.PAGETOTAL, "7");
        entry.setField(StandardField.DOI, "10.1145/3287921.3287982");
        entry.setField(StandardField.ISBN, "9781450365390");
        entry.setField(StandardField.ABSTRACT, "An episode rule of associating two episodes represents a temporal implication of the antecedent episode to the consequent episode. Episode-rule mining is a task of extracting useful patterns/episodes from large event databases. We present an episode-rule mining algorithm for finding frequent and confident serial-episode rules via first local-maximum confidence in yielding ideal window widths, if exist, in event sequences based on minimal occurrences constrained by a constant maximum gap. Results from our preliminary empirical study confirm the applicability of the episode-rule mining algorithm for Web-site traversal-pattern discovery, and show that the first local maximization yielding ideal window widths exists in real data but rarely in synthetic random data sets.");
        entry.setField(StandardField.URL, "https://doi.org/10.1145/3287921.3287982");
    }

    @Test
    public void exportForSingleEntry(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);

        exporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> lines = Files.readAllLines(file);
        String expectedXml = String.join(System.lineSeparator(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
                "<xml>",
                "  <records>",
                "    <record>",
                "      <database name=\"My EndNote Library.enl\" path=\"/path/to/My EndNote Library.enl\">",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">endnote.enl</style>",
                "      </database>",
                "      <source-app name=\"JabRef\" version=\"20.1\">",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">JabRef</style>",
                "      </source-app>",
                "      <rec-number>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">" + entry.getId() + "</style>",
                "      </rec-number>",
                "      <foreign-keys>",
                "        <key app=\"EN\">" + entry.getId() + "</key>",
                "      </foreign-keys>",
                "      <ref-type name=\"Conference Proceedings\">",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">10</style>",
                "      </ref-type>",
                "      <contributors>",
                "        <authors>",
                "          <author>",
                "            <style face=\"normal\" font=\"default\" size=\"100%\">Dai, H. K.</style>",
                "          </author>",
                "        </authors>",
                "      </contributors>",
                "      <titles>",
                "        <title>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">Episode-Rule Mining with Minimal Occurrences via First Local Maximization in Confidence</style>",
                "        </title>",
                "      </titles>",
                "      <periodical/>",
                "      <secondary-title>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">SoICT '18</style>",
                "      </secondary-title>",
                "      <pages>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">130--136</style>",
                "      </pages>",
                "      <dates>",
                "        <year>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">2018</style>",
                "        </year>",
                "      </dates>",
                "      <publisher>",
                "        <publisher>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">Association for Computing Machinery</style>",
                "        </publisher>",
                "        <address>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">New York, NY, USA</style>",
                "        </address>",
                "      </publisher>",
                "      <isbn>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">9781450365390</style>",
                "      </isbn>",
                "      <abstract>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">An episode rule of associating two episodes represents a temporal implication of the antecedent episode to the consequent episode. Episode-rule mining is a task of extracting useful patterns/episodes from large event databases. We present an episode-rule mining algorithm for finding frequent and confident serial-episode rules via first local-maximum confidence in yielding ideal window widths, if exist, in event sequences based on minimal occurrences constrained by a constant maximum gap. Results from our preliminary empirical study confirm the applicability of the episode-rule mining algorithm for Web-site traversal-pattern discovery, and show that the first local maximization yielding ideal window widths exists in real data but rarely in synthetic random data sets.</style>",
                "      </abstract>",
                "      <urls>",
                "        <related-urls>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">https://doi.org/10.1145/3287921.3287982</style>",
                "        </related-urls>",
                "      </urls>",
                "      <electronic-resource-num>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">10.1145/3287921.3287982</style>",
                "      </electronic-resource-num>",
                "      <month>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">12</style>",
                "      </month>",
                "      <day>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">6</style>",
                "      </day>",
                "      <pagetotal>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">7</style>",
                "      </pagetotal>",
                "    </record>",
                "  </records>",
                "</xml>"
        );
        assertEquals(expectedXml, String.join(System.lineSeparator(), lines));
    }

    @Test
    public void exportForEmptyEntryList(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("EmptyFile");
        Files.createFile(file);

        exporter.export(databaseContext, file, Collections.emptyList());

        assertEquals(Collections.emptyList(), Files.readAllLines(file));
    }

    @Test
    public void exportForNullDBThrowsException(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("NullDB");
        Files.createFile(file);

        assertThrows(NullPointerException.class, () ->
                exporter.export(null, file, Collections.singletonList(entry)));
    }

    @Test
    public void exportForNullExportPathThrowsException(@TempDir Path tempDir) {
        assertThrows(NullPointerException.class, () ->
                exporter.export(databaseContext, null, Collections.singletonList(entry)));
    }

    @Test
    public void exportForNullEntryListThrowsException(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("EntryNull");
        Files.createFile(file);

        assertThrows(NullPointerException.class, () ->
                exporter.export(databaseContext, file, null));
    }
}
