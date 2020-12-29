package org.jabref.gui.help;

import javafx.stage.Stage;

import org.jabref.gui.actions.SimpleCommand;

public class AboutAction extends SimpleCommand {

    private final Stage primaryStage;

    public AboutAction(Stage primaryState) {
        this.primaryStage = primaryState;
    }

    @Override
    public void execute() {
        AboutDialogView aboutDialogView = new AboutDialogView();
        aboutDialogView.initOwner(primaryStage);
        aboutDialogView.show();
    }
}
