package net.sf.jabref.gui.shared;

import java.util.Objects;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.shared.DBMSSynchronizer;
import net.sf.jabref.shared.event.ConnectionLostEvent;
import net.sf.jabref.shared.event.SharedEntryNotPresentEvent;
import net.sf.jabref.shared.event.UpdateRefusedEvent;

import com.google.common.eventbus.Subscribe;

public class SharedDatabaseUIManager {

    private final JabRefFrame jabRefFrame;
    private final DBMSSynchronizer dbmsSynchronizer;
    private final String keywordSeparator;

    public SharedDatabaseUIManager(JabRefFrame jabRefFrame, String keywordSeparator) {
        this.jabRefFrame = jabRefFrame;
        this.dbmsSynchronizer = jabRefFrame.getCurrentBasePanel().getBibDatabaseContext().getDBMSSynchronizer();
        this.keywordSeparator = keywordSeparator;
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
            OpenSharedDatabaseDialog openSharedDatabaseDialog = new OpenSharedDatabaseDialog(jabRefFrame);
            openSharedDatabaseDialog.setVisible(true);
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
}
