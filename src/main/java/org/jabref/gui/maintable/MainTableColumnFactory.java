package org.jabref.gui.maintable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.MainTableUtils;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.util.OptionalUtil;

import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MainTableColumnFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainTableColumnFactory.class);

    private final ColumnPreferences preferences;
    private final ExternalFileTypes externalFileTypes;
    private final BibDatabaseContext database;
    private final CellFactory cellFactory;
    private final UndoManager undoManager;
    private final DialogService dialogService;

    public MainTableColumnFactory(BibDatabaseContext database, ColumnPreferences preferences, ExternalFileTypes externalFileTypes, UndoManager undoManager, DialogService dialogService) {
        this.database = Objects.requireNonNull(database);
        this.preferences = Objects.requireNonNull(preferences);
        this.externalFileTypes = Objects.requireNonNull(externalFileTypes);
        this.dialogService = dialogService;
        this.cellFactory = new CellFactory(externalFileTypes, undoManager);
        this.undoManager = undoManager;
    }

    public List<TableColumn<BibEntryTableViewModel, ?>> createColumns() {
        List<TableColumn<BibEntryTableViewModel, ?>> columns = new ArrayList<>();

        preferences.getColumnNames().forEach(columnName -> {
            String[] columnTokens = columnName.split(ColumnPreferences.QUALIFIER_SEPARATOR);
            MainTableColumnType type = MainTableColumnType.parse(columnTokens[0]);
            Field field;

            switch (type) {
                case GROUPS:
                    columns.add(createGroupColumn());
                    break;
                case FILE:
                    columns.add(createFileColumn());
                    break;
                case LINKED_IDENTIFIER:
                    if (preferences.preferDoiOverUrl()) {
                        columns.add(createUrlOrDoiColumn(IconTheme.JabRefIcons.DOI, StandardField.DOI, StandardField.URL));
                    } else {
                        columns.add(createUrlOrDoiColumn(IconTheme.JabRefIcons.WWW, StandardField.URL, StandardField.DOI));
                    }
                    columns.add(createEprintColumn(IconTheme.JabRefIcons.WWW, StandardField.EPRINT));
                    break;
                case EXTRAFILE:
                    if (columnTokens.length != 2) {
                        break;
                    }
                    columns.add(new ExtraFileTableColumn(externalFileTypes, columnTokens[1]));
                    break;
                case SPECIALFIELD:
                    if (columnTokens.length != 2) {
                        break;
                    }
                    field = FieldFactory.parseField(columnTokens[1]);
                    if (field instanceof SpecialField) {
                        columns.add(new SpecialTableColumn(undoManager, (SpecialField) field));
                    }
                    break;
                case NORMALFIELD:
                    if (columnTokens.length != 2) {
                        break;
                    }
                    field = FieldFactory.parseField(columnTokens[1]);
                    columns.add(createNormalColumn(field));
                    break;
                default:
                    LOGGER.warn(Localization.lang("Column type %0 is unknown.", columnName));
            }
        });

        return columns;
    }

    private TableColumn<BibEntryTableViewModel, ?> createGroupColumn() {
        TableColumn<BibEntryTableViewModel, List<AbstractGroup>> column = new MainTableColumn<>(MainTableColumnType.GROUPS);
        Node headerGraphic = IconTheme.JabRefIcons.DEFAULT_GROUP_ICON.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Group color")));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(MainTableUtils.STYLE_ICON_COLUMN);
        MainTableUtils.setExactWidth(column, GUIGlobals.WIDTH_ICON_COLUMN);
        column.setCellValueFactory(cellData -> cellData.getValue().getMatchedGroups(database));
        new ValueTableCellFactory<BibEntryTableViewModel, List<AbstractGroup>>()
                .withGraphic(this::createGroupColorRegion)
                .install(column);
        column.setStyle("-fx-padding: 0 0 0 0;");
        column.setSortable(true);
        return column;
    }

    private Node createGroupColorRegion(BibEntryTableViewModel entry, List<AbstractGroup> matchedGroups) {
        List<Color> groupColors = matchedGroups.stream()
                                               .flatMap(group -> OptionalUtil.toStream(group.getColor()))
                                               .collect(Collectors.toList());

        if (!groupColors.isEmpty()) {
            HBox container = new HBox();
            container.setSpacing(2);
            container.setMinWidth(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(0, 2, 0, 2));

            for (Color groupColor : groupColors) {
                Rectangle groupRectangle = new Rectangle();
                groupRectangle.setWidth(3);
                groupRectangle.setHeight(18);
                groupRectangle.setFill(groupColor);
                groupRectangle.setStroke(Color.DARKGRAY);
                groupRectangle.setStrokeWidth(1);

                container.getChildren().add(groupRectangle);
            }

            String matchedGroupsString = matchedGroups.stream()
                    .map(AbstractGroup::getName)
                    .collect(Collectors.joining(", "));
            Tooltip tooltip = new Tooltip(Localization.lang("Entry is contained in the following groups:") + "\n" + matchedGroupsString);
            Tooltip.install(container, tooltip);
            return container;
        }
        return new Pane();
    }

    private TableColumn<BibEntryTableViewModel, ?> createNormalColumn(Field field) {
        String columnName = field.getName();
        NormalTableColumn column = new NormalTableColumn(columnName, FieldFactory.parseOrFields(columnName), database.getDatabase());
            new ValueTableCellFactory<BibEntryTableViewModel, String>()
                    .withText(text -> text)
                    .install(column);
            column.setSortable(true);
            column.setPrefWidth(preferences.getColumnWidth(columnName));
        return column;
    }

    private TableColumn<BibEntryTableViewModel, List<LinkedFile>> createFileColumn() {
        TableColumn<BibEntryTableViewModel, List<LinkedFile>> column = new MainTableColumn<>(MainTableColumnType.FILE);
        Node headerGraphic = IconTheme.JabRefIcons.FILE.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Linked files")));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(MainTableUtils.STYLE_ICON_COLUMN);
        MainTableUtils.setExactWidth(column, GUIGlobals.WIDTH_ICON_COLUMN);
        column.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
                new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                        .withGraphic(filetype -> MainTableUtils.createFileIcon(externalFileTypes, filetype))
                        .withTooltip(this::createFileTooltip)
                        .withMenu(this::createFileMenu)
                        .withOnMouseClickedEvent((entry, linkedFiles) -> event -> {
                            if ((event.getButton() == MouseButton.PRIMARY) && (linkedFiles.size() == 1)) {
                                // Only one linked file -> open directly
                                LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFiles.get(0), entry.getEntry(), database, Globals.TASK_EXECUTOR, dialogService, Globals.prefs.getXMPPreferences(), Globals.prefs.getFilePreferences(), externalFileTypes);
                                linkedFileViewModel.open();
                            }
                        })
                        .install(column);
        return column;
    }

    private String createFileTooltip(List<LinkedFile> linkedFiles) {
        if (linkedFiles.size() > 0) {
            return Localization.lang("Open file %0", linkedFiles.get(0).getLink());
        }
        return null;
    }

    private ContextMenu createFileMenu(BibEntryTableViewModel entry, List<LinkedFile> linkedFiles) {
        if (linkedFiles.size() <= 1) {
            return null;
        }

        ContextMenu contextMenu = new ContextMenu();

        for (LinkedFile linkedFile : linkedFiles) {
            LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFile, entry.getEntry(), database, Globals.TASK_EXECUTOR, dialogService, Globals.prefs.getXMPPreferences(), Globals.prefs.getFilePreferences(), externalFileTypes);

            MenuItem menuItem = new MenuItem(linkedFileViewModel.getDescriptionAndLink(), linkedFileViewModel.getTypeIcon().getGraphicNode());
            menuItem.setOnAction(event -> linkedFileViewModel.open());
            contextMenu.getItems().add(menuItem);
        }

        return contextMenu;
    }

    /**
     * Creates a column for DOIs or URLs.
     * The {@code firstField} is preferred to be shown over {@code secondField}.
     */
    private TableColumn<BibEntryTableViewModel, Field> createUrlOrDoiColumn(JabRefIcon icon, Field firstField, Field secondField) {
        TableColumn<BibEntryTableViewModel, Field> column = new MainTableColumn<>(MainTableColumnType.LINKED_IDENTIFIER);
        Node headerGraphic = icon.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(firstField.getDisplayName() + " / " + secondField.getDisplayName()));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(MainTableUtils.STYLE_ICON_COLUMN);
        MainTableUtils.setExactWidth(column, GUIGlobals.WIDTH_ICON_COLUMN);
        // icon is chosen based on field name in cell, so map fields to its names
        column.setCellValueFactory(cellData -> EasyBind.monadic(cellData.getValue().getField(firstField)).map(x -> firstField).orElse(EasyBind.monadic(cellData.getValue().getField(secondField)).map(x -> secondField)));
        new ValueTableCellFactory<BibEntryTableViewModel, Field>()
                .withGraphic(cellFactory::getTableIcon)
                .withTooltip(this::createIdentifierTooltip)
                .withOnMouseClickedEvent((BibEntryTableViewModel entry, Field content) -> (MouseEvent event) -> openUrlOrDoi(event, entry, content))
                .install(column);
        return column;
    }

    private void openUrlOrDoi(MouseEvent event, BibEntryTableViewModel entry, Field field) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        if (!entry.getEntry().hasField(field)) {
            LOGGER.error("Requested opening viewer for {} of entry '{}', but field is not present.", field, entry.getEntry().getId());
            return;
        }

        entry.getEntry().getField(field).ifPresent(identifier -> {
            try {
                JabRefDesktop.openExternalViewer(database, identifier, field);
            } catch (IOException e) {
                dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), e);
            }
        });
        event.consume();
    }

    private String createIdentifierTooltip(BibEntryTableViewModel entry, Field field) {
        Optional<String> value = entry.getEntry().getField(field);
        if (value.isPresent()) {
            if (StandardField.DOI.equals(field)) {
                return Localization.lang("Open %0 URL (%1)", "DOI", value.get());
            } else if (StandardField.URL.equals(field)) {
                return Localization.lang("Open URL (%0)", value.get());
            } else if (StandardField.EPRINT.equals(field)) {
                return Localization.lang("Open %0 URL (%1)", "ArXiv", value.get());
            }
        }
        return null;
    }

    private TableColumn<BibEntryTableViewModel, Field> createEprintColumn(JabRefIcon icon, Field field) {
        TableColumn<BibEntryTableViewModel, Field> column = new MainTableColumn<>(MainTableColumnType.LINKED_IDENTIFIER);
        column.setGraphic(icon.getGraphicNode());
        column.getStyleClass().add(MainTableUtils.STYLE_ICON_COLUMN);
        MainTableUtils.setExactWidth(column, GUIGlobals.WIDTH_ICON_COLUMN);
        column.setCellValueFactory(cellData -> EasyBind.monadic(cellData.getValue().getField(field)).map(x -> field));
        new ValueTableCellFactory<BibEntryTableViewModel, Field>()
                .withGraphic(cellFactory::getTableIcon)
                .withTooltip(this::createIdentifierTooltip)
                .withOnMouseClickedEvent((BibEntryTableViewModel entry, Field content) -> (MouseEvent event) -> openUrlOrDoi(event, entry, field))
                .install(column);
        return column;
    }
}
