package org.jabref.gui.ai.components.aichat;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.util.EmbeddingModelGuardedComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.message.ChatMessage;

public class AiChatGuardedComponent extends EmbeddingModelGuardedComponent {
    private final StringProperty name;
    private final ObservableList<ChatMessage> chatHistory;
    private final ObservableList<BibEntry> entries;
    private final DialogService dialogService;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final AiService aiService;

    public AiChatGuardedComponent(StringProperty name,
                                  ObservableList<ChatMessage> chatHistory,
                                  ObservableList<BibEntry> entries,
                                  DialogService dialogService,
                                  FilePreferences filePreferences,
                                  AiService aiService,
                                  BibDatabaseContext bibDatabaseContext,
                                  TaskExecutor taskExecutor
    ) {
        super(aiService, filePreferences, dialogService);

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
    protected Node showEmbeddingModelGuardedContent() {
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
}
