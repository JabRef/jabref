package org.jabref.gui.externalfiletype;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public interface ExternalFileType {
    String getName();

    String getExtension();

    String getMimeType();

    String getOpenWithApplication();

    JabRefIcon getIcon();

    /**
     * Get the bibtex field name used for this file type. Currently we assume that field name equals filename extension.
     *
     * @return The field name.
     */
    default Field getField() {
        return FieldFactory.parseField(getExtension());
    }

    /**
     * Return a String array representing this file type. This is used for storage into
     * Preferences, and the same array can be used to construct the file type later,
     * using the String[] constructor.
     *
     * @return A String[] containing all information about this file type.
     */
    default String[] toStringArray() {
        return new String[]{getName(), getExtension(), getMimeType(), getOpenWithApplication(), getIcon().name()};
    }
}
