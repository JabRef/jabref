package org.jabref.gui.preferences;

import java.util.Locale;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.textfield.CustomTextField;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined in separate classes. Tabs MUST implement the
 * PreferencesTab interface, since this dialog will call the storeSettings() method of all tabs when the user presses
 * ok.
 */
public class PreferencesDialogView extends BaseDialog<PreferencesDialogViewModel> {

    public static final String DIALOG_TITLE = Localization.lang("JabRef preferences");
    @FXML private CustomTextField searchBox;
    @FXML private ListView<PreferencesTab> preferenceTabList;
    @FXML private ScrollPane preferencesContainer;
    @FXML private ButtonType saveButton;
    @FXML private ButtonType cancelButton;
    @FXML private ToggleButton memoryStickMode;

    @Inject private DialogService dialogService;
    @Inject private GuiPreferences preferences;

    private PreferencesDialogViewModel viewModel;
    private final Class<? extends PreferencesTab> preferencesTabToSelectClass;

    public PreferencesDialogView(Class<? extends PreferencesTab> preferencesTabToSelectClass) {
        this.setTitle(DIALOG_TITLE);
        this.preferencesTabToSelectClass = preferencesTabToSelectClass;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), _ -> savePreferencesAndCloseDialog());

        // Stop the default button from firing when the user hits enter within the search box
        searchBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
            }
        });
    }

    public PreferencesDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void initialize() {
        viewModel = new PreferencesDialogViewModel(dialogService, preferences);

        preferenceTabList.itemsProperty().setValue(viewModel.getPreferenceTabs());

        // The list view does not respect the listener for the dialog and needs its own
        preferenceTabList.setOnKeyReleased(key -> {
            if (preferences.getKeyBindingRepository().checkKeyCombinationEquality(KeyBinding.CLOSE, key)) {
                this.closeDialog();
            }
        });

        PreferencesSearchHandler searchHandler = new PreferencesSearchHandler(viewModel.getPreferenceTabs());
        preferenceTabList.itemsProperty().bindBidirectional(searchHandler.filteredPreferenceTabsProperty());
        searchBox.textProperty().addListener((observable, previousText, newText) -> {
            searchHandler.filterTabs(newText.toLowerCase(Locale.ROOT));
            preferenceTabList.getSelectionModel().clearSelection();
            preferenceTabList.getSelectionModel().selectFirst();
        });
        searchBox.setPromptText(Localization.lang("Search..."));
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());

        EasyBind.subscribe(preferenceTabList.getSelectionModel().selectedItemProperty(), tab -> {
            if (tab instanceof AbstractPreferenceTabView<?> preferencesTab) {
                preferencesContainer.setContent(preferencesTab.getBuilder());
                preferencesTab.prefWidthProperty().bind(preferencesContainer.widthProperty().subtract(10d));
                preferencesTab.getStyleClass().add("preferencesTab");
                this.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (preferences.getKeyBindingRepository().checkKeyCombinationEquality(KeyBinding.CLOSE, event)) {
                        if (event.getTarget() instanceof ListView || event.getTarget() instanceof TableView || event.getTarget() instanceof TreeView || event.getTarget() instanceof TreeTableView) {
                            this.closeDialog();
                            event.consume();
                        }
                    }
                });
            } else {
                preferencesContainer.setContent(null);
            }
        });

        if (this.preferencesTabToSelectClass != null) {
            Optional<PreferencesTab> tabToSelectIfExist = preferenceTabList.getItems()
                                                                           .stream()
                                                                           .filter(prefTab -> prefTab.getClass().equals(preferencesTabToSelectClass))
                                                                           .findFirst();
            tabToSelectIfExist.ifPresent(preferencesTab -> preferenceTabList.getSelectionModel().select(preferencesTab));
        } else {
            preferenceTabList.getSelectionModel().selectFirst();
        }

        new ViewModelListCellFactory<PreferencesTab>()
                .withText(PreferencesTab::getTabName)
                .install(preferenceTabList);

        memoryStickMode.selectedProperty().bindBidirectional(viewModel.getMemoryStickProperty());

        viewModel.setValues();
    }

    @FXML
    private void closeDialog() {
        close();
    }

    @FXML
    private void savePreferencesAndCloseDialog() {
        if (viewModel.validSettings()) {
            viewModel.storeAllSettings();
            closeDialog();
        }
    }

    @FXML
    void exportPreferences() {
        viewModel.exportPreferences();
    }

    @FXML
    void importPreferences() {
        if (viewModel.importPreferences()) {
            // Hint the user that preferences are already loaded into the UI
            // ToDo: Import into the ui directly and save changes on click on Save button
            this.getDialogPane().lookupButton(cancelButton).setDisable(true);
        }
    }

    @FXML
    void showAllPreferences() {
        viewModel.showPreferences();
    }

    @FXML
    void resetPreferences() {
        if (viewModel.resetPreferences()) {
            // Hint the user that preferences are already loaded into the UI
            // ToDo: Reset the ui and save changes on click on Save button
            this.getDialogPane().lookupButton(cancelButton).setDisable(true);
        }
    }
}
