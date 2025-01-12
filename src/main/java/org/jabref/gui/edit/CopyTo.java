package org.jabref.gui.edit;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class CopyTo extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final CopyToPreferences copyToPreferences;
    // For later use
    private final List<String> checkedPaths;
    private final String path;

    public CopyTo(DialogService dialogService,
                  StateManager stateManager,
                  CopyToPreferences copyToPreferences,
                  List<String> checkedPaths,
                  String path) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.copyToPreferences = copyToPreferences;
        this.checkedPaths = checkedPaths;
        this.path = path;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        List<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        List<String> titles = selectedEntries.stream()
                .filter(entry -> entry.getTitle().isPresent())
                .map(entry -> entry.getTitle().get())
                .toList();

        boolean includeCrossReferences = askForCrossReferencedEntries();
        copyToPreferences.setShouldIncludeCrossReferences(includeCrossReferences);
    }

    private boolean askForCrossReferencedEntries() {
        if (copyToPreferences.getShouldAskForIncludingCrossReferences()) {
            return dialogService.showConfirmationDialogWithOptOutAndWait(
                    Localization.lang("Include or exclude cross-referenced entries"),
                    Localization.lang("Would you like to include cross-reference entries in the current operation?"),
                    Localization.lang("Include"),
                    Localization.lang("Exclude"),
                    Localization.lang("Do not ask again"),
                    optOut -> copyToPreferences.setShouldAskForIncludingCrossReferences(!optOut)
            );
        } else {
            return copyToPreferences.getShouldIncludeCrossReferences();
        }
    }
}
