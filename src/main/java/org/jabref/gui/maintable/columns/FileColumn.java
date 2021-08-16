package org.jabref.gui.maintable.columns;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnFactory;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

/**
 * A column that draws a clickable symbol for either all the files of a defined file type
 * or a joined column with all the files of any type
 */
public class FileColumn extends MainTableColumn<List<LinkedFile>> {

    private final ExternalFileTypes externalFileTypes;
    private final DialogService dialogService;
    private final BibDatabaseContext database;
    private final PreferencesService preferencesService;

    /**
     * Creates a joined column for all the linked files.
     */
    public FileColumn(MainTableColumnModel model,
                      BibDatabaseContext database,
                      ExternalFileTypes externalFileTypes,
                      DialogService dialogService,
                      PreferencesService preferencesService) {
        super(model);
        this.externalFileTypes = Objects.requireNonNull(externalFileTypes);
        this.database = Objects.requireNonNull(database);
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;

        setCommonSettings();

        Node headerGraphic = IconTheme.JabRefIcons.FILE.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Linked files")));
        this.setGraphic(headerGraphic);

        new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                .withGraphic(this::createFileIcon)
                .withTooltip(this::createFileTooltip)
                .withMenu(this::createFileMenu)
                .withOnMouseClickedEvent((entry, linkedFiles) -> event -> {
                    if ((event.getButton() == MouseButton.PRIMARY) && (linkedFiles.size() == 1)) {
                        // Only one linked file -> open directly
                        LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFiles.get(0),
                                entry.getEntry(),
                                database, Globals.TASK_EXECUTOR,
                                dialogService,
                                preferencesService,
                                externalFileTypes);
                        linkedFileViewModel.open();
                    }
                })
                .install(this);
    }

    /**
     * Creates a column for all the linked files of a single file type.
     */
    public FileColumn(MainTableColumnModel model,
                      BibDatabaseContext database,
                      ExternalFileTypes externalFileTypes,
                      DialogService dialogService,
                      PreferencesService preferencesService,
                      String fileType) {
        super(model);
        this.externalFileTypes = Objects.requireNonNull(externalFileTypes);
        this.database = Objects.requireNonNull(database);
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;

        setCommonSettings();

        this.setGraphic(externalFileTypes
                .getExternalFileTypeByName(fileType)
                .map(ExternalFileType::getIcon).orElse(IconTheme.JabRefIcons.FILE)
                .getGraphicNode());

        new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                .withGraphic(linkedFiles -> createFileIcon(linkedFiles.stream().filter(linkedFile ->
                                linkedFile.getFileType().equalsIgnoreCase(fileType)).collect(Collectors.toList())))
                .install(this);
    }

    private void setCommonSettings() {
        this.setResizable(false);
        MainTableColumnFactory.setExactWidth(this, ColumnPreferences.ICON_COLUMN_WIDTH);
        this.getStyleClass().add(MainTableColumnFactory.STYLE_ICON_COLUMN);
        this.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
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
            LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFile,
                    entry.getEntry(),
                    database,
                    Globals.TASK_EXECUTOR,
                    dialogService,
                    preferencesService,
                    externalFileTypes);

            MenuItem menuItem = new MenuItem(linkedFileViewModel.getTruncatedDescriptionAndLink(),
                    linkedFileViewModel.getTypeIcon().getGraphicNode());
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
}
