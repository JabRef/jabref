package org.jabref.gui.sidepane;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupModeViewModel;
import org.jabref.gui.groups.GroupTreeViewModel;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;
import org.jabref.preferences.PreferencesService;

public class GroupsSidePaneComponent extends SidePaneComponent {
    private final GroupsPreferences groupsPreferences;
    private final DialogService dialogService;

    private final GroupTreeViewModel viewModel;
    private final Button intersectionUnionToggle = IconTheme.JabRefIcons.GROUP_INTERSECTION.asButton();

    private final Button addGroup = IconTheme.JabRefIcons.ADD.asButton();

    public GroupsSidePaneComponent(SimpleCommand closeCommand,
                                   SimpleCommand moveUpCommand,
                                   SimpleCommand moveDownCommand,
                                   SidePaneContentFactory contentFactory,
                                   GroupsPreferences groupsPreferences,
                                   TaskExecutor taskExecutor,
                                   StateManager stateManager,
                                   PreferencesService preferencesService,
                                   DialogService dialogService) {
        super(SidePaneType.GROUPS, closeCommand, moveUpCommand, moveDownCommand, contentFactory);
        this.groupsPreferences = groupsPreferences;
        this.dialogService = dialogService;
        this.viewModel = new GroupTreeViewModel(stateManager, dialogService, preferencesService, taskExecutor, stateManager.getLocalDragboard());
        setupAddGroupButton();
        setupIntersectionUnionToggle();

        EasyBind.subscribe(groupsPreferences.groupViewModeProperty(), mode -> {
            GroupModeViewModel modeViewModel = new GroupModeViewModel(mode);
            addGroup.setGraphic(modeViewModel.getAddGroupGraphic());
            addGroup.setTooltip(modeViewModel.getAddGroupTooltip());
            intersectionUnionToggle.setGraphic(modeViewModel.getUnionIntersectionGraphic());
            intersectionUnionToggle.setTooltip(modeViewModel.getUnionIntersectionTooltip());
        });
    }

    private void setupAddGroupButton() {
        addExtraButtonToHeader(addGroup, 0);
        addGroup.setOnAction(event -> addNewGroup());
    }

    private void addNewGroup() {
        viewModel.addNewGroupToRoot();
    }

    private void setupIntersectionUnionToggle() {
        addExtraButtonToHeader(intersectionUnionToggle, 1);
        intersectionUnionToggle.setOnAction(event -> new ToggleUnionIntersectionAction().execute());
    }

    private class ToggleUnionIntersectionAction extends SimpleCommand {

        @Override
        public void execute() {
            GroupViewMode mode = groupsPreferences.getGroupViewMode();

            if (mode == GroupViewMode.UNION) {
                groupsPreferences.setGroupViewMode(GroupViewMode.INTERSECTION);
                dialogService.notify(Localization.lang("Group view mode set to intersection"));
            } else if (mode == GroupViewMode.INTERSECTION) {
                groupsPreferences.setGroupViewMode(GroupViewMode.UNION);
                dialogService.notify(Localization.lang("Group view mode set to union"));
            }
        }
    }
}
