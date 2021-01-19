package org.jabref.gui.groups;

import java.util.Collections;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;

import org.jabref.gui.DialogService;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * The groups side pane.
 */
public class GroupSidePane extends SidePaneComponent {

    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final Button intersectionUnionToggle = IconTheme.JabRefIcons.GROUP_INTERSECTION.asButton();
    private final StateManager stateManager;

    public GroupSidePane(SidePaneManager manager, PreferencesService preferences, DialogService dialogService, StateManager stateManager) {
        super(manager, IconTheme.JabRefIcons.TOGGLE_GROUPS, Localization.lang("Groups"));
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
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
        preferences.storeSidePanePreferences(preferences.getSidePanePreferences().withGroupsPaneVisible(false));
    }

    @Override
    public void afterOpening() {
        preferences.storeSidePanePreferences(preferences.getSidePanePreferences().withGroupsPaneVisible(true));
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
            stateManager.setGroupNodeViewModel(GroupViewMode.INTERSECTION);
            dialogService.notify(Localization.lang("Group view mode set to intersection"));
        } else if (mode == GroupViewMode.INTERSECTION) {
            preferences.setGroupViewMode(GroupViewMode.UNION);
            stateManager.setGroupNodeViewModel(GroupViewMode.UNION);
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
        return ViewLoader.view(GroupTreeView.class)
                         .load()
                         .getView();
    }

    @Override
    public SidePaneType getType() {
        return SidePaneType.GROUPS;
    }
}
