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

public class AiSummaryTab extends EntryEditorTab {

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final AiService aiService;

    public AiSummaryTab(DialogService dialogService,
                        PreferencesService preferencesService,
                        AiService aiService,
                        BibDatabaseContext bibDatabaseContext) {
        this.dialogService = dialogService;
        this.filePreferences = preferencesService.getFilePreferences();
        this.citationKeyPatternPreferences = preferencesService.getCitationKeyPatternPreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;

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
        setContent(new SummaryComponent(bibDatabaseContext, entry, aiService, filePreferences, citationKeyPatternPreferences, dialogService));
    }
}
