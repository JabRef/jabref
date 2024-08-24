package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatGuardedComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

public class AiChatTab extends EntryEditorTab {
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final AiService aiService;

    public AiChatTab(DialogService dialogService,
                     PreferencesService preferencesService,
                     AiService aiService,
                     BibDatabaseContext bibDatabaseContext,
                     TaskExecutor taskExecutor) {
        this.dialogService = dialogService;

        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.taskExecutor = taskExecutor;

        setText(Localization.lang("AI chat"));
        setTooltip(new Tooltip(Localization.lang("Chat with AI about content of attached file(s)")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    /**
     * @implNote Method similar to {@link AiSummaryTab#bindToEntry(BibEntry)}
     */
    @Override
    protected void bindToEntry(BibEntry entry) {
        if (currentEntry != null) {
            aiService.getChatHistoryService().closeChatHistoryForEntry(currentEntry);
        }

        StringProperty chatName = new SimpleStringProperty("entry " + entry.getCitationKey().orElse("<no citation key>"));
        entry.getCiteKeyBinding().addListener((observable, oldValue, newValue) -> chatName.setValue("entry " + newValue));

        setContent(new AiChatGuardedComponent(
                chatName,
                aiService.getChatHistoryService().getChatHistoryForEntry(entry),
                FXCollections.observableArrayList(new ArrayList<>(List.of(entry))),
                dialogService,
                filePreferences,
                aiService,
                bibDatabaseContext,
                taskExecutor
        ));
    }
}
