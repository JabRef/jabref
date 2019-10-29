package org.jabref.gui.util;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.model.entry.LinkedFile;

public class MainTableUtils {

    public static final String STYLE_ICON_COLUMN = "column-icon";

    private MainTableUtils() { }

    public static void setExactWidth(TableColumn<?, ?> column, int width) {
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setMaxWidth(width);
    }

    public static Node createFileIcon(ExternalFileTypes externalFileTypes, List<LinkedFile> linkedFiles) {
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
