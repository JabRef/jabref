package org.jabref.gui.ai.components.chat;

import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.guards.EmbeddingModelGuard;
import org.jabref.gui.ai.components.guards.privacynotice.AiPrivacyNoticeGuard;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.guards.GuardedComponent;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.ChatMessage;

/**
 * Main class for AI chatting. It checks if the AI features are enabled and if the embedding model is properly set up.
 */
public class AiChatComponent extends GuardedComponent {
    private final StringProperty name;

    private final ObservableList<ChatMessage> chatHistory;
    private final BibDatabaseContext bibDatabaseContext;
    private final ObservableList<BibEntry> entries;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final TaskExecutor taskExecutor;

    public AiChatComponent(StringProperty name,
                           ObservableList<ChatMessage> chatHistory,
                           BibDatabaseContext bibDatabaseContext,
                           ObservableList<BibEntry> entries,
                           AiService aiService,
                           DialogService dialogService,
                           AiPreferences aiPreferences,
                           ExternalApplicationsPreferences externalApplicationsPreferences,
                           TaskExecutor taskExecutor
    ) {
        super(List.of(
                new AiPrivacyNoticeGuard(aiPreferences, externalApplicationsPreferences, dialogService),
                new EmbeddingModelGuard(aiService)
        ));

        this.name = name;
        this.chatHistory = chatHistory;
        this.bibDatabaseContext = bibDatabaseContext;
        this.entries = entries;
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.aiPreferences = aiPreferences;
        this.taskExecutor = taskExecutor;

        checkGuards();
    }

    @Override
    protected Node showGuardedComponent() {
        return new RawAiChatComponent(
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
