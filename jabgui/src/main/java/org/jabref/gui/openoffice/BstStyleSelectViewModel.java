package org.jabref.gui.openoffice;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.style.BstStyle;

/// Row model for a [BstStyle] entry in the BST styles table, mirroring [JStyleSelectViewModel].
public class BstStyleSelectViewModel {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty file = new SimpleStringProperty("");
    private final ObjectProperty<Node> icon = new SimpleObjectProperty<>(
            IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode());
    private final BstStyle bstStyle;
    private final BooleanProperty internalStyle = new SimpleBooleanProperty(false);

    public BstStyleSelectViewModel(BstStyle bstStyle) {
        this.bstStyle = bstStyle;
        this.name.setValue(bstStyle.getName());
        this.file.setValue(bstStyle.isInternalStyle() ? Localization.lang("Internal style") : bstStyle.getPath());
        this.internalStyle.set(bstStyle.isInternalStyle());
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

    /// `true` for bundled internal styles (not deletable); `false` for user-added external styles.
    public BooleanProperty internalStyleProperty() {
        return internalStyle;
    }

    public String getStylePath() {
        return bstStyle.getPath();
    }
}
