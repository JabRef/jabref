package org.jabref.gui.entryeditor;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javafx.beans.property.SimpleBooleanProperty;

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

    /// Creates all tabs that can possibly be shown from {@link EntryEditorTabModel}, in display order.
    public List<EntryEditorTab> createTabs() {
        List<EntryEditorTab> tabs = new LinkedList<>();

        for (EntryEditorTabModel model : preferences.getEntryEditorPreferences().getTabModels()) {
            tabs.add(createTab(model));
        }

        return tabs;
    }

    /// Maps a single {@link EntryEditorTabModel} to its concrete {@link EntryEditorTab} view, wiring in the
    /// user-controlled visibility derived from the same model. Customized field-set tabs are always enabled
    /// (toggled only by adding/removing them); the Preview tab's toggle lives in the preview preferences
    /// ("show preview as a separate tab"), not in its tab model.
    public EntryEditorTab createTab(EntryEditorTabModel model) {
        EntryEditorPreferences entryEditorPreferences = preferences.getEntryEditorPreferences();
        return switch (model) {
            case EntryEditorTabModel.FieldSet(
                    EntryEditorTabModel.BuiltInFieldSet type,
                    boolean _
            ) -> {
                EntryEditorTab tab = createFieldSetTab(type);
                tab.setPreferenceDrivenVisibility(entryEditorPreferences.tabVisibleProperty(type));
                yield tab;
            }
            case EntryEditorTabModel.CustomizedFieldSet(
                    String name,
                    Set<Field> fields
            ) -> {
                EntryEditorTab tab = new UserDefinedFieldsTab(name, fields, undoManager, undoAction, redoAction, preferences, journalAbbreviationRepository, stateManager, previewPanel);
                tab.setPreferenceDrivenVisibility(new SimpleBooleanProperty(true));
                yield tab;
            }
            case EntryEditorTabModel.Feature(
                    EntryEditorTabModel.StaticTab type,
                    boolean _
            ) -> {
                EntryEditorTab tab = createFeatureTab(type);
                tab.setPreferenceDrivenVisibility(type == EntryEditorTabModel.StaticTab.PREVIEW
                                                  ? preferences.getPreviewPreferences().showPreviewAsExtraTabProperty()
                                                  : entryEditorPreferences.tabVisibleProperty(type));
                yield tab;
            }
        };
    }

    private EntryEditorTab createFieldSetTab(EntryEditorTabModel.BuiltInFieldSet type) {
        return switch (type) {
            case REQUIRED_FIELDS ->
                    new RequiredFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel);
            case IMPORTANT_OPTIONAL_FIELDS ->
                    new ImportantOptionalFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel);
            case DETAIL_OPTIONAL_FIELDS ->
                    new DetailOptionalFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel);
            case DEPRECATED_FIELDS ->
                    new DeprecatedFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel);
            case OTHER_FIELDS ->
                    new OtherFieldsTab(undoManager, undoAction, redoAction, preferences, bibEntryTypesManager, journalAbbreviationRepository, stateManager, previewPanel);
        };
    }

    private EntryEditorTab createFeatureTab(EntryEditorTabModel.StaticTab type) {
        return switch (type) {
            case PREVIEW ->
                    new PreviewTab(preferences, stateManager, previewPanel);
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
                            themeManager,
                            bibEntryTypesManager,
                            searchCitationsRelationsService);
            case COMMENTS ->
                    new CommentsTab(preferences, undoManager, undoAction, redoAction, journalAbbreviationRepository, stateManager, previewPanel);
            case MATH_SCI_NET ->
                    new MathSciNetTab();
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
