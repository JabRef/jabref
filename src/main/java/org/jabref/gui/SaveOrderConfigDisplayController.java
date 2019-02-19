package org.jabref.gui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

public class SaveOrderConfigDisplayController {

    @FXML private ToggleGroup saveOrderToggleGroup;
    @FXML private ComboBox<String> savePriSort;
    @FXML private ComboBox<String> saveSecSort;
    @FXML private ComboBox<String> saveTerSort;
    @FXML private RadioButton exportInSpecifiedOrder;
    @FXML private RadioButton exportInTableOrder;
    @FXML private RadioButton exportInOriginalOrder;
    @FXML private CheckBox savePriDesc;
    @FXML private CheckBox saveSecDesc;
    @FXML private CheckBox saveTerDesc;

    public void init() {

    }

}
