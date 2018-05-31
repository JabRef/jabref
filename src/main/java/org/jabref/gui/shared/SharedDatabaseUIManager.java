package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnection;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSSynchronizer;
import org.jabref.logic.shared.event.ConnectionLostEvent;
import org.jabref.logic.shared.event.SharedEntryNotPresentEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.model.database.shared.DatabaseSynchronizer;

import com.google.common.eventbus.Subscribe;

public class SharedDatabaseUIManager {

    private final JabRefFrame jabRefFrame;
    private DatabaseSynchronizer dbmsSynchronizer;

    public SharedDatabaseUIManager(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Subscribe
    public void listen(ConnectionLostEvent connectionLostEvent) {

        jabRefFrame.output(Localization.lang("Connection lost."));

        String[] options = {Localization.lang("Reconnect"), Localization.lang("Work offline"),
                Localization.lang("Close library")};

        int answer = JOptionPane.showOptionDialog(jabRefFrame,
                Localization.lang("The connection to the server has been terminated.") + "\n\n",
                Localization.lang("Connection lost"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);

        if (answer == 0) {
            jabRefFrame.closeCurrentTab();
            ConnectToSharedDatabaseDialog connectToSharedDatabaseDialog = new ConnectToSharedDatabaseDialog(jabRefFrame);
            connectToSharedDatabaseDialog.setVisible(true);
        } else if (answer == 1) {
            connectionLostEvent.getBibDatabaseContext().convertToLocalDatabase();
            jabRefFrame.refreshTitleAndTabs();
            jabRefFrame.updateEnabledState();
            jabRefFrame.output(Localization.lang("Working offline."));
        } else {
            jabRefFrame.closeCurrentTab();
        }
    }

    @Subscribe
    public void listen(UpdateRefusedEvent updateRefusedEvent) {

        jabRefFrame.output(Localization.lang("Update refused."));

        new MergeSharedEntryDialog(jabRefFrame, dbmsSynchronizer, updateRefusedEvent.getLocalBibEntry(),
                updateRefusedEvent.getSharedBibEntry(),
                    updateRefusedEvent.getBibDatabaseContext().getMode()).showMergeDialog();
    }

    @Subscribe
    public void listen(SharedEntryNotPresentEvent event) {
        BasePanel panel = jabRefFrame.getCurrentBasePanel();
        EntryEditor entryEditor = panel.getEntryEditor();

        panel.getUndoManager().addEdit(new UndoableRemoveEntry(panel.getDatabase(), event.getBibEntry(), panel));

        if (Objects.nonNull(entryEditor) && (entryEditor.getEntry() == event.getBibEntry())) {
            JOptionPane.showMessageDialog(jabRefFrame,
                    Localization.lang("The BibEntry you currently work on has been deleted on the shared side.")
                            + "\n" + Localization.lang("You can restore the entry using the \"Undo\" operation."),
                    Localization.lang("Shared entry is no longer present"), JOptionPane.INFORMATION_MESSAGE);

            SwingUtilities.invokeLater(() -> panel.hideBottomComponent());
        }
    }

    /**
     * Opens a new shared database tab with the given {@link DBMSConnectionProperties}.
     *
     * @param dbmsConnectionProperties Connection data
     * @param raiseTab If <code>true</code> the new tab gets selected.
     * @return BasePanel which also used by {@link SaveDatabaseAction}
     */
    public BasePanel openNewSharedDatabaseTab(DBMSConnectionProperties dbmsConnectionProperties)
            throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException {
        JabRefFrame frame = JabRefGUI.getMainFrame();
        BibDatabaseMode selectedMode = Globals.prefs.getDefaultBibDatabaseMode();
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new Defaults(selectedMode));
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(bibDatabaseContext, Globals.prefs.getKeywordDelimiter(), Globals.prefs.getKeyPattern(), Globals.getFileUpdateMonitor());
        bibDatabaseContext.convertToSharedDatabase(synchronizer);

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        dbmsSynchronizer.registerListener(this);
        frame.output(Localization.lang("Connection to %0 server established.", dbmsConnectionProperties.getType().toString()));
        return frame.addTab(bibDatabaseContext, true);
    }

    public void openSharedDatabaseFromParserResult(ParserResult parserResult)
            throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException,
            NotASharedDatabaseException {

        Optional<String> sharedDatabaseIDOptional = parserResult.getDatabase().getSharedDatabaseID();

        if (!sharedDatabaseIDOptional.isPresent()) {
            throw new NotASharedDatabaseException();
        }

        String sharedDatabaseID = sharedDatabaseIDOptional.get();
        DBMSConnectionProperties dbmsConnectionProperties = new DBMSConnectionProperties(new SharedDatabasePreferences(sharedDatabaseID));

        JabRefFrame frame = JabRefGUI.getMainFrame();
        BibDatabaseMode selectedMode = Globals.prefs.getDefaultBibDatabaseMode();
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new Defaults(selectedMode));
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(bibDatabaseContext, Globals.prefs.getKeywordDelimiter(), Globals.prefs.getKeyPattern(), Globals.getFileUpdateMonitor());
        bibDatabaseContext.convertToSharedDatabase(synchronizer);

        bibDatabaseContext.getDatabase().setSharedDatabaseID(sharedDatabaseID);
        bibDatabaseContext.setDatabaseFile(parserResult.getDatabaseContext().getDatabaseFile().orElse(null));

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(new DBMSConnection(dbmsConnectionProperties));
        dbmsSynchronizer.registerListener(this);
        parserResult.setDatabaseContext(bibDatabaseContext);
        frame.output(Localization.lang("Connection to %0 server established.", dbmsConnectionProperties.getType().toString()));
    }
}
