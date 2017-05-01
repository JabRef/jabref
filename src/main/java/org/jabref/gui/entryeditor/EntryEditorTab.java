package org.jabref.gui.entryeditor;

import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.autocompleter.AutoCompleteListener;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 */
class EntryEditorTab {

    private final JFXPanel panel = new JFXPanel();
    private final List<String> fields;
    private final EntryEditor parent;
    private final Map<String, FieldEditorFX> editors = new LinkedHashMap<>();
    private final FocusListener fieldListener = new EntryEditorTabFocusListener(this);
    private final String tabTitle;
    private final JabRefFrame frame;
    private final BasePanel basePanel;
    private FieldEditorFX activeField;
    private BibEntry entry;
    private boolean updating;


    public EntryEditorTab(JabRefFrame frame, BasePanel basePanel, List<String> fields, EntryEditor parent,
                          boolean addKeyField, boolean compressed, String tabTitle) {
        if (fields == null) {
            this.fields = new ArrayList<>();
        } else {
            this.fields = new ArrayList<>(fields);
        }
        // Add the edit field for Bibtex-key.
        if (addKeyField) {
            this.fields.add(BibEntry.KEY_FIELD);
        }

        this.parent = parent;
        this.tabTitle = tabTitle;
        this.frame = frame;
        this.basePanel = basePanel;

        // Execute on JavaFX Application Thread
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            Region root = setupPanel(frame, basePanel, addKeyField, compressed, tabTitle);

            if (GUIGlobals.currentFont != null) {
                root.setStyle(
                        "text-area-background: " + convertToHex(GUIGlobals.validFieldBackgroundColor) + ";"
                                + "text-area-foreground: " + convertToHex(GUIGlobals.editorTextColor) + ";"
                                + "text-area-highlight: " + convertToHex(GUIGlobals.activeBackgroundColor) + ";"
                );
            }

            root.getStylesheets().add("org/jabref/gui/entryeditor/EntryEditor.css");

            panel.setScene(new Scene(root));
        });

        // The following line makes sure focus cycles inside tab instead of being lost to other parts of the frame:
        panel.setFocusCycleRoot(true);
    }

    private static void addColumn(GridPane gridPane, int columnIndex, List<Label> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(new Node[nodes.size()]));
    }

    private static void addColumn(GridPane gridPane, int columnIndex, Stream<Parent> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(Node[]::new));
    }

    private String convertToHex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private Region setupPanel(JabRefFrame frame, BasePanel bPanel, boolean addKeyField,
                              boolean compressed, String title) {

        setupKeyBindings(panel.getInputMap(JComponent.WHEN_FOCUSED), panel.getActionMap());

        panel.setName(title);

        editors.clear();
        List<Label> labels = new ArrayList<>();

        for (String fieldName : fields) {

            // TODO: Reenable/migrate this
            // Store the editor for later reference:
            /*
            FieldEditor fieldEditor;
            int defaultHeight;
            int wHeight = (int) (50.0 * InternalBibtexFields.getFieldWeight(field));
            if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                fieldEditor = new EntryLinkListEditor(frame, bPanel.getBibDatabaseContext(), field, null, parent,
                        true);
                defaultHeight = 0;
            } else if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                fieldEditor = new EntryLinkListEditor(frame, bPanel.getBibDatabaseContext(), field, null, parent,
                        false);
                defaultHeight = 0;
            } else {
                fieldEditor = new TextArea(field, null, getPrompt(field));
                //parent.addSearchListener((TextArea) fieldEditor);
                defaultHeight = fieldEditor.getPane().getPreferredSize().height;
            }

            Optional<JComponent> extra = parent.getExtra(fieldEditor);

            // Add autocompleter listener, if required for this field:
            /*
            AutoCompleter<String> autoCompleter = bPanel.getAutoCompleters().get(field);
            AutoCompleteListener autoCompleteListener = null;
            if (autoCompleter != null) {
                autoCompleteListener = new AutoCompleteListener(autoCompleter);
            }
            setupJTextComponent(fieldEditor.getTextComponent(), autoCompleteListener);
            fieldEditor.setAutoCompleteListener(autoCompleteListener);
            */

            FieldEditorFX fieldEditor = FieldEditors.getForField(fieldName, Globals.taskExecutor, new FXDialogService(), Globals.journalAbbreviationLoader, Globals.prefs.getJournalAbbreviationPreferences(), Globals.prefs, bPanel.getBibDatabaseContext());
            editors.put(fieldName, fieldEditor);
            /*
            // TODO: Reenable this
            if (i == 0) {
                activeField = fieldEditor;
            }
            */

            /*
            // TODO: Reenable this
            if (!compressed) {
                fieldEditor.getPane().setPreferredSize(new Dimension(100, Math.max(defaultHeight, wHeight)));
            }
            */

            /*
            // TODO: Reenable content selector
            if (!panel.getBibDatabaseContext().getMetaData().getContentSelectorValuesForField(editor.getFieldName()).isEmpty()) {
                FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, editor, storeFieldAction, false,
                        ", ");
                contentSelectors.add(ws);
                controls.add(ws, BorderLayout.NORTH);
            }
            //} else if (!panel.getBibDatabaseContext().getMetaData().getContentSelectorValuesForField(fieldName).isEmpty()) {
            //return FieldExtraComponents.getSelectorExtraComponent(frame, panel, editor, contentSelectors, storeFieldAction);
             */

            labels.add(new FieldNameLabel(fieldName));
        }

        GridPane gridPane = new GridPane();
        gridPane.setPrefSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        gridPane.setMaxSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        gridPane.getStyleClass().add("editorPane");

        ColumnConstraints columnExpand = new ColumnConstraints();
        columnExpand.setHgrow(Priority.ALWAYS);
        int rows;
        if (compressed) {
            rows = (int) Math.ceil((double) fields.size() / 2);

            addColumn(gridPane, 0, labels.subList(0, rows));
            addColumn(gridPane, 3, labels.subList(rows, labels.size()));
            addColumn(gridPane, 1, editors.values().stream().map(FieldEditorFX::getNode).limit(rows));
            addColumn(gridPane, 4, editors.values().stream().map(FieldEditorFX::getNode).skip(rows));

            gridPane.getColumnConstraints().addAll(new ColumnConstraints(), columnExpand, new ColumnConstraints(10), new ColumnConstraints(), columnExpand);
        } else {
            rows = fields.size();

            addColumn(gridPane, 0, labels);
            addColumn(gridPane, 1, editors.values().stream().map(FieldEditorFX::getNode));

            gridPane.getColumnConstraints().addAll(new ColumnConstraints(), columnExpand);
        }


        RowConstraints rowExpand = new RowConstraints();
        rowExpand.setVgrow(Priority.ALWAYS);
        rowExpand.setPercentHeight(100 / rows);
        for (int i = 0; i < rows; i++) {
            gridPane.getRowConstraints().add(rowExpand);
        }

        return gridPane;
    }

    private String getPrompt(String field) {

        Set<FieldProperty> fieldProperties = InternalBibtexFields.getFieldProperties(field);
        if (fieldProperties.contains(FieldProperty.PERSON_NAMES)) {
            return String.format("%1$s and %1$s and others", Localization.lang("Firstname Lastname"));
        } else if (fieldProperties.contains(FieldProperty.DOI)) {
            return "10.ORGANISATION/ID";
        } else if (fieldProperties.contains(FieldProperty.DATE)) {
            return "YYYY-MM-DD";
        }

        switch (field) {
            case FieldName.YEAR:
                return "YYYY";
            case FieldName.MONTH:
                return "MM or #mmm#";
            case FieldName.URL:
                return "https://";
        }

        return "";
    }

    private BibEntry getEntry() {
        return entry;
    }

    public void setEntry(BibEntry entry) {
        try {
            updating = true;
            for (FieldEditorFX editor : editors.values()) {
                DefaultTaskExecutor.runInJavaFXThread(() -> editor.bindToEntry(entry));
            }
            this.entry = entry;
        } finally {
            updating = false;
        }
    }

    private boolean isFieldModified(FieldEditor fieldEditor) {
        String text = fieldEditor.getText().trim();

        if (text.isEmpty()) {
            return getEntry().hasField(fieldEditor.getFieldName());
        } else {
            return !Optional.of(text).equals(getEntry().getField(fieldEditor.getFieldName()));
        }
    }

    public void markIfModified(FieldEditor fieldEditor) {
        // Only mark as changed if not already is and the field was indeed modified
        if (!updating && !basePanel.isModified() && isFieldModified(fieldEditor)) {
            markBaseChanged();
        }
    }

    private void markBaseChanged() {
        basePanel.markBaseChanged();
    }

    /**
     * Only sets the activeField variable but does not focus it.
     * <p>
     * If you want to focus it call {@link #focus()} afterwards.
     */
    // TODO: Reenable or delete this
    //public void setActive(FieldEditor fieldEditor) {
    //    activeField = fieldEditor;
    //}

    //public FieldEditor getActive() {
    //    return activeField;
    //}

    public void setActive(String fieldName) {
        if (editors.containsKey(fieldName)) {
            activeField = editors.get(fieldName);
        }
    }

    public List<String> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void focus() {
        if (activeField != null) {
            activeField.requestFocus();
        }
    }

    /**
     * Reset all fields from the data in the BibEntry.
     */
    public void updateAll() {
        setEntry(getEntry());
    }

    public boolean updateField(String field, String content) {
        if (!editors.containsKey(field)) {
            return false;
        }
        // TODO: Reenable or probably better delete this
        /*
        FieldEditor fieldEditor = editors.get(field);
        if (fieldEditor.getText().equals(content)) {
            return true;
        }

        // trying to preserve current edit position (fixes SF bug #1285)
        if (fieldEditor.getTextComponent() instanceof JTextComponent) {
            int initialCaretPosition = ((JTextComponent) fieldEditor).getCaretPosition();
            fieldEditor.setText(content);
            int textLength = fieldEditor.getText().length();
            if (initialCaretPosition < textLength) {
                ((JTextComponent) fieldEditor).setCaretPosition(initialCaretPosition);
            } else {
                ((JTextComponent) fieldEditor).setCaretPosition(textLength);
            }
        } else {
            fieldEditor.setText(content);
        }
        */
        return true;
    }

    public void setEnabled(boolean enabled) {
        /*
        // TODO: Reenable this
        for (FieldEditor editor : editors.values()) {
            editor.setEnabled(enabled);
        }
        */
    }

    public Component getPane() {
        return panel;
    }

    public EntryEditor getParent() {
        return parent;
    }

    public String getTabTitle() {
        return tabTitle;
    }

    private void setupKeyBindings(final InputMap inputMap, final ActionMap actionMap) {
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_ENTRY), "prev");
        actionMap.put("prev", parent.getPrevEntryAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_ENTRY), "next");
        actionMap.put("next", parent.getNextEntryAction());

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_STORE_FIELD), "store");
        actionMap.put("store", parent.getStoreFieldAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_PANEL), "right");
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_PANEL_2), "right");
        actionMap.put("left", parent.getSwitchLeftAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_PANEL), "left");
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_PANEL_2), "left");
        actionMap.put("right", parent.getSwitchRightAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.HELP), "help");
        actionMap.put("help", parent.getHelpAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.SAVE_DATABASE), "save");
        actionMap.put("save", parent.getSaveDatabaseAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.NEXT_TAB), "nexttab");
        actionMap.put("nexttab", this.frame.nextTab);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.PREVIOUS_TAB), "prevtab");
        actionMap.put("prevtab", this.frame.prevTab);
    }

    /**
     * Set up key bindings and focus listener for the FieldEditor.
     *
     * @param component
     */
    private void setupJTextComponent(final JComponent component, final AutoCompleteListener autoCompleteListener) {

        // Here we add focus listeners to the component. The funny code is because we need
        // to guarantee that the AutoCompleteListener - if used - is called before fieldListener
        // on a focus lost event. The AutoCompleteListener is responsible for removing any
        // current suggestion when focus is lost, and this must be done before fieldListener
        // stores the current edit. Swing doesn't guarantee the order of execution of event
        // listeners, so we handle this by only adding the AutoCompleteListener and telling
        // it to call fieldListener afterwards. If no AutoCompleteListener is used, we
        // add the fieldListener normally.
        if (autoCompleteListener == null) {
            component.addFocusListener(fieldListener);
        } else {
            component.addKeyListener(autoCompleteListener);
            component.addFocusListener(autoCompleteListener);
            autoCompleteListener.setNextFocusListener(fieldListener);
        }

        setupKeyBindings(component.getInputMap(JComponent.WHEN_FOCUSED), component.getActionMap());

        Set<AWTKeyStroke> keys = new HashSet<>(
                component.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.clear();
        keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
        keys = new HashSet<>(component.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.clear();
        keys.add(KeyStroke.getKeyStroke("shift pressed TAB"));
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
    }
}
