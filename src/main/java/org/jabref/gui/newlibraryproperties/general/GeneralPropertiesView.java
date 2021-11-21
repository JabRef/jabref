package org.jabref.gui.newlibraryproperties.general;

import java.nio.charset.Charset;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.newlibraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.newlibraryproperties.PropertiesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class GeneralPropertiesView extends AbstractPropertiesTabView<GeneralPropertiesViewModel> implements PropertiesTab {
    @FXML public ComboBox<Charset> encoding;
    @FXML public ComboBox<BibDatabaseMode> databaseMode;
    @FXML public TextField generalFileDirectory;
    @FXML public TextField userSpecificFileDirectory;
    @FXML public TextField laTexFileDirectory;

    @Inject private PreferencesService preferencesService;

    public GeneralPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }

    public void initialize() {
        this.viewModel = new GeneralPropertiesViewModel(databaseContext, dialogService, preferencesService);

        new ViewModelListCellFactory<Charset>()
                .withText(Charset::displayName)
                .install(encoding);
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
