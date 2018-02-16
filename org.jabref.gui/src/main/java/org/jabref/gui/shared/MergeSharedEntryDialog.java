package org.jabref.gui.shared;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.mergeentries.MergeEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DatabaseSynchronizer;
import org.jabref.model.entry.BibEntry;

public class MergeSharedEntryDialog {

    private final JabRefFrame jabRefFrame;
    private final DatabaseSynchronizer dbmsSynchronizer;
    private final BibEntry localBibEntry;
    private final BibEntry sharedBibEntry;
    private final JDialog mergeDialog;
    private final MergeEntries mergeEntries;


    public MergeSharedEntryDialog(JabRefFrame jabRefFrame, DatabaseSynchronizer dbmsSynchronizer, BibEntry localBibEntry,
                                  BibEntry sharedBibEntry, BibDatabaseMode bibDatabaseMode) {
        this.jabRefFrame = jabRefFrame;
        this.dbmsSynchronizer = dbmsSynchronizer;
        this.localBibEntry = localBibEntry;
        this.sharedBibEntry = sharedBibEntry;
        this.mergeDialog = new JDialog(jabRefFrame, Localization.lang("Update refused"), true);
        this.mergeEntries = new MergeEntries(sharedBibEntry, localBibEntry, Localization.lang("Shared entry"),
                Localization.lang("Local entry"), bibDatabaseMode);
    }

    public void showMergeDialog() {

        mergeDialog.setMinimumSize(new Dimension(600, 600));

        StringBuilder message = new StringBuilder();
        message.append("<html>");
        message.append("<b>");
        message.append(Localization.lang("Update could not be performed due to existing change conflicts."));
        message.append("</b>");
        message.append("<br/><br/>");
        message.append(Localization.lang("You are not working on the newest version of BibEntry."));
        message.append("<br/><br/>");
        message.append(Localization.lang("Shared version: %0", String.valueOf(sharedBibEntry.getSharedBibEntryData().getVersion())));
        message.append("<br/>");
        message.append(Localization.lang("Local version: %0", String.valueOf(localBibEntry.getSharedBibEntryData().getVersion())));
        message.append("<br/><br/>");
        message.append(Localization.lang("Please merge the shared entry with yours and press \"Merge entries\" to resolve this problem."));
        message.append("<br/>");

        JLabel mergeInnformation = new JLabel(message.toString());
        mergeInnformation.setBorder(new EmptyBorder(9, 9, 9, 9));

        mergeDialog.add(mergeInnformation, BorderLayout.NORTH);
        mergeDialog.add(mergeEntries.getMergeEntryPanel(), BorderLayout.CENTER);

        JButton mergeButton = new JButton(Localization.lang("Merge entries"));
        mergeButton.addActionListener(e -> mergeEntries());

        JButton cancelButton = new JButton(Localization.lang("Cancel"));
        cancelButton.addActionListener(e -> showConfirmationDialog());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(mergeButton, BorderLayout.WEST);
        buttonPanel.add(cancelButton, BorderLayout.EAST);

        mergeDialog.add(buttonPanel, BorderLayout.SOUTH);
        mergeDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mergeDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showConfirmationDialog();
            }
        });

        mergeDialog.setLocationRelativeTo(jabRefFrame);
        mergeDialog.pack();
        mergeDialog.setVisible(true);
    }

    private void showConfirmationDialog() {
        int answer = JOptionPane.showConfirmDialog(mergeDialog,
                Localization.lang("Canceling this operation will leave your changes unsynchronized. Cancel anyway?"),
                Localization.lang("Warning"), JOptionPane.YES_NO_OPTION);

        if (answer == 0) {
            mergeDialog.dispose();
        }
    }

    private void mergeEntries() {
        BibEntry mergedBibEntry = mergeEntries.getMergeEntry();
        mergedBibEntry.getSharedBibEntryData().setSharedID(sharedBibEntry.getSharedBibEntryData().getSharedID());
        mergedBibEntry.getSharedBibEntryData().setVersion(sharedBibEntry.getSharedBibEntryData().getVersion());

        mergeDialog.dispose(); // dispose before synchronizing to avoid multiple merge windows in case of new conflict.

        dbmsSynchronizer.synchronizeSharedEntry(mergedBibEntry);
        dbmsSynchronizer.synchronizeLocalDatabase();
    }
}
