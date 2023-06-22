package org.jabref.gui.entryeditor;

import java.io.File;
import java.nio.file.Path;
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
import org.jabref.gui.entryeditor.fileannotationtab.FileAnnotationTab;
import org.jabref.gui.entryeditor.fileannotationtab.FulltextSearchResultsTab;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.GrobidOptInDialogHelper;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

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
 */
public class EntryEditor extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryEditor.class);

    private final LibraryTab libraryTab;
    private final BibDatabaseContext databaseContext;
    private final EntryEditorPreferences entryEditorPreferences;
    private final ExternalFilesEntryLinker fileLinker;
    /*
    * Tabs which can apply filter, but seems non-sense
    * */
    private final List<EntryEditorTab> tabs;
    private Subscription typeSubscription;
    /*
    * A reference to the entry this editor works on.
    * */
    private BibEntry entry;
    private SourceTab sourceTab;

    /*
    * tabs to be showed in GUI
    * */
    @FXML private TabPane tabbed;

    @FXML private Button typeChangeButton;
    @FXML private Button fetcherButton;
    @FXML private Label typeLabel;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;
    @Inject private StateManager stateManager;
    @Inject private ThemeManager themeManager;
    @Inject private FileUpdateMonitor fileMonitor;
    @Inject private CountingUndoManager undoManager;
    @Inject private BibEntryTypesManager bibEntryTypesManager;
    @Inject private KeyBindingRepository keyBindingRepository;
    @Inject private JournalAbbreviationRepository journalAbbreviationRepository;

    private final List<EntryEditorTab> entryEditorTabs = new LinkedList<>();

    public EntryEditor(LibraryTab libraryTab) {
        this.libraryTab = libraryTab;
        this.databaseContext = libraryTab.getBibDatabaseContext();

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.fileLinker = new ExternalFilesEntryLinker(preferencesService.getFilePreferences(), databaseContext);

        EasyBind.subscribe(tabbed.getSelectionModel().selectedItemProperty(), tab -> {
            EntryEditorTab activeTab = (EntryEditorTab) tab;
            if (activeTab != null) {
                activeTab.notifyAboutFocus(entry);
            }
        });

        setupKeyBindings();

        this.tabs = createTabs();

        this.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            BibEntry entry = this.getEntry();
            boolean success = false;

            if (event.getDragboard().hasContent(DataFormat.FILES)) {
                List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());
                switch (event.getTransferMode()) {
                    case COPY -> {
                        LOGGER.debug("Mode COPY");
                        fileLinker.copyFilesToFileDirAndAddToEntry(entry, files, libraryTab.getIndexingTaskManager());
                    }
                    case MOVE -> {
                        LOGGER.debug("Mode MOVE");
                        fileLinker.moveFilesToFileDirAndAddToEntry(entry, files, libraryTab.getIndexingTaskManager());
                    }
                    case LINK -> {
                        LOGGER.debug("Mode LINK");
                        fileLinker.addFilesToEntry(entry, files);
                    }
                }
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Set-up key bindings specific for the entry editor.
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
                        new HelpAction(HelpFile.ENTRY_EDITOR, dialogService).execute();
                        event.consume();
                        break;
                    case CLOSE:
                    case EDIT_ENTRY:
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
        libraryTab.delete(entry);
    }

    @FXML
    void generateCiteKeyButton() {
        GenerateCitationKeySingleAction action = new GenerateCitationKeySingleAction(getEntry(), databaseContext,
                dialogService, preferencesService, undoManager);
        action.execute();
    }

    @FXML
    void generateCleanupButton() {
        CleanupSingleAction action = new CleanupSingleAction(getEntry(), preferencesService, dialogService, stateManager, undoManager);
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
        entryEditorTabs.add(new PreviewTab(databaseContext, dialogService, preferencesService, stateManager, themeManager, libraryTab.getIndexingTaskManager()));

        // Required, optional, deprecated, and "other" fields
        entryEditorTabs.add(new RequiredFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, dialogService, preferencesService, stateManager, themeManager, libraryTab.getIndexingTaskManager(), bibEntryTypesManager, taskExecutor, journalAbbreviationRepository));
        entryEditorTabs.add(new OptionalFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, dialogService, preferencesService, stateManager, themeManager, libraryTab.getIndexingTaskManager(), bibEntryTypesManager, taskExecutor, journalAbbreviationRepository));
        entryEditorTabs.add(new OptionalFields2Tab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, dialogService, preferencesService, stateManager, themeManager, libraryTab.getIndexingTaskManager(), bibEntryTypesManager, taskExecutor, journalAbbreviationRepository));
        entryEditorTabs.add(new DeprecatedFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, dialogService, preferencesService, stateManager, themeManager, libraryTab.getIndexingTaskManager(), bibEntryTypesManager, taskExecutor, journalAbbreviationRepository));
        entryEditorTabs.add(new OtherFieldsTab(databaseContext, libraryTab.getSuggestionProviders(), undoManager, dialogService, preferencesService, stateManager, themeManager, libraryTab.getIndexingTaskManager(), bibEntryTypesManager, taskExecutor, journalAbbreviationRepository));

        // Comment Tab: Tab for general and user-specific comments
        entryEditorTabs.add(new CommentsTab(preferencesService, databaseContext, libraryTab.getSuggestionProviders(), undoManager, dialogService, stateManager, themeManager, libraryTab.getIndexingTaskManager(), taskExecutor, journalAbbreviationRepository));

        // General fields from preferences
        // First, remove all tabs that are already handled above or below; except for the source tab (which has different titles for BibTeX and BibLaTeX mode)
        Map<String, Set<Field>> entryEditorTabList = new HashMap<>(entryEditorPreferences.getEntryEditorTabs());
        entryEditorTabList.remove(PreviewTab.NAME);
        entryEditorTabList.remove(RequiredFieldsTab.NAME);
        entryEditorTabList.remove(OptionalFieldsTab.NAME);
        entryEditorTabList.remove(OptionalFields2Tab.NAME);
        entryEditorTabList.remove(DeprecatedFieldsTab.NAME);
        entryEditorTabList.remove(CommentsTab.NAME);
        entryEditorTabList.remove(MathSciNetTab.NAME);
        entryEditorTabList.remove(FileAnnotationTab.NAME);
        entryEditorTabList.remove(RelatedArticlesTab.NAME);
        entryEditorTabList.remove(LatexCitationsTab.NAME);
        entryEditorTabList.remove(FulltextSearchResultsTab.NAME);
        entryEditorTabList.remove("Comments");
        // Then show the remaining configured
        for (Map.Entry<String, Set<Field>> tab : entryEditorTabList.entrySet()) {
            entryEditorTabs.add(new UserDefinedFieldsTab(tab.getKey(), tab.getValue(), databaseContext, libraryTab.getSuggestionProviders(), undoManager, dialogService, preferencesService, stateManager, themeManager, libraryTab.getIndexingTaskManager(), taskExecutor, journalAbbreviationRepository));
        }

        // "Special" tabs
        entryEditorTabs.add(new MathSciNetTab());
        entryEditorTabs.add(new FileAnnotationTab(libraryTab.getAnnotationCache()));
        entryEditorTabs.add(new RelatedArticlesTab(this, entryEditorPreferences, preferencesService, dialogService));

        sourceTab = new SourceTab(
                databaseContext,
                undoManager,
                preferencesService.getFieldPreferences(),
                preferencesService.getImportFormatPreferences(),
                fileMonitor,
                dialogService,
                stateManager,
                keyBindingRepository);
        entryEditorTabs.add(sourceTab);

        entryEditorTabs.add(new LatexCitationsTab(databaseContext, preferencesService, taskExecutor, dialogService));

        entryEditorTabs.add(new FulltextSearchResultsTab(stateManager, preferencesService, dialogService));

        return entryEditorTabs;
    }

    private void recalculateVisibleTabs() {
        List<Tab> visibleTabs = tabs.stream().filter(tab -> tab.shouldShow(entry)).collect(Collectors.toList());

        // Start of ugly hack:
        // We need to find out, which tabs will be shown and which not and remove and re-add the appropriate tabs
        // to the editor. We don't want to simply remove all and re-add the complete list of visible tabs, because
        // the tabs give an ugly animation the looks like all tabs are shifting in from the right.
        // This hack is required since tabbed.getTabs().setAll(visibleTabs) changes the order of the tabs in the editor

        // First, remove tabs that we do not want to show
        List<EntryEditorTab> toBeRemoved = tabs.stream().filter(tab -> !tab.shouldShow(entry)).collect(Collectors.toList());
        tabbed.getTabs().removeAll(toBeRemoved);

        // Next add all the visible tabs (if not already present) at the right position
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

    /**
     * @return the currently edited entry
     */
    public BibEntry getEntry() {
        return entry;
    }

    /**
     * Sets the entry to edit.
     */
    public void setEntry(BibEntry entry) {
        Objects.requireNonNull(entry);

        // Remove subscription for old entry if existing
        if (typeSubscription != null) {
            typeSubscription.unsubscribe();
        }

        this.entry = entry;

        recalculateVisibleTabs();
        EasyBind.listen(preferencesService.getPreviewPreferences().showPreviewAsExtraTabProperty(),
                (obs, oldValue, newValue) -> recalculateVisibleTabs());

        if (entryEditorPreferences.showSourceTabByDefault()) {
            tabbed.getSelectionModel().select(sourceTab);
        }

        // Notify current tab about new entry
        getSelectedTab().notifyAboutFocus(entry);

        setupToolBar();

        // Subscribe to type changes for rebuilding the currently visible tab
        typeSubscription = EasyBind.subscribe(this.entry.typeProperty(), type -> {
            typeLabel.setText(new TypedBibEntry(entry, databaseContext.getMode()).getTypeForDisplay());
            recalculateVisibleTabs();
            getSelectedTab().notifyAboutFocus(entry);
        });
    }

    private EntryEditorTab getSelectedTab() {
        return (EntryEditorTab) tabbed.getSelectionModel().getSelectedItem();
    }

    private void setupToolBar() {
        // Update type label
        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext.getMode());
        typeLabel.setText(typedEntry.getTypeForDisplay());

        // Add type change menu
        ContextMenu typeMenu = new ChangeEntryTypeMenu(Collections.singletonList(entry), databaseContext, undoManager, keyBindingRepository, bibEntryTypesManager).asContextMenu();
        typeLabel.setOnMouseClicked(event -> typeMenu.show(typeLabel, Side.RIGHT, 0, 0));
        typeChangeButton.setOnMouseClicked(event -> typeMenu.show(typeChangeButton, Side.RIGHT, 0, 0));

        // Add menu for fetching bibliographic information
        ContextMenu fetcherMenu = new ContextMenu();
        SortedSet<EntryBasedFetcher> entryBasedFetchers = WebFetchers.getEntryBasedFetchers(
                preferencesService.getImporterPreferences(),
                preferencesService.getImportFormatPreferences(),
                preferencesService.getFilePreferences(),
                databaseContext);
        for (EntryBasedFetcher fetcher : entryBasedFetchers) {
            MenuItem fetcherMenuItem = new MenuItem(fetcher.getName());
            if (fetcher instanceof PdfMergeMetadataImporter.EntryBasedFetcherWrapper) {
                // Handle Grobid Opt-In in case of the PdfMergeMetadataImporter
                fetcherMenuItem.setOnAction(event -> {
                    GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferencesService.getGrobidPreferences());
                    PdfMergeMetadataImporter.EntryBasedFetcherWrapper pdfMergeMetadataImporter =
                            new PdfMergeMetadataImporter.EntryBasedFetcherWrapper(
                                    preferencesService.getImportFormatPreferences(),
                                    preferencesService.getFilePreferences(),
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
        new FetchAndMergeEntry(libraryTab, taskExecutor, preferencesService, dialogService).fetchAndMerge(entry, fetcher);
    }

    public void setFocusToField(Field field) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
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
        this.entryEditorTabs.forEach(EntryEditorTab::nextPreviewStyle);
    }

    public void previousPreviewStyle() {
        this.entryEditorTabs.forEach(EntryEditorTab::previousPreviewStyle);
    }
}
