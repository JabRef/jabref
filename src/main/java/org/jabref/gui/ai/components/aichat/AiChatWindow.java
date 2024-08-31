package org.jabref.gui.ai.components.aichat;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Scene;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseWindow;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.ai.AiPreferences;

import dev.langchain4j.data.message.ChatMessage;

public class AiChatWindow extends BaseWindow {
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    public AiChatWindow(AiService aiService,
                        DialogService dialogService,
                        AiPreferences aiPreferences,
                        FilePreferences filePreferences,
                        TaskExecutor taskExecutor
    ) {
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
    }

    public void setChat(StringProperty name, ObservableList<ChatMessage> chatHistory, BibDatabaseContext bibDatabaseContext, ObservableList<BibEntry> entries) {
        setTitle(Localization.lang("AI chat with %0", name.getValue()));
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
                                filePreferences,
                                taskExecutor
                        ),
                        800,
                        600
                )
        );
    }
}
