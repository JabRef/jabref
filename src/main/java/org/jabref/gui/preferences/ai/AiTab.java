package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.DoubleInputField;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import scala.Int;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    @FXML private CheckBox enableChat;
    @FXML private TextField openAiToken;

    @FXML private TextArea systemMessageTextArea;
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

        enableChat.selectedProperty().bindBidirectional(viewModel.useAiProperty());
        openAiToken.textProperty().bindBidirectional(viewModel.openAiTokenProperty());
        systemMessageTextArea.textProperty().bindBidirectional(viewModel.systemMessageProperty());
        messageWindowSizeTextField.valueProperty().bindBidirectional(viewModel.messageWindowSizeProperty().asObject());
        documentSplitterChunkSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterChunkSizeProperty().asObject());
        documentSplitterOverlapSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterOverlapSizeProperty().asObject());
        ragMaxResultsCountTextField.valueProperty().bindBidirectional(viewModel.ragMaxResultsCountProperty().asObject());
        ragMinScoreTextField.valueProperty().bindBidirectional(viewModel.ragMinScoreProperty().asObject());

        openAiToken.setDisable(!enableChat.isSelected());
        systemMessageTextArea.setDisable(!enableChat.isSelected());
        messageWindowSizeTextField.setDisable(!enableChat.isSelected());
        documentSplitterChunkSizeTextField.setDisable(!enableChat.isSelected());
        documentSplitterOverlapSizeTextField.setDisable(!enableChat.isSelected());
        ragMaxResultsCountTextField.setDisable(!enableChat.isSelected());
        ragMinScoreTextField.setDisable(!enableChat.isSelected());

        enableChat.selectedProperty().addListener((observable, oldValue, newValue) -> {
            openAiToken.setDisable(!newValue);

            systemMessageTextArea.setDisable(!newValue);
            messageWindowSizeTextField.setDisable(!newValue);
            documentSplitterChunkSizeTextField.setDisable(!newValue);
            documentSplitterOverlapSizeTextField.setDisable(!newValue);
            ragMaxResultsCountTextField.setDisable(!newValue);
            ragMinScoreTextField.setDisable(!newValue);
        });

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.getOpenAiTokenValidatorStatus(), openAiToken);
            visualizer.initVisualization(viewModel.getSystemMessageValidatorStatus(), systemMessageTextArea);
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
