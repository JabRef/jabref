package org.jabref.gui.ai.components.aichat;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Scene;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.AdaptVisibleTabs;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.BaseWindow;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import dev.langchain4j.data.message.ChatMessage;

public class AiChatWindow extends BaseWindow {
    private final BibEntryTypesManager entryTypesManager;
    private final AiPreferences aiPreferences;
    private final FieldPreferences fieldPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AdaptVisibleTabs adaptVisibleTabs;
    private final TaskExecutor taskExecutor;
    // This field is used for finding an existing AI chat window when user wants to chat with the same group again.
    private String chatName;

    public AiChatWindow(BibEntryTypesManager entryTypesManager,
                        AiPreferences aiPreferences,
                        FieldPreferences fieldPreferences,
                        ExternalApplicationsPreferences externalApplicationsPreferences,
                        AiService aiService,
                        DialogService dialogService,
                        AdaptVisibleTabs adaptVisibleTabs,
                        TaskExecutor taskExecutor
    ) {
        this.entryTypesManager = entryTypesManager;
        this.aiPreferences = aiPreferences;
        this.fieldPreferences = fieldPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.adaptVisibleTabs = adaptVisibleTabs;
        this.taskExecutor = taskExecutor;
    }

    public void setChat(StringProperty name, ObservableList<ChatMessage> chatHistory, BibDatabaseContext bibDatabaseContext, ObservableList<BibEntry> entries) {
        setTitle(Localization.lang("AI chat with %0", name.getValue()));
        chatName = name.getValue();
        setScene(
                new Scene(
                        new AiChatGuardedComponent(
                                aiService,
                                name,
                                chatHistory,
                                bibDatabaseContext,
                                entries,
                                entryTypesManager,
                                aiPreferences,
                                fieldPreferences,
                                externalApplicationsPreferences,
                                dialogService,
                                adaptVisibleTabs,
                                taskExecutor
                        ),
                        800,
                        600
                )
        );
    }

    public String getChatName() {
        return chatName;
    }
}
