package org.jabref.gui.groups;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.IconTheme.JabRefIcons;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * The groups side pane.
 */
public class GroupSidePane extends SidePaneComponent {

    private final JabRefPreferences preferences;
    private final JabRefFrame frame;
    private final Button intersectionUnionToggle = IconTheme.JabRefIcons.WWW.asButton();

    public GroupSidePane(SidePaneManager manager, JabRefPreferences preferences, JabRefFrame frame) {
        super(manager, IconTheme.JabRefIcons.TOGGLE_GROUPS, Localization.lang("Groups"));
        this.preferences = preferences;
        this.frame = frame;
    }

    @Override
    public Node getHeader() {
        Button close = IconTheme.JabRefIcons.CLOSE.asButton();
        close.setOnAction(event -> hide());

        Button up = IconTheme.JabRefIcons.UP.asButton();
        up.setOnAction(event -> moveUp());

        Button down = IconTheme.JabRefIcons.DOWN.asButton();
        down.setOnAction(event -> moveDown());

        intersectionUnionToggle.setOnAction(event -> toggleUnionIntersection());

        final HBox buttonContainer = new HBox();
        buttonContainer.getChildren().addAll(up, down, intersectionUnionToggle, close);
        BorderPane graphic = new BorderPane();
        graphic.setCenter(icon.getGraphicNode());
        //        container.setLeft(graphic);
        final Label label = new Label(title);
        BorderPane container = new BorderPane();
        container.setCenter(label);
        container.setRight(buttonContainer);
        container.getStyleClass().add("sidePaneComponentHeader");

        return container;
    }

    private Node getUnionIntersectionGraphic() {
        boolean intersectionToggled = preferences.getBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS);
        return intersectionToggled ? JabRefIcons.WWW.getGraphicNode() : JabRefIcons.TWITTER.getGraphicNode();
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
        boolean intersectionToggled = preferences.getBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS);
        boolean newState = !intersectionToggled;
        preferences.putBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS, newState);

        BasePanel basePanel = frame.getCurrentBasePanel();
        if (newState) {
            basePanel.output(Localization.lang("Group view mode set to intersection"));
        } else {
            basePanel.output(Localization.lang("Group view mode set to union"));
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
