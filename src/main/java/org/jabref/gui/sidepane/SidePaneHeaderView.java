package org.jabref.gui.sidepane;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

public class SidePaneHeaderView extends BorderPane {
    private final SidePaneType sidePaneType;
    private final SimpleCommand closeCommand;
    private final SimpleCommand moveUpCommand;
    private final SimpleCommand moveDownCommand;

    private HBox buttonContainer;

    public SidePaneHeaderView(SidePaneType sidePaneType, SimpleCommand closeCommand, SimpleCommand moveUpCommand, SimpleCommand moveDownCommand) {
        this.sidePaneType = sidePaneType;
        this.closeCommand = closeCommand;
        this.moveUpCommand = moveUpCommand;
        this.moveDownCommand = moveDownCommand;
        initView();
    }

    private void initView() {
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
        setCenter(label);
        setRight(buttonContainer);
        getStyleClass().add("sidePaneComponentHeader");
    }

    protected void addButtonAtPosition(Button button, int position) {
        this.buttonContainer.getChildren().add(position, button);
    }

    public SidePaneType getSidePaneType() {
        return sidePaneType;
    }
}
