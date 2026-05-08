package org.jabref.gui.ai.chat;

import java.nio.file.Path;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

/// Displays status and metadata for an AI chat session.
///
/// This component provides information about:
/// - The currently selected chat model
/// - The answer engine in use
/// - Entries included in the chat context
/// - The ingestion status of linked files, including any errors encountered
///
/// It also offers actions to:
/// - Export the chat history
/// - Clear the chat history
///
/// Typical usage:
/// This component is primarily used within the AiChatView, where:
/// - The chat model and answer engine are bound to this component
/// - The chat history is provided by the AI chat and displayed here
///
/// Future plans:
/// The component is intended to support configuration of chat parameters,
/// such as selecting a different chat model per session instead of relying
/// on global preferences. Currently, only the answer engine can be modified.
// [impl->req~ai.chat.ingestion-status~1]
public class AiChatStatusView extends VBox {
    // [impl->req~ai.chat.model-visibility~1]
    @FXML private Label chatModelLabel;

    @FXML private TableView<FullBibEntry> entriesTable;
    @FXML private TableColumn<FullBibEntry, String> libraryColumn;
    @FXML private TableColumn<FullBibEntry, String> citationKeyColumn;

    @FXML private TableView<AiChatStatusViewModel.IngestionStatusRow> ingestionTable;
    @FXML private TableColumn<AiChatStatusViewModel.IngestionStatusRow, String> fileColumn;
    @FXML private TableColumn<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.FileStatus> statusColumn;
    @FXML private TableColumn<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.IngestionStatusRow> actionColumn;

    @FXML private ComboBox<AnswerEngineKind> answerEngineComboBox;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;
    @Inject private BibEntryTypesManager entryTypesManager;

    private AiChatStatusViewModel viewModel;

    public AiChatStatusView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiChatStatusViewModel(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                preferences.getFieldPreferences(),
                entryTypesManager,
                dialogService,
                aiService.getEmbeddingModelCache(),
                aiService.getEmbeddingsStore()
        );

        setupEntriesTable();
        setupIngestionTable();
        setupRest();
    }

    private void setupEntriesTable() {
        entriesTable.itemsProperty().bind(viewModel.entriesProperty());

        citationKeyColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        cellData.getValue().entry().getCitationKey().orElse("")
                )
        );
        new ValueTableCellFactory<FullBibEntry, String>()
                .withText(text -> text)
                .install(citationKeyColumn);

        libraryColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(
                        cellData.getValue().databaseContext()
                                .getDatabasePath()
                                .map(Path::toString)
                                .orElse("")
                )
        );
        new ValueTableCellFactory<FullBibEntry, String>()
                .withText(text -> text)
                .install(libraryColumn);
    }

    private void setupIngestionTable() {
        ingestionTable.setItems(viewModel.getIngestionStatuses());

        fileColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<AiChatStatusViewModel.IngestionStatusRow, String>()
                .withText(text -> text)
                .install(fileColumn);

        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        new ValueTableCellFactory<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.FileStatus>()
                .withText(AiChatStatusViewModel.FileStatus::getDisplayName)
                .install(statusColumn);

        actionColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        new ValueTableCellFactory<AiChatStatusViewModel.IngestionStatusRow, AiChatStatusViewModel.IngestionStatusRow>()
                .withGraphic(row -> {
                    if (row.getStatus() == AiChatStatusViewModel.FileStatus.ERROR_WHILE_PROCESSING) {
                        return constructErrorButton(row);
                    }
                    return null;
                })
                .install(actionColumn);
    }

    private Button constructErrorButton(AiChatStatusViewModel.IngestionStatusRow row) {
        Button errorButton = new Button(Localization.lang("Show Error"));
        errorButton.getStyleClass().add("text-button");
        errorButton.setOnAction(event ->
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Ingestion Error"),
                        row.getError()
                )
        );
        return errorButton;
    }

    private void setupRest() {
        chatModelLabel.textProperty().bind(viewModel.chatModelProperty().map(AiChatStatusView::formatChatModelLabel));

        new ViewModelListCellFactory<AnswerEngineKind>()
                .withText(AiNamingUtils::getDisplayName)
                .install(answerEngineComboBox);
        answerEngineComboBox.setItems(viewModel.answerEngineKindsProperty());
        answerEngineComboBox.valueProperty().bindBidirectional(viewModel.selectedAnswerEngineKindProperty());
    }

    private static String formatChatModelLabel(ChatModel model) {
        if (model == null) {
            return "";
        }
        return Localization.lang("%0 %1", AiNamingUtils.getDisplayName(model.getAiProvider()), model.getName());
    }

    public void setAnswerEngine(AnswerEngine answerEngine) {
        viewModel.setAnswerEngine(answerEngine);
    }

    @FXML
    private void exportJson() {
        viewModel.exportJson();
    }

    @FXML
    private void exportMarkdown() {
        viewModel.exportMarkdown();
    }

    // [impl->req~ai.chat.clear-history~1]
    @FXML
    private void clearChatHistory() {
        viewModel.clearChatHistory();
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return viewModel.chatModelProperty();
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return viewModel.chatHistoryProperty();
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return viewModel.answerEngineProperty();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return viewModel.entriesProperty();
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return viewModel.generateEmbeddingsTasksProperty();
    }
}
