package org.jabref.gui.entryeditor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;

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
import org.jabref.gui.util.UiTaskExecutor;
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
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;

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
public class EntryEditor extends BorderPane implements PreviewControls, AdaptVisibleTabs {
    private final Supplier<LibraryTab> tabSupplier;
    private final ExternalFilesEntryLinker fileLinker;
    private final PreviewPanel previewPanel;
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
    @Inject private DirectoryMonitor directoryMonitor;
    @Inject private CountingUndoManager undoManager;
    @Inject private BibEntryTypesManager bibEntryTypesManager;
    @Inject private KeyBindingRepository keyBindingRepository;
    @Inject private JournalAbbreviationRepository journalAbbreviationRepository;
    @Inject private AiService aiService;
    @Inject private SearchCitationsRelationsService searchCitationsRelationsService;

    private final List<EntryEditorTab> allPossibleTabs = new ArrayList<>();

    public EntryEditor(Supplier<LibraryTab> tabSupplier, UndoAction undoAction, RedoAction redoAction) {
        this.tabSupplier = tabSupplier;
        this.undoAction = undoAction;
        this.redoAction = redoAction;

        ViewLoader.view(this)
                  .root(this)
                  .load();

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

        setupKeyBindings();

        EasyBind.subscribe(stateManager.activeTabProperty(), tab -> {
            if (tab.isPresent()) {
                tabbed.getTabs().clear();

                this.allPossibleTabs.clear();
                this.allPossibleTabs.addAll(createTabs());

                adaptVisibleTabs();
            } else {
                this.allPossibleTabs.clear();
            }
        });

        setupDragAndDrop();

        EasyBind.subscribe(tabbed.getSelectionModel().selectedItemProperty(), tab -> {
            EntryEditorTab activeTab = (EntryEditorTab) tab;
            if (activeTab != null) {
                activeTab.notifyAboutFocus(currentlyEditedEntry);
                if (activeTab instanceof FieldsEditorTab fieldsTab) {
                    Platform.runLater(() -> setupNavigationForTab(fieldsTab));
                }
            }
        });

        stateManager.getSelectedEntries().addListener((InvalidationListener) _ -> {
            if (stateManager.getSelectedEntries().isEmpty()) {
                // [impl->req~entry-editor.keep-showing~1]
                // No change in the entry editor
                // We allow users to edit the "old" entry
            } else {
                setCurrentlyEditedEntry(stateManager.getSelectedEntries().getFirst());
            }
        });

        EasyBind.listen(preferences.getPreviewPreferences().showPreviewAsExtraTabProperty(),
                (_, _, newValue) -> {
                    if (currentlyEditedEntry != null) {
                        adaptVisibleTabs();
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
        Node content = tab.getContent();
        if (content instanceof Parent parent) {
            findAndSetupTabNavigableNodes(parent);
        }
    }

    private void findAndSetupTabNavigableNodes(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            // Generic handler for other focusable controls (e.g., Button, ComboBox, CheckBox, etc.)
            child.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                    if (isLastFieldInCurrentTab(child)) {
                        moveToNextTabAndFocus();
                        event.consume();
                    }
                }
            });

            if (child instanceof Parent childParent) {
                findAndSetupTabNavigableNodes(childParent);
            }
        }
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
                        tabSupplier.get().selectNextEntry();
                        event.consume();
                        break;
                    case ENTRY_EDITOR_PREVIOUS_ENTRY:
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
        tabSupplier.get().deleteEntry(currentlyEditedEntry);
    }

    @FXML
    private void generateCiteKeyButton() {
        GenerateCitationKeySingleAction action = new GenerateCitationKeySingleAction(getCurrentlyEditedEntry(), tabSupplier.get().getBibDatabaseContext(),
                dialogService, preferences, undoManager);
        action.execute();
    }

    @FXML
    private void generateCleanupButton() {
        CleanupSingleAction action = new CleanupSingleAction(getCurrentlyEditedEntry(), preferences, dialogService, stateManager, undoManager);
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

    private List<EntryEditorTab> createTabs() {
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
                bibEntryTypesManager,
                searchCitationsRelationsService
        ));
        tabs.add(new org.jabref.gui.entryeditor.citationcontexttab.CitationContextTab(
                aiService,
                dialogService,
                stateManager,
                preferences,
                bibEntryTypesManager,
                taskExecutor
        ));
        tabs.add(new RelatedArticlesTab(buildInfo, preferences, dialogService, stateManager, taskExecutor));
        sourceTab = new SourceTab(
                undoManager,
                preferences.getFieldPreferences(),
                preferences.getImportFormatPreferences(),
                fileMonitor,
                dialogService,
                bibEntryTypesManager,
                keyBindingRepository,
                stateManager);
        tabs.add(sourceTab);
        tabs.add(new LatexCitationsTab(preferences, dialogService, stateManager, directoryMonitor));
        tabs.add(new FulltextSearchResultsTab(stateManager, preferences, dialogService, taskExecutor, this));
        tabs.add(new AiSummaryTab(stateManager, bibEntryTypesManager, preferences, aiService, dialogService, this));
        tabs.add(new AiChatTab(stateManager, bibEntryTypesManager, preferences, aiService, dialogService, this, taskExecutor));

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
        Map<String, Set<Field>> entryEditorTabList = new HashMap<>(preferences.getEntryEditorPreferences().getEntryEditorTabs());

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
        entryEditorTabList.remove(CitationRelationsTab.NAME);
        entryEditorTabList.remove(RelatedArticlesTab.NAME);
        // SourceTab is not listed, because it has different names for BibTeX and biblatex mode
        entryEditorTabList.remove(LatexCitationsTab.NAME);
        entryEditorTabList.remove(FulltextSearchResultsTab.NAME);

        return entryEditorTabList;
    }

    @Override
    public void adaptVisibleTabs() {
        // We need to find out, which tabs will be shown (and which not anymore) and remove and re-add the appropriate tabs
        // to the editor. We cannot to simply remove all and re-add the complete list of visible tabs, because
        // the tabs give an ugly animation the looks like all tabs are shifting in from the right. In other words:
        // This hack is required since tabbed.getTabs().setAll(visibleTabs) changes the order of the tabs in the editor

        if (currentlyEditedEntry == null) {
            tabbed.getTabs().clear();
            return;
        }

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

    public List<EntryEditorTab> getAllPossibleTabs() {
        return allPossibleTabs;
    }

    public void setCurrentlyEditedEntry(@NonNull BibEntry currentlyEditedEntry) {
        if (Objects.equals(this.currentlyEditedEntry, currentlyEditedEntry)) {
            return;
        }

        this.currentlyEditedEntry = currentlyEditedEntry;

        // Subscribe to type changes for rebuilding the currently visible tab
        if (typeSubscription != null) {
            // Remove subscription for old entry if existing
            typeSubscription.unsubscribe();
        }

        typeSubscription = EasyBind.subscribe(this.currentlyEditedEntry.typeProperty(), _ -> {
            typeLabel.setText(new TypedBibEntry(this.currentlyEditedEntry, tabSupplier.get().getBibDatabaseContext().getMode()).getTypeForDisplay());
            adaptVisibleTabs();
            setupToolBar();
            getSelectedTab().notifyAboutFocus(this.currentlyEditedEntry);
        });

        typeLabel.setText(new TypedBibEntry(currentlyEditedEntry, tabSupplier.get().getBibDatabaseContext().getMode()).getTypeForDisplay());

        adaptVisibleTabs();

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
            Platform.runLater(() -> selectedTab.notifyAboutFocus(currentlyEditedEntry));
        }
    }

    private EntryEditorTab getSelectedTab() {
        return (EntryEditorTab) tabbed.getSelectionModel().getSelectedItem();
    }

    private void setupToolBar() {
        // Update type label
        TypedBibEntry typedEntry = new TypedBibEntry(currentlyEditedEntry, tabSupplier.get().getBibDatabaseContext().getMode());
        typeLabel.setText(typedEntry.getTypeForDisplay());

        // Add type change menu
        ContextMenu typeMenu = new ChangeEntryTypeMenu(List.of(currentlyEditedEntry), tabSupplier.get().getBibDatabaseContext(), undoManager, bibEntryTypesManager).asContextMenu();
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
        new FetchAndMergeEntry(tabSupplier.get().getBibDatabaseContext(), taskExecutor, preferences, dialogService, undoManager, stateManager).fetchAndMerge(currentlyEditedEntry, fetcher);
    }

    public void selectField(String fieldName) {
        setFocusToField(org.jabref.model.entry.field.FieldFactory.parseField(fieldName));
    }

    public void setFocusToField(Field field) {
        UiTaskExecutor.runInJavaFXThread(() -> getTabContainingField(field).ifPresentOrElse(
                tab -> selectTabAndField(tab, field),
                () -> {
                    Field aliasField = EntryConverter.FIELD_ALIASES.get(field);
                    getTabContainingField(aliasField).ifPresent(tab -> selectTabAndField(tab, aliasField));
                }
        ));
    }

    private void selectTabAndField(FieldsEditorTab tab, Field field) {
        Platform.runLater(() -> {
            tabbed.getSelectionModel().select(tab);
            tab.requestFocus(field);
        });
        // This line explicitly brings focus back to the main window containing the Entry Editor.
        getScene().getWindow().requestFocus();
    }

    private Optional<FieldsEditorTab> getTabContainingField(Field field) {
        return tabbed.getTabs().stream()
                     .filter(FieldsEditorTab.class::isInstance)
                     .map(FieldsEditorTab.class::cast)
                     .filter(tab -> tab.getShownFields().contains(field))
                     .findFirst();
    }

    @Override
    public void nextPreviewStyle() {
        this.previewPanel.nextPreviewStyle();
    }

    @Override
    public void previousPreviewStyle() {
        this.previewPanel.previousPreviewStyle();
    }

    /**
     * Checks if the given TextField is the last field in the currently selected tab.
     *
     * @param node the Node to check
     * @return true if this is the last field in the current tab, false otherwise
     */
    boolean isLastFieldInCurrentTab(Node node) {
        if (node == null || tabbed.getSelectionModel().getSelectedItem() == null) {
            return false;
        }

        Tab selectedTab = tabbed.getSelectionModel().getSelectedItem();
        if (!(selectedTab instanceof FieldsEditorTab currentTab)) {
            return false;
        }

        Collection<Field> shownFields = currentTab.getShownFields();
        // Try field-based check first (preferred for standard field editors)
        if (!shownFields.isEmpty() && node.getId() != null) {
            Optional<Field> lastField = shownFields.stream()
                                                   .reduce((first, second) -> second);

            boolean matchesLastFieldId = lastField.map(Field::getName)
                                                  .map(displayName -> displayName.equalsIgnoreCase(node.getId()))
                                                  .orElse(false);
            if (matchesLastFieldId) {
                return true;
            }
        }

        // Fallback: determine if the node is the last focusable control within the editor grid of the current tab
        if (currentTab.getContent() instanceof Parent parent) {
            Parent searchRoot = findEditorGridParent(parent).orElse(parent);
            Optional<Node> lastFocusable = findLastFocusableNode(searchRoot);
            return lastFocusable.map(n -> n == node).orElse(false);
        }

        return false;
    }

    /**
     * Moves to the next tab and focuses on its first field.
     */
    void moveToNextTabAndFocus() {
        tabbed.getSelectionModel().selectNext();

        Platform.runLater(() -> {
            Tab selectedTab = tabbed.getSelectionModel().getSelectedItem();
            if (selectedTab instanceof FieldsEditorTab currentTab) {
                focusFirstFieldInTab(currentTab);
            }
        });
    }

    private void focusFirstFieldInTab(FieldsEditorTab tab) {
        Node tabContent = tab.getContent();
        if (tabContent instanceof Parent parent) {
            // First try to find field by ID (preferred method)
            Collection<Field> shownFields = tab.getShownFields();
            if (!shownFields.isEmpty()) {
                Field firstField = shownFields.iterator().next();
                String firstFieldId = firstField.getName();
                Optional<TextInputControl> firstTextInput = findTextInputById(parent, firstFieldId);
                if (firstTextInput.isPresent()) {
                    firstTextInput.get().requestFocus();
                    return;
                }
            }

            Optional<TextInputControl> anyTextInput = findAnyTextInput(parent);
            if (anyTextInput.isPresent()) {
                anyTextInput.get().requestFocus();
                return;
            }

            // Final fallback: focus first focusable node within the editor grid (e.g., a button-only tab)
            Parent searchRoot = findEditorGridParent(parent).orElse(parent);
            findFirstFocusableNode(searchRoot).ifPresent(Node::requestFocus);
        }
    }

    /// Recursively searches for a TextInputControl (TextField or TextArea) with the given ID.
    private Optional<TextInputControl> findTextInputById(Parent parent, String id) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof TextInputControl textInput && id.equalsIgnoreCase(textInput.getId())) {
                return Optional.of(textInput);
            } else if (child instanceof Parent childParent) {
                Optional<TextInputControl> found = findTextInputById(childParent, id);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<TextInputControl> findAnyTextInput(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof TextInputControl textInput) {
                return Optional.of(textInput);
            } else if (child instanceof Parent childParent) {
                Optional<TextInputControl> found = findAnyTextInput(childParent);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /// Returns the first focusable, visible, managed, and enabled node in depth-first order
    private Optional<Node> findFirstFocusableNode(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (isNodeFocusable(child)) {
                return Optional.of(child);
            } else if (child instanceof Parent childParent) {
                Optional<Node> found = findFirstFocusableNode(childParent);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /// Returns the last focusable, visible, managed, and enabled node in depth-first order
    private Optional<Node> findLastFocusableNode(Parent parent) {
        Optional<Node> last = Optional.empty();
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent childParent) {
                Optional<Node> sub = findLastFocusableNode(childParent);
                if (sub.isPresent()) {
                    last = sub;
                }
            }
            if (isNodeFocusable(child)) {
                last = Optional.of(child);
            }
        }
        return last;
    }

    private boolean isNodeFocusable(Node node) {
        return node.isFocusTraversable() && node.isVisible() && !node.isDisabled() && node.isManaged();
    }

    /// Tries to locate the editor grid (with style class "editorPane") inside the tab content to avoid
    /// including preview or other sibling panels when determining focus order boundaries.
    private Optional<Parent> findEditorGridParent(Parent root) {
        if (root.getStyleClass().contains("editorPane")) {
            return Optional.of(root);
        }
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Parent p) {
                Optional<Parent> found = findEditorGridParent(p);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }
}
