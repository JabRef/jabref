package net.sf.jabref.gui;

import javax.swing.JOptionPane;

import net.sf.jabref.event.RemoteConnectionLostEvent;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.DatabaseLocation;

import com.google.common.eventbus.Subscribe;

public class RemoteConnectionLostDialog {

    private final JabRefFrame jabRefFrame;


    public RemoteConnectionLostDialog(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Subscribe
    public void listen(RemoteConnectionLostEvent connectionLostEvent) {

        jabRefFrame.output(Localization.lang("Connection lost."));

        String[] options = {Localization.lang("Reconnect"), Localization.lang("Work offline"),
                Localization.lang("Close database")};

        int answer = JOptionPane.showOptionDialog(jabRefFrame,
                Localization.lang("The connection to the server has been determinated.") + "\n\n",
                Localization.lang("Connection lost"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options,
                options[0]);

        if (answer == 0) {
            jabRefFrame.closeCurrentTab();
            OpenRemoteDatabaseDialog openRemoteDBDialog = new OpenRemoteDatabaseDialog(jabRefFrame);
            openRemoteDBDialog.setLocationRelativeTo(jabRefFrame);
            openRemoteDBDialog.setVisible(true);
        } else if (answer == 1) {
            connectionLostEvent.getBibDatabaseContext().updateDatabaseLocation(DatabaseLocation.LOCAL);
            jabRefFrame.refreshTitleAndTabs();
            jabRefFrame.updateEnabledState();
            jabRefFrame.output(Localization.lang("Working offline."));
        } else {
            jabRefFrame.closeCurrentTab();
        }
    }
}
