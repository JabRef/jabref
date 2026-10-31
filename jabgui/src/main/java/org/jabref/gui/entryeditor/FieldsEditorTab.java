package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.EditorTextField;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A single tab displayed in the EntryEditor holding several FieldEditors.
abstract class FieldsEditorTab extends TabWithPreviewPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldsEditorTab.class);

    protected final Map<Field, FieldEditorFX> editors = new LinkedHashMap<>();
    protected GridPane gridPane;
    private ScrollPane editorScrollPane;
    private List<Label> regularRowLabels = List.of();
    private boolean regularRowLayoutActive;
    private boolean regularRowLayoutQueued;
    private boolean measuringRegularRows;
    private final boolean isCompressed;
    private final UndoAction undoAction;
    private final RedoAction redoAction;
    private final GuiPreferences preferences;
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    private Collection<Field> fields = new ArrayList<>();

    @SuppressWarnings("FieldCanBeLocal")
    private Subscription dividerPositionSubscription;

    public FieldsEditorTab(boolean compressed,
                           UndoAction undoAction,
                           RedoAction redoAction,
                           @NonNull GuiPreferences preferences,
                           @NonNull JournalAbbreviationRepository journalAbbreviationRepository,
                           StateManager stateManager,
                           PreviewPanel previewPanel) {
        super(stateManager, previewPanel);
        this.isCompressed = compressed;
        this.undoAction = undoAction;
        this.redoAction = redoAction;
        this.preferences = preferences;
        this.journalAbbreviationRepository = journalAbbreviationRepository;

        // Visible when the current entry (of its type) has at least one field to display in this tab.
        setContentDrivenVisibility(EasyBind.map(currentEntryProperty(),
                entry -> (entry != null) && !determineFieldsToShow(entry).isEmpty()));
    }

    private static void addColumn(GridPane gridPane, int columnIndex, List<Label> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(new Node[0]));
    }

    private static void addColumn(GridPane gridPane, int columnIndex, Stream<Parent> nodes) {
        gridPane.addColumn(columnIndex, nodes.toArray(Node[]::new));
    }

    protected void setupPanel(BibDatabaseContext bibDatabaseContext, BibEntry entry, boolean compressed) {
        // The preferences might be not initialized in tests -> return immediately
        // TODO: Replace this ugly workaround by proper injection propagation
        if (preferences == null) {
            return;
        }

        // The dropped editors need no explicit disposal. The direction that decides this is the one
        // from the entry: AbstractEditorViewModel binds via EasyBind's valueAt, which registers a
        // weak listener on the entry's field map, so nothing outside these two collections roots a
        // discarded editor. (A view model does hold its entry strongly, but that edge points the
        // other way and dies with the editor.)
        // Verified by AllFieldsTabTest#discardedEditorsAreNotRetainedAfterRebuild.
        editors.clear();
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();
        regularRowLayoutActive = false;

        fields = determineFieldsToShow(entry);

        List<Label> labels = fields
                .stream()
                .map(field -> createLabelAndEditor(bibDatabaseContext, entry, field))
                .toList();

        layoutEditors(bibDatabaseContext, entry, compressed, labels);
    }

    /// Arranges the created labels and editors inside [#gridPane]. The default layout stretches the
    /// editors to fill the tab height (one or two columns). Subclasses may override for other layouts.
    protected void layoutEditors(BibDatabaseContext bibDatabaseContext, BibEntry entry, boolean compressed, List<Label> labels) {
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

            regularRowLabels = List.copyOf(labels);
            setRegularRowLayout(gridPane, List.copyOf(fields));
            regularRowLayoutActive = true;
            editors.values().forEach(editor -> editor.getNode().layoutBoundsProperty()
                                                     .addListener((_, _, _) -> redistributeRegularRowHeights()));
            redistributeRegularRowHeights();
        }
    }

    protected Label createLabelAndEditor(BibDatabaseContext databaseContext, BibEntry entry, Field field) {
        SuggestionProviders suggestionProviders = stateManager.activeTabProperty().get()
                                                              .map(LibraryTab::getSuggestionProviders)
                                                              .orElse(new SuggestionProviders());
        FieldEditorFX fieldEditor = FieldEditors.getForField(
                field,
                journalAbbreviationRepository,
                preferences,
                databaseContext,
                entry.getType(),
                suggestionProviders,
                stateManager.getUndoManager(databaseContext),
                undoAction,
                redoAction);
        fieldEditor.bindToEntry(entry);
        // [impl->req~entry-editor.accept-single-line-value~1]
        fieldEditor.getNode().addEventHandler(ActionEvent.ACTION, event -> {
            if (event.getTarget() instanceof EditorTextField) {
                gridPane.requestFocus();
                event.consume();
            }
        });
        // Commit before moving focus: the combo box would otherwise commit on key release.
        fieldEditor.getNode().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER
                    && event.getTarget() instanceof ComboBox<?> comboBox
                    && comboBox.isEditable()
                    && !comboBox.isShowing()) {
                comboBox.commitValue();
                gridPane.requestFocus();
                event.consume();
            }
        });
        editors.put(field, fieldEditor);
        return new FieldNameLabel(field);
    }

    // [impl->req~entry-editor.custom-tabs.row-layout~1]
    static void setRegularRowLayout(GridPane gridPane, List<Field> fields) {
        gridPane.getChildren().forEach(FieldsEditorTab::useComputedPreferredHeight);
        List<RowConstraints> constraints = IntStream.range(0, fields.size())
                                                    .mapToObj(row -> {
                                                        RowConstraints rowExpand = new RowConstraints();
                                                        boolean fileRow = fields.get(row) == StandardField.FILE;
                                                        rowExpand.setVgrow(fileRow ? Priority.NEVER : Priority.ALWAYS);
                                                        rowExpand.setValignment(VPos.TOP);
                                                        rowExpand.setMaxHeight(fileRow ? Region.USE_PREF_SIZE : Double.MAX_VALUE);
                                                        return rowExpand;
                                                    }).toList();
        gridPane.getRowConstraints().addAll(constraints);
    }

    /// Keep the natural height of each editor before distributing only the available extra height
    /// according to editor weights. Percentage row heights can shrink below the content minimum
    /// when the pane is short, causing neighboring fields to overlap.
    private void redistributeRegularRowHeights() {
        if (measuringRegularRows || !regularRowLayoutActive || fields.isEmpty()
                || gridPane.getWidth() <= 0 || editorScrollPane == null) {
            return;
        }

        double viewportHeight = editorScrollPane.getViewportBounds().getHeight();
        if (viewportHeight <= 0) {
            return;
        }

        measuringRegularRows = true;
        try {
            redistributeRegularRowHeightsForCurrentSize(viewportHeight);
        } finally {
            measuringRegularRows = false;
        }
    }

    private void redistributeRegularRowHeightsForCurrentSize(double viewportHeight) {
        List<Field> rows = List.copyOf(fields);
        double[] naturalHeights = new double[rows.size()];
        double naturalTotal = gridPane.getInsets().getTop() + gridPane.getInsets().getBottom()
                + Math.max(0, rows.size() - 1) * gridPane.getVgap();
        double labelWidth = regularRowLabels.stream().mapToDouble(label -> label.prefWidth(-1)).max().orElseThrow();
        double availableEditorWidth = gridPane.getWidth() - gridPane.getInsets().getLeft() - gridPane.getInsets().getRight()
                - labelWidth - gridPane.getHgap();
        double totalWeight = 0;
        for (int row = 0; row < rows.size(); row++) {
            Field field = rows.get(row);
            Node node = editors.get(field).getNode();
            double width = node.getLayoutBounds().getWidth();
            if (width <= 0) {
                width = availableEditorWidth;
                if (node instanceof Region region && width > 0) {
                    // Tag editors need their width before they can wrap tags and report a useful height.
                    region.applyCss();
                    region.resize(width, region.prefHeight(width));
                    region.layout();
                }
            }
            if (width <= 0) {
                return;
            }
            double naturalHeight = node instanceof Region region
                                   ? Math.max(region.minHeight(width), region.prefHeight(width))
                                   : node.getLayoutBounds().getHeight();
            Label label = regularRowLabels.get(row);
            naturalHeight = Math.max(naturalHeight, label.prefHeight(label.getWidth()));
            naturalHeights[row] = naturalHeight;
            naturalTotal += naturalHeight;
            if (field != StandardField.FILE) {
                totalWeight += editors.get(field).getWeight();
            }
        }

        double extraHeight = Math.max(0, viewportHeight - naturalTotal);
        boolean rowHeightsChanged = false;
        for (int row = 0; row < rows.size(); row++) {
            Field field = rows.get(row);
            RowConstraints constraint = gridPane.getRowConstraints().get(row);
            double extra = field == StandardField.FILE || totalWeight == 0
                           ? 0 : extraHeight * editors.get(field).getWeight() / totalWeight;
            double targetHeight = naturalHeights[row] + extra;
            if (Math.abs(constraint.getPrefHeight() - targetHeight) > 0.5) {
                constraint.setMinHeight(naturalHeights[row]);
                constraint.setPrefHeight(targetHeight);
                constraint.setVgrow(Priority.NEVER);
                rowHeightsChanged = true;
            }
        }
        if (rowHeightsChanged && !regularRowLayoutQueued) {
            // A field can change its preferred height during the current layout pass. Defer the
            // next pass so GridPane uses the new row constraints without requiring a resize.
            regularRowLayoutQueued = true;
            Platform.runLater(() -> {
                regularRowLayoutQueued = false;
                if (regularRowLayoutActive) {
                    gridPane.requestLayout();
                    gridPane.layout();
                }
            });
        }
    }

    /// Percentage rows ignore child minimum heights. With natural rows, infinite preferred heights
    /// from field labels and text inputs must use their computed sizes so the grid can size each row.
    /// Labels keep their computed height so they align with controls at the top of tall rows.
    /// Text inputs with a zero preferred height can still grow to fill those rows.
    private static void useComputedPreferredHeight(Node node) {
        if (node instanceof Region region && region.getPrefHeight() == Double.POSITIVE_INFINITY) {
            region.setPrefHeight(Region.USE_COMPUTED_SIZE);
            region.setMaxHeight(node instanceof FieldNameLabel ? Region.USE_PREF_SIZE : Double.MAX_VALUE);
        }
        if (node instanceof TextInputControl textInput) {
            textInput.setMaxHeight(Double.MAX_VALUE);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(FieldsEditorTab::useComputedPreferredHeight);
        }
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

    /// `true` (default): content fills the tab height, rows share the space (classic category tabs).
    /// `false`: content keeps its natural height and the tab scrolls vertically (single-list tab).
    protected boolean stretchContentToTabHeight() {
        return true;
    }

    /// The node placed inside the tab's ScrollPane. By default the field grid itself;
    /// subclasses may wrap [#gridPane] in a larger structure (e.g. with collapsible
    /// sections) and return that instead. Called once from `initPanel()`.
    protected Node getEditorContent() {
        return gridPane;
    }

    public void requestFocus(Field fieldName) {
        if (editors.containsKey(fieldName)) {
            editors.get(fieldName).focus();
        }
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        initPanel();
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().orElse(new BibDatabaseContext());
        setupPanel(bibDatabaseContext, entry, isCompressed);
        super.bindToEntry(entry);
    }

    protected abstract SequencedSet<Field> determineFieldsToShow(BibEntry entry);

    public Collection<Field> getShownFields() {
        return fields;
    }

    private void initPanel() {
        if (gridPane == null) {
            gridPane = new GridPane() {
                @Override
                protected void layoutChildren() {
                    redistributeRegularRowHeights();
                    super.layoutChildren();
                }
            };
            gridPane.getStyleClass().addAll("editorPane", "gap-8", "padding-4");

            // Wrap everything in a scroll-pane
            ScrollPane scrollPane = new ScrollPane();
            editorScrollPane = scrollPane;
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setContent(getEditorContent());
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(stretchContentToTabHeight());
            scrollPane.viewportBoundsProperty().addListener((_, _, _) -> redistributeRegularRowHeights());
            gridPane.widthProperty().addListener((_, _, _) -> redistributeRegularRowHeights());
            SplitPane container = new SplitPane(scrollPane);
            setContent(container);

            // Adaption of visible tabs done in {@link org.jabref.gui.entryeditor.EntryEditor.EntryEditor}, where the subscription to
            // preferences.getPreviewPreferences().showPreviewAsExtraTabProperty() is established

            // save divider position
            dividerPositionSubscription = EasyBind.valueAt(container.getDividers(), 0)
                                                  .mapObservable(SplitPane.Divider::positionProperty)
                                                  .subscribeToValues(this::savePreviewWidthDividerPosition);
        }
    }

    private void addPreviewPanel() {
        assert this.getContent() instanceof SplitPane;

        // Really ensure it is not already there
        removePreviewPanelFromThisTab();

        if ((this.getContent() instanceof SplitPane splitPane) && !splitPane.getItems().contains(previewPanel) && this.getContent().isVisible()) {
            splitPane.getItems().add(1, previewPanel);
            splitPane.setDividerPositions(preferences.getEntryEditorPreferences().getPreviewWidthDividerPosition());
        }
    }

    public void removePreviewPanelFromThisTab() {
        assert this.getContent() instanceof SplitPane;
        if (this.getContent() instanceof SplitPane splitPane) {
            splitPane.getItems().remove(previewPanel);
            // Needed to "redraw" the split pane with one item (because JavaFX does not readjust if it is shown)
            // splitPane.setDividerPositions(1.0);
        }
    }

    @Override
    protected void handleFocus() {
        LOGGER.trace("This is {}", preferences.getPreviewPreferences().showPreviewAsExtraTabProperty().get());
        LOGGER.trace("This is then {}", !preferences.getPreviewPreferences().showPreviewAsExtraTabProperty().get());
        if (!preferences.getPreviewPreferences().showPreviewAsExtraTabProperty().get()) {
            LOGGER.trace("Focus on preview panel");

            // We need to move the preview panel from the non-visible tab to the visible tab
            removePreviewPanelFromOtherTabs();
            addPreviewPanel();
        }
    }

    private void savePreviewWidthDividerPosition(Number position) {
        if (position.doubleValue() == 1.0) {
            return;
        }
        if (!preferences.getPreviewPreferences().shouldShowPreviewAsExtraTab()) {
            preferences.getEntryEditorPreferences().setPreviewWidthDividerPosition(position.doubleValue());
        }
    }
}

