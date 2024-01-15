package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.mergeentries.EntriesMergeResult;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnection;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSSynchronizer;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.logic.shared.event.ConnectionLostEvent;
import org.jabref.logic.shared.event.SharedEntriesNotPresentEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;

public class SharedDatabaseUIManager {

    private final LibraryTabContainer tabContainer;
    private DatabaseSynchronizer dbmsSynchronizer;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final UndoManager undoManager;
    private final TaskExecutor taskExecutor;

    public SharedDatabaseUIManager(LibraryTabContainer tabContainer,
                                   DialogService dialogService,
                                   PreferencesService preferencesService,
                                   StateManager stateManager,
                                   BibEntryTypesManager entryTypesManager,
                                   FileUpdateMonitor fileUpdateMonitor,
                                   UndoManager undoManager,
                                   TaskExecutor taskExecutor) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.undoManager = undoManager;
        this.taskExecutor = taskExecutor;
    }

    @Subscribe
    public void listen(ConnectionLostEvent connectionLostEvent) {
        ButtonType reconnect = new ButtonType(Localization.lang("Reconnect"), ButtonData.YES);
        ButtonType workOffline = new ButtonType(Localization.lang("Work offline"), ButtonData.NO);
        ButtonType closeLibrary = new ButtonType(Localization.lang("Close library"), ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> answer = dialogService.showCustomButtonDialogAndWait(AlertType.WARNING,
                Localization.lang("Connection lost"),
                Localization.lang("The connection to the server has been terminated."),
                reconnect,
                workOffline,
                closeLibrary);

        if (answer.isPresent()) {
            if (answer.get().equals(reconnect)) {
                tabContainer.closeCurrentTab();
                dialogService.showCustomDialogAndWait(new SharedDatabaseLoginDialogView(tabContainer));
            } else if (answer.get().equals(workOffline)) {
                connectionLostEvent.getBibDatabaseContext().convertToLocalDatabase();
                tabContainer.getLibraryTabs().forEach(tab -> tab.updateTabTitle(tab.isModified()));
                dialogService.notify(Localization.lang("Working offline."));
            }
        } else {
            tabContainer.closeCurrentTab();
        }
    }

    @Subscribe
    public void listen(UpdateRefusedEvent updateRefusedEvent) {
        dialogService.notify(Localization.lang("Update refused."));

        BibEntry localBibEntry = updateRefusedEvent.getLocalBibEntry();
        BibEntry sharedBibEntry = updateRefusedEvent.getSharedBibEntry();

        String message = Localization.lang("Update could not be performed due to existing change conflicts.") + "\r\n" +
                Localization.lang("You are not working on the newest version of BibEntry.") + "\r\n" +
                Localization.lang("Shared version: %0", String.valueOf(sharedBibEntry.getSharedBibEntryData().getVersion())) + "\r\n" +
                Localization.lang("Local version: %0", String.valueOf(localBibEntry.getSharedBibEntryData().getVersion())) + "\r\n" +
                Localization.lang("Press \"Merge entries\" to merge the changes and resolve this problem.") + "\r\n" +
                Localization.lang("Canceling this operation will leave your changes unsynchronized.");

        ButtonType merge = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.YES);

        Optional<ButtonType> response = dialogService.showCustomButtonDialogAndWait(AlertType.CONFIRMATION, Localization.lang("Update refused"), message, ButtonType.CANCEL, merge);

        if (response.isPresent() && response.get().equals(merge)) {
            MergeEntriesDialog dialog = new MergeEntriesDialog(localBibEntry, sharedBibEntry, preferencesService);
            dialog.setTitle(Localization.lang("Update refused"));
            Optional<BibEntry> mergedEntry = dialogService.showCustomDialogAndWait(dialog).map(EntriesMergeResult::mergedEntry);

            mergedEntry.ifPresent(mergedBibEntry -> {
                mergedBibEntry.getSharedBibEntryData().setSharedID(sharedBibEntry.getSharedBibEntryData().getSharedID());
                mergedBibEntry.getSharedBibEntryData().setVersion(sharedBibEntry.getSharedBibEntryData().getVersion());

                dbmsSynchronizer.synchronizeSharedEntry(mergedBibEntry);
                dbmsSynchronizer.synchronizeLocalDatabase();
            });
        }
    }

    @Subscribe
    public void listen(SharedEntriesNotPresentEvent event) {
        LibraryTab libraryTab = tabContainer.getCurrentLibraryTab();
        EntryEditor entryEditor = libraryTab.getEntryEditor();

        libraryTab.getUndoManager().addEdit(new UndoableRemoveEntries(libraryTab.getDatabase(), event.getBibEntries()));

        if (entryEditor != null && (event.getBibEntries().contains(entryEditor.getEntry()))) {
            dialogService.showInformationDialogAndWait(Localization.lang("Shared entry is no longer present"),
                    Localization.lang("The entry you currently work on has been deleted on the shared side.")
                            + "\n"
                            + Localization.lang("You can restore the entry using the \"Undo\" operation."));
            libraryTab.closeBottomPane();
        }
    }

    /**
     * Opens a new shared database tab with the given {@link DBMSConnectionProperties}.
     *
     * @param dbmsConnectionProperties Connection data
     * @return BasePanel which also used by {@link SaveDatabaseAction}
     */
    public LibraryTab openNewSharedDatabaseTab(DBMSConnectionProperties dbmsConnectionProperties)
            throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException {

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setMode(preferencesService.getLibraryPreferences().getDefaultBibDatabaseMode());
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(
                bibDatabaseContext,
                preferencesService.getBibEntryPreferences().getKeywordSeparator(),
                preferencesService.getCitationKeyPatternPreferences().getKeyPattern(),
                fileUpdateMonitor);
        bibDatabaseContext.convertToSharedDatabase(synchronizer);

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        dbmsSynchronizer.registerListener(this);
        dialogService.notify(Localization.lang("Connection to %0 server established.", dbmsConnectionProperties.getType().toString()));

        LibraryTab libraryTab = LibraryTab.createLibraryTab(
                bibDatabaseContext,
                tabContainer,
                dialogService,
                preferencesService,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                taskExecutor);
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

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setMode(preferencesService.getLibraryPreferences().getDefaultBibDatabaseMode());
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(
                bibDatabaseContext,
                preferencesService.getBibEntryPreferences().getKeywordSeparator(),
                preferencesService.getCitationKeyPatternPreferences().getKeyPattern(),
                fileUpdateMonitor);
        bibDatabaseContext.convertToSharedDatabase(synchronizer);

        bibDatabaseContext.getDatabase().setSharedDatabaseID(sharedDatabaseID);
        bibDatabaseContext.setDatabasePath(parserResult.getDatabaseContext().getDatabasePath().orElse(null));

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        dbmsSynchronizer.registerListener(this);
        parserResult.setDatabaseContext(bibDatabaseContext);
        dialogService.notify(Localization.lang("Connection to %0 server established.", dbmsConnectionProperties.getType().toString()));
    }
}
