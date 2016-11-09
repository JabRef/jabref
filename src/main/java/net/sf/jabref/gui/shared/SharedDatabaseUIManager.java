package net.sf.jabref.gui.shared;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.DatabaseLocation;
import net.sf.jabref.shared.DBMSConnectionProperties;
import net.sf.jabref.shared.DBMSSynchronizer;
import net.sf.jabref.shared.event.ConnectionLostEvent;
import net.sf.jabref.shared.event.SharedEntryNotPresentEvent;
import net.sf.jabref.shared.event.UpdateRefusedEvent;
import net.sf.jabref.shared.exception.DatabaseNotSupportedException;
import net.sf.jabref.shared.exception.InvalidDBMSConnectionPropertiesException;
import net.sf.jabref.shared.exception.NotASharedDatabaseException;
import net.sf.jabref.shared.prefs.SharedDatabasePreferences;

import com.google.common.eventbus.Subscribe;

public class SharedDatabaseUIManager {

    private final JabRefFrame jabRefFrame;
    private DBMSSynchronizer dbmsSynchronizer;

    public SharedDatabaseUIManager(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Subscribe
    public void listen(ConnectionLostEvent connectionLostEvent) {

        jabRefFrame.output(Localization.lang("Connection lost."));

        String[] options = {Localization.lang("Reconnect"), Localization.lang("Work offline"),
                Localization.lang("Close database")};

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
        EntryEditor entryEditor = panel.getCurrentEditor();

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
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new Defaults(selectedMode), DatabaseLocation.SHARED,
                Globals.prefs.getKeywordDelimiter(), Globals.prefs.getKeyPattern());

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(dbmsConnectionProperties);
        dbmsSynchronizer.registerListener(this);
        frame.output(Localization.lang("Connection_to_%0_server_established.", dbmsConnectionProperties.getType().toString()));
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
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new Defaults(selectedMode), DatabaseLocation.SHARED,
                Globals.prefs.getKeywordDelimiter(), Globals.prefs.getKeyPattern());

        bibDatabaseContext.getDatabase().setSharedDatabaseID(sharedDatabaseID);
        bibDatabaseContext.setDatabaseFile(parserResult.getDatabaseContext().getDatabaseFile().orElse(null));

        dbmsSynchronizer = bibDatabaseContext.getDBMSSynchronizer();
        dbmsSynchronizer.openSharedDatabase(dbmsConnectionProperties);
        dbmsSynchronizer.registerListener(this);
        parserResult.setDatabaseContext(bibDatabaseContext);
        frame.output(Localization.lang("Connection_to_%0_server_established.", dbmsConnectionProperties.getType().toString()));
    }
}
