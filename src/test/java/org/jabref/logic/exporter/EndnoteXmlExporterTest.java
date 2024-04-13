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
    private BibEntry conferenceEntry;
    private BibEntry bookEntry;

    @BeforeEach
    public void setUp() {
        exporter = new EndnoteXmlExporter();

        conferenceEntry = new BibEntry(StandardEntryType.Conference);
        conferenceEntry.setCitationKey("Dai2018");
        conferenceEntry.setField(StandardField.AUTHOR, "Dai, H. K.");
        conferenceEntry.setField(StandardField.TITLE, "Episode-Rule Mining with Minimal Occurrences via First Local Maximization in Confidence");
        conferenceEntry.setField(StandardField.BOOKTITLE, "Proceedings of the 9th International Symposium on Information and Communication Technology");
        conferenceEntry.setField(StandardField.YEAR, "2018");
        conferenceEntry.setField(StandardField.MONTH, "12");
        conferenceEntry.setField(StandardField.SERIES, "SoICT '18");
        conferenceEntry.setField(StandardField.PAGES, "130--136");
        conferenceEntry.setField(StandardField.ADDRESS, "New York, NY, USA");
        conferenceEntry.setField(StandardField.PUBLISHER, "Association for Computing Machinery");
        conferenceEntry.setField(StandardField.DOI, "10.1145/3287921.3287982");
        conferenceEntry.setField(StandardField.ISBN, "9781450365390");
        conferenceEntry.setField(StandardField.ABSTRACT, "An episode rule of associating two episodes represents a temporal implication of the antecedent episode to the consequent episode. Episode-rule mining is a task of extracting useful patterns/episodes from large event databases. We present an episode-rule mining algorithm for finding frequent and confident serial-episode rules via first local-maximum confidence in yielding ideal window widths, if exist, in event sequences based on minimal occurrences constrained by a constant maximum gap. Results from our preliminary empirical study confirm the applicability of the episode-rule mining algorithm for Web-site traversal-pattern discovery, and show that the first local maximization yielding ideal window widths exists in real data but rarely in synthetic random data sets.");
        conferenceEntry.setField(StandardField.KEYWORDS, "Web-site traversal pattern, episode-rule mining, first local maximization");

        bookEntry = new BibEntry(StandardEntryType.Book);
        bookEntry.setCitationKey("Bhattacharyya2013");
        bookEntry.setField(StandardField.EDITOR, "Bhattacharyya, R. and McCormick, M. E.");
        bookEntry.setField(StandardField.PUBLISHER, "Elsevier Science");
        bookEntry.setField(StandardField.TITLE, "Wave Energy Conversion");
        bookEntry.setField(StandardField.YEAR, "2013");
        bookEntry.setField(StandardField.ISBN, "9780080442129");
        bookEntry.setField(StandardField.FILE, "/home/mfg/acad/ext/arts/waves/water/[R._Bhattacharyya_and_M.E._McCormick_(Eds.)]_Wave_(z-lib.org).pdf");
        bookEntry.setField(StandardField.KEYWORDS, "waves, agua");
    }

    @Test
    public void exportForSingleConferenceEntry(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("ConferenceEntry.xml");

        exporter.export(databaseContext, file, Collections.singletonList(conferenceEntry));

        List<String> lines = Files.readAllLines(file);
        String expectedXml = String.join(System.lineSeparator(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
                "<xml>",
                "  <records>",
                "    <record>",
                "      <ref-type name=\"Conference\">11</ref-type>",
                "      <contributors>",
                "        <authors>Dai, H. K.</authors>",
                "      </contributors>",
                "      <title>Episode-Rule Mining with Minimal Occurrences via First Local Maximization in Confidence</title>",
                "      <secondary-title>Proceedings of the 9th International Symposium on Information and Communication Technology</secondary-title>",
                "      <tertiary-title>SoICT '18</tertiary-title>",
                "      <pages>130--136</pages>",
                "      <year>2018</year>",
                "      <pub-dates>12</pub-dates>",
                "      <publisher>Association for Computing Machinery</publisher>",
                "      <pub-location>New York, NY, USA</pub-location>",
                "      <isbn>9781450365390</isbn>",
                "      <abstract>An episode rule of associating two episodes represents a temporal implication of the antecedent episode to the consequent episode. Episode-rule mining is a task of extracting useful patterns/episodes from large event databases. We present an episode-rule mining algorithm for finding frequent and confident serial-episode rules via first local-maximum confidence in yielding ideal window widths, if exist, in event sequences based on minimal occurrences constrained by a constant maximum gap. Results from our preliminary empirical study confirm the applicability of the episode-rule mining algorithm for Web-site traversal-pattern discovery, and show that the first local maximization yielding ideal window widths exists in real data but rarely in synthetic random data sets.</abstract>",
                "      <electronic-resource-num>10.1145/3287921.3287982</electronic-resource-num>",
                "      <keywords>Web-site traversal pattern</keywords>",
                "      <keywords>episode-rule mining</keywords>",
                "      <keywords>first local maximization</keywords>",
                "    </record>",
                "  </records>",
                "</xml>"
        );
        assertEquals(expectedXml, String.join(System.lineSeparator(), lines));
    }

    @Test
    public void exportForSingleBookEntry(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("BookEntry.xml");

        exporter.export(databaseContext, file, Collections.singletonList(bookEntry));

        List<String> lines = Files.readAllLines(file);
        String expectedXml = String.join(System.lineSeparator(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
                "<xml>",
                "  <records>",
                "    <record>",
                "      <ref-type name=\"Book\">2</ref-type>",
                "      <contributors>",
                "        <secondary-authors>Bhattacharyya, R. and McCormick, M. E.</secondary-authors>",
                "      </contributors>",
                "      <title>Wave Energy Conversion</title>",
                "      <year>2013</year>",
                "      <publisher>Elsevier Science</publisher>",
                "      <isbn>9780080442129</isbn>",
                "      <pdf-urls>/home/mfg/acad/ext/arts/waves/water/[R._Bhattacharyya_and_M.E._McCormick_(Eds.)]_Wave_(z-lib.org).pdf</pdf-urls>",
                "      <keywords>waves</keywords>",
                "      <keywords>agua</keywords>",
                "    </record>",
                "  </records>",
                "</xml>"
        );
        assertEquals(expectedXml, String.join(System.lineSeparator(), lines));
    }

    @Test
    public void exportForMultipleEntries(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("MultipleEntries.xml");

        exporter.export(databaseContext, file, List.of(conferenceEntry, bookEntry));

        List<String> lines = Files.readAllLines(file);
        String expectedXml = String.join(System.lineSeparator(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
                "<xml>",
                "  <records>",
                "    <record>",
                "      <ref-type name=\"Conference\">11</ref-type>",
                "      <contributors>",
                "        <authors>Dai, H. K.</authors>",
                "      </contributors>",
                "      <title>Episode-Rule Mining with Minimal Occurrences via First Local Maximization in Confidence</title>",
                "      <secondary-title>Proceedings of the 9th International Symposium on Information and Communication Technology</secondary-title>",
                "      <tertiary-title>SoICT '18</tertiary-title>",
                "      <pages>130--136</pages>",
                "      <year>2018</year>",
                "      <pub-dates>12</pub-dates>",
                "      <publisher>Association for Computing Machinery</publisher>",
                "      <pub-location>New York, NY, USA</pub-location>",
                "      <isbn>9781450365390</isbn>",
                "      <abstract>An episode rule of associating two episodes represents a temporal implication of the antecedent episode to the consequent episode. Episode-rule mining is a task of extracting useful patterns/episodes from large event databases. We present an episode-rule mining algorithm for finding frequent and confident serial-episode rules via first local-maximum confidence in yielding ideal window widths, if exist, in event sequences based on minimal occurrences constrained by a constant maximum gap. Results from our preliminary empirical study confirm the applicability of the episode-rule mining algorithm for Web-site traversal-pattern discovery, and show that the first local maximization yielding ideal window widths exists in real data but rarely in synthetic random data sets.</abstract>",
                "      <electronic-resource-num>10.1145/3287921.3287982</electronic-resource-num>",
                "      <keywords>Web-site traversal pattern</keywords>",
                "      <keywords>episode-rule mining</keywords>",
                "      <keywords>first local maximization</keywords>",
                "    </record>",
                "    <record>",
                "      <ref-type name=\"Book\">2</ref-type>",
                "      <contributors>",
                "        <secondary-authors>Bhattacharyya, R. and McCormick, M. E.</secondary-authors>",
                "      </contributors>",
                "      <title>Wave Energy Conversion</title>",
                "      <year>2013</year>",
                "      <publisher>Elsevier Science</publisher>",
                "      <isbn>9780080442129</isbn>",
                "      <pdf-urls>/home/mfg/acad/ext/arts/waves/water/[R._Bhattacharyya_and_M.E._McCormick_(Eds.)]_Wave_(z-lib.org).pdf</pdf-urls>",
                "      <keywords>waves</keywords>",
                "      <keywords>agua</keywords>",
                "    </record>",
                "  </records>",
                "</xml>"
        );
        assertEquals(expectedXml, String.join(System.lineSeparator(), lines));
    }

    @Test
    public void exportForEmptyEntryList(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("EmptyFile");

        exporter.export(databaseContext, file, Collections.emptyList());

        assertEquals(Collections.emptyList(), Files.readAllLines(file));
    }

    @Test
    public void exportForNullDBThrowsException(@TempDir Path tempDir) {
        Path file = tempDir.resolve("NullDB");

        assertThrows(NullPointerException.class, () ->
                exporter.export(null, file, Collections.singletonList(conferenceEntry)));
    }

    @Test
    public void exportForNullExportPathThrowsException(@TempDir Path tempDir) {
        assertThrows(NullPointerException.class, () ->
                exporter.export(databaseContext, null, Collections.singletonList(conferenceEntry)));
    }

    @Test
    public void exportForNullEntryListThrowsException(@TempDir Path tempDir) {
        Path file = tempDir.resolve("EntryNull");

        assertThrows(NullPointerException.class, () ->
                exporter.export(databaseContext, file, null));
    }
}
