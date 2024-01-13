package org.jabref.logic.anonymization;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AnonymizationTest {

    private BibtexImporter importer;

    private BibtexDatabaseWriter databaseWriter;
    private StringWriter stringWriter;
    private BibWriter bibWriter;
    private SelfContainedSaveConfiguration saveConfiguration;
    private FieldPreferences fieldPreferences;
    private CitationKeyPatternPreferences citationKeyPatternPreferences;
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    public void setUp() {
        importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, "\n");
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        fieldPreferences = new FieldPreferences(true, Collections.emptyList(), Collections.emptyList());
        citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = new BibEntryTypesManager();

        databaseWriter = new BibtexDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
    }

    @Test
    void anonymizeLibrary() throws Exception {
        Path path = Path.of(AnonymizationTest.class.getResource("Chocolate.bib").toURI());
        BibDatabaseContext databaseContext = importer.importDatabase(path).getDatabaseContext();

        Anonymization anonymization = new Anonymization();
        BibDatabaseContext result = anonymization.anonymizeLibrary(databaseContext);
        databaseWriter.saveDatabase(result);

        Path expectedPath = Path.of(AnonymizationTest.class.getResource("Chocolate-anon.bib").toURI());
        assertEquals(Files.readString(expectedPath), stringWriter.toString());
    }

    /**
     * This test can be used to anonymize a library.
     */
    @Test
    void anonymizeLibraryFile(@TempDir Path tempDir) throws Exception {
        // modify path to the file to be anonymized
        Path path = Path.of(AnonymizationTest.class.getResource("Chocolate.bib").toURI());
        // modify target to the file to be created
        Path target = tempDir.resolve("anon.bib");

        BibDatabaseContext databaseContext = importer.importDatabase(path).getDatabaseContext();

        Anonymization anonymization = new Anonymization();
        BibDatabaseContext result = anonymization.anonymizeLibrary(databaseContext);
        databaseWriter.saveDatabase(result);

        Files.writeString(target, stringWriter.toString());

        assertTrue(Files.exists(target));
    }
}
