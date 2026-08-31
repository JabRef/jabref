package org.jabref.gui.preferences.entryeditor;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.theme.StyleClasses;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.importer.fetcher.citation.CitationCountFetcherType;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.textfield.TextFields;

import static org.jabref.gui.preferences.forms.FormMetrics.GAP;

public class EntryEditorTab extends AbstractPreferenceTabView<EntryEditorTabViewModel> {

    private final TableView<EditorTabViewModel> tabsTable = new TableView<>();
    private final TableView<String> fieldsTable = new TableView<>();
    private final TextField addTabName = new TextField();
    private final TextField addFieldName = new TextField();
    private final Label fieldsPlaceholder = new Label();

    private final CustomLocalDragboard localDragboard;

    public EntryEditorTab() {
        this.viewModel = new EntryEditorTabViewModel(
                dialogService,
                preferences.getEntryEditorPreferences(),
                preferences.getMrDlibPreferences(),
                preferences.getAbbreviationPreferences(),
                taskExecutor);
        this.localDragboard = Injector.instantiateModelOrService(StateManager.class).getLocalDragboard();
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry editor");
    }

    private void buildView() {
        setContent(form()

                .checkbox(Localization.lang("Open editor when a new entry is created"), viewModel.openOnNewEntryProperty())
                .checkbox(Localization.lang("Automatically search and show unlinked files in the entry editor"), viewModel.autoLinkFilesEnabledProperty())
                .checkbox(Localization.lang("Show validation messages"), viewModel.enableValidationProperty())
                .checkbox(Localization.lang("Allow integers in 'edition' field in BibTeX mode"), viewModel.allowIntegerEditionProperty())
                .checkbox(Localization.lang("Fetch journal information online to show"), viewModel.journalPopupProperty())
                .checkbox(Localization.lang("Enable MSC keyword descriptions"), viewModel.enableMscKeywordDescriptionsProperty())
                .checkbox(Localization.lang("Show BibTeX source by default"), viewModel.defaultSourceProperty())
                .checkbox(Localization.lang("Accept recommendations from Mr. DLib"), viewModel.acceptRecommendationsProperty())

                .combo(Localization.lang("Citation count fetcher:"),
                        viewModel.citationCountFetcherTypes(),
                        viewModel.citationCountFetcherTypeProperty(), CitationCountFetcherType::getName)

                .section(Localization.lang("Editor tabs"), tabs -> tabs
                        .custom(buildTabConfigRegion()))

                .build());
    }

    // [impl->req~entry-editor.custom-tabs~1]
    private Node buildTabConfigRegion() {
        HBox columns = new HBox(GAP, buildTabsColumn(), buildFieldsColumn());
        VBox.setVgrow(columns, Priority.ALWAYS);
        return columns;
    }

    // region Tabs column

    private Node buildTabsColumn() {
        setupTabsTable();

        addTabName.setPromptText(Localization.lang("Tab name..."));
        addTabName.setOnAction(_ -> addTab());

        Button addTabButton = new Button();
        addTabButton.setPrefSize(20.0, 20.0);
        addTabButton.getStyleClass().addAll(StyleClasses.NARROW_ICON_BUTTON);
        addTabButton.setGraphic(IconTheme.JabRefIcons.ADD_NOBOX.getGraphicNode());
        addTabButton.setTooltip(new Tooltip(Localization.lang("Add new tab")));
        addTabButton.setOnAction(_ -> addTab());
        addTabButton.disableProperty().bind(addTabName.textProperty().isEmpty());

        VBox column = new VBox(GAP, tabsTable, new HBox(GAP, addTabName, addTabButton));
        column.setPrefWidth(280.0);
        return column;
    }

    private void setupTabsTable() {
        tabsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabsTable.setPrefHeight(300.0);
        VBox.setVgrow(tabsTable, Priority.ALWAYS);

        TableColumn<EditorTabViewModel, EditorTabViewModel> visibleColumn = new TableColumn<>();
        visibleColumn.setMinWidth(40.0);
        visibleColumn.setMaxWidth(40.0);
        visibleColumn.setResizable(false);
        visibleColumn.setSortable(false);
        visibleColumn.setReorderable(false);
        visibleColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        // Only built-in tabs have a visibility flag; custom tabs are shown while they exist and are
        // removed via the delete action instead.
        visibleColumn.setCellFactory(_ -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private BooleanProperty boundVisible;

            @Override
            protected void updateItem(EditorTabViewModel tab, boolean empty) {
                super.updateItem(tab, empty);
                if (boundVisible != null) {
                    checkBox.selectedProperty().unbindBidirectional(boundVisible);
                    boundVisible = null;
                }
                if (empty || (tab == null) || tab.isCustom()) {
                    setGraphic(null);
                    return;
                }
                boundVisible = tab.visibleProperty();
                checkBox.selectedProperty().bindBidirectional(boundVisible);
                setGraphic(checkBox);
            }
        });

        TableColumn<EditorTabViewModel, String> nameColumn = new TableColumn<>(Localization.lang("Tabs"));
        nameColumn.setSortable(false);
        nameColumn.setReorderable(false);
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getDisplayName()));

        TableColumn<EditorTabViewModel, EditorTabViewModel> actionsColumn = new TableColumn<>();
        actionsColumn.setMinWidth(40.0);
        actionsColumn.setMaxWidth(40.0);
        actionsColumn.setResizable(false);
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        new ValueTableCellFactory<EditorTabViewModel, EditorTabViewModel>()
                .withGraphic(tab -> tab.isCustom() ? IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode() : null)
                .withTooltip(tab -> tab.isCustom() ? Localization.lang("Remove tab %0", tab.getDisplayName()) : null)
                .withOnMouseClickedEvent(tab -> _ -> {
                    viewModel.removeTab(tab);
                    fieldsTable.refresh();
                })
                .install(actionsColumn);

        tabsTable.getColumns().add(visibleColumn);
        tabsTable.getColumns().add(nameColumn);
        tabsTable.getColumns().add(actionsColumn);
        tabsTable.setItems(viewModel.getTabs());

        new ViewModelTableRowFactory<EditorTabViewModel>()
                .setOnDragDetected((row, tab, event) -> handleOnDragDetected(tabsTable, DragAndDropDataFormats.ENTRY_EDITOR_TAB, EditorTabViewModel.class, tab, event))
                .setOnDragDropped((row, tab, event) -> handleOnDragDropped(tabsTable, EditorTabViewModel.class, row, event))
                .setOnDragOver((row, tab, event) -> handleOnDragOver(DragAndDropDataFormats.ENTRY_EDITOR_TAB, row, event))
                .setOnDragExited((row, tab, event) -> ControlHelper.removeDroppingPseudoClasses(row))
                .install(tabsTable);

        EasyBind.subscribe(tabsTable.getSelectionModel().selectedItemProperty(), this::onSelectedTabChanged);
        // The tab list is filled after construction (setValues), so select the first row once it arrives.
        viewModel.getTabs().addListener((InvalidationListener) _ -> {
            if (tabsTable.getSelectionModel().isEmpty() && !tabsTable.getItems().isEmpty()) {
                tabsTable.getSelectionModel().selectFirst();
            }
        });
    }

    private void onSelectedTabChanged(EditorTabViewModel tab) {
        if ((tab != null) && tab.isCustom()) {
            fieldsTable.setItems(tab.getFieldPatterns());
            fieldsPlaceholder.setText(Localization.lang("This tab has no fields yet."));
        } else {
            fieldsTable.setItems(FXCollections.emptyObservableList());
            fieldsPlaceholder.setText(tab == null
                                      ? Localization.lang("Select a tab to configure its fields.")
                                      : Localization.lang("The content of built-in tabs is fixed."));
        }
        addFieldName.clear();
    }

    private void addTab() {
        viewModel.addCustomTab(addTabName.getText()).ifPresent(tab -> {
            addTabName.clear();
            tabsTable.getSelectionModel().select(tab);
            tabsTable.scrollTo(tab);
        });
    }

    // endregion

    // region Fields column

    private Node buildFieldsColumn() {
        setupFieldsTable();

        addFieldName.setPrefWidth(200.0);
        addFieldName.setPromptText(Localization.lang("Field name"));
        addFieldName.setOnAction(_ -> addField());
        TextFields.bindAutoCompletion(
                addFieldName,
                FieldFactory.getAllFieldsWithOutInternal().stream()
                            .map(Field::getName)
                            .sorted()
                            .toList());

        Button addFieldButton = new Button(Localization.lang("Add"));
        addFieldButton.setOnAction(_ -> addField());

        addFieldName.disableProperty().bind(EasyBind.map(tabsTable.getSelectionModel().selectedItemProperty(),
                tab -> (tab == null) || !tab.isCustom()));
        addFieldButton.disableProperty().bind(addFieldName.disabledProperty().or(addFieldName.textProperty().isEmpty()));

        Label regexInfo = new Label(Localization.lang("A field name can also be a regular expression, e.g. \"comment-.*\"."));
        regexInfo.getStyleClass().add("italic");

        Button resetButton = new Button(Localization.lang("Reset to default tabs"));
        resetButton.setGraphic(IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        resetButton.setOnAction(_ -> resetTabs());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(GAP, addFieldName, addFieldButton, spacer, resetButton);

        VBox column = new VBox(GAP, fieldsTable, bottomRow, regexInfo);
        HBox.setHgrow(column, Priority.ALWAYS);
        return column;
    }

    private void setupFieldsTable() {
        fieldsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fieldsTable.setPrefHeight(300.0);
        fieldsTable.setPlaceholder(fieldsPlaceholder);
        VBox.setVgrow(fieldsTable, Priority.ALWAYS);

        TableColumn<String, String> patternColumn = new TableColumn<>(Localization.lang("Fields"));
        patternColumn.setSortable(false);
        patternColumn.setReorderable(false);
        patternColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue()));

        TableColumn<String, String> warningColumn = new TableColumn<>();
        warningColumn.setMinWidth(40.0);
        warningColumn.setMaxWidth(40.0);
        warningColumn.setResizable(false);
        warningColumn.setSortable(false);
        warningColumn.setReorderable(false);
        warningColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue()));
        new ValueTableCellFactory<String, String>()
                .withGraphic(pattern -> viewModel.isFieldPatternDuplicated(pattern)
                                        ? IconTheme.JabRefIcons.WARNING.getGraphicNode()
                                        : null)
                .withTooltip(pattern -> viewModel.isFieldPatternDuplicated(pattern)
                                        ? Localization.lang("The field is contained in multiple tabs.")
                                        : null)
                .install(warningColumn);

        TableColumn<String, String> actionsColumn = new TableColumn<>();
        actionsColumn.setMinWidth(40.0);
        actionsColumn.setMaxWidth(40.0);
        actionsColumn.setResizable(false);
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue()));
        new ValueTableCellFactory<String, String>()
                .withGraphic(_ -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(pattern -> Localization.lang("Remove field %0 from currently selected tab", pattern))
                .withOnMouseClickedEvent(pattern -> _ -> {
                    EditorTabViewModel tab = tabsTable.getSelectionModel().getSelectedItem();
                    if (tab != null) {
                        viewModel.removeFieldPattern(tab, pattern);
                        fieldsTable.refresh();
                    }
                })
                .install(actionsColumn);

        fieldsTable.getColumns().add(patternColumn);
        fieldsTable.getColumns().add(warningColumn);
        fieldsTable.getColumns().add(actionsColumn);

        new ViewModelTableRowFactory<String>()
                .setOnDragDetected((row, pattern, event) -> handleOnDragDetected(fieldsTable, DragAndDropDataFormats.FIELD, String.class, pattern, event))
                .setOnDragDropped((row, pattern, event) -> handleOnDragDropped(fieldsTable, String.class, row, event))
                .setOnDragOver((row, pattern, event) -> handleOnDragOver(DragAndDropDataFormats.FIELD, row, event))
                .setOnDragExited((row, pattern, event) -> ControlHelper.removeDroppingPseudoClasses(row))
                .install(fieldsTable);
    }

    private void addField() {
        EditorTabViewModel tab = tabsTable.getSelectionModel().getSelectedItem();
        if ((tab == null) || !tab.isCustom()) {
            return;
        }
        if (viewModel.addFieldPattern(tab, addFieldName.getText())) {
            addFieldName.clear();
            // Recompute the warning cells: the new pattern may now be duplicated across tabs.
            fieldsTable.refresh();
        }
    }

    private void resetTabs() {
        boolean reset = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Reset to default tabs"),
                Localization.lang("This will restore the default tabs and remove all custom tabs."),
                Localization.lang("Reset to default tabs"));
        if (reset) {
            viewModel.resetToDefaults();
            tabsTable.getSelectionModel().selectFirst();
        }
    }

    // endregion

    // region Drag & drop row reordering

    private <T> void handleOnDragDetected(TableView<T> table, DataFormat format, Class<T> type, T item, MouseEvent event) {
        ClipboardContent content = new ClipboardContent();
        Dragboard dragboard = table.startDragAndDrop(TransferMode.MOVE);
        content.put(format, "");
        localDragboard.putValue(type, item);
        dragboard.setContent(content);
        event.consume();
    }

    private <T> void handleOnDragOver(DataFormat format, TableRow<T> row, DragEvent event) {
        if (event.getDragboard().hasContent(format)) {
            event.acceptTransferModes(TransferMode.MOVE);
            ControlHelper.setDroppingPseudoClasses(row, event);
        }
    }

    private <T> void handleOnDragDropped(TableView<T> table, Class<T> type, TableRow<T> row, DragEvent event) {
        if (localDragboard.hasType(type)) {
            T item = localDragboard.getValue(type);
            table.getItems().remove(item);

            if (row.isEmpty()) {
                table.getItems().add(item);
            } else {
                // decide based on drop position whether to add the element before or after
                int offset = event.getY() > (row.getHeight() / 2) ? 1 : 0;
                table.getItems().add(row.getIndex() + offset, item);
            }
            table.getSelectionModel().select(item);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    // endregion
}
