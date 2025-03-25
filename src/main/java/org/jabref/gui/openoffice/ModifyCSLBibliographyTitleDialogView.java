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
import org.jabref.logic.openoffice.oocsltext.CSLFormatUtils;

import com.airhacks.afterburner.views.ViewLoader;

public class ModifyCSLBibliographyTitleDialogView extends BaseDialog<Void> {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> formats;

    private final OpenOfficePreferences openOfficePreferences;

    public ModifyCSLBibliographyTitleDialogView(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;

        this.setTitle(Localization.lang("Modify bibliography title"));
        this.initModality(Modality.NONE);
        this.setResizable(false);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setOnAction(event -> {
            CSLFormatUtils.setBibliographyProperties(openOfficePreferences);
            this.close();
        });
    }

    @FXML
    public void initialize() {
        ModifyCSLBibliographyTitleDialogViewModel viewModel = new ModifyCSLBibliographyTitleDialogViewModel(openOfficePreferences);

        titleField.textProperty().bindBidirectional(viewModel.cslBibliographyTitleProperty());

        new ViewModelListCellFactory<String>()
                .withText(format -> format)
                .install(formats);
        formats.itemsProperty().bind(viewModel.formatListProperty());
        formats.valueProperty().bindBidirectional(viewModel.cslBibliographySelectedHeaderFormatProperty());
    }
}
