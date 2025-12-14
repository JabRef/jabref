package org.jabref.gui.entryeditor.citationcontexttab;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

public class CitationContextTab extends EntryEditorTab {

    private final AiService aiService;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final TaskExecutor taskExecutor;
    private final EntryEditorPreferences entryEditorPreferences;

    public CitationContextTab(AiService aiService,
                              DialogService dialogService,
                              StateManager stateManager,
                              GuiPreferences preferences,
                              BibEntryTypesManager entryTypesManager,
                              TaskExecutor taskExecutor) {
        this.aiService = aiService;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.taskExecutor = taskExecutor;
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();

        setText(Localization.lang("Citation contexts"));
        setTooltip(new Tooltip(Localization.lang("View how this entry is described in related work sections of other papers")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowCitationContextTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().orElse(new BibDatabaseContext());

        setContent(new CitationContextComponent(
                aiService,
                bibDatabaseContext,
                entry,
                dialogService,
                preferences,
                entryTypesManager,
                taskExecutor
        ));
    }
}
