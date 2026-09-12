package org.jabref.logic.shared;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DatabaseTest
@Execution(ExecutionMode.SAME_THREAD)
class SharedDatabaseExportTest {

    private ConnectorTest connectorTest;
    private DBMSProcessor dbmsProcessor;
    private DBMSConnectionUrl connectionUrl;
    private CliPreferences preferences;

    @BeforeEach
    void setup() throws Exception {
        connectorTest = new ConnectorTest();
        DBMSConnection dbmsConnection = connectorTest.getTestDBMSConnection();
        dbmsProcessor = new DBMSProcessor(dbmsConnection);
        TestManager.clearTables(dbmsConnection);

        connectionUrl = DBMSConnectionUrl.parse(
                "postgresql://postgres:postgres@localhost:" + dbmsConnection.getProperties().getPort() + "/postgres").orElseThrow();

        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences()).thenReturn(BibEntryPreferences.getDefault());
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("test@test");
        when(preferences.getSelfContainedExportConfiguration()).thenReturn(new SelfContainedSaveConfiguration());
        when(preferences.getFieldPreferences()).thenReturn(new FieldPreferences(true, List.of(), List.of()));
    }

    @AfterEach
    void tearDown() throws Exception {
        connectorTest.close();
    }

    @Test
    void pullFailsOnDatabaseWithoutJabRefTables() {
        assertThrows(DatabaseNotSupportedException.class, () -> SharedDatabaseExport.pullToTemporaryBib(connectionUrl, preferences));
    }

    @Test
    void pulledFileContainsEntriesAndMetaData() throws Exception {
        dbmsProcessor.setupSharedDatabase();
        dbmsProcessor.insertEntries(List.of(
                new BibEntry(StandardEntryType.Article)
                        .withCitationKey("first")
                        .withField(StandardField.AUTHOR, "Alice")
                        .withField(StandardField.TITLE, "First"),
                new BibEntry(StandardEntryType.Book)
                        .withCitationKey("second")
                        .withField(StandardField.AUTHOR, "Bob")));
        Map<String, String> metaData = dbmsProcessor.getSharedMetaData();
        metaData.put(MetaData.DATABASE_TYPE, "biblatex;");
        dbmsProcessor.setSharedMetaData(metaData);

        Path pulled = SharedDatabaseExport.pullToTemporaryBib(connectionUrl, preferences);

        ParserResult parserResult = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor())
                .importDatabase(pulled);

        assertEquals(List.of(
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("first")
                                .withField(StandardField.AUTHOR, "Alice")
                                .withField(StandardField.TITLE, "First"),
                        new BibEntry(StandardEntryType.Book)
                                .withCitationKey("second")
                                .withField(StandardField.AUTHOR, "Bob")),
                parserResult.getDatabase().getEntries());
        assertEquals(Optional.of(BibDatabaseMode.BIBLATEX), parserResult.getMetaData().getMode());
    }
}
