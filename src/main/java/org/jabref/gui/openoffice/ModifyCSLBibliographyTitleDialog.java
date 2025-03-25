package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ModifyCSLBibliographyTitleDialog extends BaseDialog<Void> {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> formats;

    private final OpenOfficePreferences openOfficePreferences;

    public ModifyCSLBibliographyTitleDialog(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;

        this.setTitle(Localization.lang("Modify bibliography title"));
        this.initModality(Modality.NONE);
        this.setResizable(false);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    public void initialize() {
        ModifyCSLBibliographyTitleDialogViewModel viewModel = new ModifyCSLBibliographyTitleDialogViewModel(openOfficePreferences);

        titleField.textProperty().bindBidirectional(viewModel.cslBibliographyTitle());

        new ViewModelListCellFactory<String>()
                .withText(format -> format)
                .install(formats);
        formats.itemsProperty().bind(viewModel.formatListProperty());
        formats.valueProperty().bindBidirectional(viewModel.cslBibliographyHeaderSelectedFormat());
    }
}
