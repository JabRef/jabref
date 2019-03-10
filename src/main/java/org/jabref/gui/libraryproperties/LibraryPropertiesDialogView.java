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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.SaveOrderConfigDisplayView;
import org.jabref.gui.cleanup.FieldFormatterCleanupsPanel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.l10n.Localization;
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
    @FXML private CheckBox protect;
    @Inject private PreferencesService preferencesService;

    private BasePanel panel;
    private LibraryPropertiesDialogViewModel viewModel;
    private final DialogService dialogService;
    private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;
    private SaveOrderConfigDisplayView saveOrderConfigDisplayView;
    private SaveOrderConfig oldSaveOrderConfig;

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
        encoding.disableProperty().bind(viewModel.encodingDisableProperty());
        protect.disableProperty().bind(viewModel.protectDisableProperty());

        Optional<SaveOrderConfig> storedSaveOrderConfig = panel.getBibDatabaseContext().getMetaData().getSaveOrderConfig();
        if (storedSaveOrderConfig.isPresent()) {
            saveOrderConfigDisplayView = new SaveOrderConfigDisplayView(storedSaveOrderConfig.get());
            oldSaveOrderConfig = storedSaveOrderConfig.get();
        } else {
            oldSaveOrderConfig = preferencesService.loadExportSaveOrder();
            saveOrderConfigDisplayView = new SaveOrderConfigDisplayView(preferencesService.loadExportSaveOrder());
        }

        saveOrderConfigDisplayView.changeExportDescriptionToSave();
        fieldFormatterCleanupsPanel = new FieldFormatterCleanupsPanel(Localization.lang("Enable save actions"),
                                                                      Cleanups.DEFAULT_SAVE_ACTIONS);
        Label saveActions = new Label(Localization.lang("Save actions"));
        saveActions.getStyleClass().add("sectionHeader");

        contentVbox.getChildren().addAll(saveOrderConfigDisplayView, saveActions, fieldFormatterCleanupsPanel);

        protect.selectedProperty().bindBidirectional(viewModel.libraryProtectedProperty());

        setValues();
    }

    private void setValues() {
        fieldFormatterCleanupsPanel.setValues(panel.getBibDatabaseContext().getMetaData());
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
        //FIXME: Move to viewModel until fieldFormatterCleanupsPanel is property implemented
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

        SaveOrderConfig newSaveOrderConfig = saveOrderConfigDisplayView.getSaveOrderConfig();

        boolean saveOrderConfigChanged = !newSaveOrderConfig.equals(oldSaveOrderConfig);

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
        boolean encodingChanged = !newEncoding.equals(oldEncoding);

        boolean changed = saveOrderConfigChanged || encodingChanged
                          || viewModel.generalFileDirChanged() || viewModel.userFileDirChanged()
                          || viewModel.protectedValueChanged() || saveActionsChanged;
        // ... if so, mark base changed. Prevent the Undo button from removing
        // change marking:
        if (changed) {
            panel.markNonUndoableBaseChanged();
        }
    }
}
