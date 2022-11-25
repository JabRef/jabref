package org.jabref.gui.sidepane;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupModeViewModel;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;

public class GroupsSidePaneComponent extends SidePaneComponent {
    private final GroupsPreferences groupsPreferences;
    private final DialogService dialogService;
    private final Button intersectionUnionToggle = IconTheme.JabRefIcons.GROUP_INTERSECTION.asButton();

    public GroupsSidePaneComponent(SimpleCommand closeCommand,
                                   SimpleCommand moveUpCommand,
                                   SimpleCommand moveDownCommand,
                                   SidePaneContentFactory contentFactory,
                                   GroupsPreferences groupsPreferences,
                                   DialogService dialogService) {
        super(SidePaneType.GROUPS, closeCommand, moveUpCommand, moveDownCommand, contentFactory);
        this.groupsPreferences = groupsPreferences;
        this.dialogService = dialogService;
        setupIntersectionUnionToggle();

        EasyBind.subscribe(groupsPreferences.groupViewModeProperty(), mode -> {
            GroupModeViewModel modeViewModel = new GroupModeViewModel(mode);
            intersectionUnionToggle.setGraphic(modeViewModel.getUnionIntersectionGraphic());
            intersectionUnionToggle.setTooltip(modeViewModel.getUnionIntersectionTooltip());
        });
    }

    private void setupIntersectionUnionToggle() {
        addExtraButtonToHeader(intersectionUnionToggle, 0);
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
