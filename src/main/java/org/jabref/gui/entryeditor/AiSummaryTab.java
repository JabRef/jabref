package org.jabref.gui.entryeditor;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.summary.SummaryComponent;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.ai.AiPreferences;

public class AiSummaryTab extends EntryEditorTab {
    private final BibDatabaseContext bibDatabaseContext;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final EntryEditorPreferences entryEditorPreferences;

    public AiSummaryTab(BibDatabaseContext bibDatabaseContext,
                        AiService aiService,
                        DialogService dialogService,
                        PreferencesService preferencesService
    ) {
        this.bibDatabaseContext = bibDatabaseContext;

        this.aiService = aiService;
        this.dialogService = dialogService;

        this.aiPreferences = preferencesService.getAiPreferences();
        this.filePreferences = preferencesService.getFilePreferences();
        this.citationKeyPatternPreferences = preferencesService.getCitationKeyPatternPreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

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
        setContent(new SummaryComponent(
                bibDatabaseContext,
                entry,
                aiService,
                aiPreferences,
                filePreferences,
                citationKeyPatternPreferences,
                dialogService
        ));
    }
}
