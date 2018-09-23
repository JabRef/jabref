package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.util.BaseDialog;

import com.airhacks.afterburner.views.ViewLoader;

public class AdvancedCiteDialogView extends BaseDialog<Void> {

    @FXML TextField pageInfo;
    @FXML RadioButton inPar;
    @FXML RadioButton inText;
    @FXML ToggleGroup citeToggleGroup;

    public AdvancedCiteDialogView() {

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

    }

    @FXML
    private void initialize() {
    }
}
