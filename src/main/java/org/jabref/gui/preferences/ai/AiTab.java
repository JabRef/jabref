package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

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
import org.slf4j.Logger;

import java.util.List;import java.util.Map;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    @FXML private CheckBox enableChat;

    @FXML private ComboBox<AiPreferences.AiProvider> aiProviderComboBox;
    @FXML private ComboBox<String> chatModelComboBox;
    @FXML private CustomPasswordField apiTokenTextField;

    @FXML private CheckBox customizeSettingsCheckbox;

    @FXML private ComboBox<AiPreferences.EmbeddingModel> embeddingModelComboBox;
    @FXML private TextField apiBaseUrlTextField;
    @FXML private TextArea instructionTextArea;
    @FXML private DoubleInputField temperatureTextField;
    @FXML private IntegerInputField contextWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private DoubleInputField ragMinScoreTextField;

    @FXML private Button aiProviderHelp;
    @FXML private Button chatModelHelp;
    @FXML private Button apiTokenHelp;
    @FXML private Button embeddingModelHelp;
    @FXML private Button apiBaseUrlHelp;
    @FXML private Button instructionHelp;
    @FXML private Button contextWindowSizeHelp;
    @FXML private Button documentSplitterChunkSizeHelp;
    @FXML private Button documentSplitterOverlapSizeHelp;
    @FXML private Button ragMaxResultsCountHelp;
    @FXML private Button ragMinScoreHelp;

    @FXML private Button resetExpertSettingsButton;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferencesService);

        enableChat.selectedProperty().bindBidirectional(viewModel.useAiProperty());

        new ViewModelListCellFactory<AiPreferences.AiProvider>()
                .withText(AiPreferences.AiProvider::toString)
                .install(aiProviderComboBox);
        aiProviderComboBox.setItems(viewModel.aiProvidersProperty());
        aiProviderComboBox.valueProperty().bindBidirectional(viewModel.selectedAiProviderProperty());

        new ViewModelListCellFactory<String>()
                .withText(text -> text)
                .install(chatModelComboBox);
        chatModelComboBox.setItems(viewModel.chatModelsProperty());
        chatModelComboBox.valueProperty().bindBidirectional(viewModel.selectedChatModelProperty());

        new ViewModelListCellFactory<AiPreferences.EmbeddingModel>()
                .withText(AiPreferences.EmbeddingModel::toString)
                .install(embeddingModelComboBox);
        embeddingModelComboBox.setItems(viewModel.embeddingModelsProperty());
        embeddingModelComboBox.valueProperty().bindBidirectional(viewModel.selectedEmbeddingModelProperty());

        apiTokenTextField.textProperty().bindBidirectional(viewModel.openAiTokenProperty());

        customizeSettingsCheckbox.selectedProperty().bindBidirectional(viewModel.customizeSettingsProperty());

        apiBaseUrlTextField.textProperty().bindBidirectional(viewModel.apiBaseUrlProperty());
        instructionTextArea.textProperty().bindBidirectional(viewModel.instructionProperty());
        temperatureTextField.valueProperty().bindBidirectional(viewModel.temperatureProperty().asObject());
        contextWindowSizeTextField.valueProperty().bindBidirectional(viewModel.contextWindowSizeProperty().asObject());
        documentSplitterChunkSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterChunkSizeProperty().asObject());
        documentSplitterOverlapSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterOverlapSizeProperty().asObject());
        ragMaxResultsCountTextField.valueProperty().bindBidirectional(viewModel.ragMaxResultsCountProperty().asObject());
        ragMinScoreTextField.valueProperty().bindBidirectional(viewModel.ragMinScoreProperty().asObject());

        updateDisabledProperties();

        enableChat.selectedProperty().addListener(obs -> updateDisabledProperties());

        customizeSettingsCheckbox.selectedProperty().addListener(obs -> updateDisabledProperties());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.getApiTokenValidatorStatus(), apiTokenTextField);
            visualizer.initVisualization(viewModel.getChatModelValidationStatus(), chatModelComboBox);
            visualizer.initVisualization(viewModel.getApiBaseUrlValidationStatus(), apiBaseUrlTextField);
            visualizer.initVisualization(viewModel.getSystemMessageValidationStatus(), instructionTextArea);
            visualizer.initVisualization(viewModel.getTemperatureValidationStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getMessageWindowSizeValidationStatus(), contextWindowSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterChunkSizeValidationStatus(), documentSplitterChunkSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterOverlapSizeValidationStatus(), documentSplitterOverlapSizeTextField);
            visualizer.initVisualization(viewModel.getRagMaxResultsCountValidationStatus(), ragMaxResultsCountTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreValidationStatus(), ragMinScoreTextField);
        });

        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_PROVIDER, dialogService, preferencesService.getFilePreferences()), aiProviderHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_CHAT_MODEL, dialogService, preferencesService.getFilePreferences()), chatModelHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_API_TOKEN, dialogService, preferencesService.getFilePreferences()), apiTokenHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_EMBEDDING_MODEL, dialogService, preferencesService.getFilePreferences()), embeddingModelHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_API_BASE_URL, dialogService, preferencesService.getFilePreferences()), apiBaseUrlHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_INSTRUCTION, dialogService, preferencesService.getFilePreferences()), instructionHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_CONTEXT_WINDOW_SIZE, dialogService, preferencesService.getFilePreferences()), contextWindowSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_DOCUMENT_SPLITTER_CHUNK_SIZE, dialogService, preferencesService.getFilePreferences()), documentSplitterChunkSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, dialogService, preferencesService.getFilePreferences()), documentSplitterOverlapSizeHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_RAG_MAX_RESULTS_COUNT, dialogService, preferencesService.getFilePreferences()), ragMaxResultsCountHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_RAG_MIN_SCORE, dialogService, preferencesService.getFilePreferences()), ragMinScoreHelp);

        // TODO: Move editable property to View Model.
        viewModel.selectedAiProviderProperty().addListener((observable, oldValue, newValue) -> {
            chatModelComboBox.setEditable(newValue == AiPreferences.AiProvider.HUGGING_FACE);
        });
    }

    private void updateDisabledProperties() {
        aiProviderComboBox.setDisable(!viewModel.getUseAi());
        chatModelComboBox.setDisable(!viewModel.getUseAi());
        apiTokenTextField.setDisable(!viewModel.getUseAi());

        customizeSettingsCheckbox.setDisable(!viewModel.getUseAi());

        embeddingModelComboBox.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        apiBaseUrlTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        instructionTextArea.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        temperatureTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
        contextWindowSizeTextField.setDisable(!viewModel.getUseAi() || !viewModel.getCustomizeSettings());
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
