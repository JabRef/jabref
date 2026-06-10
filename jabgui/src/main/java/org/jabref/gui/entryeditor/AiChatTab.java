package org.jabref.gui.entryeditor;

import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.ai.chat.AiEntryChatView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AiChatTab extends EntryEditorTab {
    private final EntryEditorPreferences entryEditorPreferences;
    private final StateManager stateManager;

    private final AiEntryChatView aiEntryChatView = new AiEntryChatView();

    public AiChatTab(
            GuiPreferences preferences,
            StateManager stateManager
    ) {
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();
        this.stateManager = stateManager;

        setText(Localization.lang("AI chat"));
        setTooltip(new Tooltip(Localization.lang("Chat with AI about content of attached file(s)")));
        setContent(aiEntryChatView);
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    /// @implNote Method similar to {@link AiSummaryTab#bindToEntry(BibEntry)}
    @Override
    protected void bindToEntry(BibEntry entry) {
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().orElse(new BibDatabaseContext());
        aiEntryChatView.selectedEntryProperty().set(new FullBibEntry(bibDatabaseContext, entry));
    }
}
