package org.jabref.gui.sidepane;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupTreeView;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

public class SidePaneComponent extends BorderPane {
    private final SidePaneType sidePaneType;
    private final SimpleCommand closeCommand;
    private final SimpleCommand moveUpCommand;
    private final SimpleCommand moveDownCommand;
    private final SidePaneContentFactory contentFactory;
    private Button addButton;
    private Label label;
    private Node headerView;

    private HBox buttonContainer;

    public SidePaneComponent(SidePaneType sidePaneType,
                             SimpleCommand closeCommand,
                             SimpleCommand moveUpCommand,
                             SimpleCommand moveDownCommand,
                             SidePaneContentFactory contentFactory) {
        this.sidePaneType = sidePaneType;
        this.closeCommand = closeCommand;
        this.moveUpCommand = moveUpCommand;
        this.moveDownCommand = moveDownCommand;
        this.contentFactory = contentFactory;
        initialize();
        setMouseHoverListener();
    }

    private void initialize() {
        getStyleClass().add("sidePaneComponent");
        headerView = createHeaderView();
        setTop(headerView);
        setCenter(contentFactory.create(sidePaneType));
        VBox.setVgrow(this, sidePaneType == SidePaneType.GROUPS ? Priority.ALWAYS : Priority.NEVER);
    }

    private Node createHeaderView() {
        Button closeButton = IconTheme.JabRefIcons.CLOSE.asButton();
        closeButton.setTooltip(new Tooltip(Localization.lang("Hide panel")));
        closeButton.setOnAction(e -> closeCommand.execute());

        Button upButton = IconTheme.JabRefIcons.UP.asButton();
        upButton.setTooltip(new Tooltip(Localization.lang("Move panel up")));
        upButton.setOnAction(e -> moveUpCommand.execute());

        Button downButton = IconTheme.JabRefIcons.DOWN.asButton();
        downButton.setTooltip(new Tooltip(Localization.lang("Move panel down")));
        downButton.setOnAction(e -> moveDownCommand.execute());

        addButton = IconTheme.JabRefIcons.ADD.asButton();
        addButton.setTooltip(new Tooltip(Localization.lang("New group")));

        this.buttonContainer = new HBox();
        buttonContainer.getChildren().addAll(upButton, downButton, closeButton);

        label = new Label(sidePaneType.getTitle());

        BorderPane headerView = new BorderPane();
        headerView.setLeft(label);
        headerView.setRight(buttonContainer);
        headerView.getStyleClass().add("sidePaneComponentHeader");

        return headerView;
    }

    private void setMouseHoverListener() {
        headerView.setOnMouseMoved(event -> {
            double mouseX = event.getX();
            double mouseY = event.getY();
            if (isMouseInBounds(mouseX, mouseY)) {
                applyHoverEffect();
            } else {
                removeHoverEffect();
            }
        });

        headerView.setOnMouseExited(event -> {
            removeHoverEffect();
        });
    }

    private boolean isMouseInBounds(double mouseX, double mouseY) {
        double minX = label.getBoundsInParent().getMinX();
        double minY = buttonContainer.getBoundsInParent().getMinY();
        double maxX = buttonContainer.getBoundsInParent().getMaxX();
        double maxY = buttonContainer.getBoundsInParent().getMaxY();
        return (mouseX < maxX && mouseX > minX && mouseY < maxY && mouseY > minY);
    }

    private void applyHoverEffect() {
        if (!buttonContainer.getChildren().contains(addButton)) {
            addExtraButtonToHeader(addButton, 0);
        }
    }

    private void removeHoverEffect() {
        buttonContainer.getChildren().remove(addButton);
    }

    protected void addExtraButtonToHeader(Button button, int position) {
        this.buttonContainer.getChildren().add(position, button);
    }

    public void requestFocus() {
        for (Node child : getChildren()) {
            if (child instanceof GroupTreeView groupTreeView) {
                groupTreeView.requestFocusGroupTree();
                break;
            }
        }
    }
}
