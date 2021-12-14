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
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

public class SidePaneComponent extends BorderPane {
    private final SidePaneType sidePaneType;
    private final SimpleCommand closeCommand;
    private final SimpleCommand moveUpCommand;
    private final SimpleCommand moveDownCommand;
    private final SidePaneContentFactory contentFactory;

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
    }

    private void initialize() {
        getStyleClass().add("sidePaneComponent");
        setTop(createHeaderView());
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

        this.buttonContainer = new HBox();
        buttonContainer.getChildren().addAll(upButton, downButton, closeButton);

        Label label = new Label(sidePaneType.getTitle());

        BorderPane headerView = new BorderPane();
        headerView.setCenter(label);
        headerView.setRight(buttonContainer);
        headerView.getStyleClass().add("sidePaneComponentHeader");

        return headerView;
    }

    protected void addExtraButtonToHeader(Button button, int position) {
        this.buttonContainer.getChildren().add(position, button);
    }
}
