package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.util.BaseDialog;

import com.airhacks.afterburner.views.ViewLoader;

public class AdvancedCiteDialogView extends BaseDialog<AdvancedCiteDialogViewModel> {

    @FXML TextField pageInfo;
    @FXML RadioButton inPar;
    @FXML RadioButton inText;
    @FXML ToggleGroup citeToggleGroup;
    private AdvancedCiteDialogViewModel viewModel;

    public AdvancedCiteDialogView() {

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return viewModel;
            }
            return null;
        });

    }

    @FXML
    private void initialize() {
        viewModel = new AdvancedCiteDialogViewModel();

        inPar.selectedProperty().bindBidirectional(viewModel.citeInParProperty());
        inText.selectedProperty().bindBidirectional(viewModel.citeInTexTProperty());
        pageInfo.textProperty().bindBidirectional(viewModel.pageInfoProperty());

    }
}
