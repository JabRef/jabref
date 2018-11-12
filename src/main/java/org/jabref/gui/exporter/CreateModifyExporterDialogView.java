package org.jabref.gui.exporter;

import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class CreateModifyExporterDialogView extends BaseDialog<Optional<ExporterViewModel>> {

    //Browse must be a Button because ButtonTypes  must be on the buttom, not next to the filename field
    @FXML private Button browseButton;
    @FXML private TextField name;
    @FXML private TextField fileName;
    @FXML private TextField extension;
    @FXML private ButtonType saveExporter;

    private DialogService dialogService;
    private PreferencesService preferences;
    private CreateModifyExporterDialogViewModel viewModel;

    private final Optional<ExporterViewModel> exporter;
    private final JournalAbbreviationLoader loader;

    public CreateModifyExporterDialogView(Optional<ExporterViewModel> exporter, DialogService dialogService,
                                          PreferencesService preferences, JournalAbbreviationLoader loader) { //should the latter three have been injected as in the main dialog rather than passed as a param?
        this.setTitle(Localization.lang("Customize Export Formats"));
        this.exporter = exporter;
        this.loader = loader;
        this.dialogService = dialogService;
        this.preferences = preferences;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setResultConverter(button -> {
            if (button == saveExporter) {
                return viewModel.saveExporter();
            } else {
                return Optional.empty();
            }
        });

        browseButton.setOnAction(event -> browse());

    }

    @FXML
    private void initialize() {
        viewModel = new CreateModifyExporterDialogViewModel(exporter, dialogService, preferences, loader);
        name.textProperty().bindBidirectional(viewModel.getName());
        fileName.textProperty().bindBidirectional(viewModel.getLayoutFileName());
        extension.textProperty().bindBidirectional(viewModel.getExtension());
    }

    private void browse() {
        viewModel.browse();
    }

    @FXML
    private void closeDialog() {
        close();
    }
}