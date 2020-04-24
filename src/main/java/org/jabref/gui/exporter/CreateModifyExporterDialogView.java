package org.jabref.gui.exporter;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class CreateModifyExporterDialogView extends BaseDialog<ExporterViewModel> {

    @Inject private JournalAbbreviationRepository repository;
    private final ExporterViewModel exporter;
    @FXML private Button browseButton;
    @FXML private TextField name;
    @FXML private TextField fileName;
    @FXML private TextField extension;
    @FXML private ButtonType saveExporter;
    @Inject private final DialogService dialogService;
    @Inject private final PreferencesService preferences;
    private CreateModifyExporterDialogViewModel viewModel;

    public CreateModifyExporterDialogView(ExporterViewModel exporter, DialogService dialogService,
                                          PreferencesService preferences) {
        this.setTitle(Localization.lang("Customize Export Formats"));
        this.exporter = exporter;
        this.dialogService = dialogService;
        this.preferences = preferences;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setResultConverter(button -> {
            if (button == saveExporter) {
                return viewModel.saveExporter();
            } else {
                return null;
            }
        });
    }

    @FXML
    private void initialize() {
        viewModel = new CreateModifyExporterDialogViewModel(exporter, dialogService, preferences, repository);
        name.textProperty().bindBidirectional(viewModel.getName());
        fileName.textProperty().bindBidirectional(viewModel.getLayoutFileName());
        extension.textProperty().bindBidirectional(viewModel.getExtension());
    }

    @FXML
    private void browse(ActionEvent event) {
        viewModel.browse();
    }
}
