package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSSynchronizer;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Drives [SharedDatabaseUIManager#connectInBackground] on the current thread with the connection boundary replaced.
class SharedDatabaseUIManagerTest extends ApplicationTest {

    private final DialogService dialogService = mock(DialogService.class);
    private final DBMSSynchronizer synchronizer = mock(DBMSSynchronizer.class);
    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private final List<LibraryTab> opened = new ArrayList<>();

    private TabPane tabPane;
    private SharedDatabasePlaceholderTab placeholder;
    private DBMSConnectionProperties connectionProperties;
    private Exception connectionFailure;
    private int attempts;
    private SharedDatabaseUIManager manager;

    @Override
    public void start(Stage stage) {
        tabPane = new TabPane();
        stage.setScene(new Scene(tabPane));
        stage.show();
    }

    @BeforeEach
    void setUp() {
        connectionProperties = mock(DBMSConnectionProperties.class);
        when(connectionProperties.getDatabase()).thenReturn("Literature");
        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(context.getDBMSSynchronizer()).thenReturn(synchronizer);

        manager = new SharedDatabaseUIManager(mock(LibraryTabContainer.class), dialogService, mock(GuiPreferences.class),
                mock(AiService.class), mock(StateManager.class), mock(BibEntryTypesManager.class), mock(FileUpdateMonitor.class),
                mock(ClipBoardManager.class), new CurrentThreadTaskExecutor(), mock(GitHandlerRegistry.class)) {
            @Override
            public BibDatabaseContext connect(DBMSConnectionProperties properties) throws SQLException, DatabaseNotSupportedException {
                attempts++;
                if (connectionFailure instanceof SQLException sqlException) {
                    throw sqlException;
                }
                if (connectionFailure instanceof DatabaseNotSupportedException notSupported) {
                    throw notSupported;
                }
                return context;
            }

            @Override
            public LibraryTab openTab(BibDatabaseContext bibDatabaseContext) {
                return libraryTab;
            }
        };
    }

    private void connectWithPlaceholder(String sharedDatabaseId) {
        interact(() -> {
            placeholder = new SharedDatabasePlaceholderTab(sharedDatabaseId, connectionProperties);
            tabPane.getTabs().add(placeholder);
            manager.connectInBackground(placeholder, connectionProperties, opened::add);
        });
    }

    @Test
    void successHandsTheOpenedTabToTheCallback() {
        connectWithPlaceholder(null);

        assertEquals(List.of(libraryTab), opened);
    }

    @Test
    void failureShowsTheErrorAndRetryConnectsAgain() {
        connectionFailure = new SQLException("Connection refused");
        connectWithPlaceholder(null);

        assertTrue(opened.isEmpty());
        assertFalse(placeholder.getContent().lookup(".button").isDisabled());

        connectionFailure = null;
        interact(() -> placeholder.retry());

        assertEquals(2, attempts);
        assertEquals(List.of(libraryTab), opened);
    }

    @Test
    void closedPlaceholderDiscardsTheResultAndClosesTheConnection() {
        interact(() -> {
            placeholder = new SharedDatabasePlaceholderTab(null, connectionProperties);
            manager.connectInBackground(placeholder, connectionProperties, opened::add);
        });

        assertTrue(opened.isEmpty());
        verify(synchronizer).closeSharedDatabase();
    }

    @Test
    void startupReconnectionFailureNotifiesInsteadOfShowingTheMigrationDialog() {
        connectionFailure = new DatabaseNotSupportedException();
        connectWithPlaceholder("shared-1");

        verify(dialogService).notify(anyString());
        verify(dialogService, never()).showCustomButtonDialogAndWait(any(), anyString(), anyString(), any(ButtonType[].class));
    }
}
