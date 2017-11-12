package org.jabref.gui.entryeditor;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEdit;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.EntryContainer;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.entryeditor.fileannotationtab.FileAnnotationTab;
import org.jabref.gui.externalfiles.WriteXMPEntryEditorAction;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.gui.fieldeditors.TextField;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.mergeentries.EntryFetchAndMergeWorker;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQueryHighlightListener;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.event.FieldAddedOrRemovedEvent;
import org.jabref.preferences.JabRefPreferences;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fxmisc.easybind.EasyBind;


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
public class EntryEditor extends BorderPane implements EntryContainer {

    private static final Log LOGGER = LogFactory.getLog(EntryEditor.class);

    /**
     * A reference to the entry this object works on.
     */
    private BibEntry entry;
    /**
     * The currently displayed type
     */
    private String displayedBibEntryType;

    /**
     * The action that deletes the current entry, and closes the editor.
     */
    private final DeleteAction deleteAction = new DeleteAction();

    /**
     * The action for switching to the next entry.
     */
    private final AbstractAction nextEntryAction = new NextEntryAction();
    /**
     * The action for switching to the previous entry.
     */
    private final AbstractAction prevEntryAction = new PrevEntryAction();

    private final AbstractAction writeXmp;
    @FXML private TabPane tabbed;
    private final JabRefFrame frame;
    private final BasePanel panel;
    private final HelpAction helpAction = new HelpAction(HelpFile.ENTRY_EDITOR, IconTheme.JabRefIcon.HELP.getIcon());
    private final UndoAction undoAction = new UndoAction();
    private final RedoAction redoAction = new RedoAction();
    private final List<SearchQueryHighlightListener> searchListeners = new ArrayList<>();
    private final List<EntryEditorTab> tabs;

    /**
     * Indicates that we are about to go to the next or previous entry
     */
    private EntryType entryType;
    private SourceTab sourceTab;
    @FXML private Label typeLabel;

    public EntryEditor(BasePanel panel) {
        this.frame = panel.frame();
        this.panel = panel;

        ControlHelper.loadFXMLForControl(this);

        writeXmp = new WriteXMPEntryEditorAction(panel, this);

        tabbed.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabbed.setStyle(
                "-fx-font-size: " + Globals.prefs.getFontSizeFX() + "pt;" +
                        "-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");

        DefaultTaskExecutor.runInJavaFXThread(() -> {
                    EasyBind.subscribe(tabbed.getSelectionModel().selectedItemProperty(), tab -> {
                        EntryEditorTab activeTab = (EntryEditorTab) tab;
                        if (activeTab != null) {
                            activeTab.notifyAboutFocus(entry);
                        }
                    });
                });

        setupKeyBindings();

        tabs = createTabs();
    }

    public void setEntry(BibEntry entry) {
        this.entry = Objects.requireNonNull(entry);
        entryType = EntryTypes.getTypeOrDefault(entry.getType(),
                this.frame.getCurrentBasePanel().getBibDatabaseContext().getMode());

        displayedBibEntryType = entry.getType();

        DefaultTaskExecutor.runInJavaFXThread(() -> {
            recalculateVisibleTabs();
            if (Globals.prefs.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE)) {
                tabbed.getSelectionModel().select(sourceTab);
            }

            // Notify current tab about new entry
            EntryEditorTab selectedTab = (EntryEditorTab) tabbed.getSelectionModel().getSelectedItem();
            selectedTab.notifyAboutFocus(entry);

            // Update type label
            TypedBibEntry typedEntry = new TypedBibEntry(entry, panel.getBibDatabaseContext().getMode());
            typeLabel.setText(typedEntry.getTypeForDisplay());
        });
    }

    @Subscribe
    public synchronized void listen(FieldAddedOrRemovedEvent event) {
        // Rebuild entry editor based on new information (e.g. hide/add tabs)
        recalculateVisibleTabs();
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
                        helpAction.actionPerformed(null);
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

    public void close() {
        panel.entryEditorClosing(EntryEditor.this);
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

    private List<EntryEditorTab> createTabs() {
        List<EntryEditorTab> tabs = new LinkedList<>();

        // Required fields
        tabs.add(new RequiredFieldsTab(panel.getDatabaseContext(), panel.getSuggestionProviders(), panel.getUndoManager()));

        // Optional fields
        tabs.add(new OptionalFieldsTab(panel.getDatabaseContext(), panel.getSuggestionProviders(), panel.getUndoManager()));
        tabs.add(new OptionalFields2Tab(panel.getDatabaseContext(), panel.getSuggestionProviders(), panel.getUndoManager()));
        tabs.add(new DeprecatedFieldsTab(panel.getDatabaseContext(), panel.getSuggestionProviders(), panel.getUndoManager()));

        // Other fields
        tabs.add(new OtherFieldsTab(panel.getDatabaseContext(), panel.getSuggestionProviders(), panel.getUndoManager()));

        // General fields from preferences
        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        for (int i = 0; i < tabList.getTabCount(); i++) {
            tabs.add(new UserDefinedFieldsTab(tabList.getTabName(i), tabList.getTabFields(i), panel.getDatabaseContext(), panel.getSuggestionProviders(), panel.getUndoManager()));
        }

        // Special tabs
        tabs.add(new MathSciNetTab());
        tabs.add(new FileAnnotationTab(panel.getAnnotationCache()));
        tabs.add(new RelatedArticlesTab(Globals.prefs));

        // Source tab
        sourceTab = new SourceTab(panel.getBibDatabaseContext(), panel.getUndoManager(), Globals.prefs.getLatexFieldFormatterPreferences());
        tabs.add(sourceTab);
        return tabs;
    }

    public String getDisplayedBibEntryType() {
        return displayedBibEntryType;
    }

    /**
     * @return reference to the currently edited entry
     */
    @Override
    public BibEntry getEntry() {
        return entry;
    }

    public BibDatabase getDatabase() {
        return panel.getDatabase();
    }

    private ToolBar setupToolBar() {
        JPopupMenu fetcherPopup = new JPopupMenu();

        for (EntryBasedFetcher fetcher : WebFetchers
                .getEntryBasedFetchers(Globals.prefs.getImportFormatPreferences())) {
            fetcherPopup.add(new JMenuItem(new AbstractAction(fetcher.getName()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    new EntryFetchAndMergeWorker(panel, getEntry(), fetcher).execute();
                }
            }));
        }

//        fetcherButton.addMouseListener(new MouseAdapter() {

        //  @Override
        //    public void mousePressed(MouseEvent e) {
        //              fetcherPopup.show(e.getComponent(), e.getX(), e.getY());
        //        }
        //  });

        ToolBar toolBar = new ToolBar(

        );

        return toolBar;
    }

    void addSearchListener(SearchQueryHighlightListener listener) {
        // TODO: Highlight search text in entry editors
        searchListeners.add(listener);
        panel.frame().getGlobalSearchBar().getSearchQueryHighlightObservable().addSearchListener(listener);
    }

    private void removeSearchListeners() {
        for (SearchQueryHighlightListener listener : searchListeners) {
            panel.frame().getGlobalSearchBar().getSearchQueryHighlightObservable().removeSearchListener(listener);
        }
    }

    @Override
    public void requestFocus() {
        //container.requestFocus();
    }

    /**
     * Returns the name of the currently selected tab.
     */
    public String getVisibleTabName() {
        Tab selectedTab = tabbed.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            return selectedTab.getText();
        }
        return "";
    }

    public void setVisibleTab(String name) {
        for (Tab tab : tabbed.getTabs()) {
            if (tab.getText().equals(name)) {
                tabbed.getSelectionModel().select(tab);
            }
        }
    }

    public void setFocusToField(String fieldName) {
        for (Tab tab : tabbed.getTabs()) {
            if ((tab instanceof FieldsEditorTab) && ((FieldsEditorTab) tab).determineFieldsToShow(entry, entryType).contains(fieldName)) {
                FieldsEditorTab fieldsEditorTab = (FieldsEditorTab) tab;
                tabbed.getSelectionModel().select(tab);
                fieldsEditorTab.requestFocus(fieldName);
            }
        }
    }

    public void setMovingToDifferentEntry() {
        unregisterListeners();
    }

    private void unregisterListeners() {
        removeSearchListeners();
    }

    private void showChangeEntryTypePopupMenu() {
        //JPopupMenu typeMenu = new ChangeEntryTypeMenu().getChangeentryTypePopupMenu(panel);
        //typeMenu.show(this, 0, 0);
    }

    private void warnDuplicateBibtexkey() {
        panel.output(Localization.lang("Duplicate BibTeX key") + ". "
                + Localization.lang("Grouping may not work for this entry."));
    }

    private void warnEmptyBibtexkey() {
        panel.output(Localization.lang("Empty BibTeX key") + ". "
                + Localization.lang("Grouping may not work for this entry."));
    }

    private class TypeButton extends JButton {

        private TypeButton() {
            addActionListener(e -> showChangeEntryTypePopupMenu());
        }
    }

    private class TypeLabel extends Label {

        private TypeLabel(String type) {
            super(type);
            //setUI(new VerticalLabelUI(false));
            //setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
            //setFont(new Font("dialog", Font.ITALIC + Font.BOLD, 18));

            // Add a mouse listener so the user can right-click the type label to change the entry type:
            /*
            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() || (e.getButton() == MouseEvent.BUTTON3)) {
                        handleTypeChange();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isPopupTrigger() || (e.getButton() == MouseEvent.BUTTON3)) {
                        handleTypeChange();
                    }
                }

                private void handleTypeChange() {
                    showChangeEntryTypePopupMenu();
                }
            });
            */
        }
    }

    private class DeleteAction extends AbstractAction {

        private DeleteAction() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Show confirmation dialog if not disabled:
            boolean goOn = panel.showDeleteConfirmationDialog(1);

            if (!goOn) {
                return;
            }

            panel.entryEditorClosing(EntryEditor.this);
            panel.getDatabase().removeEntry(entry);
            panel.markBaseChanged();
            panel.getUndoManager().addEdit(new UndoableRemoveEntry(panel.getDatabase(), entry, panel));
            panel.output(Localization.lang("Deleted entry"));
        }
    }


    public class StoreFieldAction extends AbstractAction {

        public StoreFieldAction() {
            super("Store field value");
            putValue(Action.SHORT_DESCRIPTION, "Store field value");
        }

        @Override
        public void actionPerformed(ActionEvent event) {

            if (event.getSource() instanceof TextField) {
                // Storage from bibtex key field.
                TextField textField = (TextField) event.getSource();
                String oldValue = entry.getCiteKeyOptional().orElse(null);
                String newValue = textField.getText();

                if (newValue.isEmpty()) {
                    newValue = null;
                }

                if (((oldValue == null) && (newValue == null)) || (Objects.equals(oldValue, newValue))) {
                    return; // No change.
                }

                // Make sure the key is legal:
                String cleaned = BibtexKeyPatternUtil.checkLegalKey(newValue,
                        Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
                if ((cleaned == null) || cleaned.equals(newValue)) {
                    textField.setValidBackgroundColor();
                } else {
                    textField.setInvalidBackgroundColor();
                    if (!SwingUtilities.isEventDispatchThread()) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("Invalid BibTeX key"),
                                Localization.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                        requestFocus();
                    }
                    return;
                }

                if (newValue == null) {
                    entry.clearCiteKey();
                    warnEmptyBibtexkey();
                } else {
                    entry.setCiteKey(newValue);
                    boolean isDuplicate = panel.getDatabase().getDuplicationChecker().isDuplicateCiteKeyExisting(entry);
                    if (isDuplicate) {
                        warnDuplicateBibtexkey();
                    } else {
                        panel.output(Localization.lang("BibTeX key is unique."));
                    }
                }

                // Add an UndoableKeyChange to the baseframe's undoManager.
                UndoableKeyChange undoableKeyChange = new UndoableKeyChange(entry, oldValue, newValue);
                updateTimestamp(undoableKeyChange);

                textField.setValidBackgroundColor();

                if (textField.hasFocus()) {
                    textField.setActiveBackgroundColor();
                }

                panel.markBaseChanged();
            } else if (event.getSource() instanceof FieldEditor) {
                String toSet = null;
                FieldEditor fieldEditor = (FieldEditor) event.getSource();
                boolean set;
                // Trim the whitespace off this value
                String currentText = fieldEditor.getText().trim();
                if (!currentText.isEmpty()) {
                    toSet = currentText;
                }

                // We check if the field has changed, since we don't want to
                // mark the base as changed unless we have a real change.
                if (toSet == null) {
                    set = entry.hasField(fieldEditor.getFieldName());
                } else {
                    set = !((entry.hasField(fieldEditor.getFieldName()))
                            && toSet.equals(entry.getField(fieldEditor.getFieldName()).orElse(null)));
                }

                if (!set) {
                    // We set the field and label color.
                    fieldEditor.setValidBackgroundColor();
                } else {
                    try {
                        // The following statement attempts to write the new contents into a StringWriter, and this will
                        // cause an IOException if the field is not properly formatted. If that happens, the field
                        // is not stored and the textarea turns red.
                        if (toSet != null) {
                            new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()).format(toSet,
                                    fieldEditor.getFieldName());
                        }

                        String oldValue = entry.getField(fieldEditor.getFieldName()).orElse(null);

                        if (toSet == null) {
                            entry.clearField(fieldEditor.getFieldName());
                        } else {
                            entry.setField(fieldEditor.getFieldName(), toSet);
                        }

                        fieldEditor.setValidBackgroundColor();

                        //TODO: See if we need to update an AutoCompleter instance:
                        /*
                        AutoCompleter<String> aComp = panel.getSuggestionProviders().get(fieldEditor.getName());
                        if (aComp != null) {
                            aComp.addBibtexEntry(entry);
                        }
                        */

                        // Add an UndoableFieldChange to the baseframe's undoManager.
                        UndoableFieldChange undoableFieldChange = new UndoableFieldChange(entry,
                                fieldEditor.getFieldName(), oldValue, toSet);
                        updateTimestamp(undoableFieldChange);

                        panel.markBaseChanged();
                    } catch (InvalidFieldValueException ex) {
                        fieldEditor.setInvalidBackgroundColor();
                        if (!SwingUtilities.isEventDispatchThread()) {
                            JOptionPane.showMessageDialog(frame, Localization.lang("Error") + ": " + ex.getMessage(),
                                    Localization.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                            LOGGER.debug("Error setting field", ex);
                            requestFocus();
                        }
                    }
                }
                if (fieldEditor.hasFocus()) {
                    fieldEditor.setBackground(GUIGlobals.ACTIVE_EDITOR_COLOR);
                }
            }

            // Make sure we scroll to the entry if it moved in the table.
            // Should only be done if this editor is currently showing:
            // don't select the current entry again (eg use BasePanel#highlightEntry} in case another entry was selected)
            if (isVisible()) {
                SwingUtilities.invokeLater(() -> panel.getMainTable().ensureVisible(entry));
            }
        }

        private void updateTimestamp(UndoableEdit undoableEdit) {
            if (Globals.prefs.getTimestampPreferences().includeTimestamps()) {
                NamedCompound compound = new NamedCompound(undoableEdit.getPresentationName());
                compound.addEdit(undoableEdit);
                UpdateField.updateField(entry, Globals.prefs.getTimestampPreferences().getTimestampField(), Globals.prefs.getTimestampPreferences().now()).ifPresent(fieldChange -> compound.addEdit(new UndoableFieldChange(fieldChange)));
                compound.end();
                panel.getUndoManager().addEdit(compound);
            } else {
                panel.getUndoManager().addEdit(undoableEdit);
            }
        }
    }

    private class NextEntryAction extends AbstractAction {

        private NextEntryAction() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.selectNextEntry();
        }
    }

    private class PrevEntryAction extends AbstractAction {

        private PrevEntryAction() {

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.selectPreviousEntry();
        }
    }

    private class UndoAction extends AbstractAction {

        private UndoAction() {
            super("Undo", IconTheme.JabRefIcon.UNDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, "Undo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultTaskExecutor.runInJavaFXThread(() -> panel.runCommand(Actions.UNDO));
        }
    }

    private class RedoAction extends AbstractAction {

        private RedoAction() {
            super("Redo", IconTheme.JabRefIcon.REDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, "Redo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.runCommand(Actions.REDO);
        }
    }
}
