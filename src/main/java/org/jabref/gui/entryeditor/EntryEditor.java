package org.jabref.gui.entryeditor;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.citationkeypattern.GenerateCitationKeySingleAction;
import org.jabref.gui.cleanup.CleanupSingleAction;
import org.jabref.gui.entryeditor.citationrelationtab.CitationRelationsTab;
import org.jabref.gui.entryeditor.fileannotationtab.FileAnnotationTab;
import org.jabref.gui.entryeditor.fileannotationtab.FulltextSearchResultsTab;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.GrobidOptInDialogHelper;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.DragDrop;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.bibtex.TypedBibEntry;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.DirectoryMonitorManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI component that allows editing of the fields of a BibEntry (i.e. the one that shows up, when you double click on
 * an entry in the table)
 * <p>
 * It hosts the tabs (required, general, optional) and the buttons to the left.
 * <p>
 * EntryEditor also registers itself to the event bus, receiving events whenever a field of the entry changes, enabling
 * the text fields to update themselves if the change is made from somewhere else.
 * <p>
 * The editors for fields are created via {@link org.jabref.gui.fieldeditors.FieldEditors}.
 */
public class EntryEditor extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryEditor.class);

    private final LibraryTab libraryTab;
    private final BibDatabaseContext databaseContext;
    private final EntryEditorPreferences entryEditorPreferences;
    private final ExternalFilesEntryLinker fileLinker;
    private final DirectoryMonitorManager directoryMonitorManager;
    private final UndoAction undoAction;
    private final RedoAction redoAction;

    private Subscription typeSubscription;

    private BibEntry currentlyEditedEntry;

    private SourceTab sourceTab;

    @FXML private TabPane tabbed;

    @FXML private Button typeChangeButton;
    @FXML private Button fetcherButton;
    @FXML private Label typeLabel;

    @Inject private BuildInfo buildInfo;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private GuiPreferences preferences;
    @Inject private StateManager stateManager;
    @Inject private ThemeManager themeManager;
    @Inject private FileUpdateMonitor fileMonitor;
    @Inject private CountingUndoManager undoManager;
    @Inject private BibEntryTypesManager bibEntryTypesManager;
    @Inject private KeyBindingRepository keyBindingRepository;
    @Inject private JournalAbbreviationRepository journalAbbreviationRepository;
    @Inject private AiService aiService;

    private final List<EntryEditorTab> allPossibleTabs;
    private final Collection<OffersPreview> previewTabs;

    public EntryEditor(LibraryTab libraryTab, UndoAction undoAction, RedoAction redoAction) {
        this.libraryTab = libraryTab;
        this.databaseContext = libraryTab.getBibDatabaseContext();
        this.directoryMonitorManager = libraryTab.getDirectoryMonitorManager();
        this.undoAction = undoAction;
        this.redoAction = redoAction;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.entryEditorPreferences = preferences.getEntryEditorPreferences();
        this.fileLinker = new ExternalFilesEntryLinker(preferences.getExternalApplicationsPreferences(), preferences.getFilePreferences(), databaseContext, dialogService);

        setupKeyBindings();

        this.allPossibleTabs = createTabs();
        this.previewTabs = this.allPossibleTabs.stream().filter(OffersPreview.class::isInstance).map(OffersPreview.class::cast).toList();

        setupDragAndDrop(libraryTab);

        EasyBind.subscribe(tabbed.getSelectionModel().selectedItemProperty(), tab -> {
            EntryEditorTab activeTab = (EntryEditorTab) tab;
            if (activeTab != null) {
                activeTab.notifyAboutFocus(currentlyEditedEntry);
            }
        });

        EasyBind.listen(preferences.getPreviewPreferences().showPreviewAsExtraTabProperty(),
                (obs, oldValue, newValue) -> {
                    if (currentlyEditedEntry != null) {
                        adaptVisibleTabs();
                    }
                });
    }

    private void setupDragAndDrop(LibraryTab libraryTab) {
        this.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            BibEntry entry = this.getCurrentlyEditedEntry();
            boolean success = false;

            if (event.getDragboard().hasContent(DataFormat.FILES)) {
                TransferMode transferMode = event.getTransferMode();
                List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());
                // Modifiers do not work on macOS: https://bugs.openjdk.org/browse/JDK-8264172
                // Similar code as org.jabref.gui.externalfiles.ImportHandler.importFilesInBackground
                DragDrop.handleDropOfFiles(files, transferMode, fileLinker, entry);
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Set up key bindings specific for the entry editor.
     */
    private void setupKeyBindings() {
        this.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case ENTRY_EDITOR_NEXT_PANEL:
                    case ENTRY_EDITOR_NEXT_PANEL_2:
                        tabbed.getSelectionModel().selectNext();
                        event.consume();
                        break;
                    case ENTRY_EDITOR_PREVIOUS_PANEL:
                    case ENTRY_EDITOR_PREVIOUS_PANEL_2:
                        tabbed.getSelectionModel().selectPrevious();
                        event.consume();
                        break;
                    case ENTRY_EDITOR_NEXT_ENTRY:
                        libraryTab.selectNextEntry();
                        event.consume();
                        break;
                    case ENTRY_EDITOR_PREVIOUS_ENTRY:
                        libraryTab.selectPreviousEntry();
                        event.consume();
                        break;
                    case HELP:
                        new HelpAction(HelpFile.ENTRY_EDITOR, dialogService, preferences.getExternalApplicationsPreferences()).execute();
                        event.consume();
                        break;
                    case CLOSE:
                        // We do not want to close the entry editor as such
                        // We just want to unfocus the field
                        tabbed.requestFocus();
                        break;
                    case OPEN_CLOSE_ENTRY_EDITOR:
                        close();
                        event.consume();
                        break;
                    default:
                        // Pass other keys to parent
                }
            }
        });
    }

    @FXML
    public void close() {
        libraryTab.entryEditorClosing();
    }

    @FXML
    private void deleteEntry() {
        libraryTab.deleteEntry(currentlyEditedEntry);
    }

    @FXML
    void generateCiteKeyButton() {
        GenerateCitationKeySingleAction action = new GenerateCitationKeySingleAction(getCurrentlyEditedEntry(), databaseContext,
                dialogService, preferences, undoManager);
        action.execute();
    }

    @FXML
    void generateCleanupButton() {
        CleanupSingleAction action = new CleanupSingleAction(getCurrentlyEditedEntry(), preferences, dialogService, stateManager, undoManager);
        action.execute();
    }

    @FXML
    private void navigateToPreviousEntry() {
        libraryTab.selectPreviousEntry();
    }

    @FXML
    private void navigateToNextEntry() {
        libraryTab.selectNextEntry();
    }

    private List<EntryEditorTab> createTabs() {
        List<EntryEditorTab> tabs = new LinkedList<>();

        tabs.add(new PreviewTab(databaseContext, dialogService, preferences, themeManager, taskExecutor, libraryTab.searchQueryProperty()));

        // Required, optional (important+detail), deprecated, and "other" fields
        tabs.add(new RequiredFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, undoAction, redoAction, dialogService, preferences, themeManager, bibEntryTypesManager, taskExecutor, journalAbbreviationRepository, libraryTab.searchQueryProperty()));
        tabs.add(new ImportantOptionalFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, undoAction, redoAction, dialogService, preferences, themeManager, bibEntryTypesManager, taskExecutor, journalAbbreviationRepository, libraryTab.searchQueryProperty()));
        tabs.add(new DetailOptionalFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, undoAction, redoAction, dialogService, preferences, themeManager, bibEntryTypesManager, taskExecutor, journalAbbreviationRepository, libraryTab.searchQueryProperty()));
        tabs.add(new DeprecatedFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, undoAction, redoAction, dialogService, preferences, themeManager, bibEntryTypesManager, taskExecutor, journalAbbreviationRepository, libraryTab.searchQueryProperty()));
        tabs.add(new OtherFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, undoAction, redoAction, dialogService, preferences, themeManager, bibEntryTypesManager, taskExecutor, journalAbbreviationRepository, libraryTab.searchQueryProperty()));

        // Comment Tab: Tab for general and user-specific comments
        tabs.add(new CommentsTab(preferences, databaseContext, libraryTab.getSuggestionProviders(), undoManager, undoAction, redoAction, dialogService, themeManager, taskExecutor, journalAbbreviationRepository, libraryTab.searchQueryProperty()));

        Map<String, Set<Field>> entryEditorTabList = getAdditionalUserConfiguredTabs();
        for (Map.Entry<String, Set<Field>> tab : entryEditorTabList.entrySet()) {
            tabs.add(new UserDefinedFieldsTab(tab.getKey(), tab.getValue(), databaseContext, libraryTab.getSuggestionProviders(), undoManager, undoAction, redoAction, dialogService, preferences, themeManager, taskExecutor, journalAbbreviationRepository, libraryTab.searchQueryProperty()));
        }

        tabs.add(new MathSciNetTab());
        tabs.add(new FileAnnotationTab(libraryTab.getAnnotationCache()));
        tabs.add(new SciteTab(preferences, taskExecutor, dialogService));
        tabs.add(new CitationRelationsTab(dialogService, databaseContext,
                undoManager, stateManager, fileMonitor, preferences, libraryTab, taskExecutor, bibEntryTypesManager));
        tabs.add(new RelatedArticlesTab(buildInfo, databaseContext, preferences, dialogService, taskExecutor));
        sourceTab = new SourceTab(
                databaseContext,
                undoManager,
                preferences.getFieldPreferences(),
                preferences.getImportFormatPreferences(),
                fileMonitor,
                dialogService,
                bibEntryTypesManager,
                keyBindingRepository,
                libraryTab.searchQueryProperty());
        tabs.add(sourceTab);
        tabs.add(new LatexCitationsTab(databaseContext, preferences, dialogService, directoryMonitorManager));
        tabs.add(new FulltextSearchResultsTab(stateManager, preferences, dialogService, databaseContext, taskExecutor, libraryTab.searchQueryProperty()));
        tabs.add(new AiSummaryTab(libraryTab.getBibDatabaseContext(), aiService, dialogService, preferences));
        tabs.add(new AiChatTab(libraryTab.getBibDatabaseContext(), aiService, dialogService, preferences, taskExecutor));

        return tabs;
    }

    /**
     * The preferences allow to configure tabs to show (e.g.,"General", "Abstract")
     * These should be shown. Already hard-coded ones (above and below this code block) should be removed.
     * This method does this calculation.
     *
     * @return Map of tab names and the fields to show in them.
     */
    private Map<String, Set<Field>> getAdditionalUserConfiguredTabs() {
        Map<String, Set<Field>> entryEditorTabList = new HashMap<>(entryEditorPreferences.getEntryEditorTabs());

        // Same order as in org.jabref.gui.entryeditor.EntryEditor.createTabs before the call of getAdditionalUserConfiguredTabs
        entryEditorTabList.remove(PreviewTab.NAME);
        entryEditorTabList.remove(RequiredFieldsTab.NAME);
        entryEditorTabList.remove(ImportantOptionalFieldsTab.NAME);
        entryEditorTabList.remove(DetailOptionalFieldsTab.NAME);
        entryEditorTabList.remove(DeprecatedFieldsTab.NAME);
        entryEditorTabList.remove(OtherFieldsTab.NAME);
        entryEditorTabList.remove(CommentsTab.NAME);

        // Same order as in org.jabref.gui.entryeditor.EntryEditor.createTabs after the call of getAdditionalUserConfiguredTabs
        entryEditorTabList.remove(MathSciNetTab.NAME);
        entryEditorTabList.remove(FileAnnotationTab.NAME);
        entryEditorTabList.remove(SciteTab.NAME);
        entryEditorTabList.remove(CitationRelationsTab.NAME);
        entryEditorTabList.remove(RelatedArticlesTab.NAME);
        // SourceTab is not listed, because it has different names for BibTeX and biblatex mode
        entryEditorTabList.remove(LatexCitationsTab.NAME);
        entryEditorTabList.remove(FulltextSearchResultsTab.NAME);

        return entryEditorTabList;
    }

    /**
     * Adapt the visible tabs to the current entry type.
     */
    private void adaptVisibleTabs() {
        // We need to find out, which tabs will be shown (and which not anymore) and remove and re-add the appropriate tabs
        // to the editor. We cannot to simply remove all and re-add the complete list of visible tabs, because
        // the tabs give an ugly animation the looks like all tabs are shifting in from the right. In other words:
        // This hack is required since tabbed.getTabs().setAll(visibleTabs) changes the order of the tabs in the editor

        // First, remove tabs that we do not want to show
        List<EntryEditorTab> toBeRemoved = allPossibleTabs.stream().filter(tab -> !tab.shouldShow(currentlyEditedEntry)).toList();
        tabbed.getTabs().removeAll(toBeRemoved);

        // Next add all the visible tabs (if not already present) at the right position
        List<Tab> visibleTabs = allPossibleTabs.stream().filter(tab -> tab.shouldShow(currentlyEditedEntry)).collect(Collectors.toList());
        for (int i = 0; i < visibleTabs.size(); i++) {
            Tab toBeAdded = visibleTabs.get(i);
            Tab shown = null;

            if (i < tabbed.getTabs().size()) {
                shown = tabbed.getTabs().get(i);
            }

            if (!toBeAdded.equals(shown)) {
                tabbed.getTabs().add(i, toBeAdded);
            }
        }
    }

    public BibEntry getCurrentlyEditedEntry() {
        return currentlyEditedEntry;
    }

    public void setCurrentlyEditedEntry(BibEntry currentlyEditedEntry) {
        this.currentlyEditedEntry = Objects.requireNonNull(currentlyEditedEntry);

        // Subscribe to type changes for rebuilding the currently visible tab
        if (typeSubscription != null) {
            // Remove subscription for old entry if existing
            typeSubscription.unsubscribe();
        }
        typeSubscription = EasyBind.subscribe(this.currentlyEditedEntry.typeProperty(), type -> {
            typeLabel.setText(new TypedBibEntry(currentlyEditedEntry, databaseContext.getMode()).getTypeForDisplay());
            adaptVisibleTabs();
            getSelectedTab().notifyAboutFocus(currentlyEditedEntry);
        });

        adaptVisibleTabs();
        setupToolBar();

        if (entryEditorPreferences.showSourceTabByDefault()) {
            tabbed.getSelectionModel().select(sourceTab);
        }
        getSelectedTab().notifyAboutFocus(currentlyEditedEntry);
    }

    private EntryEditorTab getSelectedTab() {
        return (EntryEditorTab) tabbed.getSelectionModel().getSelectedItem();
    }

    private void setupToolBar() {
        // Update type label
        TypedBibEntry typedEntry = new TypedBibEntry(currentlyEditedEntry, databaseContext.getMode());
        typeLabel.setText(typedEntry.getTypeForDisplay());

        // Add type change menu
        ContextMenu typeMenu = new ChangeEntryTypeMenu(Collections.singletonList(currentlyEditedEntry), databaseContext, undoManager, bibEntryTypesManager).asContextMenu();
        typeLabel.setOnMouseClicked(event -> typeMenu.show(typeLabel, Side.RIGHT, 0, 0));
        typeChangeButton.setOnMouseClicked(event -> typeMenu.show(typeChangeButton, Side.RIGHT, 0, 0));

        // Add menu for fetching bibliographic information
        ContextMenu fetcherMenu = new ContextMenu();
        SortedSet<EntryBasedFetcher> entryBasedFetchers = WebFetchers.getEntryBasedFetchers(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFilePreferences(),
                databaseContext);
        for (EntryBasedFetcher fetcher : entryBasedFetchers) {
            MenuItem fetcherMenuItem = new MenuItem(fetcher.getName());
            if (fetcher instanceof PdfMergeMetadataImporter.EntryBasedFetcherWrapper) {
                // Handle Grobid Opt-In in case of the PdfMergeMetadataImporter
                fetcherMenuItem.setOnAction(event -> {
                    GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferences.getGrobidPreferences());
                    PdfMergeMetadataImporter.EntryBasedFetcherWrapper pdfMergeMetadataImporter =
                            new PdfMergeMetadataImporter.EntryBasedFetcherWrapper(
                                    preferences.getImportFormatPreferences(),
                                    preferences.getFilePreferences(),
                                    databaseContext);
                    fetchAndMerge(pdfMergeMetadataImporter);
                });
            } else {
                fetcherMenuItem.setOnAction(event -> fetchAndMerge(fetcher));
            }
            fetcherMenu.getItems().add(fetcherMenuItem);
        }

        fetcherButton.setOnMouseClicked(event -> fetcherMenu.show(fetcherButton, Side.RIGHT, 0, 0));
    }

    private void fetchAndMerge(EntryBasedFetcher fetcher) {
        new FetchAndMergeEntry(libraryTab.getBibDatabaseContext(), taskExecutor, preferences, dialogService, undoManager).fetchAndMerge(currentlyEditedEntry, fetcher);
    }

    public void setFocusToField(Field field) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            for (Tab tab : tabbed.getTabs()) {
                if ((tab instanceof FieldsEditorTab fieldsEditorTab)
                        && fieldsEditorTab.getShownFields().contains(field)) {
                    tabbed.getSelectionModel().select(tab);
                    fieldsEditorTab.requestFocus(field);
                }
            }
        });
    }

    public void nextPreviewStyle() {
        this.previewTabs.forEach(OffersPreview::nextPreviewStyle);
    }

    public void previousPreviewStyle() {
        this.previewTabs.forEach(OffersPreview::previousPreviewStyle);
    }
}
