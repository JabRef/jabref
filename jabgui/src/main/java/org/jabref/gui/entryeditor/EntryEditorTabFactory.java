package org.jabref.gui.entryeditor;

import java.util.LinkedList;
import java.util.List;

import javafx.beans.value.ObservableValue;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.CitationRelationsTab;
import org.jabref.gui.entryeditor.fileannotationtab.FileAnnotationTab;
import org.jabref.gui.entryeditor.fileannotationtab.FulltextSearchResultsTab;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.DirectoryMonitor;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.annotations.VisibleForTesting;
import com.tobiasdiez.easybind.EasyBind;

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
        this.fileMonitor = fileMonitor;
        this.directoryMonitor = directoryMonitor;
        this.undoManager = undoManager;
        this.bibEntryTypesManager = bibEntryTypesManager;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.keyBindingRepository = keyBindingRepository;
        this.searchCitationsRelationsService = searchCitationsRelationsService;
    }

    /// Creates all tabs that can possibly be shown from {@link EntryEditorTabModel}, in display order.
    public List<EntryEditorTab> createTabs() {
        List<EntryEditorTab> tabs = new LinkedList<>();

        for (EntryEditorTabModel model : preferences.getEntryEditorPreferences().getTabModels()) {
            tabs.add(createTab(model));
        }

        return tabs;
    }

    /// Maps a single {@link EntryEditorTabModel} to its concrete {@link EntryEditorTab} view, wiring in the
    /// user-controlled visibility derived from the same model. The Preview tab's toggle lives in the preview
    /// preferences ("show preview as a separate tab"), not in its tab model.
    public EntryEditorTab createTab(EntryEditorTabModel model) {
        EntryEditorPreferences entryEditorPreferences = preferences.getEntryEditorPreferences();
        return switch (model) {
            case EntryEditorTabModel.BuiltInTab(
                    EntryEditorTabModel.BuiltIn type,
                    boolean _
            ) -> {
                EntryEditorTab tab = createBuiltInTab(type);
                tab.setPreferenceDrivenVisibility(preferenceDrivenVisibilityFor(type, entryEditorPreferences));
                yield tab;
            }
        };
    }

    /// Determines the preference-driven visibility gate for a built-in tab. Most tabs simply follow the user's
    /// Entry Editor tab-visibility checkbox; the Preview tab uses its own preference instead, and the AI tabs
    /// are additionally hidden whenever AI features are currently disabled, so they disappear immediately
    /// (no restart needed) alongside the "AI turned off" state already shown by {@link AiSummaryTab} and
    /// {@link AiChatTab}.
    @VisibleForTesting
    ObservableValue<Boolean> preferenceDrivenVisibilityFor(EntryEditorTabModel.BuiltIn type, EntryEditorPreferences entryEditorPreferences) {
        ObservableValue<Boolean> userVisibility = type == EntryEditorTabModel.BuiltIn.PREVIEW
                                                  ? preferences.getPreviewPreferences().showPreviewAsExtraTabProperty()
                                                  : entryEditorPreferences.tabVisibleProperty(type);
        if (type == EntryEditorTabModel.BuiltIn.AI_SUMMARY || type == EntryEditorTabModel.BuiltIn.AI_CHAT) {
            return EasyBind.combine(userVisibility, preferences.getAiPreferences().aiFeaturesEnabledCurrentlyProperty(),
                    (visible, aiEnabled) -> visible && aiEnabled);
        }
        return userVisibility;
    }

    private EntryEditorTab createBuiltInTab(EntryEditorTabModel.BuiltIn type) {
        return switch (type) {
            case PREVIEW ->
                    new PreviewTab(preferences, stateManager, previewPanel);
            case ALL_FIELDS ->
                    new AllFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel);
            case RELATED_ARTICLES ->
                    new RelatedArticlesTab(buildInfo, preferences, dialogService, stateManager, taskExecutor);
            case AI_SUMMARY ->
                    new AiSummaryTab(preferences, stateManager);
            case AI_CHAT ->
                    new AiChatTab(preferences, stateManager);
            case FILE_ANNOTATIONS ->
                    new FileAnnotationTab(stateManager, preferences);
            case LATEX_CITATIONS ->
                    new LatexCitationsTab(preferences, dialogService, stateManager, directoryMonitor);
            case CITATION_INFORMATION ->
                    new CitationRelationsTab(
                            dialogService,
                            undoManager,
                            stateManager,
                            fileMonitor,
                            preferences,
                            taskExecutor,
                            bibEntryTypesManager,
                            searchCitationsRelationsService);
            case SOURCE ->
                    new SourceTab(
                            undoManager,
                            preferences.getFieldPreferences(),
                            preferences.getImportFormatPreferences(),
                            fileMonitor,
                            dialogService,
                            bibEntryTypesManager,
                            keyBindingRepository,
                            stateManager);
            case FULLTEXT_SEARCH_RESULTS ->
                    new FulltextSearchResultsTab(stateManager, preferences, dialogService, taskExecutor);
        };
    }
}
