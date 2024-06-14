package org.jabref.gui.preferences.ai;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.unitfx.DoubleInputField;
import com.dlsc.unitfx.IntegerInputField;

public class AiTab extends AbstractPreferenceTabView<AiTabViewModel> implements PreferencesTab {
    @FXML private CheckBox enableChat;
    @FXML private TextField openAiToken;

    @FXML private CheckBox customizeSettingsCheckbox;

    @FXML private TextArea systemMessageTextArea;
    @FXML private IntegerInputField messageWindowSizeTextField;
    @FXML private IntegerInputField documentSplitterChunkSizeTextField;
    @FXML private IntegerInputField documentSplitterOverlapSizeTextField;
    @FXML private IntegerInputField ragMaxResultsCountTextField;
    @FXML private DoubleInputField ragMinScoreTextField;

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
        openAiToken.textProperty().bindBidirectional(viewModel.openAiTokenProperty());
        customizeSettingsCheckbox.selectedProperty().bindBidirectional(viewModel.customizeSettingsProperty());
        systemMessageTextArea.textProperty().bindBidirectional(viewModel.systemMessageProperty());
        messageWindowSizeTextField.valueProperty().bindBidirectional(viewModel.messageWindowSizeProperty().asObject());
        documentSplitterChunkSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterChunkSizeProperty().asObject());
        documentSplitterOverlapSizeTextField.valueProperty().bindBidirectional(viewModel.documentSplitterOverlapSizeProperty().asObject());
        ragMaxResultsCountTextField.valueProperty().bindBidirectional(viewModel.ragMaxResultsCountProperty().asObject());
        ragMinScoreTextField.valueProperty().bindBidirectional(viewModel.ragMinScoreProperty().asObject());

        openAiToken.setDisable(!enableChat.isSelected());

        customizeSettingsCheckbox.setDisable(!enableChat.isSelected());

        systemMessageTextArea.setDisable(!enableChat.isSelected());
        messageWindowSizeTextField.setDisable(!enableChat.isSelected());
        documentSplitterChunkSizeTextField.setDisable(!enableChat.isSelected());
        documentSplitterOverlapSizeTextField.setDisable(!enableChat.isSelected());
        ragMaxResultsCountTextField.setDisable(!enableChat.isSelected());
        ragMinScoreTextField.setDisable(!enableChat.isSelected());
        resetExpertSettingsButton.setDisable(!enableChat.isSelected());

        messageWindowSizeTextField.setDisable(!customizeSettingsCheckbox.isSelected());
        documentSplitterChunkSizeTextField.setDisable(!customizeSettingsCheckbox.isSelected());
        documentSplitterOverlapSizeTextField.setDisable(!customizeSettingsCheckbox.isSelected());
        ragMaxResultsCountTextField.setDisable(!customizeSettingsCheckbox.isSelected());
        ragMinScoreTextField.setDisable(!customizeSettingsCheckbox.isSelected());
        resetExpertSettingsButton.setDisable(!customizeSettingsCheckbox.isSelected());

        enableChat.selectedProperty().addListener((observable, oldValue, newValue) -> {
            openAiToken.setDisable(!newValue);

            customizeSettingsCheckbox.setDisable(!newValue);

            systemMessageTextArea.setDisable(!newValue);
            messageWindowSizeTextField.setDisable(!newValue);
            documentSplitterChunkSizeTextField.setDisable(!newValue);
            documentSplitterOverlapSizeTextField.setDisable(!newValue);
            ragMaxResultsCountTextField.setDisable(!newValue);
            ragMinScoreTextField.setDisable(!newValue);
            resetExpertSettingsButton.setDisable(!newValue);
        });

        customizeSettingsCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
            systemMessageTextArea.setDisable(!newValue);
            messageWindowSizeTextField.setDisable(!newValue);
            documentSplitterChunkSizeTextField.setDisable(!newValue);
            documentSplitterOverlapSizeTextField.setDisable(!newValue);
            ragMaxResultsCountTextField.setDisable(!newValue);
            ragMinScoreTextField.setDisable(!newValue);
            resetExpertSettingsButton.setDisable(!newValue);

            viewModel.resetExpertSettings();
        }));

        ActionFactory actionFactory = new ActionFactory(preferencesService.getKeyBindingRepository());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_SYSTEM_MESSAGE, dialogService, preferencesService.getFilePreferences()), systemMessageHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AI_MESSAGE_WINDOW_SIZE, dialogService, preferencesService.getFilePreferences()), messageWindowSizeHelp);
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
