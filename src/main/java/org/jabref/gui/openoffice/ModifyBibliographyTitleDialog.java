package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.oocsltext.Format;

import com.airhacks.afterburner.views.ViewLoader;

public class ModifyBibliographyTitleDialog extends BaseDialog<Void> {

    @FXML private TextField titleField;
    @FXML private ComboBox<Format> formats;

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

        titleField.textProperty().bindBidirectional(viewModel.bibliographyTitle());

        new ViewModelListCellFactory<Format>()
                .withText(Format::getFormat)
                .install(formats);
        formats.itemsProperty().bind(viewModel.formatListProperty());
        formats.valueProperty().bindBidirectional(viewModel.selectedFormat());
    }
}
