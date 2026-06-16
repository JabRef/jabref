package org.jabref.gui.entryeditor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.application.Platform;
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
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.citationkeypattern.GenerateCitationKeySingleAction;
import org.jabref.gui.cleanup.CleanupSingleAction;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.GrobidUseDialogHelper;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewControls;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.DirectoryMonitor;
import org.jabref.gui.util.DragDrop;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.bibtex.TypedBibEntry;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;

/// GUI component that allows editing of the fields of a BibEntry (i.e. the one that shows up, when you double click on
/// an entry in the table)
///
/// It hosts the tabs (required, general, optional) and the buttons to the left.
///
/// EntryEditor also registers itself to the event bus, receiving events whenever a field of the entry changes, enabling
/// the text fields to update themselves if the change is made from somewhere else.
///
/// The editors for fields are created via {@link org.jabref.gui.fieldeditors.FieldEditors}.
public class EntryEditor extends BorderPane implements PreviewControls, AdaptVisibleTabs {
    private final Supplier<LibraryTab> tabSupplier;
    private final ExternalFilesEntryLinker fileLinker;
    private final PreviewPanel previewPanel;
    private final EntryEditorTabFactory tabFactory;
    private final UndoAction undoAction;
    private final RedoAction redoAction;

    private Subscription typeSubscription;

    private final EntryEditorViewModel viewModel;
    private final EntryEditorFocusUtils focusUtils;

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
    @Inject private DirectoryMonitor directoryMonitor;
    @Inject private CountingUndoManager undoManager;
    @Inject private BibEntryTypesManager bibEntryTypesManager;
    @Inject private KeyBindingRepository keyBindingRepository;
    @Inject private JournalAbbreviationRepository journalAbbreviationRepository;
    @Inject private AiService aiService;
    @Inject private SearchCitationsRelationsService searchCitationsRelationsService;

    private final List<EntryEditorTab> allPossibleTabs = new ArrayList<>();
    private final List<Subscription> shouldShowSubscriptions = new ArrayList<>();

    public EntryEditor(Supplier<LibraryTab> tabSupplier, UndoAction undoAction, RedoAction redoAction) {
        this.tabSupplier = tabSupplier;
        this.undoAction = undoAction;
        this.redoAction = redoAction;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new EntryEditorViewModel(stateManager, preferences.getEntryEditorPreferences());

        this.fileLinker = new ExternalFilesEntryLinker(
                preferences.getExternalApplicationsPreferences(),
                preferences.getFilePreferences(),
                dialogService,
                stateManager);

        this.previewPanel = new PreviewPanel(
                dialogService,
                preferences.getKeyBindingRepository(),
                preferences,
                themeManager,
                taskExecutor,
                stateManager);

        this.tabFactory = new EntryEditorTabFactory(
                previewPanel,
                undoAction,
                redoAction,
                buildInfo,
                dialogService,
                taskExecutor,
                preferences,
                stateManager,
                themeManager,
                fileMonitor,
                directoryMonitor,
                undoManager,
                bibEntryTypesManager,
                journalAbbreviationRepository,
                keyBindingRepository,
                searchCitationsRelationsService);

        this.focusUtils = new EntryEditorFocusUtils(tabbed, this);

        setupKeyBindings();

        EasyBind.subscribe(stateManager.activeTabProperty(), tab -> {
            if (tab.isPresent()) {
                tabbed.getTabs().clear();

                shouldShowSubscriptions.forEach(Subscription::unsubscribe);
                shouldShowSubscriptions.clear();
                this.allPossibleTabs.clear();
                this.allPossibleTabs.addAll(tabFactory.createTabs(this));
                this.sourceTab = allPossibleTabs.stream()
                                                .filter(SourceTab.class::isInstance)
                                                .map(SourceTab.class::cast)
                                                .findFirst()
                                                .orElse(null);

                allPossibleTabs.forEach(t ->
                        shouldShowSubscriptions.add(EasyBind.subscribe(t.shouldShow(), _ -> syncTabPane())));
                syncTabPane();
            } else {
                shouldShowSubscriptions.forEach(Subscription::unsubscribe);
                shouldShowSubscriptions.clear();
                this.allPossibleTabs.clear();
                close();
            }
        });

        setupDragAndDrop();

        EasyBind.subscribe(tabbed.getSelectionModel().selectedItemProperty(), tab -> {
            EntryEditorTab activeTab = (EntryEditorTab) tab;
            if (activeTab != null) {
                activeTab.notifyAboutFocus(viewModel.getCurrentlyEditedEntry());
                if (activeTab instanceof FieldsEditorTab fieldsTab) {
                    Platform.runLater(() -> setupNavigationForTab(fieldsTab));
                }
            }
        });

        EasyBind.listen(viewModel.currentlyEditedEntryProperty(), (_, _, newEntry) -> {
            if (newEntry != null) {
                onEntryChanged(newEntry);
            }
        });

        EasyBind.listen(preferences.getPreviewPreferences().showPreviewAsExtraTabProperty(),
                (_, _, newValue) -> {
                    if (viewModel.getCurrentlyEditedEntry() != null) {
                        Tab tab = tabbed.getSelectionModel().selectedItemProperty().get();
                        if (newValue && tab instanceof FieldsEditorTab fieldsEditorTab) {
                            fieldsEditorTab.removePreviewPanelFromThisTab();
                        }
                        if (tab instanceof TabWithPreviewPanel previewTab) {
                            previewTab.handleFocus();
                        }
                    }
                });
    }

    private void setupDragAndDrop() {
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

    private void setupNavigationForTab(FieldsEditorTab tab) {
        focusUtils.setupNavigationForTab(tab);
    }

    /// Set up key bindings specific for the entry editor.
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
                        focusUtils.captureFocusedField();
                        tabSupplier.get().selectNextEntry();
                        event.consume();
                        break;
                    case ENTRY_EDITOR_PREVIOUS_ENTRY:
                        focusUtils.captureFocusedField();
                        tabSupplier.get().selectPreviousEntry();
                        event.consume();
                        break;
                    case JUMP_TO_FIELD:
                        selectFieldDialog();
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

    public void selectFieldDialog() {
        if (getCurrentlyEditedEntry() == null) {
            return;
        }
        JumpToFieldDialog dialog = new JumpToFieldDialog(this);
        dialog.initModality(Modality.NONE);
        dialog.show();
    }

    @FXML
    private void close() {
        stateManager.getEditorShowing().set(false);
    }

    @FXML
    private void deleteEntry() {
        tabSupplier.get().deleteEntry(viewModel.getCurrentlyEditedEntry());
    }

    @FXML
    private void generateCiteKeyButton() {
        GenerateCitationKeySingleAction action = new GenerateCitationKeySingleAction(getCurrentlyEditedEntry(), tabSupplier.get().getBibDatabaseContext(),
                dialogService, preferences, undoManager);
        action.execute();
    }

    @FXML
    private void generateCleanupButton() {
        CleanupSingleAction action = new CleanupSingleAction(getCurrentlyEditedEntry(), preferences, dialogService, stateManager, undoManager, journalAbbreviationRepository);
        action.execute();
    }

    @FXML
    private void navigateToPreviousEntry() {
        tabSupplier.get().selectPreviousEntry();
    }

    @FXML
    private void navigateToNextEntry() {
        tabSupplier.get().selectNextEntry();
    }

    @Override
    public void adaptVisibleTabs() {
        BibEntry entry = viewModel.getCurrentlyEditedEntry();
        if (entry != null) {
            allPossibleTabs.forEach(tab -> tab.currentEntryProperty().set(entry));
        }
        syncTabPane();
    }

    /// Diffs {@link #allPossibleTabs} against the current {@link #tabbed} contents and adds/removes tabs to match
    /// the tabs whose {@link EntryEditorTab#shouldShow()} is {@code true}, preserving order without a full replace
    /// (a full replace causes an ugly shift-in animation for all tabs).
    private void syncTabPane() {
        if (viewModel.getCurrentlyEditedEntry() == null) {
            tabbed.getTabs().clear();
            return;
        }

        List<EntryEditorTab> toBeRemoved = allPossibleTabs.stream().filter(tab -> !tab.shouldShow().getValue()).toList();
        tabbed.getTabs().removeAll(toBeRemoved);

        List<Tab> visibleTabs = allPossibleTabs.stream().filter(tab -> tab.shouldShow().getValue()).collect(Collectors.toList());
        for (int i = 0; i < visibleTabs.size(); i++) {
            Tab toBeAdded = visibleTabs.get(i);
            Tab shown = i < tabbed.getTabs().size() ? tabbed.getTabs().get(i) : null;
            if (!toBeAdded.equals(shown)) {
                tabbed.getTabs().add(i, toBeAdded);
            }
        }
    }

    public BibEntry getCurrentlyEditedEntry() {
        return viewModel.getCurrentlyEditedEntry();
    }

    public List<EntryEditorTab> getAllPossibleTabs() {
        return allPossibleTabs;
    }

    public void setCurrentlyEditedEntry(@NonNull BibEntry entry) {
        viewModel.currentlyEditedEntryProperty().set(entry);
    }

    private void onEntryChanged(@NonNull BibEntry entry) {
        allPossibleTabs.forEach(tab -> tab.currentEntryProperty().set(entry));

        if (typeSubscription != null) {
            typeSubscription.unsubscribe();
        }

        typeSubscription = EasyBind.subscribe(entry.typeProperty(), _ -> {
            typeLabel.setText(new TypedBibEntry(entry, tabSupplier.get().getBibDatabaseContext().getMode()).getTypeForDisplay());
            setupToolBar();
            getSelectedTab().notifyAboutFocus(entry);
        });

        typeLabel.setText(new TypedBibEntry(entry, tabSupplier.get().getBibDatabaseContext().getMode()).getTypeForDisplay());

        setupToolBar();

        if (preferences.getEntryEditorPreferences().showSourceTabByDefault()) {
            tabbed.getSelectionModel().select(sourceTab);
        }
        Platform.runLater(() -> {
            for (Tab tab : tabbed.getTabs()) {
                if (tab instanceof FieldsEditorTab fieldsTab) {
                    setupNavigationForTab(fieldsTab);
                }
            }
        });

        EntryEditorTab selectedTab = getSelectedTab();
        if (selectedTab != null) {
            Platform.runLater(() -> selectedTab.notifyAboutFocus(entry));
        }

        focusUtils.restoreLastFocusedField();
    }

    private EntryEditorTab getSelectedTab() {
        return (EntryEditorTab) tabbed.getSelectionModel().getSelectedItem();
    }

    private void setupToolBar() {
        BibEntry entry = viewModel.getCurrentlyEditedEntry();
        // Update type label
        TypedBibEntry typedEntry = new TypedBibEntry(entry, tabSupplier.get().getBibDatabaseContext().getMode());
        typeLabel.setText(typedEntry.getTypeForDisplay());

        // Add type change menu
        ContextMenu typeMenu = new ChangeEntryTypeMenu(List.of(entry), tabSupplier.get().getBibDatabaseContext(), undoManager, bibEntryTypesManager).asContextMenu();
        typeLabel.setOnMouseClicked(event -> typeMenu.show(typeLabel, Side.RIGHT, 0, 0));
        typeChangeButton.setOnMouseClicked(event -> typeMenu.show(typeChangeButton, Side.RIGHT, 0, 0));

        // Add menu for fetching bibliographic information
        ContextMenu fetcherMenu = new ContextMenu();
        SortedSet<EntryBasedFetcher> entryBasedFetchers = WebFetchers.getEntryBasedFetchers(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFilePreferences(),
                tabSupplier.get().getBibDatabaseContext());
        for (EntryBasedFetcher fetcher : entryBasedFetchers) {
            MenuItem fetcherMenuItem = new MenuItem(fetcher.getName());
            if (fetcher instanceof PdfMergeMetadataImporter.EntryBasedFetcherWrapper) {
                // Handle Grobid Opt-In in case of the PdfMergeMetadataImporter
                fetcherMenuItem.setOnAction(event -> {
                    GrobidUseDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferences.getGrobidPreferences());
                    PdfMergeMetadataImporter.EntryBasedFetcherWrapper pdfMergeMetadataImporter =
                            new PdfMergeMetadataImporter.EntryBasedFetcherWrapper(
                                    preferences.getImportFormatPreferences(),
                                    preferences.getFilePreferences(),
                                    tabSupplier.get().getBibDatabaseContext());
                    fetchAndMerge(pdfMergeMetadataImporter);
                });
            } else {
                fetcherMenuItem.setOnAction(_ -> fetchAndMerge(fetcher));
            }
            fetcherMenu.getItems().add(fetcherMenuItem);
        }

        fetcherButton.setOnMouseClicked(_ -> fetcherMenu.show(fetcherButton, Side.RIGHT, 0, 0));
    }

    private void fetchAndMerge(EntryBasedFetcher fetcher) {
        new FetchAndMergeEntry(tabSupplier.get().getBibDatabaseContext(), taskExecutor, preferences, dialogService, undoManager, stateManager).fetchAndMerge(viewModel.getCurrentlyEditedEntry(), fetcher);
    }

    public void selectField(String fieldName) {
        focusUtils.setFocusToField(FieldFactory.parseField(fieldName));
    }

    public void setFocusToField(Field field) {
        focusUtils.setFocusToField(field);
    }

    @Override
    public void nextPreviewStyle() {
        this.previewPanel.nextPreviewStyle();
    }

    @Override
    public void previousPreviewStyle() {
        this.previewPanel.previousPreviewStyle();
    }
}
