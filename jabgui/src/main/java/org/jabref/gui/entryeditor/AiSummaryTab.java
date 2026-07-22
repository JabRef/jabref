package org.jabref.gui.entryeditor;

import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.ai.summary.AiSummaryView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

// [impl->feat~ai.summarization.entries~1]
public class AiSummaryTab extends EntryEditorTab {
    private final StateManager stateManager;

    private final AiSummaryView aiSummaryView;

    public AiSummaryTab(
            GuiPreferences preferences,
            StateManager stateManager
    ) {
        this.stateManager = stateManager;

        this.aiSummaryView = new AiSummaryView();

        setText(EntryEditorTabModel.BuiltIn.AI_SUMMARY.displayName());
        setTooltip(new Tooltip(Localization.lang("AI-generated summary of attached file(s)")));
        setContent(aiSummaryView);
    }

    /// @implNote Method similar to {@link AiChatTab#bindToEntry(BibEntry)}
    @Override
    protected void bindToEntry(BibEntry entry) {
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().orElse(new BibDatabaseContext());
        aiSummaryView.entryProperty().set(new FullBibEntry(bibDatabaseContext, entry));
    }
}
