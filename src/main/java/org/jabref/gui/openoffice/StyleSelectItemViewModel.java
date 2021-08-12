package org.jabref.gui.openoffice;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.openoffice.style.OOBibStyle;

public class StyleSelectItemViewModel {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty journals = new SimpleStringProperty("");
    private final StringProperty file = new SimpleStringProperty("");
    private final ObjectProperty<Node> icon = new SimpleObjectProperty<>(IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode());
    private final OOBibStyle style;
    private final BooleanProperty internalStyle = new SimpleBooleanProperty();

    public StyleSelectItemViewModel(String name, String journals, String file, OOBibStyle style) {
        this.name.setValue(name);
        this.journals.setValue(journals);
        this.file.setValue(file);
        this.style = style;
        this.internalStyle.set(style.isInternalStyle());
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty journalsProperty() {
        return journals;
    }

    public ObjectProperty<Node> iconProperty() {
        return icon;
    }

    public StringProperty fileProperty() {
        return file;
    }

    public OOBibStyle getStyle() {
        return style;
    }

    public BooleanProperty internalStyleProperty() {
        return internalStyle;
    }

    public String getStylePath() {
        return style.getPath();
    }
}
