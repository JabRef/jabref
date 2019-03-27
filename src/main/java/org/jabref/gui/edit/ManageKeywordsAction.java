package org.jabref.gui.edit;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

/**
 * An Action for launching keyword managing dialog
 *
 */
public class ManageKeywordsAction extends SimpleCommand {

    private final JabRefFrame frame;

    public ManageKeywordsAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        BasePanel basePanel = frame.getCurrentBasePanel();
        if (basePanel == null) {
            return;
        }
        if (basePanel.getSelectedEntries().isEmpty()) {
            basePanel.output(Localization.lang("Select at least one entry to manage keywords."));
            return;
        }

        ManageKeywordsDialog dialog = new ManageKeywordsDialog(basePanel.getSelectedEntries());
        dialog.showAndWait();
    }
}
