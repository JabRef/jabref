package org.jabref.logic.pseudonymization;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PseudonymizationTest {

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
    void pseudonymizeTwoEntries() throws Exception {
        BibEntry first = new BibEntry("first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry("second")
                .withField(StandardField.AUTHOR, "Author Two")
                .withField(StandardField.PAGES, "some pages");

        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(first, second)));

        Pseudonymization pseudonymization = new Pseudonymization();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

        BibEntry firstPseudo = new BibEntry("citationkey-1")
                .withField(StandardField.AUTHOR, "author-1")
                .withField(StandardField.PAGES, "pages-1");
        BibEntry secondPseudo = new BibEntry("citationkey-2")
                .withField(StandardField.AUTHOR, "author-2")
                .withField(StandardField.PAGES, "pages-1");
        BibDatabaseContext bibDatabaseContextExpected = new BibDatabaseContext(new BibDatabase(List.of(firstPseudo, secondPseudo)));
        bibDatabaseContextExpected.setMode(BibDatabaseMode.BIBLATEX);
        Pseudonymization.Result expected = new Pseudonymization.Result(
                bibDatabaseContextExpected,
                Map.of("author-1", "Author One", "author-2", "Author Two", "pages-1", "some pages", "citationkey-1", "first", "citationkey-2", "second"));

        assertEquals(expected, result);
    }

    @Test
    void pseudonymizeLibrary() throws Exception {
        Path path = Path.of(PseudonymizationTest.class.getResource("Chocolate.bib").toURI());
        BibDatabaseContext databaseContext = importer.importDatabase(path).getDatabaseContext();

        Pseudonymization pseudonymization = new Pseudonymization();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);
        databaseWriter.saveDatabase(result.bibDatabaseContext());

        Path expectedPath = Path.of(PseudonymizationTest.class.getResource("Chocolate-pseudnomyized.bib").toURI());
        assertEquals(Files.readString(expectedPath), stringWriter.toString());
    }

    /**
     * This test can be used to anonymize a library.
     */
    @Test
    void pseudonymizeLibraryFile(@TempDir Path tempDir) throws Exception {
        // modify path to the file to be anonymized
        Path path = Path.of(PseudonymizationTest.class.getResource("Chocolate.bib").toURI());
        // modify target to the files to be created
        Path target = tempDir.resolve("pseudo.bib");
        Path mappingInfoTarget = target.resolveSibling("pseudo.bib.mapping.csv");

        BibDatabaseContext databaseContext = importer.importDatabase(path).getDatabaseContext();

        Pseudonymization pseudonymization = new Pseudonymization();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);
        databaseWriter.saveDatabase(result.bibDatabaseContext());

        Files.writeString(target, stringWriter.toString());

        PseudonymizationResultCsvWriter.writeValuesMappingAsCsv(mappingInfoTarget, result);

        assertTrue(Files.exists(target));
    }
}
