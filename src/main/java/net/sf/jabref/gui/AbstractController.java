package net.sf.jabref.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

public class AbstractController<T extends AbstractViewModel> {

    @FXML protected T viewModel;

    /**
     * Gets the associated view model.
     *
     * Without this method the {@link FXMLLoader} is not able to resolve references in the fxml file of the form
     * text="${controller.viewModel.someProperty}"
     */
    public T getViewModel() {
        return viewModel;
    }
}
