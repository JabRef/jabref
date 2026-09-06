package org.jabref.gui.shared;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

/// This offers the user to connect to a remove SQL database.
/// Moreover, it directly opens the shared database after successful connection.
public class SharedDatabaseLoginDialogView extends BaseDialog<Void> {
    @FXML private TextField host;
    @FXML private TextField database;
    @FXML private TextField port;
    @FXML private TextField user;
    @FXML private PasswordField password;
    @FXML private CheckBox rememberPassword;
    @FXML private SplitPane rememberPasswordWrapper;
    @FXML private TextField folder;
    @FXML private Button browseButton;
    @FXML private CheckBox autosave;
    @FXML private ButtonType connectButton;
    @FXML private CheckBox useSSL;
    @FXML private TextField jdbcUrl;
    @FXML private CheckBox expertMode;
    @FXML private TextField connectionUrl;
    @FXML private TitledPane advancedPane;

    @Inject private DialogService dialogService;
    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private StateManager stateManager;
    @Inject private BibEntryTypesManager entryTypesManager;
    @Inject private FileUpdateMonitor fileUpdateMonitor;
    @Inject private ClipBoardManager clipBoardManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private JournalAbbreviationRepository journalAbbreviationRepository;
    @Inject private GitHandlerRegistry gitHandlerRegistry;

    private final LibraryTabContainer tabContainer;
    private SharedDatabaseLoginDialogViewModel viewModel;
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public SharedDatabaseLoginDialogView(LibraryTabContainer tabContainer) {
        this.tabContainer = tabContainer;
        this.setTitle(Localization.lang("Connect to shared database"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(connectButton, this.getDialogPane(), event -> openDatabase());
        Button btnConnect = (Button) this.getDialogPane().lookupButton(connectButton);
        // must be set here, because in initialize the button is still null
        btnConnect.disableProperty().bind(viewModel.formValidation().validProperty().not().or(viewModel.loadingProperty()));
        btnConnect.textProperty().bind(EasyBind.map(viewModel.loadingProperty(), loading -> loading ? Localization.lang("Connecting...") : Localization.lang("Connect")));
    }

    @FXML
    private void openDatabase() {
        viewModel.openDatabase(this::close);
    }

    @FXML
    private void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        viewModel = new SharedDatabaseLoginDialogViewModel(
                tabContainer,
                dialogService,
                preferences,
                aiService,
                stateManager,
                entryTypesManager,
                fileUpdateMonitor,
                clipBoardManager,
                taskExecutor,
                journalAbbreviationRepository,
                gitHandlerRegistry);
        connectionUrl.textProperty().bindBidirectional(viewModel.connectionUrlProperty());
        database.textProperty().bindBidirectional(viewModel.databaseproperty());
        host.textProperty().bindBidirectional(viewModel.hostProperty());
        user.textProperty().bindBidirectional(viewModel.userProperty());
        password.textProperty().bindBidirectional(viewModel.passwordProperty());
        port.textProperty().bindBidirectional(viewModel.portProperty());

        folder.textProperty().bindBidirectional(viewModel.folderProperty());
        browseButton.disableProperty().bind(viewModel.autosaveProperty().not());
        folder.disableProperty().bind(viewModel.autosaveProperty().not());
        autosave.selectedProperty().bindBidirectional(viewModel.autosaveProperty());

        useSSL.selectedProperty().bindBidirectional(viewModel.useSSLProperty());

        expertMode.selectedProperty().bindBidirectional(viewModel.expertModeProperty());
        jdbcUrl.textProperty().bindBidirectional(viewModel.jdbcUrlProperty());
        jdbcUrl.disableProperty().bind(viewModel.expertModeProperty().not());
        host.disableProperty().bind(viewModel.expertModeProperty());
        port.disableProperty().bind(viewModel.expertModeProperty());
        database.disableProperty().bind(viewModel.expertModeProperty());

        rememberPassword.selectedProperty().bindBidirectional(viewModel.rememberPasswordProperty());
        if (!viewModel.isKeyringAvailable()) {
            rememberPassword.setDisable(true);
            // A disabled control gets no mouse events, so the tooltip has to sit on the wrapper
            rememberPasswordWrapper.setTooltip(new Tooltip(Localization.lang("Credential store not available.")));
        }

        // Settings a pasted URL or the last login switched on must not stay hidden
        EasyBind.subscribe(viewModel.useSSLProperty(), this::expandAdvancedIf);
        EasyBind.subscribe(viewModel.expertModeProperty(), this::expandAdvancedIf);
        EasyBind.subscribe(advancedPane.expandedProperty(), expanded -> Platform.runLater(() -> {
            if (getDialogPane().getScene() != null) {
                getDialogPane().getScene().getWindow().sizeToScene();
            }
        }));

        // Must be executed after the initialization of the view, otherwise it doesn't work
        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.connectionUrlValidation(), connectionUrl, false);
            visualizer.initVisualization(viewModel.dbValidation(), database, true);
            visualizer.initVisualization(viewModel.hostValidation(), host, true);
            visualizer.initVisualization(viewModel.portValidation(), port, true);
            visualizer.initVisualization(viewModel.userValidation(), user, true);

            EasyBind.subscribe(autosave.selectedProperty(), selected ->
                    visualizer.initVisualization(viewModel.folderValidation(), folder, true));
        });
    }

    private void expandAdvancedIf(boolean active) {
        if (active) {
            advancedPane.setExpanded(true);
        }
    }

    @FXML
    private void showSaveDbToFileDialog(ActionEvent event) {
        viewModel.showSaveDbToFileDialog();
    }
}
