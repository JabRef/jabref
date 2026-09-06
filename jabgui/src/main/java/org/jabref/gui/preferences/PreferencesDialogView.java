package org.jabref.gui.preferences;

import java.util.Locale;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.textfield.CustomTextField;

/// Preferences dialog. Contains a TabbedPane, and tabs will be defined in separate classes. Tabs MUST implement the
/// PreferencesTab interface, since this dialog will call the storeSettings() method of all tabs when the user presses
/// ok.
public class PreferencesDialogView extends BaseDialog<PreferencesDialogViewModel> {

    public static final String DIALOG_TITLE = Localization.lang("JabRef preferences");
    @FXML private CustomTextField searchBox;
    @FXML private ListView<PreferencesTab> preferencesTabList;
    @FXML private Label tabTitle;
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

        PreferencesSearchHandler searchHandler = new PreferencesSearchHandler(viewModel.getPreferenceTabs());
        preferencesTabList.itemsProperty().bindBidirectional(searchHandler.filteredPreferenceTabsProperty());
        searchBox.textProperty().addListener((observable, previousText, newText) -> {
            searchHandler.filterTabs(newText.toLowerCase(Locale.ROOT));
            preferencesTabList.getSelectionModel().clearSelection();
            preferencesTabList.getSelectionModel().selectFirst();
        });
        searchBox.setPromptText(Localization.lang("Search..."));
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());

        EasyBind.subscribe(preferencesTabList.getSelectionModel().selectedItemProperty(), tab -> {
            if (tab == null) {
                tabTitle.setText("");
                preferencesContainer.setContent(null);
                return;
            }
            tabTitle.setText(tab.getTitle());
            Node content = tab.getContent();
            preferencesContainer.setContent(content);
            if (content instanceof Region region) {
                region.prefWidthProperty().bind(preferencesContainer.widthProperty().subtract(10d));
            }
            content.getStyleClass().add("padding-6");
        });

        if (this.preferencesTabToSelectClass != null) {
            Optional<PreferencesTab> tabToSelectIfExist = preferencesTabList.getItems()
                                                                            .stream()
                                                                            .filter(prefTab -> prefTab.getClass().equals(preferencesTabToSelectClass))
                                                                            .findFirst();
            tabToSelectIfExist.ifPresent(preferencesTab -> preferencesTabList.getSelectionModel().select(preferencesTab));
        } else {
            preferencesTabList.getSelectionModel().selectFirst();
        }

        new ViewModelListCellFactory<PreferencesTab>()
                .withText(PreferencesTab::getTabName)
                .install(preferencesTabList);

        memoryStickMode.selectedProperty().bindBidirectional(viewModel.getMemoryStickProperty());

        viewModel.setValues();
    }

    @FXML
    private void closeDialog() {
        close();
    }

    @FXML
    private void savePreferencesAndCloseDialog() {
        if (viewModel.storeAllSettings()) {
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
