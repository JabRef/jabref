package org.jabref.gui.preferences.ai;

import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiProvider;
import org.jabref.model.ai.EmbeddingModel;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.textfield.CustomPasswordField;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    private static final String HUGGING_FACE_CHAT_MODEL_PROMPT = "TinyLlama/TinyLlama_v1.1 (or any other model name)";
    private static final String GPT_4_ALL_CHAT_MODEL_PROMPT = "Phi-3.1-mini (or any other local model name from GPT4All)";

    @FXML private CheckBox enableAi;
    @FXML private CheckBox autoGenerateEmbeddings;
    @FXML private CheckBox autoGenerateSummaries;
    @FXML private CheckBox generateFollowUpQuestions;
    @FXML private Spinner<Integer> followUpQuestionsCountSpinner;
    @FXML private Tab followUpQuestionsTab;
    @FXML private TextArea followUpQuestionsTextArea;
    @FXML private Label followUpQuestionsCountLabel;

    @FXML private ComboBox<AiProvider> aiProviderComboBox;
    @FXML private ComboBox<String> chatModelComboBox;
    @FXML private CustomPasswordField apiKeyTextField;

    @FXML private CheckBox customizeExpertSettingsCheckbox;
    @FXML private VBox expertSettingsPane;

    @FXML private TextField apiBaseUrlTextField;
    @FXML private SearchableComboBox<EmbeddingModel> embeddingModelComboBox;
    @FXML private TextField temperatureTextField;
    @FXML private IntegerInputField contextWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private TextField ragMinScoreTextField;

    @FXML private TabPane templatesTabPane;
    @FXML private Tab systemMessageForChattingTab;
    @FXML private Tab userMessageForChattingTab;
    @FXML private Tab summarizationChunkSystemMessageTab;
    @FXML private Tab summarizationChunkUserMessageTab;
    @FXML private Tab summarizationCombineSystemMessageTab;
    @FXML private Tab summarizationCombineUserMessageTab;
    @FXML private Tab citationParsingSystemMessageTab;
    @FXML private Tab citationParsingUserMessageTab;

    @FXML private TextArea systemMessageTextArea;
    @FXML private TextArea userMessageTextArea;
    @FXML private TextArea summarizationChunkSystemMessageTextArea;
    @FXML private TextArea summarizationChunkUserMessageTextArea;
    @FXML private TextArea summarizationCombineSystemMessageTextArea;
    @FXML private TextArea summarizationCombineUserMessageTextArea;
    @FXML private TextArea citationParsingSystemMessageTextArea;
    @FXML private TextArea citationParsingUserMessageTextArea;

    @FXML private Button generalSettingsHelp;
    @FXML private Button expertSettingsHelp;
    @FXML private Button templatesHelp;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

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
        initializeHelp();
    }

    private void initializeHelp() {
        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_GENERAL_SETTINGS, dialogService, preferences.getExternalApplicationsPreferences()), generalSettingsHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_EXPERT_SETTINGS, dialogService, preferences.getExternalApplicationsPreferences()), expertSettingsHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_TEMPLATES, dialogService, preferences.getExternalApplicationsPreferences()), templatesHelp);
    }

    private void initializeTemplates() {
        systemMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.CHATTING_SYSTEM_MESSAGE));
        userMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.CHATTING_USER_MESSAGE));
        summarizationChunkSystemMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE));
        summarizationChunkUserMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE));
        summarizationCombineSystemMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE));
        summarizationCombineUserMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE));
        citationParsingSystemMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE));
        citationParsingUserMessageTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.CITATION_PARSING_USER_MESSAGE));
        followUpQuestionsTextArea.textProperty().bindBidirectional(viewModel.getTemplateSources().get(AiTemplate.FOLLOW_UP_QUESTIONS));
        templatesTabPane.getSelectionModel().selectedItemProperty().addListener(_ -> viewModel.selectedTemplateProperty().set(getAiTemplate()));
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

        // bindBidirectional doesn't work well with number input fields ({@link IntegerInputField}, {@link DoubleInputField}),
        // so they are expanded into `addListener` calls.

        contextWindowSizeTextField.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.contextWindowSizeProperty().set(newValue == null ? 0 : newValue));

        viewModel.contextWindowSizeProperty().addListener((observable, oldValue, newValue) ->
                contextWindowSizeTextField.valueProperty().set(newValue == null ? 0 : newValue.intValue()));

        contextWindowSizeTextField.disableProperty().bind(viewModel.disableExpertSettingsProperty());

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
        // Disable if GPT4ALL is selected
        apiKeyTextField.disableProperty().bind(
                Bindings.or(
                        viewModel.disableBasicSettingsProperty(),
                        aiProviderComboBox.valueProperty().isEqualTo(AiProvider.GPT4ALL)
                )
        );
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
            if (newValue == AiProvider.GPT4ALL) {
                chatModelComboBox.setPromptText(GPT_4_ALL_CHAT_MODEL_PROMPT);
            }
        });
    }

    private void initializeAiProvider() {
        new ViewModelListCellFactory<AiProvider>()
                .withText(AiProvider::toString)
                .install(aiProviderComboBox);
        aiProviderComboBox.itemsProperty().bind(viewModel.aiProvidersProperty());
        aiProviderComboBox.valueProperty().bindBidirectional(viewModel.selectedAiProviderProperty());
        aiProviderComboBox.disableProperty().bind(viewModel.disableBasicSettingsProperty());
    }

    private void initializeEnableAi() {
        enableAi.selectedProperty().bindBidirectional(viewModel.enableAi());
        autoGenerateSummaries.selectedProperty().bindBidirectional(viewModel.autoGenerateSummaries());
        autoGenerateSummaries.disableProperty().bind(viewModel.disableAutoGenerateSummaries());
        autoGenerateEmbeddings.selectedProperty().bindBidirectional(viewModel.autoGenerateEmbeddings());
        autoGenerateEmbeddings.disableProperty().bind(viewModel.disableAutoGenerateEmbeddings());
        generateFollowUpQuestions.selectedProperty().bindBidirectional(viewModel.generateFollowUpQuestions());
        followUpQuestionsCountSpinner.setValueFactory(AiTabViewModel.followUpQuestionsCountValueFactory);
        followUpQuestionsCountSpinner.getValueFactory().valueProperty().bindBidirectional(viewModel.followUpQuestionsCountProperty().asObject());
        followUpQuestionsCountSpinner.disableProperty().bind(generateFollowUpQuestions.selectedProperty().not());
        followUpQuestionsCountLabel.disableProperty().bind(generateFollowUpQuestions.selectedProperty().not());
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
        viewModel.resetCurrentTemplate();
    }

    public ReadOnlyBooleanProperty aiEnabledProperty() {
        return enableAi.selectedProperty();
    }

    public Optional<AiTemplate> getAiTemplate() {
        Tab selectedTab = templatesTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == systemMessageForChattingTab) {
            return Optional.of(AiTemplate.CHATTING_SYSTEM_MESSAGE);
        } else if (selectedTab == userMessageForChattingTab) {
            return Optional.of(AiTemplate.CHATTING_USER_MESSAGE);
        } else if (selectedTab == summarizationChunkSystemMessageTab) {
            return Optional.of(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE);
        } else if (selectedTab == summarizationChunkUserMessageTab) {
            return Optional.of(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE);
        } else if (selectedTab == summarizationCombineSystemMessageTab) {
            return Optional.of(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE);
        } else if (selectedTab == summarizationCombineUserMessageTab) {
            return Optional.of(AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE);
        } else if (selectedTab == citationParsingSystemMessageTab) {
            return Optional.of(AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE);
        } else if (selectedTab == citationParsingUserMessageTab) {
            return Optional.of(AiTemplate.CITATION_PARSING_USER_MESSAGE);
        } else if (selectedTab == followUpQuestionsTab) {
            return Optional.of(AiTemplate.FOLLOW_UP_QUESTIONS);
        }

        return Optional.empty();
    }
}
