package org.jabref.gui.ai.components.aichat;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.util.EmbeddingModelGuardedComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.ChatMessage;

/**
 * Main class for AI chatting. It checks if the AI features are enabled and if the embedding model is properly set up.
 */
public class AiChatGuardedComponent extends EmbeddingModelGuardedComponent {
    /// This field is used for two purposes:
    /// 1. Logging
    /// 2. Title of group chat window
    /// Thus, if you use {@link AiChatGuardedComponent} for one entry in {@link EntryEditor}, then you may not localize
    /// this parameter. However, for group chat window, you should.
    private final StringProperty name;

    private final ObservableList<ChatMessage> chatHistory;
    private final BibDatabaseContext bibDatabaseContext;
    private final ObservableList<BibEntry> entries;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final TaskExecutor taskExecutor;

    public AiChatGuardedComponent(StringProperty name,
                                  ObservableList<ChatMessage> chatHistory,
                                  BibDatabaseContext bibDatabaseContext,
                                  ObservableList<BibEntry> entries,
                                  AiService aiService,
                                  DialogService dialogService,
                                  AiPreferences aiPreferences,
                                  ExternalApplicationsPreferences externalApplicationsPreferences,
                                  TaskExecutor taskExecutor
    ) {
        super(aiService, aiPreferences, externalApplicationsPreferences, dialogService);

        this.name = name;
        this.chatHistory = chatHistory;
        this.bibDatabaseContext = bibDatabaseContext;
        this.entries = entries;
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.aiPreferences = aiPreferences;
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
                aiPreferences,
                dialogService,
                taskExecutor
        );
    }
}
