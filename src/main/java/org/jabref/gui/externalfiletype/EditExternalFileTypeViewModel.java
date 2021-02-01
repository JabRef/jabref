package org.jabref.gui.externalfiletype;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

public class EditExternalFileTypeViewModel {

    private final StringProperty extensionProperty = new SimpleStringProperty("");
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final StringProperty mimeTypeProperty = new SimpleStringProperty("");
    private final StringProperty selectedApplicationProperty = new SimpleStringProperty("");
    private final BooleanProperty defaultApplicationSelectedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty customApplicationSelectedProperty = new SimpleBooleanProperty(false);
    private final Node icon;
    private final CustomExternalFileType fileType;

    public EditExternalFileTypeViewModel(CustomExternalFileType fileType) {
        this.fileType = fileType;
        extensionProperty.setValue(fileType.getExtension());
        nameProperty.setValue(fileType.getName());
        mimeTypeProperty.setValue(fileType.getMimeType());
        selectedApplicationProperty.setValue(fileType.getOpenWithApplication());
        icon = fileType.getIcon().getGraphicNode();

        if (fileType.getOpenWithApplication().isEmpty()) {
            defaultApplicationSelectedProperty.setValue(true);
        } else {
            customApplicationSelectedProperty.setValue(true);
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

    public BooleanProperty customApplicationSelectedProperty() {
        return customApplicationSelectedProperty;
    }

    public Node getIcon() {
        return icon;
    }

    public void storeSettings() {

        fileType.setName(nameProperty.getValue().trim());
        fileType.setMimeType(mimeTypeProperty.getValue().trim());

        String ext = extensionProperty.getValue().trim();
        if (!ext.isEmpty() && (ext.charAt(0) == '.')) {
            fileType.setExtension(ext.substring(1));
        } else {
            fileType.setExtension(ext);
        }

        String application = selectedApplicationProperty.getValue().trim();

        // store application as empty if the "Default" option is selected, or if the application name is empty:
        if (defaultApplicationSelectedProperty.getValue() || application.isEmpty()) {
            fileType.setOpenWith("");
            selectedApplicationProperty.setValue("");
        } else {
            fileType.setOpenWith(application);
        }
    }
}
