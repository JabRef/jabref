package org.jabref.gui.auximport;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;

/**
 * A wizard dialog for generating a new sub database from existing TeX AUX file
 */
public class FromAuxDialog extends BaseDialog<Void> {
    @FXML private ButtonType generateButtonType;
    @FXML private TextField auxFileField;
    @FXML private ListView<String> notFoundList;
    @FXML private TextArea statusInfos;
    @FXML private ComboBox<BibDatabaseContext> libraryListView;

    @Inject private PreferencesService preferences;
    @Inject private DialogService dialogService;
    @Inject private ThemeManager themeManager;
    @Inject private StateManager stateManager;

    private final LibraryTabContainer tabContainer;
    private FromAuxDialogViewModel viewModel;

    public FromAuxDialog(LibraryTabContainer tabContainer) {
        this.tabContainer = tabContainer;
        this.setTitle(Localization.lang("AUX file import"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        Button generateButton = (Button) this.getDialogPane().lookupButton(generateButtonType);
        generateButton.disableProperty().bind(viewModel.parseFailedProperty());
        generateButton.defaultButtonProperty().bind(generateButton.disableProperty().not());
        setResultConverter(button -> {
            if (button == generateButtonType) {
                viewModel.addResultToTabContainer();
            }
            return null;
        });

        themeManager.updateFontStyle(getDialogPane().getScene());
    }

    @FXML
    private void initialize() {
        viewModel = new FromAuxDialogViewModel(tabContainer, dialogService, preferences, stateManager);

        auxFileField.textProperty().bindBidirectional(viewModel.auxFileProperty());
        statusInfos.textProperty().bindBidirectional(viewModel.statusTextProperty());
        notFoundList.itemsProperty().bind(viewModel.notFoundList());

        libraryListView.setEditable(false);
        libraryListView.itemsProperty().bind(viewModel.librariesProperty());
        libraryListView.valueProperty().bindBidirectional(viewModel.selectedLibraryProperty());
        new ViewModelListCellFactory<BibDatabaseContext>()
                .withText(viewModel::getDatabaseName)
                .install(libraryListView);
        EasyBind.listen(libraryListView.getSelectionModel().selectedItemProperty(), (obs, oldValue, newValue) -> parseActionPerformed());
    }

    @FXML
    private void parseActionPerformed() {
        viewModel.parse();
    }

    @FXML
    private void browseButtonClicked() {
        viewModel.browse();
    }
}
