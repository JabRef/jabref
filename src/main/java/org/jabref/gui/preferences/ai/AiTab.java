package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

import org.controlsfx.control.textfield.CustomPasswordField;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.AiPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.DoubleInputField;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    @FXML private CheckBox enableChat;

    @FXML private ComboBox<AiPreferences.AiProvider> aiProviderComboBox;
    @FXML private ComboBox<String> chatModelComboBox;
    @FXML private CustomPasswordField apiTokenTextField;

    @FXML private CheckBox customizeSettingsCheckbox;

    @FXML private ComboBox<AiPreferences.EmbeddingModel> embeddingModelComboBox;
    @FXML private TextArea systemMessageTextArea;
    @FXML private DoubleInputField temperatureTextField;
    @FXML private IntegerInputField messageWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private DoubleInputField ragMinScoreTextField;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    @FXML private Button aiProviderHelp;
    @FXML private Button chatModelHelp;
    @FXML private Button apiTokenHelp;
    @FXML private Button embeddingModelHelp;
    @FXML private Button systemMessageHelp;
    @FXML private Button messageWindowSizeHelp;
    @FXML private Button documentSplitterChunkSizeHelp;
    @FXML private Button documentSplitterOverlapSizeHelp;
    @FXML private Button ragMaxResultsCountHelp;
    @FXML private Button ragMinScoreHelp;

    @FXML private Button resetExpertSettingsButton;

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferencesService);

        enableChat.selectedProperty().bindBidirectional(viewModel.useAiProperty());

        new ViewModelListCellFactory<AiPreferences.AiProvider>()
            .withText(AiPreferences.AiProvider::getName)
            .install(aiProviderComboBox);

        aiProviderComboBox.itemsProperty().bind(viewModel.aiProvidersProperty());
        aiProviderComboBox.valueProperty().bindBidirectional(viewModel.selectedAiProviderProperty());

        chatModelComboBox.itemsProperty().bind(viewModel.chatModelsProperty());
        chatModelComboBox.valueProperty().bindBidirectional(viewModel.selectedChatModelProperty());

        apiTokenTextField.textProperty().bindBidirectional(viewModel.apiTokenProperty());

        customizeSettingsCheckbox.selectedProperty().bindBidirectional(viewModel.customizeSettingsProperty());

        embeddingModelComboBox.getItems().addAll(AiPreferences.EmbeddingModel.values());
        embeddingModelComboBox.valueProperty().bindBidirectional(viewModel.embeddingModelProperty());

        systemMessageTextArea.textProperty().bindBidirectional(viewModel.systemMessageProperty());
        temperatureTextField.valueProperty().bindBidirectional(viewModel.temperatureProperty().asObject());
        messageWindowSizeTextField.valueProperty().bindBidirectional(viewModel.messageWindowSizeProperty().asObject());
        documentSplitterChunkSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterChunkSizeProperty().asObject());
        documentSplitterOverlapSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterOverlapSizeProperty().asObject());
        ragMaxResultsCountTextField.valueProperty().bindBidirectional(viewModel.ragMaxResultsCountProperty().asObject());
        ragMinScoreTextField.valueProperty().bindBidirectional(viewModel.ragMinScoreProperty().asObject());

        updateDisabilities();

        enableChat.selectedProperty().addListener(obs -> updateDisabilities());

        customizeSettingsCheckbox.selectedProperty().addListener(obs -> updateDisabilities());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.getApiTokenValidatorStatus(), apiTokenTextField);
            visualizer.initVisualization(viewModel.getSystemMessageValidatorStatus(), systemMessageTextArea);
            visualizer.initVisualization(viewModel.getTemperatureValidatorStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getMessageWindowSizeValidatorStatus(), messageWindowSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterChunkSizeValidatorStatus(), documentSplitterChunkSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterOverlapSizeValidatorStatus(), documentSplitterOverlapSizeTextField);
            visualizer.initVisualization(viewModel.getRagMaxResultsCountValidatorStatus(), ragMaxResultsCountTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreValidatorStatus(), ragMinScoreTextField);
        });

        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_PROVIDER, dialogService, preferencesService.getFilePreferences()), aiProviderHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_CHAT_MODEL, dialogService, preferencesService.getFilePreferences()), chatModelHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_API_TOKEN, dialogService, preferencesService.getFilePreferences()), apiTokenHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_EMBEDDING_MODEL, dialogService, preferencesService.getFilePreferences()), embeddingModelHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_SYSTEM_MESSAGE, dialogService, preferencesService.getFilePreferences()), systemMessageHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_MESSAGE_WINDOW_SIZE, dialogService, preferencesService.getFilePreferences()), messageWindowSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_DOCUMENT_SPLITTER_CHUNK_SIZE, dialogService, preferencesService.getFilePreferences()), documentSplitterChunkSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, dialogService, preferencesService.getFilePreferences()), documentSplitterOverlapSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_RAG_MAX_RESULTS_COUNT, dialogService, preferencesService.getFilePreferences()), ragMaxResultsCountHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_RAG_MIN_SCORE, dialogService, preferencesService.getFilePreferences()), ragMinScoreHelp);
    }

    private void updateDisabilities() {
        aiProviderComboBox.setDisable(!viewModel.getUseAi());
        chatModelComboBox.setDisable(!viewModel.getUseAi());
        apiTokenTextField.setDisable(!viewModel.getUseAi());

        customizeSettingsCheckbox.setDisable(!viewModel.getUseAi());

        embeddingModelComboBox.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());

        systemMessageTextArea.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        temperatureTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        messageWindowSizeTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        documentSplitterChunkSizeTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        documentSplitterOverlapSizeTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        ragMaxResultsCountTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        ragMinScoreTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        resetExpertSettingsButton.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
    }

    @Override
    public String getTabName() {
        return Localization.lang("AI");
    }

    @FXML
    private void onResetExpertSettingsButtonClick() {
        viewModel.resetExpertSettings();
    }
}
