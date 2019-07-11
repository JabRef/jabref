package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
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
import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 */
abstract class FieldsEditorTab extends EntryEditorTab {

    private final Map<Field, FieldEditorFX> editors = new LinkedHashMap<>();
    private final boolean isCompressed;
    private final SuggestionProviders suggestionProviders;

    private FieldEditorFX activeField;
    private final BibDatabaseContext databaseContext;
    private UndoManager undoManager;
    private Collection<Field> fields = new ArrayList<>();
    private final DialogService dialogService;
    private GridPane gridPane;

    public FieldsEditorTab(boolean compressed, BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        this.isCompressed = compressed;
        this.databaseContext = databaseContext;
        this.suggestionProviders = suggestionProviders;
        this.undoManager = undoManager;
        this.dialogService = dialogService;
    }

    private static void addColumn(GridPane gridPane, int columnIndex, List<Label> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(new Node[nodes.size()]));
    }

    private static void addColumn(GridPane gridPane, int columnIndex, Stream<Parent> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(Node[]::new));
    }

    private void setupPanel(BibEntry entry, boolean compressed, SuggestionProviders suggestionProviders, UndoManager undoManager) {
        // The preferences might be not initialized in tests -> return immediately
        // TODO: Replace this ugly workaround by proper injection propagation
        if (Globals.prefs == null) {
            return;
        }

        editors.clear();
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();

        fields = determineFieldsToShow(entry, BibEntryTypesManager.getTypeOrDefault(entry.getType(), databaseContext.getMode()));

        List<Label> labels = new ArrayList<>();
        boolean isFirstField = true;
        for (Field field : fields) {
            FieldEditorFX fieldEditor = FieldEditors.getForField(field, Globals.TASK_EXECUTOR, dialogService,
                    Globals.journalAbbreviationLoader.getRepository(Globals.prefs.getJournalAbbreviationPreferences()),
                    Globals.prefs, databaseContext, entry.getType(), suggestionProviders, undoManager);
            fieldEditor.bindToEntry(entry);

            editors.put(field, fieldEditor);
            if (isFirstField) {
                activeField = fieldEditor;
                isFirstField = false;
            }

            labels.add(new FieldNameLabel(field));
        }

        ColumnConstraints columnExpand = new ColumnConstraints();
        columnExpand.setPercentWidth(60);

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
            addColumn(gridPane, 0, labels);
            addColumn(gridPane, 1, editors.values().stream().map(FieldEditorFX::getNode));

            gridPane.getColumnConstraints().addAll(columnDoNotContract, columnExpand);

            setRegularRowLayout(gridPane);
        }
    }

    private void setRegularRowLayout(GridPane gridPane) {
        double totalWeight = fields.stream()
                                   .mapToDouble(field -> editors.get(field).getWeight())
                                   .sum();

        List<RowConstraints> constraints = new ArrayList<>();
        for (Field field : fields) {
            RowConstraints rowExpand = new RowConstraints();
            rowExpand.setVgrow(Priority.ALWAYS);
            rowExpand.setValignment(VPos.TOP);
            rowExpand.setPercentHeight(100 * editors.get(field).getWeight() / totalWeight);
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
            rowExpand.setPercentHeight(100 / (double) rows);
        }
        for (int i = 0; i < rows; i++) {
            gridPane.getRowConstraints().add(rowExpand);
        }
    }

    private String getPrompt(Field field) {
        Set<FieldProperty> fieldProperties = field.getProperties();
        if (fieldProperties.contains(FieldProperty.PERSON_NAMES)) {
            return String.format("%1$s and %1$s and others", Localization.lang("Firstname Lastname"));
        } else if (fieldProperties.contains(FieldProperty.DOI)) {
            return "10.ORGANISATION/ID";
        } else if (fieldProperties.contains(FieldProperty.DATE)) {
            return "YYYY-MM-DD";
        }

        if (StandardField.YEAR.equals(field)) {
            return "YYYY";
        } else if (StandardField.MONTH.equals(field)) {
            return "MM or #mmm#";
        } else if (StandardField.URL.equals(field)) {
            return "https://";
        }

        return "";
    }

    /**
     * Focuses the given field.
     */
    public void requestFocus(Field fieldName) {
        if (editors.containsKey(fieldName)) {
            activeField = editors.get(fieldName);
            activeField.focus();
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return !determineFieldsToShow(entry, BibEntryTypesManager.getTypeOrDefault(entry.getType(), databaseContext.getMode())).isEmpty();
    }

    @Override
    public void handleFocus() {
        if (activeField != null) {
            activeField.focus();
        }
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        Optional<Field> selectedFieldName = editors.entrySet()
                                                   .stream()
                                                   .filter(editor -> editor.getValue().childIsFocused())
                                                   .map(Map.Entry::getKey)
                                                   .findFirst();

        initPanel();
        setupPanel(entry, isCompressed, suggestionProviders, undoManager);

        Platform.runLater(() -> {
            // Restore focus to field (run this async so that editor is already initialized correctly)
            selectedFieldName.ifPresent(this::requestFocus);
        });
    }

    protected abstract Collection<Field> determineFieldsToShow(BibEntry entry, BibEntryType entryType);

    public Collection<Field> getShownFields() {
        return fields;
    }

    private void initPanel() {
        if (gridPane == null) {
            gridPane = new GridPane();
            gridPane.getStyleClass().add("editorPane");

            // Warp everything in a scroll-pane
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setContent(gridPane);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            setContent(scrollPane);
        }
    }
}
