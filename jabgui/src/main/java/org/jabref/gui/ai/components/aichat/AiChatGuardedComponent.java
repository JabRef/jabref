package org.jabref.gui.ai.components.aichat;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.util.EmbeddingModelGuardedComponent;
import org.jabref.gui.entryeditor.AdaptVisibleTabs;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import dev.langchain4j.data.message.ChatMessage;

/**
 * Main class for AI chatting. It checks if the AI features are enabled and if the embedding model is properly set up.
 */
public class AiChatGuardedComponent extends EmbeddingModelGuardedComponent {
    /// This field is used for two purposes:
    /// 1. Logging
    /// 2. Title of group chat window
    /// Thus, if you use {@link AiChatGuardedComponent} for one entry in {@link org.jabref.gui.entryeditor.EntryEditor}, then you may not localize
    /// this parameter. However, for group chat window, you should.
    private final StringProperty name;

    private final ObservableList<ChatMessage> chatHistory;
    private final BibDatabaseContext bibDatabaseContext;
    private final ObservableList<BibEntry> entries;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final TaskExecutor taskExecutor;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    public AiChatGuardedComponent(StringProperty name,
                                  ObservableList<ChatMessage> chatHistory,
                                  BibDatabaseContext bibDatabaseContext,
                                  ObservableList<BibEntry> entries,
                                  AiService aiService,
                                  TaskExecutor taskExecutor,
                                  AiPreferences aiPreferences,
                                  FieldPreferences fieldPreferences,
                                  BibEntryTypesManager entryTypesManager,
                                  ExternalApplicationsPreferences externalApplicationsPreferences,
                                  DialogService dialogService,
                                  AdaptVisibleTabs adaptVisibleTabs
    ) {
        super(aiService, aiPreferences, externalApplicationsPreferences, dialogService, adaptVisibleTabs);

        this.name = name;
        this.chatHistory = chatHistory;
        this.bibDatabaseContext = bibDatabaseContext;
        this.entries = entries;
        this.aiService = aiService;
        this.taskExecutor = taskExecutor;
        this.aiPreferences = aiPreferences;
        this.fieldPreferences = fieldPreferences;
        this.entryTypesManager = entryTypesManager;
        this.dialogService = dialogService;

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
                entryTypesManager,
                aiPreferences,
                fieldPreferences,
                dialogService,
                taskExecutor
        );
    }
}
