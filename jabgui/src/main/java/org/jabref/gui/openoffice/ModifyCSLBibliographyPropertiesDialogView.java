package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ModifyCSLBibliographyPropertiesDialogView extends BaseDialog<Void> {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> headerFormats;
    @FXML private ComboBox<String> bodyFormats;

    private final ModifyCSLBibliographyPropertiesDialogViewModel viewModel;

    public ModifyCSLBibliographyPropertiesDialogView(OpenOfficePreferences openOfficePreferences) {
        this.viewModel = new ModifyCSLBibliographyPropertiesDialogViewModel(openOfficePreferences);

        this.setTitle(Localization.lang("Modify bibliography properties"));
        this.initModality(Modality.NONE);
        this.setResizable(false);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                viewModel.savePreferences();
            }
            return null;
        });
    }

    @FXML
    public void initialize() {
        titleField.textProperty().bindBidirectional(viewModel.cslBibliographyTitleProperty());

        new ViewModelListCellFactory<String>()
                .withText(format -> format)
                .install(headerFormats);
        headerFormats.itemsProperty().bind(viewModel.headerFormatListProperty());
        headerFormats.valueProperty().bindBidirectional(viewModel.cslBibliographySelectedHeaderFormatProperty());

        new ViewModelListCellFactory<String>()
                .withText(format -> format)
                .install(bodyFormats);
        bodyFormats.itemsProperty().bind(viewModel.bodyFormatListProperty());
        bodyFormats.valueProperty().bindBidirectional(viewModel.cslBibliographySelectedBodyFormatProperty());
    }
}
