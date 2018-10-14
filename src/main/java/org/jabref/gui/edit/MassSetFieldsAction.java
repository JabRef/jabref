package org.jabref.gui.edit;

import java.util.List;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.entry.BibEntry;

/**
 * An Action for launching mass field.
 *
 * Functionality:
 * * Defaults to selected entries, or all entries if none are selected.
 * * Input field name
 * * Either set field, or clear field.
 */
public class MassSetFieldsAction extends SimpleCommand {

    private final JabRefFrame frame;

    public MassSetFieldsAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        BasePanel bp = frame.getCurrentBasePanel();
        if (bp == null) {
            return;
        }

        List<BibEntry> entries = bp.getSelectedEntries();
        MassSetFieldsDialog dialog = new MassSetFieldsDialog(entries, bp);
        dialog.showAndWait();
    }

}
