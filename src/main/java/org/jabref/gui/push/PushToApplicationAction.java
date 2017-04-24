package org.jabref.gui.push;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * An Action class representing the process of invoking a PushToApplication operation.
 */
class PushToApplicationAction extends AbstractAction implements Runnable {
    private final PushToApplication operation;
    private final JabRefFrame frame;
    private BasePanel panel;
    private List<BibEntry> entries;


    public PushToApplicationAction(JabRefFrame frame, PushToApplication operation) {
        this.frame = frame;
        putValue(Action.SMALL_ICON, operation.getIcon());
        putValue(Action.NAME, operation.getName());
        putValue(Action.SHORT_DESCRIPTION, operation.getTooltip());
        this.operation = operation;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        panel = frame.getCurrentBasePanel();

        // Check if a BasePanel exists:
        if (panel == null) {
            return;
        }

        // Check if any entries are selected:
        entries = panel.getSelectedEntries();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("This operation requires one or more entries to be selected."), (String) getValue(Action.NAME), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // If required, check that all entries have BibTeX keys defined:
        if (operation.requiresBibtexKeys()) {
            for (BibEntry entry : entries) {
                if (!(entry.getCiteKeyOptional().isPresent()) || entry.getCiteKeyOptional().get().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(frame,
                            Localization
                                    .lang("This operation requires all selected entries to have BibTeX keys defined."),
                            (String) getValue(Action.NAME), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        // All set, call the operation in a new thread:
        JabRefExecutorService.INSTANCE.execute(this);
    }

    @Override
    public void run() {
        // Do the operation:
        operation.pushEntries(panel.getDatabase(), entries, getKeyString(entries), panel.getBibDatabaseContext().getMetaData());

        // Call the operationCompleted() method on the event dispatch thread:
        SwingUtilities.invokeLater(() -> operation.operationCompleted(panel));
    }

    private static String getKeyString(List<BibEntry> bibentries) {
        StringBuilder result = new StringBuilder();
        Optional<String> citeKey;
        boolean first = true;
        for (BibEntry bes : bibentries) {
            citeKey = bes.getCiteKeyOptional();
            // if the key is empty we give a warning and ignore this entry
            // TODO: Give warning
            if (!(citeKey.isPresent()) || citeKey.get().isEmpty()) {
                continue;
            }
            if (first) {
                result.append(citeKey.get());
                first = false;
            } else {
                result.append(',').append(citeKey.get());
            }
        }
        return result.toString();
    }
}
