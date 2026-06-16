package org.jabref.gui.entryeditor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // Fixed tabs shown before the user-configured ones
        List<EntryEditorTab> leadingTabs = List.of(
                new PreviewTab(preferences, stateManager, previewPanel),
                // Required, optional (important+detail), deprecated, and "other" fields
                new RequiredFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel),
                new ImportantOptionalFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel),
                new DetailOptionalFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel),
                new DeprecatedFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel),
                new OtherFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel),
                // Comment Tab: Tab for general and user-specific comments
                new CommentsTab(preferences, undoManager, undoAction, redoAction, journalAbbreviationRepository, stateManager, previewPanel)
        );

        // Fixed tabs shown after the user-configured ones
        List<EntryEditorTab> trailingTabs = List.of(
                new MathSciNetTab(),
                new FileAnnotationTab(stateManager, preferences),
                new CitationRelationsTab(
                        dialogService,
                        undoManager,
                        stateManager,
                        fileMonitor,
                        preferences,
                        taskExecutor,
                        themeManager,
                        bibEntryTypesManager,
                        searchCitationsRelationsService
                ),
                new RelatedArticlesTab(buildInfo, preferences, dialogService, stateManager, taskExecutor),
                // SourceTab is not a NamedEntryEditorTab, because it has different names for BibTeX and biblatex mode
                new SourceTab(
                        undoManager,
                        preferences.getFieldPreferences(),
                        preferences.getImportFormatPreferences(),
                        fileMonitor,
                        dialogService,
                        bibEntryTypesManager,
                        keyBindingRepository,
                        stateManager),
                new LatexCitationsTab(preferences, dialogService, stateManager, directoryMonitor),
                new FulltextSearchResultsTab(stateManager, preferences, dialogService, taskExecutor, entryEditor),
                new AiSummaryTab(preferences, stateManager),
                new AiChatTab(preferences, stateManager)
        );

        tabs.addAll(leadingTabs);

        // ToDo: Needs to be recreated on preferences change
        Map<String, Set<Field>> entryEditorTabList = getAdditionalUserConfiguredTabs(leadingTabs, trailingTabs);
        for (Map.Entry<String, Set<Field>> tab : entryEditorTabList.entrySet()) {
            tabs.add(new UserDefinedFieldsTab(tab.getKey(), tab.getValue(), undoManager, undoAction, redoAction, preferences, journalAbbreviationRepository, stateManager, previewPanel));
        }

        tabs.addAll(trailingTabs);

        return tabs;
    }

    /// The preferences allow to configure tabs to show (e.g.,"General", "Abstract")
    /// These should be shown. The fixed tabs (passed in) should be removed, since they are
    /// already covered by built-in tabs. The names of those built-in tabs are derived
    /// automatically from any {@link NamedEntryEditorTab} among them, so this exclusion
    /// can never go out of sync with the tabs actually created above.
    ///
    /// @return Map of tab names and the fields to show in them.
    private Map<String, Set<Field>> getAdditionalUserConfiguredTabs(List<EntryEditorTab> leadingTabs, List<EntryEditorTab> trailingTabs) {
        Map<String, Set<Field>> entryEditorTabList = new HashMap<>(preferences.getEntryEditorPreferences().getEntryEditorTabs());

        Set<String> builtInTabNames = Stream.concat(leadingTabs.stream(), trailingTabs.stream())
                                            .filter(NamedEntryEditorTab.class::isInstance)
                                            .map(NamedEntryEditorTab.class::cast)
                                            .map(NamedEntryEditorTab::getName)
                                            .collect(Collectors.toSet());
        builtInTabNames.forEach(entryEditorTabList::remove);

        return entryEditorTabList;
    }

    /// Builds the {@link EntryEditorTabModel.Feature} entries for every {@link EntryEditorTabModel.StaticTab},
    /// in enum declaration order, so callers can append them directly to a tab-models list.
    public static List<EntryEditorTabModel> fromBoolean(
            boolean showRelatedArticles,
            boolean showAISummary,
            boolean showAIChat,
            boolean showLaTeXCitations,
            boolean showFileAnnotations,
            boolean showCitationInformation,
            boolean showUserComments) {
        return List.of(
                new EntryEditorTabModel.Feature(EntryEditorTabModel.StaticTab.RELATED_ARTICLES, showRelatedArticles),
                new EntryEditorTabModel.Feature(EntryEditorTabModel.StaticTab.AI_SUMMARY, showAISummary),
                new EntryEditorTabModel.Feature(EntryEditorTabModel.StaticTab.AI_CHAT, showAIChat),
                new EntryEditorTabModel.Feature(EntryEditorTabModel.StaticTab.LATEX_CITATIONS, showLaTeXCitations),
                new EntryEditorTabModel.Feature(EntryEditorTabModel.StaticTab.FILE_ANNOTATIONS, showFileAnnotations),
                new EntryEditorTabModel.Feature(EntryEditorTabModel.StaticTab.CITATION_INFORMATION, showCitationInformation),
                new EntryEditorTabModel.Feature(EntryEditorTabModel.StaticTab.USER_COMMENTS, showUserComments)
        );
    }

    /// Builds the {@link EntryEditorTabModel.FieldSet} entries for the built-in field tabs
    /// (required/important-optional/detail-optional/deprecated/other), in display order.
    public static List<EntryEditorTabModel> builtInFieldSetsFromBoolean(
            boolean showRequiredFields,
            boolean showImportantOptionalFields,
            boolean showDetailOptionalFields,
            boolean showDeprecatedFields,
            boolean showOtherFields) {
        return List.of(
                new EntryEditorTabModel.FieldSet(EntryEditorTabModel.BuiltInFieldSet.REQUIRED_FIELDS, showRequiredFields),
                new EntryEditorTabModel.FieldSet(EntryEditorTabModel.BuiltInFieldSet.IMPORTANT_OPTIONAL_FIELDS, showImportantOptionalFields),
                new EntryEditorTabModel.FieldSet(EntryEditorTabModel.BuiltInFieldSet.DETAIL_OPTIONAL_FIELDS, showDetailOptionalFields),
                new EntryEditorTabModel.FieldSet(EntryEditorTabModel.BuiltInFieldSet.DEPRECATED_FIELDS, showDeprecatedFields),
                new EntryEditorTabModel.FieldSet(EntryEditorTabModel.BuiltInFieldSet.OTHER_FIELDS, showOtherFields)
        );
    }
}
