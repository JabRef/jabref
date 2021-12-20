package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.undo.UndoManager;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 */
abstract class FieldsEditorTab extends EntryEditorTab {
    protected final BibDatabaseContext databaseContext;
    private final Map<Field, FieldEditorFX> editors = new LinkedHashMap<>();
    private final boolean isCompressed;
    private final SuggestionProviders suggestionProviders;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final ExternalFileTypes externalFileTypes;
    private final TaskExecutor taskExecutor;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final StateManager stateManager;
    private final IndexingTaskManager indexingTaskManager;
    private PreviewPanel previewPanel;
    private final UndoManager undoManager;
    private Collection<Field> fields = new ArrayList<>();
    private GridPane gridPane;

    public FieldsEditorTab(boolean compressed,
                           BibDatabaseContext databaseContext,
                           SuggestionProviders suggestionProviders,
                           UndoManager undoManager,
                           DialogService dialogService,
                           PreferencesService preferences,
                           StateManager stateManager,
                           ExternalFileTypes externalFileTypes,
                           TaskExecutor taskExecutor,
                           JournalAbbreviationRepository journalAbbreviationRepository, IndexingTaskManager indexingTaskManager) {
        this.isCompressed = compressed;
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.suggestionProviders = Objects.requireNonNull(suggestionProviders);
        this.undoManager = Objects.requireNonNull(undoManager);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.preferences = Objects.requireNonNull(preferences);
        this.externalFileTypes = Objects.requireNonNull(externalFileTypes);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.journalAbbreviationRepository = Objects.requireNonNull(journalAbbreviationRepository);
        this.stateManager = stateManager;
        this.indexingTaskManager = indexingTaskManager;
    }

    private static void addColumn(GridPane gridPane, int columnIndex, List<Label> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(new Node[0]));
    }

    private static void addColumn(GridPane gridPane, int columnIndex, Stream<Parent> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(Node[]::new));
    }

    private void setupPanel(BibEntry entry, boolean compressed) {
        // The preferences might be not initialized in tests -> return immediately
        // TODO: Replace this ugly workaround by proper injection propagation
        if (preferences == null) {
            return;
        }

        editors.clear();
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();

        fields = determineFieldsToShow(entry);

        List<Label> labels = new ArrayList<>();
        for (Field field : fields) {
            FieldEditorFX fieldEditor = FieldEditors.getForField(field, taskExecutor, dialogService,
                    journalAbbreviationRepository,
                    preferences, databaseContext, entry.getType(), suggestionProviders, undoManager);
            fieldEditor.bindToEntry(entry);

            editors.put(field, fieldEditor);
            labels.add(new FieldNameLabel(field));
        }

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

            columnExpand.setPercentWidth(40);
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

    /**
     * Focuses the given field.
     */
    public void requestFocus(Field fieldName) {
        if (editors.containsKey(fieldName)) {
            editors.get(fieldName).focus();
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return !determineFieldsToShow(entry).isEmpty();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        initPanel();
        setupPanel(entry, isCompressed);

        if (previewPanel != null) {
            previewPanel.setEntry(entry);
        }
    }

    @Override
    protected void nextPreviewStyle() {
        if (previewPanel != null) {
            previewPanel.nextPreviewStyle();
        }
    }

    @Override
    protected void previousPreviewStyle() {
        if (previewPanel != null) {
            previewPanel.previousPreviewStyle();
        }
    }

    protected abstract Set<Field> determineFieldsToShow(BibEntry entry);

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

            SplitPane container = new SplitPane(scrollPane);
            if (!preferences.getPreviewPreferences().showPreviewAsExtraTab()) {
                previewPanel = new PreviewPanel(databaseContext, dialogService, externalFileTypes, preferences.getKeyBindingRepository(), preferences, stateManager, indexingTaskManager);
                container.getItems().add(previewPanel);
            }

            setContent(container);
        }
    }
}
