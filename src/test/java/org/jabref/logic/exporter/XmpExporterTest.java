package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class XmpExporterTest {

    private Exporter exporter;
    private BibDatabaseContext databaseContext;
    private Charset encoding;

    @Rule public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        Map<String, TemplateExporter> customFormats = new HashMap<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);

        exporter = exporterFactory.getExporterByName("xmp").get();

        databaseContext = new BibDatabaseContext();
        encoding = StandardCharsets.UTF_8;
    }

    @Test
    public void exportSingleEntry() throws Exception {

        Path file = testFolder.newFile().toPath();

        BibEntry entry = new BibEntry();
        entry.setField("author", "Alan Turing");

        exporter.export(databaseContext, file, encoding, Arrays.asList(entry));

        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.size() == 18);
        assertEquals("<rdf:li>Alan Turing</rdf:li>", lines.get(6).trim());
    }

    @Test
    public void writeMutlipleEntriesInASingleFile() throws Exception {

        Path file = testFolder.newFile().toPath();

        BibEntry entryTuring = new BibEntry();
        entryTuring.setField("author", "Alan Turing");

        BibEntry entryArmbrust = new BibEntry();
        entryArmbrust.setField("author", "Michael Armbrust");
        entryArmbrust.setCiteKey("Armbrust2010");

        exporter.export(databaseContext, file, encoding, Arrays.asList(entryTuring, entryArmbrust));

        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.size() == 36);
        assertEquals("<rdf:li>Alan Turing</rdf:li>", lines.get(6).trim());
        assertEquals("<rdf:li>Michael Armbrust</rdf:li>", lines.get(19).trim());
    }

    @Test
    public void writeMultipleEntriesInDifferentFiles() throws Exception {

        Path file = testFolder.newFile("split").toPath();

        BibEntry entryTuring = new BibEntry();
        entryTuring.setField("author", "Alan Turing");

        BibEntry entryArmbrust = new BibEntry();
        entryArmbrust.setField("author", "Michael Armbrust");
        entryArmbrust.setCiteKey("Armbrust2010");

        exporter.export(databaseContext, file, encoding, Arrays.asList(entryTuring, entryArmbrust));

        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.size() == 0);

        Path fileTuring = Paths.get(file.getParent().toString() + "/" + entryTuring.getId() + "_null.xmp");
        List<String> linesTuring = Files.readAllLines(fileTuring);
        assertTrue(linesTuring.size() == 18);
        assertEquals("<rdf:li>Alan Turing</rdf:li>", linesTuring.get(6).trim());

        Path fileArmbrust = Paths.get(file.getParent().toString() + "/" + entryArmbrust.getId() + "_Armbrust2010.xmp");
        List<String> linesArmbrust = Files.readAllLines(fileArmbrust);
        assertTrue(linesArmbrust.size() == 23);
        assertEquals("<rdf:li>Michael Armbrust</rdf:li>", linesArmbrust.get(6).trim());
    }
}
