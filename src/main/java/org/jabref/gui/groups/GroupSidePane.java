package org.jabref.gui.groups;

import java.util.Collections;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.sidepane.SidePane;
import org.jabref.gui.sidepane.SidePaneComponent;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

/**
 * The groups side pane.
 */
public class GroupSidePane extends SidePaneComponent {

    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final StateManager stateManager;
    private final Button intersectionUnionToggle = IconTheme.JabRefIcons.GROUP_INTERSECTION.asButton();

    public GroupSidePane(SidePane sidePane, TaskExecutor taskExecutor, StateManager stateManager, PreferencesService preferences, DialogService dialogService) {
        super(sidePane, IconTheme.JabRefIcons.TOGGLE_GROUPS, Localization.lang("Groups"));
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
    }

    @Override
    protected List<Node> getAdditionalHeaderButtons() {
        intersectionUnionToggle.setOnAction(event -> toggleUnionIntersection());
        return Collections.singletonList(intersectionUnionToggle);
    }

    @Override
    public Priority getResizePolicy() {
        return Priority.ALWAYS;
    }

    @Override
    public void beforeClosing() {
        preferences.getSidePanePreferences().visiblePanes().remove(SidePaneType.GROUPS);
    }

    @Override
    public void afterOpening() {
        preferences.getSidePanePreferences().visiblePanes().add(SidePaneType.GROUPS);
        setGraphicsAndTooltipForButton(preferences.getGroupViewMode());
    }

    @Override
    public Action getToggleAction() {
        return StandardActions.TOGGLE_GROUPS;
    }

    private void toggleUnionIntersection() {
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

    private void setGraphicsAndTooltipForButton(GroupViewMode mode) {
        GroupModeViewModel modeViewModel = new GroupModeViewModel(mode);
        intersectionUnionToggle.setGraphic(modeViewModel.getUnionIntersectionGraphic());
        intersectionUnionToggle.setTooltip(modeViewModel.getUnionIntersectionTooltip());
    }

    @Override
    protected Node createContentPane() {
        return new GroupTreeView(taskExecutor, stateManager, preferences, dialogService);
    }

    @Override
    public SidePaneType getType() {
        return SidePaneType.GROUPS;
    }
}
