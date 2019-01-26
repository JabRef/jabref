package org.jabref.gui.dbproperties;

import java.nio.charset.Charset;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

    public LibraryPropertiesDialogView(DialogService dialogService) {
        this.dialogService = dialogService;
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(btn -> {

            return null;
        });

        setTitle(Localization.lang("Library properties"));

    }

    @FXML
    private void initialize() {
        viewModel = new LibraryPropertiesDialogViewModel(dialogService, preferencesService.getWorkingDir());

        generalFileDirectory.textProperty().bindBidirectional(viewModel.generalFileDirectoryPropertyProperty());
        userSpecificFileDirectory.textProperty().bindBidirectional(viewModel.userSpecificFileDirectoryProperty());

        encoding.valueProperty().bind(viewModel.selectedEncodingProperty());
        saveOrderPanel = new SaveOrderConfigDisplay();
        saveOrdervbox.getChildren().add(saveOrderPanel.getJFXPanel());
        FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel = new FieldFormatterCleanupsPanel(Localization.lang("Enable save actions"),
                                                                                                  Cleanups.DEFAULT_SAVE_ACTIONS);

        contentVbox.getChildren().add(fieldFormatterCleanupsPanel);
        saveInOriginalOrder.selectedProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        saveInSpecifiedOrder.selectedProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
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

    @FXML
    public void browseGeneralFileDirectory(ActionEvent event) {
        viewModel.browseGeneralDir();
    }

    @FXML
    public void browseUserSpecificFileDirectory(ActionEvent event) {
        viewModel.browseUserDir();
    }
}
