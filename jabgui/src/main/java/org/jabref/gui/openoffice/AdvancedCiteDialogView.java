package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.openoffice.style.CitationType;

import com.airhacks.afterburner.views.ViewLoader;

public class AdvancedCiteDialogView extends BaseDialog<AdvancedCiteDialogViewModel> {

    @FXML private TextField pageInfo;
    @FXML private RadioButton inPar;
    @FXML private RadioButton inText;
    @FXML private RadioButton noPar;
    @FXML private RadioButton authorOnly;
    @FXML private RadioButton yearOnly;
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

        inPar.setUserData(CitationType.AUTHORYEAR_PAR);
        inText.setUserData(CitationType.AUTHORYEAR_INTEXT);
        noPar.setUserData(CitationType.AUTHORYEAR_NOPAR);
        authorOnly.setUserData(CitationType.AUTHOR_ONLY);
        yearOnly.setUserData(CitationType.YEAR_ONLY);

        pageInfo.disableProperty().bind(authorOnly.selectedProperty());

        citeToggleGroup.selectedToggleProperty().addListener((_, _, selected) -> {
            if (selected != null) {
                viewModel.citationTypeProperty().set((CitationType) selected.getUserData());
            }
        });
        for (Toggle toggle : citeToggleGroup.getToggles()) {
            if (toggle.getUserData() == viewModel.citationTypeProperty().get()) {
                citeToggleGroup.selectToggle(toggle);
            }
        }

        pageInfo.textProperty().bindBidirectional(viewModel.pageInfoProperty());
    }
}
