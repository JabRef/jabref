package org.jabref.gui.externalfiletype;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

/**
 * Unknown external file type.
 * <p>
 * This instance represents a file type unknown to JabRef.
 * This can happen, for example, when a database is loaded which contains links to files of a type that has not been defined on this JabRef instance.
 */
public class UnknownExternalFileType implements ExternalFileType {

    private final String name;
    private final String extension;

    public UnknownExternalFileType(String name) {
        this(name, "");
    }

    public UnknownExternalFileType(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getMimeType() {
        return "";
    }

    @Override
    public String getOpenWithApplication() {
        return "";
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.FILE;
    }
}
