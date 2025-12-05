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
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final TaskExecutor taskExecutor;
    private final AdaptVisibleTabs adaptVisibleTabs;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    // This field is used for finding an existing AI chat window when user wants to chat with the same group again.
    private String chatName;

    public AiChatWindow(AiService aiService,
                        DialogService dialogService,
                        AiPreferences aiPreferences,
                        ExternalApplicationsPreferences externalApplicationsPreferences,
                        AdaptVisibleTabs adaptVisibleTabs,
                        TaskExecutor taskExecutor,
                        BibEntryTypesManager entryTypesManager,
                        FieldPreferences fieldPreferences
    ) {
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.aiPreferences = aiPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.adaptVisibleTabs = adaptVisibleTabs;
        this.taskExecutor = taskExecutor;
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
    }

    public void setChat(StringProperty name, ObservableList<ChatMessage> chatHistory, BibDatabaseContext bibDatabaseContext, ObservableList<BibEntry> entries) {
        setTitle(Localization.lang("AI chat with %0", name.getValue()));
        chatName = name.getValue();
        setScene(
                new Scene(
                        new AiChatGuardedComponent(
                                name,
                                chatHistory,
                                bibDatabaseContext,
                                entries,
                                aiService,
                                dialogService,
                                aiPreferences,
                                externalApplicationsPreferences,
                                adaptVisibleTabs,
                                taskExecutor,
                                entryTypesManager,
                                fieldPreferences
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
