package org.jabref.gui.groups;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

/// Assigns the selected entries to a group chosen by name, so that entries can be sorted into
/// groups without leaving the keyboard.
///
/// Remembers the last group used, because sorting entries one by one mostly targets the same group.
@NullMarked
public class AddToGroupAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private Optional<GroupTreeNode> lastUsedGroup = Optional.empty();

    public AddToGroupAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (!isExecutable()) {
            return;
        }
        stateManager.getActiveDatabase().ifPresent(this::assignSelectedEntries);
    }

    private void assignSelectedEntries(BibDatabaseContext library) {
        library.getMetaData().getGroups()
               .filter(AddToGroupDialogViewModel::hasAssignableGroups)
               .ifPresentOrElse(
                       root -> showGroupPicker(library, root),
                       () -> dialogService.notify(Localization.lang("This library has no group to assign entries to.")));
    }

    // [impl->feat~groups.assign-entries-by-keyboard~1]
    private void showGroupPicker(BibDatabaseContext library, GroupTreeNode root) {
        List<BibEntry> entries = List.copyOf(stateManager.getSelectedEntries());
        AddToGroupDialogViewModel viewModel = new AddToGroupDialogViewModel(root, entries, stateManager.getUndoManager(library), lastUsedGroup);

        dialogService.showCustomDialogAndWait(new AddToGroupDialog(viewModel))
                     .filter(Boolean::booleanValue)
                     .flatMap(_ -> viewModel.assignEntriesToSelectedGroup())
                     .ifPresent(this::rememberAssignedGroup);
    }

    private void rememberAssignedGroup(GroupTreeNode group) {
        lastUsedGroup = Optional.of(group);
        dialogService.notify(Localization.lang("Assigned entries to group \"%0\".", group.getName()));
    }
}
