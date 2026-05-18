package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.EnhancedPasswordField;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    private static final String HUGGING_FACE_CHAT_MODEL_PROMPT = "TinyLlama/TinyLlama_v1.1 (or any other model name)";

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    @FXML private CheckBox enableAi;
    // [impl->req~ai.ingestion.automatic-trigger~1]
    @FXML private CheckBox autoGenerateEmbeddings;
    // [impl->req~ai.summarization.entries.auto~1]
    @FXML private CheckBox autoGenerateSummaries;
    // [impl->feat~ai.llms.providers~1]
    @FXML private ComboBox<AiProvider> aiProviderComboBox;
    @FXML private ComboBox<String> chatModelComboBox;
    @FXML private EnhancedPasswordField apiKeyTextField;
    @FXML private CheckBox customizeExpertSettingsCheckbox;
    @FXML private VBox expertSettingsPane;
    @FXML private TextField apiBaseUrlTextField;
    @FXML private SearchableComboBox<PredefinedEmbeddingModel> embeddingModelComboBox;
    // [impl->req~ai.answer-engines.default~1]
    @FXML private ComboBox<AnswerEngineKind> answerEngineComboBox;
    // [impl->req~ai.summarization.algorithm.default~1]
    @FXML private ComboBox<SummarizatorKind> summarizationAlgorithmComboBox;
    @FXML private ComboBox<TokenEstimatorKind> tokenEstimationAlgorithmComboBox;
    // [impl->req~ai.expert-settings.chat-inference-global~1]
    @FXML private TextField temperatureTextField;
    @FXML private IntegerInputField contextWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    // [impl->req~ai.expert-settings.rag-global~1]
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private TextField ragMinScoreTextField;
    // [impl->req~ai.expert-settings.templates~1]
    @FXML private TabPane templatesTabPane;
    @FXML private Tab systemMessageForChattingTab;
    @FXML private Tab userMessageForChattingTab;
    @FXML private Tab summarizationChunkSystemMessageTab;
    @FXML private Tab summarizationCombineSystemMessageTab;
    @FXML private Tab summarizationFullDocumentSystemMessageTab;
    @FXML private Tab citationParsingSystemMessageTab;
    @FXML private Tab markdownChatExportTemplateTab;
    @FXML private Tab followUpQuestionsTemplateTab;
    // [impl->req~ai.chat.customize-system-prompt~1]
    @FXML private TextArea systemMessageTextArea;
    // [impl->req~ai.answer-engines.embeddings-search.prompt~1]
    // [impl->req~ai.answer-engines.full-document.prompt~1]
    @FXML private TextArea userMessageTextArea;
    // [impl->req~ai.summarization.algorithms.chunked.system-prompt-chunk~1]
    @FXML private TextArea summarizationChunkSystemMessageTextArea;
    // [impl->req~ai.summarization.algorithms.chunked.system-prompt-combine~1]
    @FXML private TextArea summarizationCombineSystemMessageTextArea;
    // [impl->req~ai.summarization.algorithms.full.system-prompt~1]
    @FXML private TextArea summarizationFullDocumentSystemMessageTextArea;
    // [impl->req~ai.citation-parsing.system-prompt-config~1]
    @FXML private TextArea citationParsingSystemMessageTextArea;
    @FXML private TextArea markdownChatExportTemplateTextArea;
    @FXML private TextArea followUpQuestionsTemplateTextArea;
    @FXML private Button generalSettingsHelp;
    @FXML private Button expertSettingsHelp;
    @FXML private Button templatesHelp;
    @FXML private Button resetCurrentTemplateButton;
    @FXML private Button resetTemplatesButton;
    @FXML private CheckBox generateFollowUpQuestions;
    @FXML private Spinner<Integer> followUpQuestionsCountSpinner;

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferences);

        initializeEnableAi();
        initializeAiProvider();
        initializeChatModel();
        initializeApiKey();
        initializeExpertSettings();
        initializeValidations();
        initializeTemplates();
        initializeFollowUpQuestions();
        initializeHelp();
    }

    private void initializeFollowUpQuestions() {
        generateFollowUpQuestions.selectedProperty().bindBidirectional(viewModel.generateFollowUpQuestionsProperty());
        generateFollowUpQuestions.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        followUpQuestionsCountSpinner.setValueFactory(AiTabViewModel.followUpQuestionsCountValueFactory);
        followUpQuestionsCountSpinner.getValueFactory().valueProperty().bindBidirectional(viewModel.followUpQuestionsCountProperty().asObject());
        followUpQuestionsCountSpinner.disableProperty().bind(generateFollowUpQuestions.selectedProperty().not());
    }

    private void initializeHelp() {
        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_GENERAL_SETTINGS, dialogService, preferences.getExternalApplicationsPreferences()), generalSettingsHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_EXPERT_SETTINGS, dialogService, preferences.getExternalApplicationsPreferences()), expertSettingsHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_TEMPLATES, dialogService, preferences.getExternalApplicationsPreferences()), templatesHelp);
    }

    private void initializeTemplates() {
        systemMessageTextArea.textProperty().bindBidirectional(viewModel.chattingSystemMessageTemplateProperty());
        userMessageTextArea.textProperty().bindBidirectional(viewModel.chattingUserMessageTemplateProperty());
        summarizationChunkSystemMessageTextArea.textProperty().bindBidirectional(viewModel.summarizationChunkSystemMessageTemplateProperty());
        summarizationCombineSystemMessageTextArea.textProperty().bindBidirectional(viewModel.summarizationCombineSystemMessageTemplateProperty());
        summarizationFullDocumentSystemMessageTextArea.textProperty().bindBidirectional(viewModel.summarizationFullDocumentSystemMessageTemplateProperty());
        citationParsingSystemMessageTextArea.textProperty().bindBidirectional(viewModel.citationParsingSystemMessageTemplateProperty());
        markdownChatExportTemplateTextArea.textProperty().bindBidirectional(viewModel.markdownChatExportTemplateProperty());
        followUpQuestionsTemplateTextArea.textProperty().bindBidirectional(viewModel.followUpQuestionsTemplateProperty());

        BooleanBinding aiDisabled = enableAi.selectedProperty().not();

        systemMessageTextArea.disableProperty().bind(aiDisabled);
        userMessageTextArea.disableProperty().bind(aiDisabled);
        summarizationChunkSystemMessageTextArea.disableProperty().bind(aiDisabled);
        summarizationCombineSystemMessageTextArea.disableProperty().bind(aiDisabled);
        summarizationFullDocumentSystemMessageTextArea.disableProperty().bind(aiDisabled);
        citationParsingSystemMessageTextArea.disableProperty().bind(aiDisabled);
        markdownChatExportTemplateTextArea.disableProperty().bind(aiDisabled);
        followUpQuestionsTemplateTextArea.disableProperty().bind(aiDisabled);

        resetCurrentTemplateButton.disableProperty().bind(aiDisabled);
        resetTemplatesButton.disableProperty().bind(aiDisabled);
    }

    private void initializeValidations() {
        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.getApiTokenValidationStatus(), apiKeyTextField);
            visualizer.initVisualization(viewModel.getChatModelValidationStatus(), chatModelComboBox);
            visualizer.initVisualization(viewModel.getApiBaseUrlValidationStatus(), apiBaseUrlTextField);
            visualizer.initVisualization(viewModel.getEmbeddingModelValidationStatus(), embeddingModelComboBox);
            visualizer.initVisualization(viewModel.getTemperatureTypeValidationStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getTemperatureRangeValidationStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getMessageWindowSizeValidationStatus(), contextWindowSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterChunkSizeValidationStatus(), documentSplitterChunkSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterOverlapSizeValidationStatus(), documentSplitterOverlapSizeTextField);
            visualizer.initVisualization(viewModel.getRagMaxResultsCountValidationStatus(), ragMaxResultsCountTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreTypeValidationStatus(), ragMinScoreTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreRangeValidationStatus(), ragMinScoreTextField);
        });
    }

    private void initializeExpertSettings() {
        customizeExpertSettingsCheckbox.selectedProperty().bindBidirectional(viewModel.customizeExpertSettingsProperty());
        customizeExpertSettingsCheckbox.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        expertSettingsPane.visibleProperty().bind(customizeExpertSettingsCheckbox.selectedProperty());
        expertSettingsPane.managedProperty().bind(customizeExpertSettingsCheckbox.selectedProperty());

        new ViewModelListCellFactory<PredefinedEmbeddingModel>()
                .withText(PredefinedEmbeddingModel::fullInfo)
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

        contextWindowSizeTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.contextWindowSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.contextWindowSizeProperty().addListener((observable, oldValue, newValue) ->
                contextWindowSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        contextWindowSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        new ViewModelListCellFactory<AnswerEngineKind>()
                .withText(AiNamingUtils::getDisplayName)
                .install(answerEngineComboBox);
        answerEngineComboBox.setItems(viewModel.answerEngineKindsProperty());
        answerEngineComboBox.valueProperty().bindBidirectional(viewModel.answerEngineProperty());
        answerEngineComboBox.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        new ViewModelListCellFactory<SummarizatorKind>()
                .withText(AiNamingUtils::getDisplayName)
                .install(summarizationAlgorithmComboBox);
        summarizationAlgorithmComboBox.setItems(viewModel.summarizationAlgorithmsProperty());
        summarizationAlgorithmComboBox.valueProperty().bindBidirectional(viewModel.summarizationAlgorithmProperty());
        summarizationAlgorithmComboBox.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        new ViewModelListCellFactory<TokenEstimatorKind>()
                .withText(AiNamingUtils::getDisplayName)
                .install(tokenEstimationAlgorithmComboBox);
        tokenEstimationAlgorithmComboBox.setItems(viewModel.tokenEstimationAlgorithmsProperty());
        tokenEstimationAlgorithmComboBox.valueProperty().bindBidirectional(viewModel.tokenEstimationAlgorithmProperty());
        tokenEstimationAlgorithmComboBox.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        temperatureTextField.textProperty().bindBidirectional(viewModel.temperatureProperty());
        temperatureTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        documentSplitterChunkSizeTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.documentSplitterChunkSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.documentSplitterChunkSizeProperty().addListener((observable, oldValue, newValue) ->
                documentSplitterChunkSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        documentSplitterChunkSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        documentSplitterOverlapSizeTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.documentSplitterOverlapSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.documentSplitterOverlapSizeProperty().addListener((observable, oldValue, newValue) ->
                documentSplitterOverlapSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        documentSplitterOverlapSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        ragMaxResultsCountTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.ragMaxResultsCountProperty().set(newValue == null ? 0 : newValue));

        viewModel.ragMaxResultsCountProperty().addListener((observable, oldValue, newValue) ->
                ragMaxResultsCountTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        ragMaxResultsCountTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

        ragMinScoreTextField.textProperty().bindBidirectional(viewModel.ragMinScoreProperty());
        ragMinScoreTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());
    }

    private void initializeApiKey() {
        apiKeyTextField.textProperty().bindBidirectional(viewModel.apiKeyProperty());
        apiKeyTextField.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        Button revealApiKeyButton = IconTheme.JabRefIcons.PASSWORD_REVEALED.asButton();
        revealApiKeyButton.disableProperty().bind(apiKeyTextField.disableProperty());
        revealApiKeyButton.setOnAction(_ -> apiKeyTextField.setShowPassword(!apiKeyTextField.isShowPassword()));

        Button clearApiKeyButton = IconTheme.JabRefIcons.DELETE_ENTRY.asButton();
        clearApiKeyButton.disableProperty().bind(apiKeyTextField.disableProperty());
        clearApiKeyButton.setOnAction(_ -> {
            apiKeyTextField.clear();
            apiKeyTextField.requestFocus();
        });

        apiKeyTextField.setRight(new HBox(revealApiKeyButton, clearApiKeyButton));
    }

    private void initializeChatModel() {
        new ViewModelListCellFactory<String>()
                .withText(text -> text)
                .install(chatModelComboBox);
        chatModelComboBox.itemsProperty().bind(viewModel.chatModelsProperty());
        chatModelComboBox.valueProperty().bindBidirectional(viewModel.selectedChatModelProperty());
        chatModelComboBox.disableProperty().bind(viewModel.disableBasicSettingsProperty());

        this.aiProviderComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == AiProvider.HUGGING_FACE) {
                chatModelComboBox.setPromptText(HUGGING_FACE_CHAT_MODEL_PROMPT);
            }
        });
    }

    private void initializeAiProvider() {
        new ViewModelListCellFactory<AiProvider>()
                .withText(AiNamingUtils::getDisplayName)
                .install(aiProviderComboBox);
        aiProviderComboBox.itemsProperty().bind(viewModel.aiProvidersProperty());
        aiProviderComboBox.valueProperty().bindBidirectional(viewModel.selectedAiProviderProperty());
        aiProviderComboBox.disableProperty().bind(viewModel.disableBasicSettingsProperty());
    }

    private void initializeEnableAi() {
        enableAi.selectedProperty().bindBidirectional(viewModel.enableAi());
        autoGenerateSummaries.selectedProperty().bindBidirectional(viewModel.autoGenerateSummaries());
        autoGenerateSummaries.disableProperty().bind(
                Bindings.or(
                        enableAi.selectedProperty().not(),
                        viewModel.disableAutoGenerateSummaries()
                )
        );
        autoGenerateEmbeddings.selectedProperty().bindBidirectional(viewModel.autoGenerateEmbeddings());
        autoGenerateEmbeddings.disableProperty().bind(
                Bindings.or(
                        enableAi.selectedProperty().not(),
                        viewModel.disableAutoGenerateEmbeddings()
                )
        );
    }

    @Override
    public String getTabName() {
        return Localization.lang("AI");
    }

    @FXML
    private void onResetExpertSettingsButtonClick() {
        viewModel.resetExpertSettings();
    }

    @FXML
    private void onResetTemplatesButtonClick() {
        viewModel.resetTemplates();
    }

    @FXML
    private void onResetCurrentTemplateButtonClick() {
        Tab selectedTab = templatesTabPane.getSelectionModel().getSelectedItem();

        if (selectedTab == systemMessageForChattingTab) {
            viewModel.resetChattingSystemMessageTemplate();
        } else if (selectedTab == userMessageForChattingTab) {
            viewModel.resetChattingUserMessageTemplate();
        } else if (selectedTab == summarizationChunkSystemMessageTab) {
            viewModel.resetSummarizationChunkSystemMessageTemplate();
        } else if (selectedTab == summarizationCombineSystemMessageTab) {
            viewModel.resetSummarizationCombineSystemMessageTemplate();
        } else if (selectedTab == summarizationFullDocumentSystemMessageTab) {
            viewModel.resetSummarizationFullDocumentSystemMessageTemplate();
        } else if (selectedTab == citationParsingSystemMessageTab) {
            viewModel.resetCitationParsingSystemMessageTemplate();
        } else if (selectedTab == markdownChatExportTemplateTab) {
            viewModel.resetMarkdownChatExportTemplate();
        } else if (selectedTab == followUpQuestionsTemplateTab) {
            viewModel.resetFollowUpQuestionsTemplate();
        }
    }

    public ReadOnlyBooleanProperty aiEnabledProperty() {
        return enableAi.selectedProperty();
    }
}
