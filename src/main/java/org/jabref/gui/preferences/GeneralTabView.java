package org.jabref.gui.preferences;

import java.nio.charset.Charset;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class GeneralTabView extends AbstractPreferenceTabView<GeneralTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<Language> language;
    @FXML private ComboBox<Charset> defaultEncoding;
    @FXML private ComboBox<BibDatabaseMode> biblatexMode;
    @FXML private CheckBox inspectionWarningDuplicate;
    @FXML private CheckBox confirmDelete;
    @FXML private CheckBox allowIntegerEdition;
    @FXML private CheckBox memoryStickMode;
    @FXML private CheckBox collectTelemetry;
    @FXML private CheckBox showAdvancedHints;
    @FXML private CheckBox markOwner;
    @FXML private TextField markOwnerName;
    @FXML private CheckBox markOwnerOverwrite;
    @FXML private Button markOwnerHelp;
    @FXML private CheckBox markTimestamp;
    @FXML private TextField markTimeStampFormat;
    @FXML private Label markTimeStampFormatLabel;
    @FXML private CheckBox markTimeStampOverwrite;
    @FXML private TextField markTimeStampFieldName;
    @FXML private Label markTimeStampFieldNameLabel;
    @FXML private Button markTimeStampHelp;
    @FXML private CheckBox updateTimeStamp;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public GeneralTabView(PreferencesService preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }

    public void initialize() {
        this.viewModel = new GeneralTabViewModel(dialogService, preferences);

        new ViewModelListCellFactory<Language>()
                .withText(Language::getDisplayName)
                .install(language);
        language.itemsProperty().bind(viewModel.languagesListProperty());
        language.valueProperty().bindBidirectional(viewModel.selectedLanguageProperty());

        new ViewModelListCellFactory<Charset>()
                .withText(Charset::displayName)
                .install(defaultEncoding);
        defaultEncoding.itemsProperty().bind(viewModel.encodingsListProperty());
        defaultEncoding.valueProperty().bindBidirectional(viewModel.selectedEncodingProperty());

        new ViewModelListCellFactory<BibDatabaseMode>()
                .withText(BibDatabaseMode::getFormattedName)
                .install(biblatexMode);
        biblatexMode.itemsProperty().bind(viewModel.biblatexModeListProperty());
        biblatexMode.valueProperty().bindBidirectional(viewModel.selectedBiblatexModeProperty());

        inspectionWarningDuplicate.selectedProperty().bindBidirectional(viewModel.inspectionWarningDuplicateProperty());
        confirmDelete.selectedProperty().bindBidirectional(viewModel.confirmDeleteProperty());
        allowIntegerEdition.selectedProperty().bindBidirectional(viewModel.allowIntegerEditionProperty());
        memoryStickMode.selectedProperty().bindBidirectional(viewModel.memoryStickModeProperty());
        collectTelemetry.selectedProperty().bindBidirectional(viewModel.collectTelemetryProperty());
        showAdvancedHints.selectedProperty().bindBidirectional(viewModel.showAdvancedHintsProperty());

        markOwner.selectedProperty().bindBidirectional(viewModel.markOwnerProperty());
        markOwnerName.textProperty().bindBidirectional(viewModel.markOwnerNameProperty());
        markOwnerName.disableProperty().bind(markOwner.selectedProperty().not());
        markOwnerOverwrite.selectedProperty().bindBidirectional(viewModel.markOwnerOverwriteProperty());
        markOwnerOverwrite.disableProperty().bind(markOwner.selectedProperty().not());

        markTimestamp.selectedProperty().bindBidirectional(viewModel.markTimestampProperty());
        markTimeStampFormatLabel.disableProperty().bind(markTimestamp.selectedProperty().not());
        markTimeStampFormat.textProperty().bindBidirectional(viewModel.markTimeStampFormatProperty());
        markTimeStampFormat.disableProperty().bind(markTimestamp.selectedProperty().not());
        markTimeStampOverwrite.selectedProperty().bindBidirectional(viewModel.markTimeStampOverwriteProperty());
        markTimeStampOverwrite.disableProperty().bind(markTimestamp.selectedProperty().not());
        markTimeStampFieldNameLabel.disableProperty().bind(markTimestamp.selectedProperty().not());
        markTimeStampFieldName.textProperty().bindBidirectional(viewModel.markTimeStampFieldNameProperty());
        markTimeStampFieldName.disableProperty().bind(markTimestamp.selectedProperty().not());
        updateTimeStamp.selectedProperty().bindBidirectional(viewModel.updateTimeStampProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.OWNER), markOwnerHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.TIMESTAMP), markTimeStampHelp);

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.markTimeStampFormatValidationStatus(), markTimeStampFormat));
    }
}
