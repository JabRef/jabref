package org.jabref.gui.preferences.ocr;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.models.OcrEngineConfig;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

/**
 * Tab for OCR preferences in JabRef's preferences dialog.
 * <p>
 * This class demonstrates how to create a preference tab using JabRef's
 * UI framework and MVVM pattern.
 */
public class OcrTab extends AbstractPreferenceTabView<OcrTabViewModel> implements PreferencesTab {
    
    @FXML private CheckBox enableOcrCheckBox;
    @FXML private ComboBox<String> engineComboBox;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private CheckBox preprocessImagesCheckBox;
    @FXML private ComboBox<OcrEngineConfig.QualityPreset> qualityPresetComboBox;
    @FXML private TextField tesseractPathTextField;
    @FXML private Button tesseractPathBrowseButton;
    @FXML private Button helpButton;
    
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    
    /**
     * Create a new OCR tab for JabRef preferences.
     */
    public OcrTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }
    
    /**
     * Initialize the tab.
     */
    @FXML
    public void initialize() {
        this.viewModel = new OcrTabViewModel(preferences);
        
        // Bind UI components to view model properties
        enableOcrCheckBox.selectedProperty().bindBidirectional(viewModel.ocrEnabledProperty());
        
        // Set up combo boxes with models
        new ViewModelListCellFactory<String>()
                .withText(name -> name)
                .install(engineComboBox);
        engineComboBox.itemsProperty().bind(viewModel.availableEnginesProperty());
        engineComboBox.valueProperty().bindBidirectional(viewModel.defaultOcrEngineProperty());
        engineComboBox.disableProperty().bind(enableOcrCheckBox.selectedProperty().not());
        
        new ViewModelListCellFactory<String>()
                .withText(name -> name)
                .install(languageComboBox);
        languageComboBox.itemsProperty().bind(viewModel.availableLanguagesProperty());
        languageComboBox.valueProperty().bindBidirectional(viewModel.defaultLanguageProperty());
        languageComboBox.disableProperty().bind(enableOcrCheckBox.selectedProperty().not());
        
        preprocessImagesCheckBox.selectedProperty().bindBidirectional(viewModel.preprocessImagesProperty());
        preprocessImagesCheckBox.disableProperty().bind(enableOcrCheckBox.selectedProperty().not());
        
        new ViewModelListCellFactory<OcrEngineConfig.QualityPreset>()
                .withText(OcrEngineConfig.QualityPreset::getDescription)
                .install(qualityPresetComboBox);
        qualityPresetComboBox.itemsProperty().bind(viewModel.availableQualityPresetsProperty());
        qualityPresetComboBox.valueProperty().bindBidirectional(viewModel.qualityPresetProperty());
        qualityPresetComboBox.disableProperty().bind(enableOcrCheckBox.selectedProperty().not());
        
        tesseractPathTextField.textProperty().bindBidirectional(viewModel.tesseractPathProperty());
        tesseractPathTextField.disableProperty().bind(enableOcrCheckBox.selectedProperty().not());
        tesseractPathBrowseButton.disableProperty().bind(enableOcrCheckBox.selectedProperty().not());
        
        // Setup validation
        visualizer.initVisualization(viewModel.getTesseractPathValidationStatus(), tesseractPathTextField);
        
        // Configure help button
        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP,
                new HelpAction(HelpFile.IMPORT_USING_OCR, dialogService, preferences.getExternalApplicationsPreferences()),
                helpButton);
    }
    
    /**
     * Handle the browse button click for Tesseract path.
     */
    @FXML
    private void onBrowseTesseractPath() {
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Globals.prefs.get(Globals.WORKING_DIRECTORY))
                .build();
        
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration)
                .ifPresent(selectedDirectory -> viewModel.tesseractPathProperty().setValue(selectedDirectory.toString()));
    }
    
    @Override
    public String getTabName() {
        return Localization.lang("OCR");
    }
}