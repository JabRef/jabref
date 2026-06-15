package org.jabref.gui.entryeditor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.CitationRelationsTab;
import org.jabref.gui.entryeditor.fileannotationtab.FileAnnotationTab;
import org.jabref.gui.entryeditor.fileannotationtab.FulltextSearchResultsTab;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.DirectoryMonitor;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.FileUpdateMonitor;

/// Builds the {@link EntryEditorTab} controls shown in the {@link EntryEditor}.
///
/// Analogous to {@link org.jabref.gui.maintable.MainTableColumnFactory}: it turns the tab configuration
/// ({@link EntryEditorTabModel}) plus the fixed, always-present tabs into the concrete JavaFX tab views,
/// keeping tab creation (and the GUI dependencies it needs) out of the view and the view model.
public class EntryEditorTabFactory {

    private final PreviewPanel previewPanel;
    private final UndoAction undoAction;
    private final RedoAction redoAction;
    private final BuildInfo buildInfo;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final ThemeManager themeManager;
    private final FileUpdateMonitor fileMonitor;
    private final DirectoryMonitor directoryMonitor;
    private final CountingUndoManager undoManager;
    private final BibEntryTypesManager bibEntryTypesManager;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final KeyBindingRepository keyBindingRepository;
    private final SearchCitationsRelationsService searchCitationsRelationsService;

    public EntryEditorTabFactory(PreviewPanel previewPanel,
                                 UndoAction undoAction,
                                 RedoAction redoAction,
                                 BuildInfo buildInfo,
                                 DialogService dialogService,
                                 TaskExecutor taskExecutor,
                                 GuiPreferences preferences,
                                 StateManager stateManager,
                                 ThemeManager themeManager,
                                 FileUpdateMonitor fileMonitor,
                                 DirectoryMonitor directoryMonitor,
                                 CountingUndoManager undoManager,
                                 BibEntryTypesManager bibEntryTypesManager,
                                 JournalAbbreviationRepository journalAbbreviationRepository,
                                 KeyBindingRepository keyBindingRepository,
                                 SearchCitationsRelationsService searchCitationsRelationsService) {
        this.previewPanel = previewPanel;
        this.undoAction = undoAction;
        this.redoAction = redoAction;
        this.buildInfo = buildInfo;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
        this.fileMonitor = fileMonitor;
        this.directoryMonitor = directoryMonitor;
        this.undoManager = undoManager;
        this.bibEntryTypesManager = bibEntryTypesManager;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.keyBindingRepository = keyBindingRepository;
        this.searchCitationsRelationsService = searchCitationsRelationsService;
    }

    /// Creates all tabs that can possibly be shown in the entry editor, in display order.
    ///
    /// @param entryEditor the editor hosting the tabs (needed by {@link FulltextSearchResultsTab})
    public List<EntryEditorTab> createTabs(EntryEditor entryEditor) {
        List<EntryEditorTab> tabs = new LinkedList<>();

        tabs.add(new PreviewTab(preferences, stateManager, previewPanel));

        // Required, optional (important+detail), deprecated, and "other" fields
        tabs.add(new RequiredFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel));
        tabs.add(new ImportantOptionalFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel));
        tabs.add(new DetailOptionalFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel));
        tabs.add(new DeprecatedFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel));
        tabs.add(new OtherFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel));

        // Comment Tab: Tab for general and user-specific comments
        tabs.add(new CommentsTab(preferences, undoManager, undoAction, redoAction, journalAbbreviationRepository, stateManager, previewPanel));

        // ToDo: Needs to be recreated on preferences change
        Map<String, Set<Field>> entryEditorTabList = getAdditionalUserConfiguredTabs();
        for (Map.Entry<String, Set<Field>> tab : entryEditorTabList.entrySet()) {
            tabs.add(new UserDefinedFieldsTab(tab.getKey(), tab.getValue(), undoManager, undoAction, redoAction, preferences, journalAbbreviationRepository, stateManager, previewPanel));
        }

        tabs.add(new MathSciNetTab());
        tabs.add(new FileAnnotationTab(stateManager, preferences));
        tabs.add(new CitationRelationsTab(
                dialogService,
                undoManager,
                stateManager,
                fileMonitor,
                preferences,
                taskExecutor,
                themeManager,
                bibEntryTypesManager,
                searchCitationsRelationsService
        ));
        tabs.add(new RelatedArticlesTab(buildInfo, preferences, dialogService, stateManager, taskExecutor));
        tabs.add(new SourceTab(
                undoManager,
                preferences.getFieldPreferences(),
                preferences.getImportFormatPreferences(),
                fileMonitor,
                dialogService,
                bibEntryTypesManager,
                keyBindingRepository,
                stateManager));
        tabs.add(new LatexCitationsTab(preferences, dialogService, stateManager, directoryMonitor));
        tabs.add(new FulltextSearchResultsTab(stateManager, preferences, dialogService, taskExecutor, entryEditor));
        tabs.add(new AiSummaryTab(preferences, stateManager));
        tabs.add(new AiChatTab(preferences, stateManager));

        return tabs;
    }

    /// The preferences allow to configure tabs to show (e.g.,"General", "Abstract")
    /// These should be shown. Already hard-coded ones (above and below this code block) should be removed.
    /// This method does this calculation.
    ///
    /// @return Map of tab names and the fields to show in them.
    private Map<String, Set<Field>> getAdditionalUserConfiguredTabs() {
        Map<String, Set<Field>> entryEditorTabList = new HashMap<>(preferences.getEntryEditorPreferences().getEntryEditorTabs());

        // Same order as in createTabs before the call of getAdditionalUserConfiguredTabs
        entryEditorTabList.remove(PreviewTab.NAME);
        entryEditorTabList.remove(RequiredFieldsTab.NAME);
        entryEditorTabList.remove(ImportantOptionalFieldsTab.NAME);
        entryEditorTabList.remove(DetailOptionalFieldsTab.NAME);
        entryEditorTabList.remove(DeprecatedFieldsTab.NAME);
        entryEditorTabList.remove(OtherFieldsTab.NAME);
        entryEditorTabList.remove(CommentsTab.NAME);

        // Same order as in createTabs after the call of getAdditionalUserConfiguredTabs
        entryEditorTabList.remove(MathSciNetTab.NAME);
        entryEditorTabList.remove(FileAnnotationTab.NAME);
        entryEditorTabList.remove(CitationRelationsTab.NAME);
        entryEditorTabList.remove(RelatedArticlesTab.NAME);
        // SourceTab is not listed, because it has different names for BibTeX and biblatex mode
        entryEditorTabList.remove(LatexCitationsTab.NAME);
        entryEditorTabList.remove(FulltextSearchResultsTab.NAME);

        return entryEditorTabList;
    }
}
