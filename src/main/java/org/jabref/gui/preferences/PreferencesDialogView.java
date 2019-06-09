package org.jabref.gui.preferences;

import java.util.Locale;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.easybind.EasyBind;

/**
 * Preferences dialog. Contains a TabbedPane, and tabs will be defined in separate classes. Tabs MUST implement the
 * PrefsTab interface, since this dialog will call the storeSettings() method of all tabs when the user presses ok.
 */
public class PreferencesDialogView extends BaseDialog<PreferencesDialogViewModel> {

    @FXML private CustomTextField searchBox;
    @FXML private ListView<PrefsTab> preferenceTabList;
    @FXML private ScrollPane preferencePaneContainer;
    @FXML private ButtonType saveButton;

    @Inject private DialogService dialogService;

    private JabRefFrame frame;
    private TaskExecutor taskExecutor;
    private PreferencesDialogViewModel viewModel;

    public PreferencesDialogView(JabRefFrame frame, TaskExecutor taskExecutor) {
        this.frame = frame;
        this.taskExecutor = taskExecutor;
        this.setTitle(Localization.lang("JabRef preferences"));

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> savePreferencesAndCloseDialog());

        // ToDo: After conversion of all tabs to mvvm, make validSettings bindable
        // ControlHelper.getButton(saveButton, getDialogPane()).disableProperty().bind(viewModel.validSettings().not());
    }

    public PreferencesDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void initialize() {
        viewModel = new PreferencesDialogViewModel(dialogService, taskExecutor, frame);

        preferenceTabList.itemsProperty().setValue(viewModel.getPreferenceTabs());

        PreferencesSearchHandler searchHandler = new PreferencesSearchHandler(viewModel.getPreferenceTabs());
        preferenceTabList.itemsProperty().bindBidirectional(searchHandler.filteredPreferenceTabsProperty());
        searchBox.textProperty().addListener((observable, previousText, newText) -> {
            searchHandler.filterTabs(newText.toLowerCase(Locale.ROOT));
            preferenceTabList.getSelectionModel().clearSelection();
            preferenceTabList.getSelectionModel().selectFirst();
        });
        searchBox.setPromptText(Localization.lang("Search") + "...");
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());

        EasyBind.subscribe(preferenceTabList.getSelectionModel().selectedItemProperty(), tab -> {
            if (tab != null) {
                preferencePaneContainer.setContent(tab.getBuilder());
            } else {
                preferencePaneContainer.setContent(null);
            }
        });

        preferenceTabList.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<PrefsTab>()
                .withText(PrefsTab::getTabName)
                .install(preferenceTabList);

        viewModel.setValues(); // ToDo: Remove this after conversion of all tabs
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
        viewModel.importPreferences();
    }

    @FXML
    void showAllPreferences() {
        viewModel.showPreferences();
    }

    @FXML
    void resetPreferences() {
        viewModel.resetPreferences();
    }

    public static void createValidationVisualization(final ValidationStatus status, final Control control) {
        ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
        validationVisualizer.initVisualization(status, control);
        validationVisualizer.setDecoration(new IconValidationDecorator());
    }
}
