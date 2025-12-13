package org.jabref.gui.ai.components.aichat;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
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
    private final StateManager stateManager;
    private final ObservableList<BibEntry> entries;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final TaskExecutor taskExecutor;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;


    public AiChatGuardedComponent(AiService aiService,
                                  StringProperty name,
                                  ObservableList<ChatMessage> chatHistory,
                                  StateManager stateManager,
                                  BibDatabaseContext bibDatabaseContext,
                                  ObservableList<BibEntry> entries,
                                  BibEntryTypesManager entryTypesManager,
                                  AiPreferences aiPreferences,
                                  FieldPreferences fieldPreferences,
                                  ExternalApplicationsPreferences externalApplicationsPreferences,
                                  DialogService dialogService,
                                  AdaptVisibleTabs adaptVisibleTabs,
                                  TaskExecutor taskExecutor
    ) {
        super(aiService, aiPreferences, externalApplicationsPreferences, dialogService, adaptVisibleTabs);

        this.aiService = aiService;
        this.name = name;
        this.chatHistory = chatHistory;
        this.stateManager = stateManager;
        this.bibDatabaseContext = bibDatabaseContext;
        this.entries = entries;
        this.entryTypesManager = entryTypesManager;
        this.aiPreferences = aiPreferences;
        this.fieldPreferences = fieldPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        rebuildUi();
    }

    @Override
    protected Node showEmbeddingModelGuardedContent() {
        return new AiChatComponent(
                aiService,
                name,
                chatHistory,
                stateManager,
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
