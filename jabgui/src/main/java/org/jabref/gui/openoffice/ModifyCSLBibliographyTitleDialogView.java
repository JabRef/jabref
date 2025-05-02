package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ModifyCSLBibliographyTitleDialogView extends BaseDialog<Void> {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> headerFormats;
    @FXML private ComboBox<String> bodyFormats;

    private final ModifyCSLBibliographyTitleDialogViewModel viewModel;

    public ModifyCSLBibliographyTitleDialogView(OpenOfficePreferences openOfficePreferences) {
        this.viewModel = new ModifyCSLBibliographyTitleDialogViewModel(openOfficePreferences);

        this.setTitle(Localization.lang("Modify bibliography properties"));
        this.initModality(Modality.NONE);
        this.setResizable(false);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setOnAction(_ -> {
            this.close();
        });
    }

    @FXML
    public void initialize() {
        titleField.textProperty().bindBidirectional(viewModel.cslBibliographyTitleProperty());

        new ViewModelListCellFactory<String>()
                .withText(format -> format)
                .install(headerFormats);
        headerFormats.itemsProperty().bind(viewModel.formatListProperty());
        headerFormats.valueProperty().bindBidirectional(viewModel.cslBibliographySelectedHeaderFormatProperty());

        new ViewModelListCellFactory<String>()
                .withText(format -> format)
                .install(bodyFormats);
        bodyFormats.itemsProperty().bind(viewModel.formatListProperty());
        bodyFormats.valueProperty().bindBidirectional(viewModel.cslBibliographySelectedBodyFormatProperty());
    }
}
