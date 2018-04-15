package org.jabref.gui.groups;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;

import org.jabref.gui.DialogService;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * The groups side pane.
 */
public class GroupSidePane extends SidePaneComponent {

    private final JabRefPreferences preferences;
    private final DialogService dialogService;
    private final Button intersectionUnionToggle = IconTheme.JabRefIcons.WWW.asButton();

    public GroupSidePane(SidePaneManager manager, JabRefPreferences preferences, DialogService dialogService) {
        super(manager, IconTheme.JabRefIcons.TOGGLE_GROUPS, Localization.lang("Groups"));
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    @Override
    protected Optional<Node> getAddtionalHeaderButtons() {
        intersectionUnionToggle.setOnAction(event -> toggleUnionIntersection());
        return Optional.of(intersectionUnionToggle);
    }

    private Node getUnionIntersectionGraphic() {
        return preferences.getGroupViewMode().getIcon().getGraphicNode();
    }

    @Override
    public void afterOpening() {
        preferences.putBoolean(JabRefPreferences.GROUP_SIDEPANE_VISIBLE, Boolean.TRUE);
        intersectionUnionToggle.setGraphic(getUnionIntersectionGraphic());
    }

    @Override
    public Priority getResizePolicy() {
        return Priority.ALWAYS;
    }

    @Override
    public void beforeClosing() {
        preferences.putBoolean(JabRefPreferences.GROUP_SIDEPANE_VISIBLE, Boolean.FALSE);
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
        }

        if (mode == GroupViewMode.INTERSECTION) {
            preferences.setGroupViewMode(GroupViewMode.UNION);
            dialogService.notify(Localization.lang("Group view mode set to union"));
        }

        intersectionUnionToggle.setGraphic(getUnionIntersectionGraphic());
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
