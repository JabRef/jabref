package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.undo.UndoableRemoveEntries;
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

import com.google.common.eventbus.Subscribe;

public class SharedDatabaseUIManager {

    private final JabRefFrame jabRefFrame;
    private DatabaseSynchronizer dbmsSynchronizer;
    private final DialogService dialogService;

    public SharedDatabaseUIManager(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
        this.dialogService = jabRefFrame.getDialogService();
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
                jabRefFrame.closeCurrentTab();
                dialogService.showCustomDialogAndWait(new SharedDatabaseLoginDialogView(jabRefFrame));
            } else if (answer.get().equals(workOffline)) {
                connectionLostEvent.getBibDatabaseContext().convertToLocalDatabase();
                jabRefFrame.getLibraryTabs().forEach(tab -> tab.updateTabTitle(tab.isModified()));
                jabRefFrame.getDialogService().notify(Localization.lang("Working offline."));
            }
        } else {
            jabRefFrame.closeCurrentTab();
        }
    }

    @Subscribe
    public void listen(UpdateRefusedEvent updateRefusedEvent) {

        jabRefFrame.getDialogService().notify(Localization.lang("Update refused."));

        BibEntry localBibEntry = updateRefusedEvent.getLocalBibEntry();
        BibEntry sharedBibEntry = updateRefusedEvent.getSharedBibEntry();

        StringBuilder message = new StringBuilder();
        message.append(Localization.lang("Update could not be performed due to existing change conflicts."));
        message.append("\r\n");
        message.append(Localization.lang("You are not working on the newest version of BibEntry."));
        message.append("\r\n");
        message.append(Localization.lang("Shared version: %0", String.valueOf(sharedBibEntry.getSharedBibEntryData().getVersion())));
        message.append("\r\n");
        message.append(Localization.lang("Local version: %0", String.valueOf(localBibEntry.getSharedBibEntryData().getVersion())));
        message.append("\r\n");
        message.append(Localization.lang("Press \"Merge entries\" to merge the changes and resolve this problem."));
        message.append("\r\n");
        message.append(Localization.lang("Canceling this operation will leave your changes unsynchronized."));

        ButtonType merge = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.YES);

        Optional<ButtonType> response = dialogService.showCustomButtonDialogAndWait(AlertType.CONFIRMATION, Localization.lang("Update refused"), message.toString(), ButtonType.CANCEL, merge);

        if (response.isPresent() && response.get().equals(merge)) {
            MergeEntriesDialog dialog = new MergeEntriesDialog(localBibEntry, sharedBibEntry);
            Optional<BibEntry> mergedEntry = dialogService.showCustomDialogAndWait(dialog);

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
        LibraryTab libraryTab = jabRefFrame.getCurrentLibraryTab();
        EntryEditor entryEditor = libraryTab.getEntryEditor();

        libraryTab.getUndoManager().addEdit(new UndoableRemoveEntries(libraryTab.getDatabase(), event.getBibEntries()));

        if (Objects.nonNull(entryEditor) && (event.getBibEntries().contains(entryEditor.getEntry()))) {

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
        bibDatabaseContext.setMode(Globals.prefs.getGeneralPreferences().getDefaultBibDatabaseMode());
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(bibDatabaseContext, Globals.prefs.getKeywordDelimiter(), Globals.prefs.getGlobalCitationKeyPattern(), Globals.getFileUpdateMonitor());
        bibDatabaseContext.convertToSharedDatabase(synchronizer);

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        dbmsSynchronizer.registerListener(this);
        jabRefFrame.getDialogService().notify(Localization.lang("Connection to %0 server established.", dbmsConnectionProperties.getType().toString()));
        return jabRefFrame.addTab(bibDatabaseContext, true);
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
        bibDatabaseContext.setMode(Globals.prefs.getGeneralPreferences().getDefaultBibDatabaseMode());
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(bibDatabaseContext, Globals.prefs.getKeywordDelimiter(), Globals.prefs.getGlobalCitationKeyPattern(), Globals.getFileUpdateMonitor());
        bibDatabaseContext.convertToSharedDatabase(synchronizer);

        bibDatabaseContext.getDatabase().setSharedDatabaseID(sharedDatabaseID);
        bibDatabaseContext.setDatabasePath(parserResult.getDatabaseContext().getDatabasePath().orElse(null));

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        dbmsSynchronizer.registerListener(this);
        parserResult.setDatabaseContext(bibDatabaseContext);
        jabRefFrame.getDialogService().notify(Localization.lang("Connection to %0 server established.", dbmsConnectionProperties.getType().toString()));
    }
}
