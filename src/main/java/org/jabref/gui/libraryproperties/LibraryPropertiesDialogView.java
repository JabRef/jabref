package org.jabref.gui.libraryproperties;

import java.nio.charset.Charset;
import java.util.Optional;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.SaveOrderConfigDisplay;
import org.jabref.gui.cleanup.FieldFormatterCleanupsPanel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryPropertiesDialogView extends BaseDialog<Void> {

    @FXML private VBox contentVbox;
    @FXML private ComboBox<Charset> encoding;
    @FXML private TextField generalFileDirectory;
    @FXML private Button browseGeneralFileDir;
    @FXML private TextField userSpecificFileDirectory;
    @FXML private Button browseUserSpefiicFileDir;
    @FXML private VBox saveOrdervbox;
    @FXML private ToggleGroup saveOrderToggleGroup;
    @FXML private RadioButton saveInOriginalOrder;
    @FXML private RadioButton saveInSpecifiedOrder;
    @FXML private CheckBox protect;
    private SaveOrderConfigDisplay saveOrderPanel;
    private BasePanel panel;
    private LibraryPropertiesDialogViewModel viewModel;
    private final DialogService dialogService;
    @Inject private PreferencesService preferencesService;
    private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;
    private Object oldSaveOrderConfig;

    public LibraryPropertiesDialogView(BasePanel panel, DialogService dialogService) {
        this.dialogService = dialogService;
        this.panel = panel;
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                storeSettings();
            }
            return null;
        });

        setTitle(Localization.lang("Library properties"));

    }

    @FXML
    private void initialize() {
        viewModel = new LibraryPropertiesDialogViewModel(panel, dialogService, preferencesService);

        generalFileDirectory.textProperty().bindBidirectional(viewModel.generalFileDirectoryPropertyProperty());
        userSpecificFileDirectory.textProperty().bindBidirectional(viewModel.userSpecificFileDirectoryProperty());

        encoding.itemsProperty().bind(viewModel.encodingsProperty());
        encoding.valueProperty().bindBidirectional(viewModel.selectedEncodingProperty());
        saveOrderPanel = new SaveOrderConfigDisplay();
        saveOrdervbox.getChildren().add(saveOrderPanel.getJFXPanel());
        fieldFormatterCleanupsPanel = new FieldFormatterCleanupsPanel(Localization.lang("Enable save actions"),
                                                                      Cleanups.DEFAULT_SAVE_ACTIONS);

        contentVbox.getChildren().add(fieldFormatterCleanupsPanel);
        saveInOriginalOrder.selectedProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        saveInSpecifiedOrder.selectedProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());

        protect.selectedProperty().bindBidirectional(viewModel.libraryProtectedProperty());
        saveOrderPanel.getJFXPanel().disableProperty().bind(viewModel.saveInOriginalProperty());

        setValues();
        updateEnableStatus();
    }

    private void setValues() {

        Optional<SaveOrderConfig> storedSaveOrderConfig = panel.getBibDatabaseContext().getMetaData().getSaveOrderConfig();
        boolean selected;
        if (!storedSaveOrderConfig.isPresent()) {
            viewModel.saveInOriginalProperty().setValue(true);
            oldSaveOrderConfig = SaveOrderConfig.getDefaultSaveOrder();
            selected = false;
        } else {
            SaveOrderConfig saveOrderConfig = storedSaveOrderConfig.get();
            oldSaveOrderConfig = saveOrderConfig;
            if (saveOrderConfig.saveInOriginalOrder()) {
                viewModel.saveInOriginalProperty().setValue(true);
                selected = false;
            } else {
                viewModel.saveInSpecifiedOrderProperty().setValue(true);
                selected = true;
            }
            saveOrderPanel.setSaveOrderConfig(saveOrderConfig);

        }
        saveOrderPanel.setEnabled(selected);

        fieldFormatterCleanupsPanel.setValues(panel.getBibDatabaseContext().getMetaData());
    }

    public void updateEnableStatus() {
        DatabaseLocation location = panel.getBibDatabaseContext().getLocation();
        boolean isShared = (location == DatabaseLocation.SHARED);
        encoding.setDisable(isShared); // the encoding of shared database is always UTF-8
        saveInOriginalOrder.setDisable(isShared);
        saveInSpecifiedOrder.setDisable(isShared);
        saveOrderPanel.setEnabled(!isShared);
        protect.setDisable(isShared);
    }

    @FXML
    public void browseGeneralFileDirectory(ActionEvent event) {
        viewModel.browseGeneralDir();
    }

    @FXML
    public void browseUserSpecificFileDirectory(ActionEvent event) {
        viewModel.browseUserDir();
    }

    private void storeSettings() {
        MetaData metaData = panel.getBibDatabaseContext().getMetaData();
        Charset oldEncoding = metaData.getEncoding()
                                      .orElse(preferencesService.getDefaultEncoding());
        Charset newEncoding = viewModel.selectedEncodingProperty().getValue();
        metaData.setEncoding(newEncoding);

        String text = viewModel.generalFileDirectoryPropertyProperty().getValue().trim();
        if (text.isEmpty()) {
            metaData.clearDefaultFileDirectory();
        } else {
            metaData.setDefaultFileDirectory(text);
        }
        // Repeat for individual file dir - reuse 'text' and 'dir' vars
        text = viewModel.userSpecificFileDirectoryProperty().getValue();
        if (text.isEmpty()) {
            metaData.clearUserFileDirectory(preferencesService.getUser());
        } else {
            metaData.setUserFileDirectory(preferencesService.getUser(), text);
        }

        if (viewModel.libraryProtectedProperty().getValue()) {
            metaData.markAsProtected();
        } else {
            metaData.markAsNotProtected();
        }

        SaveOrderConfig newSaveOrderConfig = getNewSaveOrderConfig();

        boolean saveOrderConfigChanged = !getNewSaveOrderConfig().equals(oldSaveOrderConfig);

        // See if any of the values have been modified:
        if (saveOrderConfigChanged) {
            if (newSaveOrderConfig.equals(SaveOrderConfig.getDefaultSaveOrder())) {
                metaData.clearSaveOrderConfig();
            } else {
                metaData.setSaveOrderConfig(newSaveOrderConfig);
            }
        }

        boolean saveActionsChanged = fieldFormatterCleanupsPanel.hasChanged();
        if (saveActionsChanged) {
            if (fieldFormatterCleanupsPanel.isDefaultSaveActions()) {
                metaData.clearSaveActions();
            } else {
                fieldFormatterCleanupsPanel.storeSettings(metaData);
            }
        }

        boolean changed = saveOrderConfigChanged || !newEncoding.equals(oldEncoding)
                          || viewModel.generalFileDirChanged() || viewModel.userFileDirChanged()
                          || viewModel.protectedValueChanged() || saveActionsChanged;
        // ... if so, mark base changed. Prevent the Undo button from removing
        // change marking:
        if (changed) {
            panel.markNonUndoableBaseChanged();
        }
    }

    private SaveOrderConfig getNewSaveOrderConfig() {
        SaveOrderConfig saveOrderConfig = null;
        if (saveInOriginalOrder.isSelected()) {
            saveOrderConfig = SaveOrderConfig.getDefaultSaveOrder();
        } else {
            saveOrderConfig = saveOrderPanel.getSaveOrderConfig();
            saveOrderConfig.setSaveInSpecifiedOrder();
        }
        return saveOrderConfig;
    }

}
