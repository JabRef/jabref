package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;

import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefIcon;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.model.database.BibDatabase;
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
    private final BibDatabase database;
    private final CellFactory cellFactory;

    public MainTableColumnFactory(BibDatabase database, ColumnPreferences preferences, ExternalFileTypes externalFileTypes) {
        this.database = Objects.requireNonNull(database);
        this.preferences = Objects.requireNonNull(preferences);
        this.externalFileTypes = Objects.requireNonNull(externalFileTypes);
        this.cellFactory = new CellFactory(externalFileTypes);
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
            StringTableColumn column = new StringTableColumn(columnName, Arrays.asList(fields), database);
            column.setPrefWidth(preferences.getPrefColumnWidth(columnName));
            columns.add(column);
        }
        return columns;
    }

    private TableColumn<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>> createSpecialFieldColumn(SpecialField specialField) {
        TableColumn<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>> column = new TableColumn<>();
        SpecialFieldViewModel specialFieldViewModel = new SpecialFieldViewModel(specialField);
        column.setGraphic(specialFieldViewModel.getIcon().getGraphicNode());
        column.getStyleClass().add(STYLE_ICON);
        if (specialField == SpecialField.RANKING) {
            setExactWidth(column, GUIGlobals.WIDTH_ICON_COL_RANKING);
            column.setCellFactory(
                    new ValueTableCellFactory<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>>()
                            .withGraphic(value -> value.map(this::createRating).orElse(null))
            );
        } else {
            setExactWidth(column, GUIGlobals.WIDTH_ICON_COL);

            if (specialField.isSingleValueField()) {
                column.setCellFactory(
                        new ValueTableCellFactory<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>>()
                                .withGraphic(value -> createSpecialFieldIcon(value, specialFieldViewModel))
                );
            } else {
                column.setCellFactory(
                        new ValueTableCellFactory<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>>()
                                .withGraphic(value -> createSpecialFieldIcon(value, specialFieldViewModel))
                                .withContextMenu(value -> createSpecialFieldContextMenu(specialFieldViewModel))
                );
            }
        }
        column.setCellValueFactory(cellData -> cellData.getValue().getSpecialField(specialField));


        return column;
    }

    private Rating createRating(SpecialFieldValueViewModel value) {
        Rating ranking = new Rating();
        ranking.setRating(toRating(value.getValue()));
        EasyBind.subscribe(ranking.ratingProperty(), rating -> {
        });
        return ranking;
    }

    private int toRating(SpecialFieldValue ranking) {
        switch (ranking) {
            case RANK_1:
                return 1;
            case RANK_2:
                return 2;
            case RANK_3:
                return 3;
            case RANK_4:
                return 4;
            case RANK_5:
                return 5;
            default:
                throw new UnsupportedOperationException(ranking + "is not a valid ranking");
        }
    }

    private ContextMenu createSpecialFieldContextMenu(SpecialFieldViewModel specialField) {
        ContextMenu contextMenu = new ContextMenu();

        for (SpecialFieldValueViewModel value : specialField.getValues()) {
            contextMenu.getItems().add(new MenuItem(value.getMenuString(), value.getIcon().map(JabRefIcon::getGraphicNode).orElse(null)));
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
        column.setCellFactory(
                new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                        .withGraphic(this::createFileIcon));
        return column;
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
        column.setCellFactory(
                new ValueTableCellFactory<BibEntryTableViewModel, String>()
                        .withGraphic(cellFactory::getTableIcon));
        return column;
    }

    private TableColumn<BibEntryTableViewModel, String> createIconColumn(JabRefIcon icon, String field) {
        TableColumn<BibEntryTableViewModel, String> column = new TableColumn<>();
        column.setGraphic(icon.getGraphicNode());
        column.getStyleClass().add(STYLE_ICON);
        setExactWidth(column, GUIGlobals.WIDTH_ICON_COL);
        column.setCellValueFactory(cellData -> cellData.getValue().getField(field));
        column.setCellFactory(
                new ValueTableCellFactory<BibEntryTableViewModel, String>()
                        .withGraphic(cellFactory::getTableIcon));
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
        column.setCellFactory(
                new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                        .withGraphic(linkedFiles -> createFileIcon(linkedFiles.stream().filter(linkedFile -> linkedFile.getFileType().equalsIgnoreCase(externalFileTypeName)).collect(Collectors.toList()))));

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
