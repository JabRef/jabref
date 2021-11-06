package org.jabref.gui.sidepane;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupModeViewModel;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

public class GroupsSidePaneComponent extends SidePaneComponent {
    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final Button intersectionUnionToggle = IconTheme.JabRefIcons.GROUP_INTERSECTION.asButton();

    public GroupsSidePaneComponent(SimpleCommand closeCommand, SimpleCommand moveUpCommand, SimpleCommand moveDownCommand, SidePaneContentFactory contentFactory, PreferencesService preferences, DialogService dialogService) {
        super(SidePaneType.GROUPS, closeCommand, moveUpCommand, moveDownCommand, contentFactory);
        this.preferences = preferences;
        this.dialogService = dialogService;
        setupIntersectionUnionToggle();
    }

    private void setupIntersectionUnionToggle() {
        addExtraButtonToHeader(intersectionUnionToggle, 2);
        intersectionUnionToggle.setOnAction(event -> new ToggleUnionIntersectionAction().execute());
    }

    public void afterOpening() {
        setGraphicsAndTooltipForButton(preferences.getGroupViewMode());
    }

    private void setGraphicsAndTooltipForButton(GroupViewMode mode) {
        GroupModeViewModel modeViewModel = new GroupModeViewModel(mode);
        intersectionUnionToggle.setGraphic(modeViewModel.getUnionIntersectionGraphic());
        intersectionUnionToggle.setTooltip(modeViewModel.getUnionIntersectionTooltip());
    }

    private class ToggleUnionIntersectionAction extends SimpleCommand {

        @Override
        public void execute() {
            GroupViewMode mode = preferences.getGroupViewMode();

            if (mode == GroupViewMode.UNION) {
                preferences.setGroupViewMode(GroupViewMode.INTERSECTION);
                dialogService.notify(Localization.lang("Group view mode set to intersection"));
            } else if (mode == GroupViewMode.INTERSECTION) {
                preferences.setGroupViewMode(GroupViewMode.UNION);
                dialogService.notify(Localization.lang("Group view mode set to union"));
            }
            setGraphicsAndTooltipForButton(mode);
        }
    }
}
