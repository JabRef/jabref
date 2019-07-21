package org.jabref.gui.preferences;

import java.nio.charset.Charset;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class GeneralTabView extends VBox implements PrefsTab {

    @FXML private ComboBox<Language> language;
    @FXML private ComboBox<Charset> defaultEncoding;
    @FXML private ComboBox<BibDatabaseMode> biblatexMode;
    @FXML private CheckBox inspectionWarningDuplicate;
    @FXML private CheckBox confirmDelete;
    @FXML private CheckBox enforceLegalKeys;
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

    @Inject private DialogService dialogService;
    private final JabRefPreferences preferences;

    private GeneralTabViewModel viewModel;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public GeneralTabView(JabRefPreferences preferences) {
        this.preferences = preferences;
        ViewLoader.view(this)
                .root(this)
                .load();
    }

    public void initialize() {
        viewModel = new GeneralTabViewModel(dialogService, preferences);

        language.itemsProperty().bind(viewModel.languagesListProperty());
        language.valueProperty().bindBidirectional(viewModel.selectedLanguageProperty());

        defaultEncoding.itemsProperty().bind(viewModel.encodingsListProperty());
        defaultEncoding.valueProperty().bindBidirectional(viewModel.selectedEncodingProperty());

        biblatexMode.itemsProperty().bind(viewModel.biblatexModeListProperty());
        biblatexMode.valueProperty().bindBidirectional(viewModel.selectedBiblatexModeProperty());

        inspectionWarningDuplicate.selectedProperty().bindBidirectional(viewModel.inspectionWarningDuplicateProperty());
        confirmDelete.selectedProperty().bindBidirectional(viewModel.confirmDeleteProperty());
        enforceLegalKeys.selectedProperty().bindBidirectional(viewModel.enforceLegalKeysProperty());
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

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues() {
        // ToDo: Remove this after conversion of all tabs
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }
}
