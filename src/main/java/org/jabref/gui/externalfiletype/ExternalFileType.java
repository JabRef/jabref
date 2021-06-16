package org.jabref.gui.externalfiletype;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public interface ExternalFileType {
    String getName();

    String getExtension();

    String getMimeType();

    /**
     * Get the bibtex field name used for this file type. Currently we assume that field name equals filename extension.
     *
     * @return The field name.
     */
    default Field getField() {
        return FieldFactory.parseField(getExtension());
    }

    String getOpenWithApplication();

    JabRefIcon getIcon();
}
