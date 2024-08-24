package org.jabref.gui.ai.components.aichat;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.message.ChatMessage;

public class AiChatDialog extends BaseDialog<Void> {

    public AiChatDialog(StringProperty name,
                        ObservableList<ChatMessage> chatHistory,
                        ObservableList<BibEntry> entries,
                        DialogService dialogService,
                        FilePreferences filePreferences,
                        AiService aiService,
                        BibDatabaseContext bibDatabaseContext,
                        TaskExecutor taskExecutor
    ) {
        this.initModality(Modality.NONE);

        this.getDialogPane().getScene().getWindow().setOnCloseRequest(e -> this.close());

        this.getDialogPane().setContent(new AiChatGuardedComponent(
                name,
                chatHistory,
                entries,
                dialogService,
                filePreferences,
                aiService,
                bibDatabaseContext,
                taskExecutor
        ));
    }
}
