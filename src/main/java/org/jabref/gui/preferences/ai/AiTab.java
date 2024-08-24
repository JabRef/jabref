package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.ai.AiApiKeyProvider;
import org.jabref.preferences.ai.AiProvider;
import org.jabref.preferences.ai.EmbeddingModel;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ResizableTextArea;
import com.dlsc.unitfx.DoubleInputField;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.textfield.CustomPasswordField;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    private static final String HUGGING_FACE_CHAT_MODEL_PROMPT = "TinyLlama/TinyLlama_v1.1 (or any other model name)";

    @Inject private AiApiKeyProvider aiApiKeyProvider;

    @FXML private CheckBox enableAi;

    @FXML private ComboBox<AiProvider> aiProviderComboBox;
    @FXML private ComboBox<String> chatModelComboBox;
    @FXML private CustomPasswordField apiKeyTextField;

    @FXML private CheckBox customizeExpertSettingsCheckbox;

    @FXML private TextField apiBaseUrlTextField;
    @FXML private SearchableComboBox<EmbeddingModel> embeddingModelComboBox;
    @FXML private ResizableTextArea instructionTextArea;
    @FXML private DoubleInputField temperatureTextField;
    @FXML private IntegerInputField contextWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private DoubleInputField ragMinScoreTextField;

    @FXML private Button enableAiHelp;
    @FXML private Button aiProviderHelp;
    @FXML private Button chatModelHelp;
    @FXML private Button apiKeyHelp;
    @FXML private Button apiBaseUrlHelp;
    @FXML private Button embeddingModelHelp;
    @FXML private Button instructionHelp;
    @FXML private Button contextWindowSizeHelp;
    @FXML private Button temperatureHelp;
    @FXML private Button documentSplitterChunkSizeHelp;
    @FXML private Button documentSplitterOverlapSizeHelp;
    @FXML private Button ragMaxResultsCountHelp;
    @FXML private Button ragMinScoreHelp;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferencesService, aiApiKeyProvider);

        enableAi.selectedProperty().bindBidirectional(viewModel.enableAi());

        new ViewModelListCellFactory<AiProvider>()
                .withText(AiProvider::toString)
                .install(aiProviderComboBox);
        aiProviderComboBox.setItems(viewModel.aiProvidersProperty());
        aiProviderComboBox.valueProperty().bindBidirectional(viewModel.selectedAiProviderProperty());
        aiProviderComboBox.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        new ViewModelListCellFactory<String>()
                .withText(text -> text)
                .install(chatModelComboBox);
        chatModelComboBox.setItems(viewModel.chatModelsProperty());
        chatModelComboBox.valueProperty().bindBidirectional(viewModel.selectedChatModelProperty());
        chatModelComboBox.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        this.aiProviderComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == AiProvider.HUGGING_FACE) {
                chatModelComboBox.setPromptText(HUGGING_FACE_CHAT_MODEL_PROMPT);
            }
        });

        apiKeyTextField.textProperty().bindBidirectional(viewModel.apiKeyProperty());
        apiKeyTextField.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        customizeExpertSettingsCheckbox.selectedProperty().bindBidirectional(viewModel.customizeExpertSettingsProperty());
        customizeExpertSettingsCheckbox.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        new ViewModelListCellFactory<EmbeddingModel>()
                .withText(EmbeddingModel::fullInfo)
                .install(embeddingModelComboBox);
        embeddingModelComboBox.setItems(viewModel.embeddingModelsProperty());
        embeddingModelComboBox.valueProperty().bindBidirectional(viewModel.selectedEmbeddingModelProperty());
        embeddingModelComboBox.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        apiBaseUrlTextField.textProperty().bindBidirectional(viewModel.apiBaseUrlProperty());

        viewModel.disableExpertSettingsProperty().addListener((observable, oldValue, newValue) ->
            apiBaseUrlTextField.setDisable(newValue || viewModel.disableApiBaseUrlProperty().get())
        );

        viewModel.disableApiBaseUrlProperty().addListener((observable, oldValue, newValue) ->
            apiBaseUrlTextField.setDisable(newValue || viewModel.disableExpertSettingsProperty().get())
        );

        instructionTextArea.textProperty().bindBidirectional(viewModel.instructionProperty());
        instructionTextArea.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        temperatureTextField.valueProperty().bindBidirectional(viewModel.temperatureProperty().asObject());
        temperatureTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        // bindBidirectional doesn't work well with number input fields ({@link IntegerInputField}, {@link DoubleInputField}),
        // so they are expanded into `addListener` calls.

        contextWindowSizeTextField.valueProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.contextWindowSizeProperty().set(newValue == null ? 0 : newValue);
        });

        viewModel.contextWindowSizeProperty().addListener((observable, oldValue, newValue) -> {
            contextWindowSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue());
        });

        temperatureTextField.valueProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.temperatureProperty().set(newValue == null ? 0 : newValue);
        });

        viewModel.temperatureProperty().addListener((observable, oldValue, newValue) -> {
            temperatureTextField.valueProperty().set(newValue == null ? 0 : newValue.doubleValue());
        });

        temperatureTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        contextWindowSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        documentSplitterChunkSizeTextField.valueProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.documentSplitterChunkSizeProperty().set(newValue == null ? 0 : newValue);
        });

        viewModel.documentSplitterChunkSizeProperty().addListener((observable, oldValue, newValue) -> {
            documentSplitterChunkSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue());
        });

        documentSplitterChunkSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        documentSplitterOverlapSizeTextField.valueProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.documentSplitterOverlapSizeProperty().set(newValue == null ? 0 : newValue);
        });

        viewModel.documentSplitterOverlapSizeProperty().addListener((observable, oldValue, newValue) -> {
            documentSplitterOverlapSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue());
        });

        documentSplitterOverlapSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        ragMaxResultsCountTextField.valueProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.ragMaxResultsCountProperty().set(newValue == null ? 0 : newValue);
        });

        viewModel.ragMaxResultsCountProperty().addListener((observable, oldValue, newValue) -> {
            ragMaxResultsCountTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue());
        });

        ragMaxResultsCountTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        ragMinScoreTextField.valueProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.ragMinScoreProperty().set(newValue == null ? 0.0 : newValue);
        });

        viewModel.ragMinScoreProperty().addListener((observable, oldValue, newValue) -> {
            ragMinScoreTextField.valueProperty().set(newValue == null ? 0.0 : newValue.doubleValue());
        });

        ragMinScoreTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.getApiTokenValidationStatus(), apiKeyTextField);
            visualizer.initVisualization(viewModel.getChatModelValidationStatus(), chatModelComboBox);
            visualizer.initVisualization(viewModel.getApiBaseUrlValidationStatus(), apiBaseUrlTextField);
            visualizer.initVisualization(viewModel.getEmbeddingModelValidationStatus(), embeddingModelComboBox);
            visualizer.initVisualization(viewModel.getSystemMessageValidationStatus(), instructionTextArea);
            visualizer.initVisualization(viewModel.getTemperatureValidationStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getMessageWindowSizeValidationStatus(), contextWindowSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterChunkSizeValidationStatus(), documentSplitterChunkSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterOverlapSizeValidationStatus(), documentSplitterOverlapSizeTextField);
            visualizer.initVisualization(viewModel.getRagMaxResultsCountValidationStatus(), ragMaxResultsCountTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreValidationStatus(), ragMinScoreTextField);
        });

        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_ENABLE, dialogService, preferencesService.getFilePreferences()), enableAiHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_PROVIDER, dialogService, preferencesService.getFilePreferences()), aiProviderHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_CHAT_MODEL, dialogService, preferencesService.getFilePreferences()), chatModelHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_API_KEY, dialogService, preferencesService.getFilePreferences()), apiKeyHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_EMBEDDING_MODEL, dialogService, preferencesService.getFilePreferences()), embeddingModelHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_API_BASE_URL, dialogService, preferencesService.getFilePreferences()), apiBaseUrlHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_INSTRUCTION, dialogService, preferencesService.getFilePreferences()), instructionHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_CONTEXT_WINDOW_SIZE, dialogService, preferencesService.getFilePreferences()), contextWindowSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_TEMPERATURE, dialogService, preferencesService.getFilePreferences()), temperatureHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_DOCUMENT_SPLITTER_CHUNK_SIZE, dialogService, preferencesService.getFilePreferences()), documentSplitterChunkSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, dialogService, preferencesService.getFilePreferences()), documentSplitterOverlapSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_RAG_MAX_RESULTS_COUNT, dialogService, preferencesService.getFilePreferences()), ragMaxResultsCountHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_RAG_MIN_SCORE, dialogService, preferencesService.getFilePreferences()), ragMinScoreHelp);
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
