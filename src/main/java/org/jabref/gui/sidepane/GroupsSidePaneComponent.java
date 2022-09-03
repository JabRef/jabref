package org.jabref.gui.sidepane;

import java.util.EnumSet;

import javafx.collections.SetChangeListener;
import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupModeViewModel;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

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

        groupsPreferences.groupViewModeProperty().addListener((SetChangeListener<GroupViewMode>) change -> {
            GroupModeViewModel modeViewModel = new GroupModeViewModel(groupsPreferences.groupViewModeProperty());
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
            EnumSet<GroupViewMode> mode = groupsPreferences.getGroupViewMode();

            if (mode.contains(GroupViewMode.INTERSECTION)) {
                groupsPreferences.setGroupViewMode(GroupViewMode.INTERSECTION, false);
                dialogService.notify(Localization.lang("Group view mode set to intersection"));
            } else {
                groupsPreferences.setGroupViewMode(GroupViewMode.INTERSECTION, true);
                dialogService.notify(Localization.lang("Group view mode set to union"));
            }
        }
    }
}
