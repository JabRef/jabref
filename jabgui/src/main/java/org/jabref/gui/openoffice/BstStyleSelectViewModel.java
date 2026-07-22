package org.jabref.gui.openoffice;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.openoffice.style.BstStyle;

/// Row model for a [BstStyle] entry in the BST styles table, mirroring [JStyleSelectViewModel].
public class BstStyleSelectViewModel {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty file = new SimpleStringProperty("");
    private final ObjectProperty<Node> icon = new SimpleObjectProperty<>(
            IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode());
    private final BstStyle bstStyle;
    private final BooleanProperty internalStyle = new SimpleBooleanProperty(false);

    public BstStyleSelectViewModel(String name, String file, BstStyle bstStyle) {
        this.name.setValue(name);
        this.file.setValue(file);
        this.bstStyle = bstStyle;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty fileProperty() {
        return file;
    }

    public ObjectProperty<Node> iconProperty() {
        return icon;
    }

    public BstStyle getBstStyle() {
        return bstStyle;
    }

    /// BST styles are always external — the delete icon is always shown.
    public BooleanProperty internalStyleProperty() {
        return internalStyle;
    }

    public String getStylePath() {
        return bstStyle.getPath();
    }
}
