package org.jabref.gui.externalfiletype;

import javafx.beans.property.StringProperty;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public interface ExternalFileType {
    StringProperty getName();

    String getNameAsString();

    StringProperty getExtension();

    String getExtensionAsString();

    StringProperty getMimeType();

    /**
     * Get the bibtex field name used for this file type.
     * Currently we assume that field name equals filename extension.
     *
     * @return The field name.
     */
    default Field getField() {
        return FieldFactory.parseField(getExtension().getValue());
    }

    StringProperty getOpenWithApplication();

    JabRefIcon getIcon();

    String toString();
}
