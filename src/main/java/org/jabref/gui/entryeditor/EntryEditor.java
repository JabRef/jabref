package org.jabref.gui.entryeditor;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.StateManager;
import org.jabref.gui.bibtexkeypattern.GenerateBibtexKeySingleAction;
import org.jabref.gui.entryeditor.fileannotationtab.FileAnnotationTab;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.ColorUtil;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI component that allows editing of the fields of a BibEntry (i.e. the
 * one that shows up, when you double click on an entry in the table)
 * <p>
 * It hosts the tabs (required, general, optional) and the buttons to the left.
 * <p>
 * EntryEditor also registers itself to the event bus, receiving
 * events whenever a field of the entry changes, enabling the text fields to
 * update themselves if the change is made from somewhere else.
 */
public class EntryEditor extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryEditor.class);

    private final BibDatabaseContext databaseContext;
    private final CountingUndoManager undoManager;
    private final BasePanel panel;
    private Subscription typeSubscription;
    private final List<EntryEditorTab> tabs;
    private final FileUpdateMonitor fileMonitor;
    /**
     * A reference to the entry this editor works on.
     */
    private BibEntry entry;
    private SourceTab sourceTab;

    @FXML private TabPane tabbed;
    @FXML private Button typeChangeButton;
    @FXML private Button fetcherButton;
    @FXML private Label typeLabel;

    private final EntryEditorPreferences preferences;
    private final DialogService dialogService;
    private final ExternalFilesEntryLinker fileLinker;
    private final TaskExecutor taskExecutor;
    private final StateManager stateManager;

    public EntryEditor(BasePanel panel, EntryEditorPreferences preferences, FileUpdateMonitor fileMonitor, DialogService dialogService, ExternalFileTypes externalFileTypes, TaskExecutor taskExecutor, StateManager stateManager) {
        this.panel = panel;
        this.databaseContext = panel.getBibDatabaseContext();
        this.undoManager = panel.getUndoManager();
        this.preferences = Objects.requireNonNull(preferences);
        this.fileMonitor = fileMonitor;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.stateManager = stateManager;

        fileLinker = new ExternalFilesEntryLinker(externalFileTypes, Globals.prefs.getFilePreferences(), databaseContext);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        if (GUIGlobals.currentFont != null) {
            setStyle(
                     "text-area-background: " + ColorUtil.toHex(GUIGlobals.validFieldBackgroundColor) + ";"
                     + "text-area-foreground: " + ColorUtil.toHex(GUIGlobals.editorTextColor) + ";"
                     + "text-area-highlight: " + ColorUtil.toHex(GUIGlobals.activeBackgroundColor) + ";");
        }

        EasyBind.subscribe(tabbed.getSelectionModel().selectedItemProperty(), tab -> {
            EntryEditorTab activeTab = (EntryEditorTab) tab;
            if (activeTab != null) {
                activeTab.notifyAboutFocus(entry);
            }
        });

        setupKeyBindings();

        tabs = createTabs();

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

                FileDragDropPreferenceType dragDropPreferencesType = Globals.prefs.getEntryEditorFileLinkPreference();

                if (dragDropPreferencesType == FileDragDropPreferenceType.MOVE) {
                    if (event.getTransferMode() == TransferMode.LINK) //alt on win
                    {
                        LOGGER.debug("Mode LINK");
                        fileLinker.addFilesToEntry(entry, files);
                    } else if (event.getTransferMode() == TransferMode.COPY) //ctrl on win, no modifier on Xubuntu
                    {
                        LOGGER.debug("Mode COPY");
                        fileLinker.copyFilesToFileDirAndAddToEntry(entry, files);
                    } else {
                        LOGGER.debug("Mode MOVE"); //shift on win or no modifier
                        fileLinker.moveFilesToFileDirAndAddToEntry(entry, files);
                    }
                }

                if (dragDropPreferencesType == FileDragDropPreferenceType.COPY) {
                    if (event.getTransferMode() == TransferMode.COPY) //ctrl on win, no modifier on Xubuntu
                    {
                        LOGGER.debug("Mode MOVE");
                        fileLinker.moveFilesToFileDirAndAddToEntry(entry, files);
                    } else if (event.getTransferMode() == TransferMode.LINK) //alt on win
                    {
                        LOGGER.debug("Mode LINK");
                        fileLinker.addFilesToEntry(entry, files);
                    } else {
                        LOGGER.debug("Mode COPY"); //shift on win or no modifier
                        fileLinker.copyFilesToFileDirAndAddToEntry(entry, files);
                    }
                }

                if (dragDropPreferencesType == FileDragDropPreferenceType.LINK) {
                    if (event.getTransferMode() == TransferMode.COPY) //ctrl on win, no modifier on Xubuntu
                    {
                        LOGGER.debug("Mode COPY");
                        fileLinker.copyFilesToFileDirAndAddToEntry(entry, files);
                    } else if (event.getTransferMode() == TransferMode.LINK) //alt on win
                    {
                        LOGGER.debug("Mode MOVE");
                        fileLinker.moveFilesToFileDirAndAddToEntry(entry, files);
                    } else {
                        LOGGER.debug("Mode LINK"); //shift on win or no modifier
                        fileLinker.addFilesToEntry(entry, files);
                    }
                }
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
            Optional<KeyBinding> keyBinding = preferences.getKeyBindings().mapToKeyBinding(event);
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
                        panel.selectNextEntry();
                        event.consume();
                        break;
                    case ENTRY_EDITOR_PREVIOUS_ENTRY:
                        panel.selectPreviousEntry();
                        event.consume();
                        break;
                    case HELP:
                        HelpAction.openHelpPage(HelpFile.ENTRY_EDITOR);
                        event.consume();
                        break;
                    case CLOSE:
                        close();
                        event.consume();
                        break;
                    case CLOSE_ENTRY:
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
        panel.entryEditorClosing(EntryEditor.this);
    }

    @FXML
    private void deleteEntry() {
        panel.delete(entry);
    }

    @FXML
    void generateCiteKeyButton() {
        GenerateBibtexKeySingleAction action = new GenerateBibtexKeySingleAction(getEntry(), databaseContext, dialogService, preferences, undoManager);
        action.execute();
    }

    @FXML
    private void navigateToPreviousEntry() {
        panel.selectPreviousEntry();
    }

    @FXML
    private void navigateToNextEntry() {
        panel.selectNextEntry();
    }

    private List<EntryEditorTab> createTabs() {
        List<EntryEditorTab> tabs = new LinkedList<>();

        // Required fields
        tabs.add(new RequiredFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager, dialogService));

        // Optional fields
        tabs.add(new OptionalFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager, dialogService));
        tabs.add(new OptionalFields2Tab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager, dialogService));
        tabs.add(new DeprecatedFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager, dialogService));

        // Other fields
        tabs.add(new OtherFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager, preferences.getCustomTabFieldNames(), dialogService));

        // General fields from preferences
        for (Map.Entry<String, List<String>> tab : preferences.getEntryEditorTabList().entrySet()) {
            tabs.add(new UserDefinedFieldsTab(tab.getKey(), tab.getValue(), panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager, dialogService));
        }

        // Special tabs
        tabs.add(new MathSciNetTab());
        tabs.add(new FileAnnotationTab(panel.getAnnotationCache()));
        tabs.add(new RelatedArticlesTab(this, preferences, dialogService));

        // Source tab
        sourceTab = new SourceTab(databaseContext, undoManager, preferences.getLatexFieldFormatterPreferences(), preferences.getImportFormatPreferences(), fileMonitor, dialogService, stateManager);
        tabs.add(sourceTab);
        return tabs;
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
        if (preferences.showSourceTabByDefault()) {
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
        ContextMenu typeMenu = new ChangeEntryTypeMenu().getChangeEntryTypePopupMenu(entry, databaseContext, undoManager);
        typeLabel.setOnMouseClicked(event -> typeMenu.show(typeLabel, Side.RIGHT, 0, 0));
        typeChangeButton.setOnMouseClicked(event -> typeMenu.show(typeChangeButton, Side.RIGHT, 0, 0));
        // Add menu for fetching bibliographic information
        ContextMenu fetcherMenu = new ContextMenu();
        for (EntryBasedFetcher fetcher : WebFetchers.getEntryBasedFetchers(preferences.getImportFormatPreferences())) {
            MenuItem fetcherMenuItem = new MenuItem(fetcher.getName());
            fetcherMenuItem.setOnAction(event -> fetchAndMerge(fetcher));
            fetcherMenu.getItems().add(fetcherMenuItem);
        }
        fetcherButton.setOnMouseClicked(event -> fetcherMenu.show(fetcherButton, Side.RIGHT, 0, 0));
    }

    private void fetchAndMerge(EntryBasedFetcher fetcher) {
        new FetchAndMergeEntry(panel, taskExecutor).fetchAndMerge(entry, fetcher);
    }

    public void setFocusToField(String fieldName) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            for (Tab tab : tabbed.getTabs()) {
                if ((tab instanceof FieldsEditorTab) && ((FieldsEditorTab) tab).getShownFields().contains(fieldName)) {
                    FieldsEditorTab fieldsEditorTab = (FieldsEditorTab) tab;
                    tabbed.getSelectionModel().select(tab);
                    fieldsEditorTab.requestFocus(fieldName);
                }
            }
        });
    }
}
