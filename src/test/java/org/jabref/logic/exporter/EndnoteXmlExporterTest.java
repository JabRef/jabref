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

        entry = new BibEntry(StandardEntryType.Article);
        entry.setCitationKey("Tural2020");
        entry.setField(StandardField.AUTHOR, "Tural, Eray and Tural, Hale");
        entry.setField(StandardField.TITLE, "An overview of research on Big Data");
        entry.setField(StandardField.JOURNAL, "Journal of Big Data Analytics");
        entry.setField(StandardField.YEAR, "2020");
        entry.setField(StandardField.VOLUME, "7");
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.DOI, "10.1186/s41044-020-00047-z");
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
                "      <ref-type name=\"Journal Article\">",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">9</style>",
                "      </ref-type>",
                "      <contributors>",
                "        <authors>",
                "          <author>",
                "            <style face=\"normal\" font=\"default\" size=\"100%\">Tural, Eray</style>",
                "          </author>",
                "          <author>",
                "            <style face=\"normal\" font=\"default\" size=\"100%\">Tural, Hale</style>",
                "          </author>",
                "        </authors>",
                "      </contributors>",
                "      <titles>",
                "        <title>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">An overview of research on Big Data</style>",
                "        </title>",
                "      </titles>",
                "      <periodical>",
                "        <full-title>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">Journal of Big Data Analytics</style>",
                "        </full-title>",
                "      </periodical>",
                "      <volume>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">7</style>",
                "      </volume>",
                "      <number>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">1</style>",
                "      </number>",
                "      <dates>",
                "        <year>",
                "          <style face=\"normal\" font=\"default\" size=\"100%\">2020</style>",
                "        </year>",
                "      </dates>",
                "      <urls/>",
                "      <electronic-resource-num>",
                "        <style face=\"normal\" font=\"default\" size=\"100%\">10.1186/s41044-020-00047-z</style>",
                "      </electronic-resource-num>",
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
