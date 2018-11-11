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

    //Browse seems to have to be a button because otherwise it must be on the buttom, not next to the filename field
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
        //Temporarily getting rid of exporter from this, see note in VM -what?
        //When viewModel announces the creation of a TE, only then should all the binding happen, you might need a listener here or use EasyBind, see https://stackoverflow.com/questions/25937258/javafx-binding-and-null-values, maybe monadic, selectpropety(naem property), or else, empty stirng, which then would somehow be fired into the EVM, maybe check with
        viewModel = new CreateModifyExporterDialogViewModel(exporter, dialogService, preferences, loader);
        //ExporterViewModel expViewModel = exporter.map(exp -> new ExporterViewModel(exp)).orElse(null); //change to flatmap to make the viewmodel optional, and change null to something else if you want something else for add exporter //not necessary if inpuytting empty EVM into dialog
        //viewModel.getName().bindBidirectional(name.textProperty());//this may have to be unidirectional if you have both VM's bound to it, in which case you would have to initialize this field's value in the View
        name.textProperty().bindBidirectional(viewModel.getName());
        //viewModel.getLayoutFileName().bindBidirectional(fileName.textProperty());
        fileName.textProperty().bindBidirectional(viewModel.getLayoutFileName());
        //viewModel.getExtension().bindBidirectional(extension.textProperty());
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