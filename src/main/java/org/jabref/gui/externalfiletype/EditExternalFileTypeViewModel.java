package org.jabref.gui.externalfiletype;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

public class EditExternalFileTypeViewModel {

    private final StringProperty extensionProperty = new SimpleStringProperty("");
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final StringProperty mimeTypeProperty = new SimpleStringProperty("");
    private final StringProperty selectedApplicationProperty = new SimpleStringProperty("");
    private final BooleanProperty defaultApplicationSelectedProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<Node> iconProperty = new SimpleObjectProperty<>();

    public EditExternalFileTypeViewModel(CustomExternalFileType entry) {
        extensionProperty.setValue(entry.getExtension());
        nameProperty.setValue(entry.getFieldName());
        mimeTypeProperty.setValue(entry.getMimeType());
        selectedApplicationProperty.setValue(entry.getOpenWithApplication());
        iconProperty.setValue(entry.getIcon().getGraphicNode());

        if (entry.getOpenWithApplication().isEmpty()) {
            defaultApplicationSelectedProperty.setValue(true);
        }

    }

    public StringProperty extensionProperty() {
        return extensionProperty;
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty mimeTypeProperty() {
        return mimeTypeProperty;
    }

    public StringProperty selectedApplicationProperty() {
        return selectedApplicationProperty;
    }

    public BooleanProperty defaultApplicationSelectedProperty() {
        return defaultApplicationSelectedProperty;
    }

    public ObjectProperty<Node> iconProperty() {
        return iconProperty;
    }

    public void storeSettings() {
        // TODO Auto-generated method stub

    }

}
