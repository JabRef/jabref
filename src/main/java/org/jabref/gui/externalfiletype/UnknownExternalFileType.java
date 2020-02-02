package org.jabref.gui.externalfiletype;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

/**
 * Unknown external file type.
 *
 * This instance represents a file type unknown to JabRef.
 * This can happen, for example, when a database is loaded which contains links to files of a type that has not been defined on this JabRef instance.
 */
public class UnknownExternalFileType implements ExternalFileType {

    private final StringProperty name;
    private final StringProperty extension;

    public UnknownExternalFileType(String name) {
        this(name, "");
    }

    public UnknownExternalFileType(String name, String extension) {
        this.name = new SimpleStringProperty(name);
        this.extension = new SimpleStringProperty(extension);
    }

    @Override
    public StringProperty getName() {
        return name;
    }

    @Override
    public String getNameAsString() {
        return name.getValue();
    }

    @Override
    public StringProperty getExtension() {
        return extension;
    }

    @Override
    public String getExtensionAsString() {
        return extension.getValue();
    }

    @Override
    public StringProperty getMimeType() {
        return new SimpleStringProperty("");
    }

    @Override
    public StringProperty getOpenWithApplication() {
        return new SimpleStringProperty("");
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.FILE;
    }
}
