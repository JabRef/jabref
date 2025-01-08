package org.jabref.gui.groups;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupTreeNode;

public class AddToGroupAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;

    public AddToGroupAction(StateManager stateManager, DialogService dialogService) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            List<GroupTreeNode> groups = stateManager.getSelectedGroups(databaseContext);
            groups.forEach(groupTreeNode -> groupTreeNode.addEntriesToGroup(stateManager.getSelectedEntries()));
            dialogService.notify(Localization.lang("Added %0 entries to group(s) \"%1\"",
                    stateManager.getSelectedEntries().size(),
                    groups.stream().map(GroupTreeNode::getName).collect(Collectors.joining(","))));
        });
    }
}
