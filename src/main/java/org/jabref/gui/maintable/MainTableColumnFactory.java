package org.jabref.gui.maintable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.util.OptionalValueTableCellFactory;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.comparator.RankingFieldComparator;
import org.jabref.gui.util.comparator.ReadStatusFieldComparator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.util.OptionalUtil;

import org.controlsfx.control.Rating;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MainTableColumnFactory {

    private static final String STYLE_ICON_COLUMN = "column-icon";
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

        preferences.getColumns().forEach(column -> {

            switch (column.getType()) {
                case INDEX:
                    columns.add(createIndexColumn(column));
                    break;
                case GROUPS:
                    columns.add(createGroupColumn(column));
                    break;
                case FILES:
                    columns.add(createFilesColumn(column));
                    break;
                case LINKED_IDENTIFIER:
                    columns.add(createIdentifierColumn(column));
                    break;
                case EXTRAFILE:
                    if (!column.getQualifier().isBlank()) {
                        columns.add(createExtraFileColumn(column));
                    }
                    break;
                case SPECIALFIELD:
                    if (!column.getQualifier().isBlank()) {
                        Field field = FieldFactory.parseField(column.getQualifier());
                        if (field instanceof SpecialField) {
                            columns.add(createSpecialFieldColumn(column));
                        } else {
                            LOGGER.warn(Localization.lang("Special field type %0 is unknown. Using normal column type.", column.getQualifier()));
                            columns.add(createFieldColumn(column));
                        }
                    }
                    break;
                default:
                case NORMALFIELD:
                    if (!column.getQualifier().isBlank()) {
                        columns.add(createFieldColumn(column));
                    }
                    break;
            }
        });

        return columns;
    }

    private void setExactWidth(TableColumn<?, ?> column, double width) {
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * Creates a column with a continous number
     */
    private TableColumn<BibEntryTableViewModel, String> createIndexColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, String> column = new MainTableColumn<>(columnModel);
        Node header = new Text("#");
        Tooltip.install(header, new Tooltip(MainTableColumnModel.Type.INDEX.getDisplayName()));
        column.setGraphic(header);
        column.setStyle("-fx-alignment: CENTER-RIGHT;");
        column.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                String.valueOf(cellData.getTableView().getItems().indexOf(cellData.getValue()) + 1)));
        new ValueTableCellFactory<BibEntryTableViewModel, String>()
                .withText(text -> text)
                .install(column);
        column.setSortable(false);
        return column;
    }

    /**
     * Creates a column for group color bars.
     */
    private TableColumn<BibEntryTableViewModel, ?> createGroupColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, List<AbstractGroup>> column = new MainTableColumn<>(columnModel);
        Node headerGraphic = IconTheme.JabRefIcons.DEFAULT_GROUP_ICON.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Group color")));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(STYLE_ICON_COLUMN);
        setExactWidth(column, ColumnPreferences.ICON_COLUMN_WIDTH);
        column.setResizable(false);
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
                groupRectangle.getStyleClass().add("groupColumnBackground");
                groupRectangle.setWidth(3);
                groupRectangle.setHeight(18);
                groupRectangle.setFill(groupColor);
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

    /**
     * Creates a text column to display any standard field.
     */
    private TableColumn<BibEntryTableViewModel, ?> createFieldColumn(MainTableColumnModel columnModel) {
        FieldColumn column = new FieldColumn(columnModel,
                FieldFactory.parseOrFields(columnModel.getQualifier()),
                database.getDatabase());
        new ValueTableCellFactory<BibEntryTableViewModel, String>()
                .withText(text -> text)
                .install(column);
        column.setSortable(true);
        return column;
    }

    /**
     * Creates a clickable icons column for DOIs, URLs, URIs and EPrints.
     */
    private TableColumn<BibEntryTableViewModel, Map<Field, String>> createIdentifierColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, Map<Field, String>> column = new MainTableColumn<>(columnModel);
        Node headerGraphic = IconTheme.JabRefIcons.WWW.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Linked identifiers")));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(STYLE_ICON_COLUMN);
        setExactWidth(column, ColumnPreferences.ICON_COLUMN_WIDTH);
        column.setResizable(false);
        column.setCellValueFactory(cellData -> cellData.getValue().getLinkedIdentifiers());
        new ValueTableCellFactory<BibEntryTableViewModel, Map<Field, String>>()
                .withGraphic(this::createIdentifierGraphic)
                .withTooltip(this::createIdentifierTooltip)
                .withMenu(this::createIdentifierMenu)
                .install(column);
        return column;
    }

    private Node createIdentifierGraphic(Map<Field, String> values) {
        if (values.isEmpty()) {
            return null;
        } else {
            return cellFactory.getTableIcon(StandardField.URL);
        }
    }

    private String createIdentifierTooltip(Map<Field, String> values) {
        StringBuilder identifiers = new StringBuilder();
        values.keySet().forEach(field -> identifiers.append(field.getDisplayName()).append(": ").append(values.get(field)).append("\n"));
        return identifiers.toString();
    }

    private ContextMenu createIdentifierMenu(BibEntryTableViewModel entry, Map<Field, String> values) {
        ContextMenu contextMenu = new ContextMenu();

        values.keySet().forEach(field -> {
            MenuItem menuItem = new MenuItem(field.getDisplayName() + ": " + values.get(field), cellFactory.getTableIcon(field));
            menuItem.setOnAction(event -> {
                try {
                    JabRefDesktop.openExternalViewer(database, values.get(field), field);
                } catch (IOException e) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), e);
                }
                event.consume();
            });
            contextMenu.getItems().add(menuItem);
        });

        return contextMenu;
    }

    /**
     * A column that displays a SpecialField
     */
    private TableColumn<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>> createSpecialFieldColumn(MainTableColumnModel columnModel) {
        SpecialField specialField = (SpecialField) FieldFactory.parseField(columnModel.getQualifier());
        TableColumn<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>> column = new MainTableColumn<>(columnModel);
        SpecialFieldViewModel specialFieldViewModel = new SpecialFieldViewModel(specialField, undoManager);
        Node headerGraphic = specialFieldViewModel.getIcon().getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(specialFieldViewModel.getLocalization()));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(STYLE_ICON_COLUMN);
        if (specialField == SpecialField.RANKING) {
            setExactWidth(column, SpecialFieldsPreferences.COLUMN_RANKING_WIDTH);
            column.setResizable(false);
            new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                    .withGraphicIfPresent(this::createSpecialRating)
                    .install(column);
        } else {
            setExactWidth(column, ColumnPreferences.ICON_COLUMN_WIDTH);
            column.setResizable(false);

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

        if (specialField == SpecialField.RANKING) {
            column.setComparator(new RankingFieldComparator());
        }

        // Added comparator for Read Status
        if (specialField == SpecialField.READ_STATUS) {
            column.setComparator(new ReadStatusFieldComparator());
        }

        column.setSortable(true);

        return column;
    }

    private Rating createSpecialRating(BibEntryTableViewModel entry, SpecialFieldValueViewModel value) {
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

    /**
     * Creates a column for all the linked files. Instead of creating a column for a single file type, like {@code
     * createExtraFileColumn} does, this creates one single column collecting all file links.
     */
    private TableColumn<BibEntryTableViewModel, List<LinkedFile>> createFilesColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, List<LinkedFile>> column = new MainTableColumn<>(columnModel);
        Node headerGraphic = IconTheme.JabRefIcons.FILE.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Linked files")));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(STYLE_ICON_COLUMN);
        setExactWidth(column, ColumnPreferences.ICON_COLUMN_WIDTH);
        column.setResizable(false);
        column.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
        new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                .withGraphic(this::createFileIcon)
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

    /**
     * Creates a column for all the linked files of a single file type.
     */
    private TableColumn<BibEntryTableViewModel, List<LinkedFile>> createExtraFileColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, List<LinkedFile>> column = new MainTableColumn<>(columnModel);
        column.setGraphic(externalFileTypes
                .getExternalFileTypeByName(columnModel.getQualifier())
                .map(ExternalFileType::getIcon).orElse(IconTheme.JabRefIcons.FILE)
                .getGraphicNode());
        column.getStyleClass().add(STYLE_ICON_COLUMN);
        setExactWidth(column, ColumnPreferences.ICON_COLUMN_WIDTH);
        column.setResizable(false);
        column.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
        new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                .withGraphic(linkedFiles -> createFileIcon(linkedFiles.stream().filter(linkedFile ->
                        linkedFile.getFileType().equalsIgnoreCase(columnModel.getQualifier())).collect(Collectors.toList())))
                .install(column);

        return column;
    }
}
