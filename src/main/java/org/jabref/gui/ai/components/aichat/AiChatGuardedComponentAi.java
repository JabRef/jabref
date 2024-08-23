package org.jabref.gui.ai.components.aichat;

import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.privacynotice.AiPrivacyNoticeGuardedComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.models.JabRefEmbeddingModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.data.message.ChatMessage;

public class AiChatGuardedComponentAi extends AiPrivacyNoticeGuardedComponent {
    private final String name;
    private final ObservableList<ChatMessage> chatHistory;
    private final ObservableList<BibEntry> entries;
    private final DialogService dialogService;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final AiService aiService;

    public AiChatGuardedComponentAi(String name,
                                    ObservableList<ChatMessage> chatHistory,
                                    ObservableList<BibEntry> entries,
                                    DialogService dialogService,
                                    FilePreferences filePreferences,
                                    AiService aiService,
                                    BibDatabaseContext bibDatabaseContext,
                                    TaskExecutor taskExecutor
    ) {
        super(aiService.getPreferences(), filePreferences, dialogService);

        this.name = name;
        this.chatHistory = chatHistory;
        this.entries = entries;
        this.dialogService = dialogService;
        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.taskExecutor = taskExecutor;

        rebuildUi();
    }

    @Override
    protected Node showGuardedContent() {
        if (!aiService.getEmbeddingModel().isPresent()) {
            if (aiService.getEmbeddingModel().hadErrorWhileBuildingModel()) {
                return showErrorWhileBuildingEmbeddingModel();
            } else {
                return showBuildingEmbeddingModel();
            }
        } else {
            return showAiChat();
        }
    }

    private Node showErrorWhileBuildingEmbeddingModel() {
        return ErrorStateComponent.withTextAreaAndButton(
                Localization.lang("Unable to chat"),
                Localization.lang("An error occurred while building the embedding model"),
                aiService.getEmbeddingModel().getErrorWhileBuildingModel(),
                Localization.lang("Rebuild"),
                () -> aiService.getEmbeddingModel().startRebuildingTask()
        );
    }

    public Node showBuildingEmbeddingModel() {
        return ErrorStateComponent.withSpinner(
                Localization.lang("Downloading..."),
                Localization.lang("Downloading embedding model... Afterward, you will be able to chat with your files.")
        );
    }

    private Node showAiChat() {
        return new AiChatComponent(
                aiService,
                name,
                chatHistory,
                entries,
                bibDatabaseContext,
                dialogService,
                taskExecutor
        );
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuiltEvent event) {
        UiTaskExecutor.runInJavaFXThread(AiChatGuardedComponentAi.this::rebuildUi);
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuildingErrorEvent event) {
        UiTaskExecutor.runInJavaFXThread(AiChatGuardedComponentAi.this::rebuildUi);
    }
}
