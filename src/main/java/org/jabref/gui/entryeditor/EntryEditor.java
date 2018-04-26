package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.entryeditor.fileannotationtab.FileAnnotationTab;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.EntryFetchAndMergeWorker;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.search.SearchQueryHighlightListener;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

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

    private final BibDatabaseContext bibDatabaseContext;
    private final CountingUndoManager undoManager;
    private final BasePanel panel;
    private final List<SearchQueryHighlightListener> searchListeners = new ArrayList<>();
    private Subscription typeSubscription;
    private final List<EntryEditorTab> tabs;
    /**
     * A reference to the entry this editor works on.
     */
    private BibEntry entry;
    private SourceTab sourceTab;

    @FXML private TabPane tabbed;
    @FXML private Button typeChangeButton;
    @FXML private Button fetcherButton;
    @FXML private Label typeLabel;

    public EntryEditor(BasePanel panel) {
        this.panel = panel;
        this.bibDatabaseContext = panel.getBibDatabaseContext();
        this.undoManager = panel.getUndoManager();

        ControlHelper.loadFXMLForControl(this);

        getStylesheets().add(EntryEditor.class.getResource("EntryEditor.css").toExternalForm());
        setStyle("-fx-font-size: " + Globals.prefs.getFontSizeFX() + "pt;");

        EasyBind.subscribe(tabbed.getSelectionModel().selectedItemProperty(), tab -> {
            EntryEditorTab activeTab = (EntryEditorTab) tab;
            if (activeTab != null) {
                activeTab.notifyAboutFocus(entry);
            }
        });

        setupKeyBindings();

        tabs = createTabs();
    }

    /**
     * Set-up key bindings specific for the entry editor.
     */
    private void setupKeyBindings() {
        tabbed.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = Globals.getKeyPrefs().mapToKeyBinding(event);
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
                    case HELP:
                        HelpAction.openHelpPage(HelpFile.ENTRY_EDITOR);
                        event.consume();
                        break;
                    case CLOSE_ENTRY_EDITOR:
                        close();
                        event.consume();
                        break;
                    default:
                        // Pass other keys to children
                }
            }
        });
    }

    @FXML
    public void close() {
        panel.entryEditorClosing(EntryEditor.this);
    }

    @FXML
    public void generateKey() {
        new BibtexKeyGenerator(bibDatabaseContext, Globals.prefs.getBibtexKeyPatternPreferences())
                .generateAndSetKey(entry)
                .ifPresent(change -> undoManager.addEdit(new UndoableKeyChange(change)));
    }

    @FXML
    private void deleteEntry() {
        panel.delete(entry);
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
        tabs.add(new RequiredFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager));

        // Optional fields
        tabs.add(new OptionalFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager));
        tabs.add(new OptionalFields2Tab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager));
        tabs.add(new DeprecatedFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager));

        // Other fields
        tabs.add(new OtherFieldsTab(panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager));

        // General fields from preferences
        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        for (int i = 0; i < tabList.getTabCount(); i++) {
            tabs.add(new UserDefinedFieldsTab(tabList.getTabName(i), tabList.getTabFields(i), panel.getBibDatabaseContext(), panel.getSuggestionProviders(), undoManager));
        }

        // Special tabs
        tabs.add(new MathSciNetTab());
        tabs.add(new FileAnnotationTab(panel.getAnnotationCache()));
        tabs.add(new RelatedArticlesTab(Globals.prefs));

        // Source tab
        sourceTab = new SourceTab(bibDatabaseContext, undoManager, Globals.prefs.getLatexFieldFormatterPreferences(), Globals.prefs, Globals.getFileUpdateMonitor());
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

        // remove subscription for old entry if existing
        if (typeSubscription != null) {
            typeSubscription.unsubscribe();
        }
        this.entry = entry;

        DefaultTaskExecutor.runInJavaFXThread(() -> {
            recalculateVisibleTabs();
            if (Globals.prefs.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE)) {
                tabbed.getSelectionModel().select(sourceTab);
            }

            // Notify current tab about new entry
            EntryEditorTab selectedTab = (EntryEditorTab) tabbed.getSelectionModel().getSelectedItem();
            selectedTab.notifyAboutFocus(entry);

            setupToolBar();
        });

        // subscribe to type changes for rebuilding the currently visible tab
        typeSubscription = EasyBind.subscribe(this.entry.typeProperty(), type -> {
            DefaultTaskExecutor.runInJavaFXThread(() -> {
                typeLabel.setText(new TypedBibEntry(entry, bibDatabaseContext.getMode()).getTypeForDisplay());
                recalculateVisibleTabs();
                EntryEditorTab selectedTab = (EntryEditorTab) tabbed.getSelectionModel().getSelectedItem();
                selectedTab.notifyAboutFocus(entry);
            });
        });
    }

    private void setupToolBar() {
        // Update type label
        TypedBibEntry typedEntry = new TypedBibEntry(entry, bibDatabaseContext.getMode());
        typeLabel.setText(typedEntry.getTypeForDisplay());

        // Add type change menu
        ContextMenu typeMenu = new ChangeEntryTypeMenu().getChangeEntryTypePopupMenu(entry, bibDatabaseContext, undoManager);
        typeLabel.setOnMouseClicked(event -> typeMenu.show(typeLabel, Side.RIGHT, 0, 0));
        typeChangeButton.setOnMouseClicked(event -> typeMenu.show(typeChangeButton, Side.RIGHT, 0, 0));
        // Add menu for fetching bibliographic information
        ContextMenu fetcherMenu = new ContextMenu();
        for (EntryBasedFetcher fetcher : WebFetchers
                .getEntryBasedFetchers(Globals.prefs.getImportFormatPreferences())) {
            MenuItem fetcherMenuItem = new MenuItem(fetcher.getName());
            fetcherMenuItem.setOnAction(event -> new EntryFetchAndMergeWorker(panel, getEntry(), fetcher).execute());
            fetcherMenu.getItems().add(fetcherMenuItem);
        }
        fetcherButton.setOnMouseClicked(event -> fetcherMenu.show(fetcherButton, Side.RIGHT, 0, 0));
    }

    void addSearchListener(SearchQueryHighlightListener listener) {
        // TODO: Highlight search text in entry editors
        searchListeners.add(listener);
        panel.frame().getGlobalSearchBar().getSearchQueryHighlightObservable().addSearchListener(listener);
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
