package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 */
class FieldsEditorTab extends EntryEditorTab {

    private final Region panel;
    private final List<String> fields;
    private final EntryEditor parent;
    private final Map<String, FieldEditorFX> editors = new LinkedHashMap<>();
    private final JabRefFrame frame;
    private final BasePanel basePanel;
    private FieldEditorFX activeField;
    private final BibEntry entry;

    public FieldsEditorTab(JabRefFrame frame, BasePanel basePanel, List<String> fields, EntryEditor parent, boolean addKeyField, boolean compressed, BibEntry entry) {
        this.entry = Objects.requireNonNull(entry);
        this.fields = new ArrayList<>(Objects.requireNonNull(fields));

        // Add the edit field for Bibtex-key.
        if (addKeyField) {
            this.fields.add(BibEntry.KEY_FIELD);
        }

        this.parent = parent;
        this.frame = frame;
        this.basePanel = basePanel;

        panel = setupPanel(frame, basePanel, compressed);

        // The following line makes sure focus cycles inside tab instead of being lost to other parts of the frame:
        //panel.setFocusCycleRoot(true);
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

    private Region setupPanel(JabRefFrame frame, BasePanel bPanel, boolean compressed) {

        //setupKeyBindings(panel.getInputMap(JComponent.WHEN_FOCUSED), panel.getActionMap());

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

            FieldEditorFX fieldEditor = FieldEditors.getForField(fieldName, Globals.taskExecutor, new FXDialogService(),
                    Globals.journalAbbreviationLoader, Globals.prefs.getJournalAbbreviationPreferences(), Globals.prefs,
                    bPanel.getBibDatabaseContext(), entry.getType());
            fieldEditor.bindToEntry(entry);

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
        gridPane.getStyleClass().add("editorPane");

        ColumnConstraints columnExpand = new ColumnConstraints();
        columnExpand.setHgrow(Priority.ALWAYS);

        ColumnConstraints columnDoNotContract = new ColumnConstraints();
        columnDoNotContract.setMinWidth(Region.USE_PREF_SIZE);
        int rows;
        if (compressed) {
            rows = (int) Math.ceil((double) fields.size() / 2);

            addColumn(gridPane, 0, labels.subList(0, rows));
            addColumn(gridPane, 3, labels.subList(rows, labels.size()));
            addColumn(gridPane, 1, editors.values().stream().map(FieldEditorFX::getNode).limit(rows));
            addColumn(gridPane, 4, editors.values().stream().map(FieldEditorFX::getNode).skip(rows));

            gridPane.getColumnConstraints().addAll(columnDoNotContract, columnExpand, new ColumnConstraints(10),
                    columnDoNotContract, columnExpand);
        } else {
            rows = fields.size();

            addColumn(gridPane, 0, labels);
            addColumn(gridPane, 1, editors.values().stream().map(FieldEditorFX::getNode));

            gridPane.getColumnConstraints().addAll(columnDoNotContract, columnExpand);
        }

        RowConstraints rowExpand = new RowConstraints();
        rowExpand.setVgrow(Priority.ALWAYS);
        rowExpand.setValignment(VPos.TOP);
        if (rows == 0) {
            rowExpand.setPercentHeight(100);
        } else {
            rowExpand.setPercentHeight(100 / rows);
        }
        for (int i = 0; i < rows; i++) {
            gridPane.getRowConstraints().add(rowExpand);
        }

        if (GUIGlobals.currentFont != null) {
            gridPane.setStyle(
                    "text-area-background: " + convertToHex(GUIGlobals.validFieldBackgroundColor) + ";"
                            + "text-area-foreground: " + convertToHex(GUIGlobals.editorTextColor) + ";"
                            + "text-area-highlight: " + convertToHex(GUIGlobals.activeBackgroundColor) + ";");
        }
        gridPane.getStylesheets().add("org/jabref/gui/entryeditor/EntryEditor.css");

        // Warp everything in a scroll-pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
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

    /**
     * Only sets the activeField variable but does not focus it.
     * <p>
     * If you want to focus it call {@link #focus()} afterwards.
     */
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

    public EntryEditor getParent() {
        return parent;
    }

    @Override
    public boolean shouldShow() {
        return !fields.isEmpty();
    }

    @Override
    public void requestFocus() {
        if (activeField != null) {
            activeField.requestFocus();
        }
    }

    @Override
    protected void initialize() {
        setContent(panel);
    }
}
