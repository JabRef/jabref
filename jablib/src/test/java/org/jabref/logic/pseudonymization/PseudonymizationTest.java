package org.jabref.logic.pseudonymization;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
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

    private BibDatabaseWriter databaseWriter;
    private StringWriter stringWriter;
    private BibWriter bibWriter;
    private SelfContainedSaveConfiguration saveConfiguration;
    private FieldPreferences fieldPreferences;
    private CitationKeyPatternPreferences citationKeyPatternPreferences;
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, "\n");
        // Reverted to WITH_JABREF_META_DATA to fix compilation error.
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = new BibEntryTypesManager();

        databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
    }

    @Test
    void pseudonymizeTwoEntries() {
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
        
        // 1. Check that the entries match (BibEntry.equals compares content, not UUIDs)
        assertEquals(List.of(firstPseudo, secondPseudo), result.bibDatabaseContext().getEntries());

        // 2. Check that the value mapping matches (Map.equals ignores order, so Map.of is safe)
        Map<String, String> expectedMap = Map.of(
                "author-1", "Author One", 
                "author-2", "Author Two", 
                "pages-1", "some pages", 
                "citationkey-1", "first", 
                "citationkey-2", "second"
        );
        assertEquals(expectedMap, result.valueMapping());
    }
}
