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
import org.jabref.preferences.ai.AiPreferences;

import jakarta.annotation.Nullable;

public class AiChatTab extends EntryEditorTab {
    private final BibDatabaseContext bibDatabaseContext;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final TaskExecutor taskExecutor;

    private @Nullable BibEntry previousBibEntry;

    public AiChatTab(BibDatabaseContext bibDatabaseContext,
                     AiService aiService,
                     DialogService dialogService,
                     PreferencesService preferencesService,
                     TaskExecutor taskExecutor
    ) {
        this.bibDatabaseContext = bibDatabaseContext;

        this.aiService = aiService;
        this.dialogService = dialogService;

        this.aiPreferences = preferencesService.getAiPreferences();
        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

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
        if (previousBibEntry != null) {
            aiService.getChatHistoryService().closeChatHistoryForEntry(previousBibEntry.getId());
        }

        previousBibEntry = entry;

        StringProperty chatName = new SimpleStringProperty("entry " + entry.getCitationKey().orElse("<no citation key>"));
        entry.getCiteKeyBinding().addListener((observable, oldValue, newValue) -> chatName.setValue("entry " + newValue));

        setContent(new AiChatGuardedComponent(
                chatName,
                aiService.getChatHistoryService().getChatHistoryForEntry(entry),
                bibDatabaseContext,
                FXCollections.observableArrayList(new ArrayList<>(List.of(entry))),
                aiService,
                dialogService,
                aiPreferences,
                filePreferences,
                taskExecutor
        ));
    }
}
