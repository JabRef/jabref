package org.jabref.gui.entryeditor;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.ai.components.summary.SummaryComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AiSummaryTab extends EntryEditorTab {
    private final AiService aiService;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final AdaptVisibleTabs adaptVisibleTabs;
    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final EntryEditorPreferences entryEditorPreferences;

    public AiSummaryTab(AiService aiService,
                        DialogService dialogService,
                        StateManager stateManager,
                        AdaptVisibleTabs adaptVisibleTabs,
                        GuiPreferences preferences) {
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.adaptVisibleTabs = adaptVisibleTabs;

        this.aiPreferences = preferences.getAiPreferences();
        this.externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        this.citationKeyPatternPreferences = preferences.getCitationKeyPatternPreferences();
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();

        setText(Localization.lang("AI summary"));
        setTooltip(new Tooltip(Localization.lang("AI-generated summary of attached file(s)")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiSummaryTab();
    }

    /**
     * @implNote Method similar to {@link AiChatTab#bindToEntry(BibEntry)}
     */
    @Override
    protected void bindToEntry(BibEntry entry) {
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().orElse(BibDatabaseContext.empty());
        setContent(new SummaryComponent(
                bibDatabaseContext,
                entry,
                aiService,
                aiPreferences,
                externalApplicationsPreferences,
                citationKeyPatternPreferences,
                dialogService,
                adaptVisibleTabs
        ));
    }
}
