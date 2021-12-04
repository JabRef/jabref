package org.jabref.gui.sidepane;

import java.util.Collections;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.l10n.Localization;

public abstract class SidePaneComponent {

    private final SidePane sidePane;
    private final ToggleCommand toggleCommand;
    private final JabRefIcon icon;
    private final String title;
    private Node contentNode;

    public SidePaneComponent(SidePane sidePane, JabRefIcon icon, String title) {
        this.sidePane = sidePane;
        this.icon = icon;
        this.title = title;
        this.toggleCommand = new ToggleCommand(this);
    }

    protected void hide() {
        sidePane.hide(this.getType());
    }

    protected void show() {
        sidePane.show(this.getType());
    }

    protected void moveUp() {
        sidePane.moveUp(this);
    }

    protected void moveDown() {
        sidePane.moveDown(this);
    }

    /**
     * Override this method if the component needs to make any changes before it can close.
     */
    public void beforeClosing() {
        // Nothing to do by default
    }

    /**
     * Override this method if the component needs to do any actions after it is shown.
     */
    public void afterOpening() {
        // Nothing to do by default
    }

    /**
     * Specifies how to this side pane component behaves if there is additional vertical space.
     */
    public abstract Priority getResizePolicy();

    /**
     * @return the command which toggles this {@link SidePaneComponent}
     */
    public ToggleCommand getToggleCommand() {
        return toggleCommand;
    }

    /**
     * @return the action to toggle this {@link SidePaneComponent}
     */
    public abstract Action getToggleAction();

    /**
     * @return the content of this component
     */
    public final Node getContentPane() {
        if (contentNode == null) {
            contentNode = createContentPane();
        }

        return contentNode;
    }

    /**
     * @return the header pane for this component
     */
    public final Node getHeader() {
        Button close = IconTheme.JabRefIcons.CLOSE.asButton();
        close.setTooltip(new Tooltip(Localization.lang("Hide panel")));
        close.setOnAction(event -> hide());

        Button up = IconTheme.JabRefIcons.UP.asButton();
        up.setTooltip(new Tooltip(Localization.lang("Move panel up")));
        up.setOnAction(event -> moveUp());

        Button down = IconTheme.JabRefIcons.DOWN.asButton();
        down.setTooltip(new Tooltip(Localization.lang("Move panel down")));
        down.setOnAction(event -> moveDown());

        final HBox buttonContainer = new HBox();
        buttonContainer.getChildren().addAll(up, down);
        buttonContainer.getChildren().addAll(getAdditionalHeaderButtons());
        buttonContainer.getChildren().add(close);

        BorderPane graphic = new BorderPane();
        graphic.setCenter(icon.getGraphicNode());

        final Label label = new Label(title);
        BorderPane container = new BorderPane();
        container.setCenter(label);
        container.setRight(buttonContainer);
        container.getStyleClass().add("sidePaneComponentHeader");

        return container;
    }

    protected List<Node> getAdditionalHeaderButtons() {
        return Collections.emptyList();
    }

    /**
     * Create the content of this component
     *
     * @implNote The {@link SidePane} always creates an instance of every side component (e.g., to get the toggle action)
     * but we only want to create the content view if the component is shown to save resources.
     * This is the reason for the lazy loading.
     */
    protected abstract Node createContentPane();

    /**
     * @return the type of this component
     */
    public abstract SidePaneType getType();

    public class ToggleCommand extends SimpleCommand {

        private final SidePaneComponent component;

        public ToggleCommand(SidePaneComponent component) {
            this.component = component;
        }

        @Override
        public void execute() {
            sidePane.toggle(component.getType());
        }
    }
}
