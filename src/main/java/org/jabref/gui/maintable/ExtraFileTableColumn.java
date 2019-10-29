package org.jabref.gui.maintable;

import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.TableColumn;

import org.jabref.gui.GUIGlobals;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.MainTableUtils;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.model.entry.LinkedFile;

/**
 * A column for specific file types. Shows the icon for the given type (or the FILE_MULTIPLE icon)
 */
public class ExtraFileTableColumn extends MainTableColumn<List<LinkedFile>> {

    /**
     * Creates the column.
     *
     * @param externalFileTypes a list of available external file types
     * @param externalFileTypeName the name of the externalFileType
     */
    ExtraFileTableColumn(ExternalFileTypes externalFileTypes, String externalFileTypeName) {
        super(MainTableColumnType.EXTRAFILE);

        TableColumn<BibEntryTableViewModel, List<LinkedFile>> column = new MainTableColumn<>(MainTableColumnType.EXTRAFILE);
        column.setGraphic(externalFileTypes
                .getExternalFileTypeByName(externalFileTypeName)
                .map(ExternalFileType::getIcon).orElse(IconTheme.JabRefIcons.FILE)
                .getGraphicNode());
        column.getStyleClass().add(MainTableUtils.STYLE_ICON_COLUMN);
        MainTableUtils.setExactWidth(column, GUIGlobals.WIDTH_ICON_COLUMN);
        column.setCellValueFactory(cellData -> cellData.getValue().getLinkedFiles());
        new ValueTableCellFactory<BibEntryTableViewModel, List<LinkedFile>>()
                .withGraphic(linkedFiles -> MainTableUtils.createFileIcon(externalFileTypes, linkedFiles.stream().filter(linkedFile -> linkedFile.getFileType().equalsIgnoreCase(externalFileTypeName)).collect(Collectors.toList())))
                .install(column);
    }
}
