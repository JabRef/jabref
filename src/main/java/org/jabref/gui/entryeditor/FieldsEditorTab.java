package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.undo.UndoManager;

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
import org.jabref.gui.FXDialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 */
abstract class FieldsEditorTab extends EntryEditorTab {

    private final Map<String, FieldEditorFX> editors = new LinkedHashMap<>();
    private final boolean isCompressed;
    private final SuggestionProviders suggestionProviders;

    private FieldEditorFX activeField;
    private final BibDatabaseContext databaseContext;
    private UndoManager undoManager;
    private Collection<String> fields;

    public FieldsEditorTab(boolean compressed, BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager) {
        this.isCompressed = compressed;
        this.databaseContext = databaseContext;
        this.suggestionProviders = suggestionProviders;
        this.undoManager = undoManager;
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

    private Region setupPanel(BibEntry entry, boolean compressed, SuggestionProviders suggestionProviders, UndoManager undoManager) {
        editors.clear();

        EntryType entryType = EntryTypes.getTypeOrDefault(entry.getType(), databaseContext.getMode());
        fields = determineFieldsToShow(entry, entryType);

        List<Label> labels = new ArrayList<>();
        for (String fieldName : fields) {
            FieldEditorFX fieldEditor = FieldEditors.getForField(fieldName, Globals.TASK_EXECUTOR, new FXDialogService(),
                    Globals.journalAbbreviationLoader, Globals.prefs.getJournalAbbreviationPreferences(), Globals.prefs,
                    databaseContext, entry.getType(),
                    suggestionProviders, undoManager);
            fieldEditor.bindToEntry(entry);

            editors.put(fieldName, fieldEditor);
            /*
            // TODO: Reenable this
            if (i == 0) {
                activeField = fieldEditor;
            }
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

            setCompressedRowLayout(gridPane, rows);
        } else {
            rows = fields.size();

            addColumn(gridPane, 0, labels);
            addColumn(gridPane, 1, editors.values().stream().map(FieldEditorFX::getNode));

            gridPane.getColumnConstraints().addAll(columnDoNotContract, columnExpand);

            setRegularRowLayout(gridPane, rows);
        }

        if (GUIGlobals.currentFont != null) {
            gridPane.setStyle(
                    "text-area-background: " + convertToHex(GUIGlobals.validFieldBackgroundColor) + ";"
                            + "text-area-foreground: " + convertToHex(GUIGlobals.editorTextColor) + ";"
                            + "text-area-highlight: " + convertToHex(GUIGlobals.activeBackgroundColor) + ";");
        }

        // Warp everything in a scroll-pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }

    private void setRegularRowLayout(GridPane gridPane, int rows) {
        List<RowConstraints> constraints = new ArrayList<>(rows);
        for (String field : fields) {
            RowConstraints rowExpand = new RowConstraints();
            rowExpand.setVgrow(Priority.ALWAYS);
            rowExpand.setValignment(VPos.TOP);
            if (rows == 0) {
                rowExpand.setPercentHeight(100);
            } else {
                rowExpand.setPercentHeight(100 / rows * editors.get(field).getWeight());
            }
            constraints.add(rowExpand);
        }
        gridPane.getRowConstraints().addAll(constraints);
    }

    private void setCompressedRowLayout(GridPane gridPane, int rows) {
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
     * Focuses the given field.
     */
    public void requestFocus(String fieldName) {
        if (editors.containsKey(fieldName)) {
            activeField = editors.get(fieldName);
            activeField.requestFocus();
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        EntryType entryType = EntryTypes.getTypeOrDefault(entry.getType(), databaseContext.getMode());
        return !determineFieldsToShow(entry, entryType).isEmpty();
    }

    @Override
    public void handleFocus() {
        if (activeField != null) {
            activeField.requestFocus();
        }
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        Region panel = setupPanel(entry, isCompressed, suggestionProviders, undoManager);
        setContent(panel);
    }

    protected abstract Collection<String> determineFieldsToShow(BibEntry entry, EntryType entryType);

    public Collection<String> getShownFields() {
        return fields;
    }
}
