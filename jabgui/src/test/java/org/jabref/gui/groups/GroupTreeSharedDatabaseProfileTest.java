package org.jabref.gui.groups;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.search.NoOpSearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.search.sqlbased.PostgresServer;
import org.jabref.logic.shared.DatabaseConnection;
import org.jabref.logic.shared.DatabaseConnectionProperties;
import org.jabref.logic.shared.DBMSConnectionPropertiesBuilder;
import org.jabref.logic.shared.DBMSSynchronizer;
import org.jabref.logic.shared.DBMSType;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Temporary profiler for the generated large library through the shared-database path.
@ExtendWith(ApplicationExtension.class)
class GroupTreeSharedDatabaseProfileTest {

    private static final Path LIBRARY = Path.of("..", "generated-large-library.bib");

    @Test
    void profileSharedDatabaseGroupTree() throws Exception {
        ImportFormatPreferences importPreferences = mock(ImportFormatPreferences.class, RETURNS_DEEP_STUBS);
        when(importPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(importPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());

        long parseStarted = System.nanoTime();
        ParserResult parsedLibrary;
        try (Reader reader = Files.newBufferedReader(LIBRARY, StandardCharsets.UTF_8)) {
            parsedLibrary = new BibtexParser(importPreferences).parse(reader);
        }
        long parseDuration = System.nanoTime() - parseStarted;

        try (PostgresServer postgres = new PostgresServer()) {
            DatabaseConnection sourceConnection = openConnection(postgres);
            DatabaseConnection targetConnection = openConnection(postgres);
            BibDatabaseContext sourceContext = new BibDatabaseContext();
            DBMSSynchronizer sourceSynchronizer = newSynchronizer(sourceContext);
            sourceContext.convertToSharedDatabase(sourceSynchronizer);
            sourceSynchronizer.openSharedDatabase(sourceConnection);
            insertEntriesInBatches(sourceContext, parsedLibrary.getDatabase().getEntries());
            parsedLibrary.getMetaData().getGroups().ifPresent(sourceContext.getMetaData()::setGroups);

            BibDatabaseContext targetContext = new BibDatabaseContext();
            DBMSSynchronizer targetSynchronizer = newSynchronizer(targetContext);
            targetContext.convertToSharedDatabase(targetSynchronizer);

            try {
                long synchronizationStarted = System.nanoTime();
                targetSynchronizer.openSharedDatabase(targetConnection);
                long synchronizationDuration = System.nanoTime() - synchronizationStarted;

                long treeCreationStarted = System.nanoTime();
                GroupTreeViewModel groupTree = createGroupTree(targetContext);
                long treeCreationDuration = System.nanoTime() - treeCreationStarted;

                System.out.printf("entries=%d parseMs=%d sharedSynchronizationMs=%d groupTreeMs=%d%n",
                        targetContext.getDatabase().getEntryCount(),
                        Duration.ofNanos(parseDuration).toMillis(),
                        Duration.ofNanos(synchronizationDuration).toMillis(),
                        Duration.ofNanos(treeCreationDuration).toMillis());
                assertTrue(groupTree.rootGroupProperty().get() != null);
            } finally {
                sourceSynchronizer.closeSharedDatabase();
                targetSynchronizer.closeSharedDatabase();
            }
        }
    }

    private DatabaseConnection openConnection(PostgresServer postgres) {
        DatabaseConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("postgres")
                .setUser("postgres")
                .setPassword("postgres")
                .setUseSSL(false)
                .createDBMSConnectionProperties();
        return new DatabaseConnection() {
            @Override
            public DatabaseConnectionProperties getProperties() {
                return properties;
            }

            @Override
            public java.sql.Connection getConnection() {
                java.sql.Connection connection = postgres.getConnection();
                assertNotNull(connection);
                return connection;
            }

            @Override
            public java.sql.Connection openNewConnection() {
                java.sql.Connection connection = postgres.getConnection();
                assertNotNull(connection);
                return connection;
            }
        };
    }

    private void insertEntriesInBatches(BibDatabaseContext context, List<BibEntry> entries) {
        int batchSize = 1_000;
        for (int start = 0; start < entries.size(); start += batchSize) {
            context.getDatabase().insertEntries(entries.subList(start, Math.min(start + batchSize, entries.size())));
        }
    }

    private DBMSSynchronizer newSynchronizer(BibDatabaseContext context) {
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        return new DBMSSynchronizer(
                context,
                ',',
                fieldPreferences,
                GlobalCitationKeyPatterns.fromPattern("[auth][year]"),
                new DummyFileUpdateMonitor(),
                "PerformanceProfile");
    }

    private GroupTreeViewModel createGroupTree(BibDatabaseContext context) throws InterruptedException {
        StateManager stateManager = mock(JabRefGuiStateManager.class);
        OptionalObjectProperty<BibDatabaseContext> activeDatabase = OptionalObjectProperty.empty();
        activeDatabase.setValue(Optional.of(context));
        when(stateManager.activeDatabaseProperty()).thenReturn(activeDatabase);
        when(stateManager.getSearchContext(context)).thenReturn(new SearchContext(
                new SimpleBooleanProperty(false),
                NoOpSearchBackend::new,
                NoOpSearchBackend::new));
        when(stateManager.getSelectedGroups(context)).thenReturn(FXCollections.observableArrayList());
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getLibraryPreferences()).thenReturn(new LibraryPreferences(
                context.getMode(), false, false, false, "Imported entries"));
        when(preferences.getGroupsPreferences()).thenReturn(new GroupsPreferences(
                EnumSet.noneOf(GroupViewMode.class), true, true, false,
                GroupHierarchyType.INDEPENDENT, false));
        BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');

        CountDownLatch completed = new CountDownLatch(1);
        GroupTreeViewModel[] groupTree = new GroupTreeViewModel[1];
        Platform.runLater(() -> {
            groupTree[0] = new GroupTreeViewModel(
                    stateManager,
                    mock(BibEntryTypesManager.class),
                    preferences,
                    mock(DialogService.class),
                    mock(AiService.class),
                    new CustomLocalDragboard(),
                    new CurrentThreadTaskExecutor());
            completed.countDown();
        });
        assertTrue(completed.await(10, TimeUnit.MINUTES));
        return groupTree[0];
    }
}
