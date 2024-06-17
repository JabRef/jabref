package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.AiPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.DoubleInputField;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    @FXML private CheckBox enableChat;
    @FXML private TextField openAiToken;
    @FXML private ComboBox<AiPreferences.ChatModel> aiModelComboBox;
    @FXML private ComboBox<AiPreferences.EmbeddingModel> embeddingModelComboBox;

    @FXML private TextArea systemMessageTextArea;
    @FXML private DoubleInputField temperatureTextField;
    @FXML private IntegerInputField messageWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private DoubleInputField ragMinScoreTextField;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public AiTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new AiTabViewModel(preferencesService);

        aiModelComboBox.getItems().addAll(AiPreferences.ChatModel.values());
        embeddingModelComboBox.getItems().addAll(AiPreferences.EmbeddingModel.values());

        enableChat.selectedProperty().bindBidirectional(viewModel.useAiProperty());
        openAiToken.textProperty().bindBidirectional(viewModel.openAiTokenProperty());

        aiModelComboBox.valueProperty().bindBidirectional(viewModel.aiModelProperty());
        embeddingModelComboBox.valueProperty().bindBidirectional(viewModel.embeddingModelProperty());

        systemMessageTextArea.textProperty().bindBidirectional(viewModel.systemMessageProperty());
        temperatureTextField.valueProperty().bindBidirectional(viewModel.temperatureProperty().asObject());
        messageWindowSizeTextField.valueProperty().bindBidirectional(viewModel.messageWindowSizeProperty().asObject());
        documentSplitterChunkSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterChunkSizeProperty().asObject());
        documentSplitterOverlapSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterOverlapSizeProperty().asObject());
        ragMaxResultsCountTextField.valueProperty().bindBidirectional(viewModel.ragMaxResultsCountProperty().asObject());
        ragMinScoreTextField.valueProperty().bindBidirectional(viewModel.ragMinScoreProperty().asObject());

        openAiToken.setDisable(!enableChat.isSelected());

        aiModelComboBox.setDisable(!enableChat.isSelected());
        embeddingModelComboBox.setDisable(!enableChat.isSelected());

        systemMessageTextArea.setDisable(!enableChat.isSelected());
        temperatureTextField.setDisable(!enableChat.isSelected());
        messageWindowSizeTextField.setDisable(!enableChat.isSelected());
        documentSplitterChunkSizeTextField.setDisable(!enableChat.isSelected());
        documentSplitterOverlapSizeTextField.setDisable(!enableChat.isSelected());
        ragMaxResultsCountTextField.setDisable(!enableChat.isSelected());
        ragMinScoreTextField.setDisable(!enableChat.isSelected());

        enableChat.selectedProperty().addListener((observable, oldValue, newValue) -> {
            openAiToken.setDisable(!newValue);

            aiModelComboBox.setDisable(!newValue);
            embeddingModelComboBox.setDisable(!newValue);

            systemMessageTextArea.setDisable(!newValue);
            temperatureTextField.setDisable(!newValue);
            messageWindowSizeTextField.setDisable(!newValue);
            documentSplitterChunkSizeTextField.setDisable(!newValue);
            documentSplitterOverlapSizeTextField.setDisable(!newValue);
            ragMaxResultsCountTextField.setDisable(!newValue);
            ragMinScoreTextField.setDisable(!newValue);
        });

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.getOpenAiTokenValidatorStatus(), openAiToken);
            visualizer.initVisualization(viewModel.getSystemMessageValidatorStatus(), systemMessageTextArea);
            visualizer.initVisualization(viewModel.getTemperatureValidatorStatus(), temperatureTextField);
            visualizer.initVisualization(viewModel.getMessageWindowSizeValidatorStatus(), messageWindowSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterChunkSizeValidatorStatus(), documentSplitterChunkSizeTextField);
            visualizer.initVisualization(viewModel.getDocumentSplitterOverlapSizeValidatorStatus(), documentSplitterOverlapSizeTextField);
            visualizer.initVisualization(viewModel.getRagMaxResultsCountValidatorStatus(), ragMaxResultsCountTextField);
            visualizer.initVisualization(viewModel.getRagMinScoreValidatorStatus(), ragMinScoreTextField);
        });
    }

    @Override
    public String getTabName() {
        return Localization.lang("AI");
    }
}
