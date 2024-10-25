package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.stream.Stream;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
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
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.query.SearchQuery;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 */
abstract class FieldsEditorTab extends EntryEditorTab implements OffersPreview {
    protected final BibDatabaseContext databaseContext;
    protected final Map<Field, FieldEditorFX> editors = new LinkedHashMap<>();
    protected GridPane gridPane;
    private final boolean isCompressed;
    private final SuggestionProviders suggestionProviders;
    private final UndoAction undoAction;
    private final RedoAction redoAction;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final ThemeManager themeManager;
    private final TaskExecutor taskExecutor;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private PreviewPanel previewPanel;
    private final UndoManager undoManager;
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty;
    private Collection<Field> fields = new ArrayList<>();
    @SuppressWarnings("FieldCanBeLocal")
    private Subscription dividerPositionSubscription;

    public FieldsEditorTab(boolean compressed,
                           BibDatabaseContext databaseContext,
                           SuggestionProviders suggestionProviders,
                           UndoManager undoManager,
                           UndoAction undoAction,
                           RedoAction redoAction,
                           DialogService dialogService,
                           GuiPreferences preferences,
                           ThemeManager themeManager,
                           TaskExecutor taskExecutor,
                           JournalAbbreviationRepository journalAbbreviationRepository,
                           OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        this.isCompressed = compressed;
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.suggestionProviders = Objects.requireNonNull(suggestionProviders);
        this.undoManager = Objects.requireNonNull(undoManager);
        this.undoAction = undoAction;
        this.redoAction = redoAction;
        this.dialogService = Objects.requireNonNull(dialogService);
        this.preferences = Objects.requireNonNull(preferences);
        this.themeManager = themeManager;
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.journalAbbreviationRepository = Objects.requireNonNull(journalAbbreviationRepository);
        this.searchQueryProperty = searchQueryProperty;
    }

    private static void addColumn(GridPane gridPane, int columnIndex, List<Label> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(new Node[0]));
    }

    private static void addColumn(GridPane gridPane, int columnIndex, Stream<Parent> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(Node[]::new));
    }

    protected void setupPanel(BibEntry entry, boolean compressed) {
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

        List<Label> labels = fields
                .stream()
                .map(field -> createLabelAndEditor(entry, field))
                .toList();

        ColumnConstraints columnExpand = new ColumnConstraints();
        columnExpand.setHgrow(Priority.ALWAYS);

        ColumnConstraints columnDoNotContract = new ColumnConstraints();
        columnDoNotContract.setMinWidth(Region.USE_PREF_SIZE);
        if (compressed) {
            int rows = (int) Math.ceil((double) fields.size() / 2);

            addColumn(gridPane, 0, labels.subList(0, rows));
            addColumn(gridPane, 1, editors.values().stream().map(FieldEditorFX::getNode).limit(rows));
            addColumn(gridPane, 3, labels.subList(rows, labels.size()));
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

    protected Label createLabelAndEditor(BibEntry entry, Field field) {
        FieldEditorFX fieldEditor = FieldEditors.getForField(
                field,
                taskExecutor,
                dialogService,
                journalAbbreviationRepository,
                preferences,
                databaseContext,
                entry.getType(),
                suggestionProviders,
                undoManager,
                undoAction,
                redoAction);
        fieldEditor.bindToEntry(entry);
        editors.put(field, fieldEditor);
        return new FieldNameLabel(field);
    }

    private void setRegularRowLayout(GridPane gridPane) {
        double totalWeight = fields.stream()
                                   .mapToDouble(field -> editors.get(field).getWeight())
                                   .sum();
        List<RowConstraints> constraints = fields
                .stream()
                .map(field -> {
                    RowConstraints rowExpand = new RowConstraints();
                    rowExpand.setVgrow(Priority.ALWAYS);
                    rowExpand.setValignment(VPos.TOP);
                    rowExpand.setPercentHeight(100 * editors.get(field).getWeight() / totalWeight);
                    return rowExpand;
                }).toList();
        gridPane.getRowConstraints().addAll(constraints);
    }

    protected static void setCompressedRowLayout(GridPane gridPane, int rows) {
        RowConstraints rowExpand = new RowConstraints();
        rowExpand.setVgrow(Priority.ALWAYS);
        rowExpand.setValignment(VPos.TOP);
        if (rows == 0) {
            rowExpand.setPercentHeight(100);
        } else {
            rowExpand.setPercentHeight(100 / (double) rows);
        }

        ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
        rowConstraints.clear();
        for (int i = 0; i < rows; i++) {
            rowConstraints.add(rowExpand);
        }
    }

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
    public void nextPreviewStyle() {
        if (previewPanel != null) {
            previewPanel.nextPreviewStyle();
        }
    }

    @Override
    public void previousPreviewStyle() {
        if (previewPanel != null) {
            previewPanel.previousPreviewStyle();
        }
    }

    protected abstract SequencedSet<Field> determineFieldsToShow(BibEntry entry);

    public Collection<Field> getShownFields() {
        return fields;
    }

    private void initPanel() {
        if (gridPane == null) {
            gridPane = new GridPane();
            gridPane.getStyleClass().add("editorPane");

            // Wrap everything in a scroll-pane
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setContent(gridPane);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            SplitPane container = new SplitPane(scrollPane);
            previewPanel = new PreviewPanel(
                    databaseContext,
                    dialogService,
                    preferences.getKeyBindingRepository(),
                    preferences,
                    themeManager,
                    taskExecutor,
                    searchQueryProperty);
            EasyBind.subscribe(preferences.getPreviewPreferences().showPreviewAsExtraTabProperty(), show -> {
                if (show) {
                    container.getItems().remove(previewPanel);
                } else {
                    container.getItems().add(1, previewPanel);
                    container.setDividerPositions(preferences.getEntryEditorPreferences().getPreviewWidthDividerPosition());
                }
            });

            // save position
            dividerPositionSubscription = EasyBind.valueAt(container.getDividers(), 0)
                                                  .mapObservable(SplitPane.Divider::positionProperty)
                                                  .subscribeToValues(this::savePreviewWidthDividerPosition);
            setContent(container);
        }
    }

    private void savePreviewWidthDividerPosition(Number position) {
        if (!preferences.getPreviewPreferences().shouldShowPreviewAsExtraTab()) {
            preferences.getEntryEditorPreferences().setPreviewWidthDividerPosition(position.doubleValue());
        }
    }
}

