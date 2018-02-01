package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseButton;

import org.jabref.Globals;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefIcon;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.util.OptionalValueTableCellFactory;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

import org.controlsfx.control.Rating;
import org.fxmisc.easybind.EasyBind;

class MainTableColumnFactory {

    private static final String STYLE_ICON = "column-icon";

    private final ColumnPreferences preferences;
    private final ExternalFileTypes externalFileTypes;
    private final BibDatabaseContext database;
    private final CellFactory cellFactory;
    private final UndoManager undoManager;

    public MainTableColumnFactory(BibDatabaseContext database, ColumnPreferences preferences, ExternalFileTypes externalFileTypes, UndoManager undoManager) {
        this.database = Objects.requireNonNull(database);
        this.preferences = Objects.requireNonNull(preferences);
        this.externalFileTypes = Objects.requireNonNull(externalFileTypes);
        this.cellFactory = new CellFactory(externalFileTypes, undoManager);
        this.undoManager = undoManager;
    }

    public List<TableColumn<BibEntryTableViewModel, ?>> createColumns() {
        List<TableColumn<BibEntryTableViewModel, ?>> columns = new ArrayList<>();

        // Add column for linked files
        if (preferences.showFileColumn()) {
            columns.add(createFileColumn());
        }

        // Add column for DOI/URL
        if (preferences.showUrlColumn()) {
            if (preferences.preferDoiOverUrl()) {
                columns.add(createIconColumn(IconTheme.JabRefIcons.DOI, FieldName.DOI, FieldName.URL));
            } else {
                columns.add(createIconColumn(IconTheme.JabRefIcons.WWW, FieldName.URL, FieldName.DOI));
            }
        }

        // Add column for eprints
        if (preferences.showEprintColumn()) {
            columns.add(createIconColumn(IconTheme.JabRefIcons.WWW, FieldName.EPRINT));
        }

        // Add columns for other file types
        columns.addAll(preferences.getExtraFileColumns().stream().map(this::createExtraFileColumn).collect(Collectors.toList()));

        // Add 'normal' bibtex fields as configured in the preferences
        columns.addAll(createNormalColumns());

        // Add the "special" icon columns (e.g., ranking, file, ...) that are enabled in preferences
        for (SpecialField field : preferences.getSpecialFieldColumns()) {
            columns.add(createSpecialFieldColumn((field)));
        }

        return columns;
    }

    private List<TableColumn<BibEntryTableViewModel, ?>> createNormalColumns() {
        List<TableColumn<BibEntryTableViewModel, ?>> columns = new ArrayList<>();

        // Read table columns from preferences
        for (String columnName : preferences.getNormalColumns()) {
            // Stored column name will be used as header
            // There might be more than one field to display, e.g., "author/editor" or "date/year" - so split
            String[] fields = columnName.split(FieldName.FIELD_SEPARATOR);
            StringTableColumn column = new StringTableColumn(columnName, Arrays.asList(fields), database.getDatabase());
            new ValueTableCellFactory<BibEntryTableViewModel, String>()
                    .withText(text -> text)
                    .install(column);
            column.setPrefWidth(preferences.getPrefColumnWidth(columnName));
            columns.add(column);
        }
        return columns;
    }

    private TableColumn<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>> createSpecialFieldColumn(SpecialField specialField) {
        TableColumn<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>> column = new TableColumn<>();
        SpecialFieldViewModel specialFieldViewModel = new SpecialFieldViewModel(specialField, undoManager);
        column.setGraphic(specialFieldViewModel.getIcon().getGraphicNode());
        column.getStyleClass().add(STYLE_ICON);
        if (specialField == SpecialField.RANKING) {
            setExactWidth(column, GUIGlobals.WIDTH_ICON_COL_RANKING);
            new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                    .withGraphicIfPresent(this::createRating)
                    .install(column);
        } else {
            setExactWidth(column, GUIGlobals.WIDTH_ICON_COL);

            if (specialField.isSingleValueField()) {
                new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                        .withGraphic((entry, value) -> createSpecialFieldIcon(value, specialFieldViewModel))
                        .withOnMouseClickedEvent((entry, value) -> event -> {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                specialFieldViewModel.toggle(entry.getEntry());
                            }
                        })
                        .install(column);
            } else {
                new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                        .withGraphic((entry, value) -> createSpecialFieldIcon(value, specialFieldViewModel))
                        .withMenu((entry, value) -> createSpecialFieldMenu(entry.getEntry(), specialFieldViewModel))
                        .install(column);
            }
        }
        column.setCellValueFactory(cellData -> cellData.getValue().getSpecialField(specialField));

        return column;
    }

    private Rating createRating(BibEntryTableViewModel entry, SpecialFieldValueViewModel value) {
        Rating ranking = new Rating();
        ranking.setRating(value.getValue().toRating());
        EasyBind.subscribe(ranking.ratingProperty(), rating ->
                new SpecialFieldViewModel(SpecialField.RANKING, undoManager).setSpecialFieldValue(entry.getEntry(), SpecialFieldValue.getRating(rating.intValue()))
        );
        return ranking;
    }

    private ContextMenu createSpecialFieldMenu(BibEntry entry, SpecialFieldViewModel specialField) {
        ContextMenu contextMenu = new ContextMenu();

        for (SpecialFieldValueViewModel value : specialField.getValues()) {
            MenuItem menuItem = new MenuItem(value.getMenuString(), value.getIcon().map(JabRefIcon::getGraphicNode).orElse(null));
            menuItem.setOnAction(event -> specialField.setSpecialFieldValue(entry, value.getValue()));
            contextMenu.getItems().add(menuItem);
        }

        return contextMenu;
    }

    private Node createSpecialFieldIcon(Optional<SpecialFieldValueViewModel> fieldValue, SpecialFieldViewModel specialField) {
        return fieldValue
                .flatMap(SpecialFieldValueViewModel::getIcon)
                .map(JabRefIcon::getGraphicNode)
                .orElseGet(() -> {
                    Node node = specialField.getEmptyIcon().getGraphicNode();
                    node.getStyleClass().add("empty-special-field");
                    return node;
                });
    }

    private void setExactWidth(TableColumn<?, ?> column, int widthIconCol) {
        column.setMinWidth(widthIconCol);
        column.setPrefWidth(widthIconCol);
        column.setMaxWidth(widthIconCol);
    }

    private TableColumn<BibEntryTableViewModel, List<LinkedFile>> createFileColumn() {
        TableColumn<BibEntryTableViewModel, List<LinkedFile>> column = new TableColumn<>();
        column.setGraphic(IconTheme.JabRefIcons.FILE.getGraphicNode());
        column.getStyleClass().add(STYLE_ICON);
        setExactWidth(column, GUIGlobals.WIDTH_ICON_COL);
        column.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
                new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                        .withGraphic(this::createFileIcon)
                        .withMenu(this::createFileMenu)
                        .withOnMouseClickedEvent((entry, linkedFiles) -> event -> {
                            if (event.getButton() == MouseButton.PRIMARY && linkedFiles.size() == 1) {
                                // Only one linked file -> open directly
                                LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFiles.get(0), entry.getEntry(), database, Globals.TASK_EXECUTOR);
                                linkedFileViewModel.open();
                            }
                        })
                        .install(column);
        return column;
    }

    private ContextMenu createFileMenu(BibEntryTableViewModel entry, List<LinkedFile> linkedFiles) {
        if (linkedFiles.size() <= 1) {
            return null;
        }

        ContextMenu contextMenu = new ContextMenu();

        for (LinkedFile linkedFile : linkedFiles) {
            LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFile, entry.getEntry(), database, Globals.TASK_EXECUTOR);

            MenuItem menuItem = new MenuItem(linkedFileViewModel.getDescriptionAndLink(), linkedFileViewModel.getTypeIcon().getGraphicNode());
            menuItem.setOnAction(event -> linkedFileViewModel.open());
            contextMenu.getItems().add(menuItem);
        }

        return contextMenu;
    }

    /**
     * Creates a column which shows an icon instead of the textual content
     */
    private TableColumn<BibEntryTableViewModel, String> createIconColumn(JabRefIcon icon, String firstField, String secondField) {
        TableColumn<BibEntryTableViewModel, String> column = new TableColumn<>();
        column.setGraphic(icon.getGraphicNode());
        column.getStyleClass().add(STYLE_ICON);
        setExactWidth(column, GUIGlobals.WIDTH_ICON_COL);
        column.setCellValueFactory(cellData -> EasyBind.monadic(cellData.getValue().getField(firstField)).orElse(cellData.getValue().getField(secondField)));
                new ValueTableCellFactory<BibEntryTableViewModel, String>()
                        .withGraphic(cellFactory::getTableIcon)
                        .install(column);
        return column;
    }

    private TableColumn<BibEntryTableViewModel, String> createIconColumn(JabRefIcon icon, String field) {
        TableColumn<BibEntryTableViewModel, String> column = new TableColumn<>();
        column.setGraphic(icon.getGraphicNode());
        column.getStyleClass().add(STYLE_ICON);
        setExactWidth(column, GUIGlobals.WIDTH_ICON_COL);
        column.setCellValueFactory(cellData -> cellData.getValue().getField(field));
                new ValueTableCellFactory<BibEntryTableViewModel, String>()
                        .withGraphic(cellFactory::getTableIcon)
                        .install(column);
        return column;
    }

    /**
     * Creates a column for specific file types. Shows the icon for the given type (or the FILE_MULTIPLE icon)
     *
     * @param externalFileTypeName the name of the externalFileType
     */
    private TableColumn<BibEntryTableViewModel, List<LinkedFile>> createExtraFileColumn(String externalFileTypeName) {
        TableColumn<BibEntryTableViewModel, List<LinkedFile>> column = new TableColumn<>();
        column.setGraphic(
                externalFileTypes.getExternalFileTypeByName(externalFileTypeName)
                        .map(ExternalFileType::getIcon).orElse(IconTheme.JabRefIcons.FILE)
                        .getGraphicNode());
        column.getStyleClass().add(STYLE_ICON);
        setExactWidth(column, GUIGlobals.WIDTH_ICON_COL);
        column.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
                new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                        .withGraphic(linkedFiles -> createFileIcon(linkedFiles.stream().filter(linkedFile -> linkedFile.getFileType().equalsIgnoreCase(externalFileTypeName)).collect(Collectors.toList())))
                        .install(column);

        return column;
    }

    private Node createFileIcon(List<LinkedFile> linkedFiles) {
        if (linkedFiles.size() > 1) {
            return IconTheme.JabRefIcons.FILE_MULTIPLE.getGraphicNode();
        } else if (linkedFiles.size() == 1) {
            return externalFileTypes.fromLinkedFile(linkedFiles.get(0), false)
                    .map(ExternalFileType::getIcon)
                    .orElse(IconTheme.JabRefIcons.FILE)
                    .getGraphicNode();
        } else {
            return null;
        }
    }
}
