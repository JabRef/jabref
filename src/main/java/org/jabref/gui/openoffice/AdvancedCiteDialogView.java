package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class AdvancedCiteDialogView extends BaseDialog<AdvancedCiteDialogViewModel> {

    @FXML private TextField pageInfo;
    @FXML private RadioButton inPar;
    @FXML private RadioButton inText;
    @FXML private ToggleGroup citeToggleGroup;
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

        setTitle(Localization.lang("Cite special"));
    }

    @FXML
    private void initialize() {
        viewModel = new AdvancedCiteDialogViewModel();

        inPar.selectedProperty().bindBidirectional(viewModel.citeInParProperty());
        inText.selectedProperty().bindBidirectional(viewModel.citeInTextProperty());
        pageInfo.textProperty().bindBidirectional(viewModel.pageInfoProperty());
    }
}
