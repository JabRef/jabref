package org.jabref.gui;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class AbstractController<T extends AbstractViewModel> {

    @FXML protected T viewModel;
    private Stage stage;

    /**
     * Gets the associated view model.
     *
     * Without this method the {@link FXMLLoader} is not able to resolve references in the fxml file of the form
     * text="${controller.viewModel.someProperty}"
     */
    public T getViewModel() {
        return viewModel;
    }

    /**
     * Returns the stage where this controller is displayed.
     * The stage can be used to e.g. close the dialog.
     */
    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = Objects.requireNonNull(stage);
    }
}
