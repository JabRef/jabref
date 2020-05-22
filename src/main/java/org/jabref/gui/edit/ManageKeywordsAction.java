package org.jabref.gui.edit;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.l10n.Localization;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * An Action for launching keyword managing dialog
 */
public class ManageKeywordsAction extends SimpleCommand {

    private final StateManager stateManager;

    public ManageKeywordsAction(StateManager stateManager) {
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(this.executable, "", Localization.lang("Select at least one entry to manage keywords.")));
    }

    @Override
    public void execute() {
        ManageKeywordsDialog dialog = new ManageKeywordsDialog(stateManager.getSelectedEntries());
        dialog.showAndWait();
    }
}
