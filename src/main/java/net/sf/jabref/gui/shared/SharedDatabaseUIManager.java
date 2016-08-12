package net.sf.jabref.gui.shared;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.DatabaseLocation;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.shared.DBMSSynchronizer;
import net.sf.jabref.shared.event.ConnectionLostEvent;
import net.sf.jabref.shared.event.SharedEntryNotPresentEvent;
import net.sf.jabref.shared.event.UpdateRefusedEvent;

import com.google.common.eventbus.Subscribe;

public class SharedDatabaseUIManager {

    private final JabRefFrame jabRefFrame;
    private final DBMSSynchronizer dbmsSynchronizer;

    public SharedDatabaseUIManager(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
        this.dbmsSynchronizer = jabRefFrame.getCurrentBasePanel().getBibDatabaseContext().getDBSynchronizer();
    }

    @Subscribe
    public void listen(ConnectionLostEvent connectionLostEvent) {

        jabRefFrame.output(Localization.lang("Connection lost."));

        String[] options = {Localization.lang("Reconnect"), Localization.lang("Work offline"),
                Localization.lang("Close database")};

        int answer = JOptionPane.showOptionDialog(jabRefFrame,
                Localization.lang("The connection to the server has been determinated.") + "\n\n",
                Localization.lang("Connection lost"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);

        if (answer == 0) {
            jabRefFrame.closeCurrentTab();
            OpenSharedDatabaseDialog openSharedDatabaseDialog = new OpenSharedDatabaseDialog(jabRefFrame);
            openSharedDatabaseDialog.setVisible(true);
        } else if (answer == 1) {
            connectionLostEvent.getBibDatabaseContext().updateDatabaseLocation(DatabaseLocation.LOCAL);
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
    public void listen(SharedEntryNotPresentEvent sharedEntryNotPresentEvent) {
        BibEntry bibEntry = sharedEntryNotPresentEvent.getBibEntry();

        String[] options = {Localization.lang("Keep"), Localization.lang("Delete")};

        int answer = JOptionPane.showOptionDialog(jabRefFrame,
                Localization.lang("The BibEntry you currently work on has been deleted on the shared side. "
                        + "Hit \"Keep\" to recover the entry.") + "\n\n",
                Localization.lang("Update refused"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (answer == 0) {
            dbmsSynchronizer.getDBProcessor().insertEntry(bibEntry);
        } else if (answer == 1) {
            jabRefFrame.getCurrentBasePanel().hideBottomComponent();
        }
        dbmsSynchronizer.pullChanges();
    }

}