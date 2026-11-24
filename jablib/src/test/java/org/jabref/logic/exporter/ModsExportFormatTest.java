package org.jabref.logic.exporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("exporter")
class ModsExportFormatTest {

    private ModsExporter modsExportFormat;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() throws URISyntaxException {
        databaseContext = new BibDatabaseContext();
        modsExportFormat = new ModsExporter();
        new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());
        Path.of(ModsExportFormatTest.class.getResource("ModsExportFormatTestAllFields.bib").toURI());
    }

    @Test
    final void exportForNoEntriesWritesNothing(@TempDir Path tempFile) throws IOException, SaveException {
        Path file = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        modsExportFormat.export(databaseContext, tempFile, List.of());
        assertEquals(List.of(), Files.readAllLines(file));
    }

    @Test
    void exportNamesWithAuthorAndEditorRoles(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Book);
        entry.setField(StandardField.TITLE, "MODS Name Export Test");
        entry.setField(StandardField.AUTHOR, "Sandra Sanders and John Smith");
        entry.setField(StandardField.EDITOR, "Sam Flanders and Alice Johnson");
        entry.setField(StandardField.YEAR, "2026");

        Path file = tempDir.resolve("names.xml");
        modsExportFormat.export(databaseContext, file, List.of(entry));

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        var document = factory.newDocumentBuilder().parse(file.toFile());
        String namespace = "http://www.loc.gov/mods/v3";
        var names = document.getElementsByTagNameNS(namespace, "name");

        assertEquals(4, names.getLength());

        List<String> expected = List.of(
                "Sanders|Sandra|aut",
                "Smith|John|aut",
                "Flanders|Sam|edt",
                "Johnson|Alice|edt"
        );

        for (int i = 0; i < names.getLength(); i++) {
            Element name = (Element) names.item(i);
            var parts = name.getElementsByTagNameNS(namespace, "namePart");

            assertEquals(2, parts.getLength());

            Element family = (Element) parts.item(0);
            Element given = (Element) parts.item(1);
            Element role = (Element) name.getElementsByTagNameNS(namespace, "roleTerm").item(0);

            assertEquals("personal", name.getAttribute("type"));
            assertEquals("family", family.getAttribute("type"));
            assertEquals("given", given.getAttribute("type"));
            assertEquals("code", role.getAttribute("type"));
            assertEquals("marcrelator", role.getAttribute("authority"));

            assertEquals(expected.get(i),
                    family.getTextContent() + "|" + given.getTextContent() + "|" + role.getTextContent());
        }
    }
}
