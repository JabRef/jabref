package org.jabref.gui.libraryproperties;

import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Optional;

import javax.inject.Inject;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryPropertiesDialogView extends BaseDialog<Void> {

    @FXML private VBox contentVbox;
    @FXML private ComboBox<Charset> encoding;
    @FXML private ComboBox<String> databaseMode;
    @FXML private TextField generalFileDirectory;
    @FXML private TextField userSpecificFileDirectory;
    @FXML private TextField laTexFileDirectory;
    @FXML private CheckBox protect;
    @FXML private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;
    @Inject private PreferencesService preferencesService;

    private BasePanel panel;
    private LibraryPropertiesDialogViewModel viewModel;
    private final DialogService dialogService;
    private FieldFormatterCleanups oldFieldFormatterCleanups;
    private SaveOrderConfigPanel saveOrderConfigPanel;
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
        laTexFileDirectory.textProperty().bindBidirectional(viewModel.laTexFileDirectoryProperty());

        encoding.itemsProperty().bind(viewModel.encodingsProperty());
        encoding.valueProperty().bindBidirectional(viewModel.selectedEncodingProperty());
        encoding.disableProperty().bind(viewModel.encodingDisableProperty());
        protect.disableProperty().bind(viewModel.protectDisableProperty());

        databaseMode.itemsProperty().bind(viewModel.databaseModesProperty());
        databaseMode.valueProperty().bindBidirectional(viewModel.selectedDatabaseModeProperty());

        saveOrderConfigPanel = new SaveOrderConfigPanel();
        Optional<SaveOrderConfig> storedSaveOrderConfig = panel.getBibDatabaseContext().getMetaData().getSaveOrderConfig();
        oldSaveOrderConfig = storedSaveOrderConfig.orElseGet(preferencesService::loadExportSaveOrder);

        saveOrderConfigPanel.changeExportDescriptionToSave();

        contentVbox.getChildren().addAll(saveOrderConfigPanel);

        protect.selectedProperty().bindBidirectional(viewModel.libraryProtectedProperty());

        setValues();
    }

    private void setValues() {
        Optional<FieldFormatterCleanups> saveActions = panel.getBibDatabaseContext().getMetaData().getSaveActions();
        saveActions.ifPresentOrElse(value -> {
            oldFieldFormatterCleanups = value;
            fieldFormatterCleanupsPanel.cleanupsDisableProperty().setValue(!value.isEnabled());
            fieldFormatterCleanupsPanel.cleanupsProperty().setValue(FXCollections.observableArrayList(value.getConfiguredActions()));
        }, () -> {
            oldFieldFormatterCleanups = Cleanups.DEFAULT_SAVE_ACTIONS;
            fieldFormatterCleanupsPanel.cleanupsDisableProperty().setValue(!Cleanups.DEFAULT_SAVE_ACTIONS.isEnabled());
            fieldFormatterCleanupsPanel.cleanupsProperty().setValue(FXCollections.observableArrayList(Cleanups.DEFAULT_SAVE_ACTIONS.getConfiguredActions()));
        });

        saveOrderConfigPanel.setValues(oldSaveOrderConfig);
    }

    @FXML
    public void browseGeneralFileDirectory(ActionEvent event) {
        viewModel.browseGeneralDir();
    }

    @FXML
    public void browseUserSpecificFileDirectory(ActionEvent event) {
        viewModel.browseUserDir();
    }

    @FXML
    void browseLatexFileDirectory(ActionEvent event) {
        viewModel.browseLatexDir();
    }

    private void storeSettings() {
        //FIXME: Move to viewModel until fieldFormatterCleanupsPanel is property implemented
        MetaData metaData = panel.getBibDatabaseContext().getMetaData();
        Charset oldEncoding = metaData.getEncoding()
                                      .orElse(preferencesService.getDefaultEncoding());
        Charset newEncoding = viewModel.selectedEncodingProperty().getValue();
        metaData.setEncoding(newEncoding);

        BibDatabaseMode newMode = BibDatabaseMode.parse(viewModel.selectedDatabaseModeProperty().getValue());
        metaData.setMode(newMode);

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

        text = viewModel.laTexFileDirectoryProperty().getValue();
        if (text.isEmpty()) {
            metaData.clearLatexFileDirectory(preferencesService.getUser());
        } else {
            metaData.setLatexFileDirectory(preferencesService.getUser(), Paths.get(text));
        }

        if (viewModel.libraryProtectedProperty().getValue()) {
            metaData.markAsProtected();
        } else {
            metaData.markAsNotProtected();
        }

        SaveOrderConfig newSaveOrderConfig = saveOrderConfigPanel.getSaveOrderConfig();

        boolean saveOrderConfigChanged = !newSaveOrderConfig.equals(oldSaveOrderConfig);

        // See if any of the values have been modified:
        if (saveOrderConfigChanged) {
            if (newSaveOrderConfig.equals(SaveOrderConfig.getDefaultSaveOrder())) {
                metaData.clearSaveOrderConfig();
            } else {
                metaData.setSaveOrderConfig(newSaveOrderConfig);
            }
        }

        FieldFormatterCleanups fieldFormatterCleanups = new FieldFormatterCleanups(
                !fieldFormatterCleanupsPanel.cleanupsDisableProperty().getValue(),
                fieldFormatterCleanupsPanel.cleanupsProperty());

        if (Cleanups.DEFAULT_SAVE_ACTIONS.equals(fieldFormatterCleanups)) {
            metaData.clearSaveActions();
        } else {
            // if all actions have been removed, remove the save actions from the MetaData
            if (fieldFormatterCleanups.getConfiguredActions().isEmpty()) {
                metaData.clearSaveActions();
            } else {
                metaData.setSaveActions(fieldFormatterCleanups);
            }
        }

        boolean encodingChanged = !newEncoding.equals(oldEncoding);

        boolean changed = saveOrderConfigChanged || encodingChanged
                || viewModel.generalFileDirChanged() || viewModel.userFileDirChanged()
                || viewModel.protectedValueChanged() || viewModel.laTexFileDirChanged()
                || !(oldFieldFormatterCleanups.equals(fieldFormatterCleanups));
        // ... if so, mark base changed. Prevent the Undo button from removing
        // change marking:
        if (changed) {
            panel.markNonUndoableBaseChanged();
        }
    }
}
