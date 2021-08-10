package org.jabref.gui.libraryproperties;

import java.nio.charset.Charset;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryPropertiesDialogView extends BaseDialog<Void> {

    @FXML private ComboBox<Charset> encoding;
    @FXML private ComboBox<BibDatabaseMode> databaseMode;
    @FXML private TextField generalFileDirectory;
    @FXML private TextField userSpecificFileDirectory;
    @FXML private TextField laTexFileDirectory;
    @FXML private CheckBox protect;

    @FXML private SaveOrderConfigPanel saveOrderConfigPanel;
    @FXML private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;

    private final LibraryTab libraryTab;
    private LibraryPropertiesDialogViewModel viewModel;

    public LibraryPropertiesDialogView(LibraryTab libraryTab) {
        this.libraryTab = libraryTab;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                viewModel.storeSettings();
            }
            return null;
        });

        setTitle(Localization.lang("Library properties"));
    }

    @FXML
    private void initialize() {
        viewModel = new LibraryPropertiesDialogViewModel(libraryTab.getBibDatabaseContext(), dialogService, preferencesService);

        encoding.disableProperty().bind(viewModel.encodingDisableProperty());
        encoding.itemsProperty().bind(viewModel.encodingsProperty());
        encoding.valueProperty().bindBidirectional(viewModel.selectedEncodingProperty());

        new ViewModelListCellFactory<BibDatabaseMode>()
                .withText(BibDatabaseMode::getFormattedName)
                .install(databaseMode);
        databaseMode.itemsProperty().bind(viewModel.databaseModesProperty());
        databaseMode.valueProperty().bindBidirectional(viewModel.selectedDatabaseModeProperty());

        generalFileDirectory.textProperty().bindBidirectional(viewModel.generalFileDirectoryPropertyProperty());
        userSpecificFileDirectory.textProperty().bindBidirectional(viewModel.userSpecificFileDirectoryProperty());
        laTexFileDirectory.textProperty().bindBidirectional(viewModel.laTexFileDirectoryProperty());

        protect.disableProperty().bind(viewModel.protectDisableProperty());
        protect.selectedProperty().bindBidirectional(viewModel.libraryProtectedProperty());

        saveOrderConfigPanel.saveInOriginalProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        saveOrderConfigPanel.saveInTableOrderProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        saveOrderConfigPanel.saveInSpecifiedOrderProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        saveOrderConfigPanel.sortableFieldsProperty().bind(viewModel.sortableFieldsProperty());
        saveOrderConfigPanel.sortCriteriaProperty().bindBidirectional(viewModel.sortCriteriaProperty());

        fieldFormatterCleanupsPanel.cleanupsDisableProperty().bindBidirectional(viewModel.cleanupsDisableProperty());
        fieldFormatterCleanupsPanel.cleanupsProperty().bindBidirectional(viewModel.cleanupsProperty());
    }

    @FXML
    public void browseGeneralFileDirectory() {
        viewModel.browseGeneralDir();
    }

    @FXML
    public void browseUserSpecificFileDirectory() {
        viewModel.browseUserDir();
    }

    @FXML
    void browseLatexFileDirectory() {
        viewModel.browseLatexDir();
    }
}
