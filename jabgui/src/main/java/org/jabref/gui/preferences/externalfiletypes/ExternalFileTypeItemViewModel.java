package org.jabref.gui.preferences.externalfiletypes;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

public class ExternalFileTypeItemViewModel {
    private final ObjectProperty<JabRefIcon> icon = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty extension = new SimpleStringProperty();
    private final StringProperty mimetype = new SimpleStringProperty();
    private final StringProperty application = new SimpleStringProperty();

    public ExternalFileTypeItemViewModel(ExternalFileType fileType) {
        this.icon.setValue(fileType.getIcon());
        this.name.setValue(fileType.getName());
        this.extension.setValue(fileType.getExtension());
        this.mimetype.setValue(fileType.getMimeType());
        this.application.setValue(fileType.getOpenWithApplication());
    }

    public ExternalFileTypeItemViewModel() {
        this(new CustomExternalFileType("", "", "", "", "new", IconTheme.JabRefIcons.FILE));
    }

    public ObjectProperty<JabRefIcon> iconProperty() {
        return icon;
    }

    /**
     * Used for sorting in the table
     */
    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty extensionProperty() {
        return extension;
    }

    public StringProperty mimetypeProperty() {
        return mimetype;
    }

    public StringProperty applicationProperty() {
        return application;
    }

    public ExternalFileType toExternalFileType() {
        return new CustomExternalFileType(
                this.name.get(),
                this.extension.get(),
                this.mimetype.get(),
                this.application.get(),
                this.icon.get().name(),
                this.icon.get());
    }
}
