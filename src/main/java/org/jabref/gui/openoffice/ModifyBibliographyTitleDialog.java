package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class ModifyBibliographyTitleDialog extends BaseDialog<Void> {

    @FXML private TextField titleField;
    @FXML private ComboBox<Formats> formats;

    @Inject private DialogService dialogService;

    private final OpenOfficePreferences openOfficePreferences;

    private ModifyBibliographyTitleDialogViewModel viewModel;

    public ModifyBibliographyTitleDialog(OpenOfficePreferences openOfficePreferences) {
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
        viewModel = new ModifyBibliographyTitleDialogViewModel(openOfficePreferences);

        titleField.textProperty().bindBidirectional(viewModel.bibliographyTitle);

        new ViewModelListCellFactory<Formats>()
                .withText(Formats::getFormat)
                .install(formats);
        formats.itemsProperty().bind(viewModel.formatListProperty());
        formats.valueProperty().bindBidirectional(viewModel.selectedFormat());
    }
}
