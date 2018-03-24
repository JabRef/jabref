package org.jabref.gui.externalfiletype;

import java.util.Objects;

import javax.swing.JLabel;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

public interface ExternalFileType {
    String getName();

    String getExtension();

    String getMimeType();

    /**
     * Get the bibtex field name used to extension to this file type.
     * Currently we assume that field name equals filename extension.
     *
     * @return The field name.
     */
    default String getFieldName() {
        return getExtension();
    }

    String getOpenWithApplication();

    JabRefIcon getIcon();
}
