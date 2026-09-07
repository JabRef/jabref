package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.mergeentries.threewaymerge.EntriesMergeResult;
import org.jabref.gui.mergeentries.threewaymerge.MergeEntriesDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnection;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSSynchronizer;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.logic.shared.event.ConnectionLostEvent;
import org.jabref.logic.shared.event.SharedEntriesNotPresentEvent;
import org.jabref.logic.shared.event.SharedWriteFailedEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.undo.UndoableRemoveEntries;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.eventbus.Subscribe;

public class SharedDatabaseUIManager {

    private final LibraryTabContainer tabContainer;
    private DatabaseSynchronizer dbmsSynchronizer;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final AiService aiService;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;

    public SharedDatabaseUIManager(LibraryTabContainer tabContainer,
                                   DialogService dialogService,
                                   GuiPreferences preferences,
                                   AiService aiService,
                                   StateManager stateManager,
                                   BibEntryTypesManager entryTypesManager,
                                   FileUpdateMonitor fileUpdateMonitor,
                                   ClipBoardManager clipBoardManager,
                                   TaskExecutor taskExecutor,
                                   GitHandlerRegistry gitHandlerRegistry) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;
    }

    @Subscribe
    public void listen(ConnectionLostEvent connectionLostEvent) {
        // Shared-database events are posted from background threads
        UiTaskExecutor.runNowOrInJavaFXThread(() -> handleConnectionLost(connectionLostEvent));
    }

    private void handleConnectionLost(ConnectionLostEvent connectionLostEvent) {
        BibDatabaseContext bibDatabaseContext = connectionLostEvent.bibDatabaseContext();
        if (bibDatabaseContext.getLocation() != DatabaseLocation.SHARED) {
            // Already handled - the connection loss is reported by every failing operation
            return;
        }
        ButtonType reconnect = new ButtonType(Localization.lang("Reconnect"), ButtonData.YES);
        ButtonType workOffline = new ButtonType(Localization.lang("Work offline"), ButtonData.NO);
        ButtonType closeLibrary = new ButtonType(Localization.lang("Close library"), ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> answer = dialogService.showCustomButtonDialogAndWait(AlertType.WARNING,
                Localization.lang("Connection lost"),
                Localization.lang("The connection to the server has been terminated."),
                reconnect,
                workOffline,
                closeLibrary);

        // The affected tab is not necessarily the active one (several shared libraries may be open)
        Optional<LibraryTab> affectedTab = tabContainer.getLibraryTabs().stream()
                                                       .filter(tab -> tab.getBibDatabaseContext() == bibDatabaseContext)
                                                       .findFirst();
        if (answer.isPresent() && answer.get().equals(workOffline)) {
            // Same teardown as closing the tab - otherwise the notification listener keeps
            // reconnecting and would pull the shared state into the now local library
            bibDatabaseContext.convertToLocalDatabase();
            bibDatabaseContext.getDBMSSynchronizer().closeSharedDatabase();
            bibDatabaseContext.clearDBMSSynchronizer();
            affectedTab.ifPresent(tab -> tab.updateTabTitle(tab.isModified()));
            dialogService.notify(Localization.lang("Working offline."));
            return;
        }
        affectedTab.ifPresent(tabContainer::closeTab);
        if (answer.isPresent() && answer.get().equals(reconnect)) {
            dialogService.showCustomDialogAndWait(new SharedDatabaseLoginDialogView(tabContainer));
        }
    }

    @Subscribe
    public void listen(SharedWriteFailedEvent event) {
        // notify() marshals to the JavaFX thread itself
        dialogService.notify(Localization.lang("Could not save changes to the shared database. The latest changes are not synchronized."));
    }

    @Subscribe
    public void listen(UpdateRefusedEvent updateRefusedEvent) {
        UiTaskExecutor.runNowOrInJavaFXThread(() -> handleUpdateRefused(updateRefusedEvent));
    }

    // [impl->req~shared-database.conflict-merge-dialog~1]
    private void handleUpdateRefused(UpdateRefusedEvent updateRefusedEvent) {
        dialogService.notify(Localization.lang("Update refused."));

        BibEntry localBibEntry = updateRefusedEvent.localBibEntry();
        BibEntry sharedBibEntry = updateRefusedEvent.sharedBibEntry();

        String message = Localization.lang("Update could not be performed due to existing change conflicts.") + "\r\n" +
                Localization.lang("You are not working on the newest version of the entry.") + "\r\n" +
                Localization.lang("Shared version: %0", String.valueOf(sharedBibEntry.getSharedBibEntryData().getVersion())) + "\r\n" +
                Localization.lang("Local version: %0", String.valueOf(localBibEntry.getSharedBibEntryData().getVersion())) + "\r\n" +
                Localization.lang("Press \"Merge entries\" to merge the changes and resolve this problem.") + "\r\n" +
                Localization.lang("Canceling this operation will leave your changes unsynchronized.");

        ButtonType merge = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.YES);

        Optional<ButtonType> response = dialogService.showCustomButtonDialogAndWait(AlertType.CONFIRMATION, Localization.lang("Update refused"), message, ButtonType.CANCEL, merge);

        if (response.isPresent() && response.get().equals(merge)) {
            MergeEntriesDialog dialog = new MergeEntriesDialog(localBibEntry, sharedBibEntry, preferences, stateManager);
            dialog.setTitle(Localization.lang("Update refused"));
            Optional<BibEntry> mergedEntry = dialogService.showCustomDialogAndWait(dialog).map(EntriesMergeResult::mergedEntry);

            mergedEntry.ifPresent(mergedBibEntry -> {
                mergedBibEntry.getSharedBibEntryData().setSharedId(sharedBibEntry.getSharedBibEntryData().getSharedIdAsString());
                mergedBibEntry.getSharedBibEntryData().setVersion(sharedBibEntry.getSharedBibEntryData().getVersion());

                DatabaseSynchronizer synchronizer = updateRefusedEvent.bibDatabaseContext().getDBMSSynchronizer();
                synchronizer.synchronizeSharedEntry(mergedBibEntry);
                synchronizer.synchronizeLocalDatabase();
            });
        }
    }

    @Subscribe
    public void listen(SharedEntriesNotPresentEvent event) {
        UiTaskExecutor.runNowOrInJavaFXThread(() -> handleSharedEntriesNotPresent(event));
    }

    /// Another client removed entries. Live synchronization makes this an everyday event, so it
    /// is reported without interrupting the user; the undo edit keeps the entries' last local
    /// state (including unsynchronized edits) recoverable.
    private void handleSharedEntriesNotPresent(SharedEntriesNotPresentEvent event) {
        BibDatabaseContext databaseContext = event.bibDatabaseContext();
        stateManager.getUndoManager(databaseContext)
                    .addEdit(new UndoableRemoveEntries(databaseContext.getDatabase(), event.bibEntries()));
        dialogService.notify(Localization.lang("%0 entries were deleted on the shared side. Use \"Undo\" to restore them.",
                String.valueOf(event.bibEntries().size())));
    }

    /// Opens a new shared database tab with the given [DBMSConnectionProperties].
    ///
    /// @param dbmsConnectionProperties Connection data
    /// @return BasePanel which also used by [org.jabref.gui.exporter.SaveDatabaseAction]
    /// Connects and loads the shared database. Blocks on the network, so call it off the JavaFX thread and hand the
    /// result to [#openTab(BibDatabaseContext)].
    public BibDatabaseContext connect(DBMSConnectionProperties dbmsConnectionProperties)
            throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException {
        BibDatabaseContext bibDatabaseContext = getBibDatabaseContextForSharedDatabase();
        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        return bibDatabaseContext;
    }

    /// Shows a database returned by [#connect(DBMSConnectionProperties)] in a new tab. JavaFX thread only.
    public LibraryTab openTab(BibDatabaseContext bibDatabaseContext) {
        dbmsSynchronizer.registerListener(this);
        dialogService.notify(Localization.lang("Connection to %0 server established.", dbmsSynchronizer.getConnectionProperties().getType().toString()));

        LibraryTab libraryTab = LibraryTab.createLibraryTab(
                bibDatabaseContext,
                tabContainer,
                dialogService,
                aiService,
                preferences,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                clipBoardManager,
                taskExecutor,
                gitHandlerRegistry);
        tabContainer.addTab(libraryTab, true);
        return libraryTab;
    }

    public void openSharedDatabaseFromParserResult(ParserResult parserResult)
            throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException,
            NotASharedDatabaseException {

        Optional<String> sharedDatabaseIDOptional = parserResult.getDatabase().getSharedDatabaseID();

        if (sharedDatabaseIDOptional.isEmpty()) {
            throw new NotASharedDatabaseException();
        }

        String sharedDatabaseID = sharedDatabaseIDOptional.get();
        DBMSConnectionProperties dbmsConnectionProperties = new DBMSConnectionProperties(new SharedDatabasePreferences(sharedDatabaseID));

        BibDatabaseContext bibDatabaseContext = getBibDatabaseContextForSharedDatabase();

        bibDatabaseContext.getDatabase().setSharedDatabaseID(sharedDatabaseID);
        bibDatabaseContext.setDatabasePath(parserResult.getDatabaseContext().getDatabasePath().orElse(null));

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        dbmsSynchronizer.registerListener(this);
        dialogService.notify(Localization.lang("Connection to %0 server established.", dbmsConnectionProperties.getType().toString()));

        parserResult.setDatabaseContext(bibDatabaseContext);
    }

    private BibDatabaseContext getBibDatabaseContextForSharedDatabase() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setMode(preferences.getLibraryPreferences().getDefaultBibDatabaseMode());
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(
                bibDatabaseContext,
                preferences.getBibEntryPreferences().getKeywordSeparator(),
                preferences.getFieldPreferences(),
                preferences.getCitationKeyPatternPreferences().getKeyPatterns(),
                fileUpdateMonitor,
                preferences.getFilePreferences().getUserAndHost(),
                UiTaskExecutor::runNowOrInJavaFXThread);
        bibDatabaseContext.convertToSharedDatabase(synchronizer);
        return bibDatabaseContext;
    }
}
