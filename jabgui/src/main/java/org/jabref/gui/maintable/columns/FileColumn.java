package org.jabref.gui.maintable.columns;

import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnFactory;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NonNull;

/**
 * A column that draws a clickable symbol for either all the files of a defined file type
 * or a joined column with all the files of any type
 */
public class FileColumn extends MainTableColumn<List<LinkedFile>> {

    private final DialogService dialogService;
    private final BibDatabaseContext database;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;

    /**
     * Creates a joined column for all the linked files.
     */
    public FileColumn(MainTableColumnModel model,
                      @NonNull BibDatabaseContext database,
                      DialogService dialogService,
                      GuiPreferences preferences,
                      TaskExecutor taskExecutor) {
        super(model);
        this.database = database;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

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
                        LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFiles.getFirst(),
                                entry.getEntry(),
                                database,
                                taskExecutor,
                                dialogService,
                                preferences);
                        linkedFileViewModel.open();
                    }
                })
                .install(this);
    }

    /**
     * Creates a column for all the linked files of a single file type.
     */
    public FileColumn(MainTableColumnModel model,
                      @NonNull BibDatabaseContext database,
                      DialogService dialogService,
                      GuiPreferences preferences,
                      String fileType,
                      TaskExecutor taskExecutor) {
        super(model);
        this.database = database;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        setCommonSettings();

        this.setGraphic(ExternalFileTypes.getExternalFileTypeByName(fileType, preferences.getExternalApplicationsPreferences())
                                         .map(ExternalFileType::getIcon).orElse(IconTheme.JabRefIcons.FILE)
                                         .getGraphicNode());

        new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                .withGraphic((entry, linkedFiles) -> createFileIcon(entry, linkedFiles.stream()
                                                                                      .filter(linkedFile -> linkedFile.getFileType().equalsIgnoreCase(fileType))
                                                                                      .collect(Collectors.toList())))
                .withOnMouseClickedEvent((entry, linkedFiles) -> event -> {
                    List<LinkedFile> filteredFiles = linkedFiles.stream()
                                                                .filter(linkedFile -> linkedFile.getFileType().equalsIgnoreCase(fileType))
                                                                .toList();

                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (filteredFiles.size() == 1) {
                            // Only one file - open directly
                            LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(filteredFiles.getFirst(),
                                    entry.getEntry(), database, taskExecutor, dialogService, preferences);
                            linkedFileViewModel.open();
                        } else if (filteredFiles.size() > 1) {
                            // Multiple files - show context menu to choose file
                            ContextMenu contextMenu = new ContextMenu();
                            for (LinkedFile linkedFile : filteredFiles) {
                                LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(linkedFile,
                                        entry.getEntry(), database, taskExecutor, dialogService, preferences);
                                MenuItem menuItem = new MenuItem(linkedFileViewModel.getTruncatedDescriptionAndLink(),
                                        linkedFileViewModel.getTypeIcon().getGraphicNode());
                                menuItem.setOnAction(_ -> linkedFileViewModel.open());
                                contextMenu.getItems().add(menuItem);
                            }
                            contextMenu.show((Node) event.getSource(), event.getScreenX(), event.getScreenY());
                        }
                    }
                })
                .install(this);
    }

    private void setCommonSettings() {
        this.setResizable(false);
        MainTableColumnFactory.setExactWidth(this, ColumnPreferences.ICON_COLUMN_WIDTH);
        this.getStyleClass().add(MainTableColumnFactory.STYLE_ICON_COLUMN);
        this.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
    }

    private String createFileTooltip(List<LinkedFile> linkedFiles) {
        if (linkedFiles.isEmpty()) {
            return null;
        }

        String filePaths = linkedFiles.stream()
                                      .map(LinkedFile::getLink)
                                      .collect(Collectors.joining("\n"));

        if (linkedFiles.size() == 1) {
            return Localization.lang("Open file %0", filePaths);
        }
        return filePaths;
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
                    taskExecutor,
                    dialogService,
                    preferences);

            MenuItem menuItem = new MenuItem(linkedFileViewModel.getTruncatedDescriptionAndLink(),
                    linkedFileViewModel.getTypeIcon().getGraphicNode());
            menuItem.setOnAction(event -> linkedFileViewModel.open());
            contextMenu.getItems().add(menuItem);
        }

        return contextMenu;
    }

    private Node createFileIcon(BibEntryTableViewModel entry, List<LinkedFile> linkedFiles) {
        if (entry.hasFullTextResultsProperty().get()) {
            return IconTheme.JabRefIcons.FILE_SEARCH.getGraphicNode();
        }
        if (linkedFiles.size() > 1) {
            return IconTheme.JabRefIcons.FILE_MULTIPLE.getGraphicNode();
        } else if (linkedFiles.size() == 1) {
            return ExternalFileTypes.getExternalFileTypeByLinkedFile(linkedFiles.getFirst(), true, preferences.getExternalApplicationsPreferences())
                                    .map(ExternalFileType::getIcon)
                                    .orElse(IconTheme.JabRefIcons.FILE)
                                    .getGraphicNode();
        } else {
            return null;
        }
    }
}
