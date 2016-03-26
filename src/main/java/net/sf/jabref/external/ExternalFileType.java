/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.external;

import java.util.Objects;
import javax.swing.*;

import net.sf.jabref.gui.IconTheme;

/**
 * This class defines a type of external files that can be linked to from JabRef.
 * The class contains enough information to provide an icon, a standard extension
 * and a link to which application handles files of this type.
 */
public class ExternalFileType implements Comparable<ExternalFileType> {

    private String name;
    private String extension;
    private String openWith;
    private String iconName;
    private String mimeType;
    private Icon icon;
    private final JLabel label = new JLabel();

    public ExternalFileType(String name, String extension, String mimeType,
                            String openWith, String iconName, Icon icon) {
        label.setText(null);
        this.name = name;
        label.setToolTipText(this.name);
        this.extension = extension;
        this.mimeType = mimeType;
        this.openWith = openWith;

        setIconName(iconName);
        setIcon(icon);
    }

    /**
     * Construct an ExternalFileType from a String array. This is used when
     * reading file type definitions from Preferences, where the available data types are
     * limited. We assume that the array contains the same values as the main constructor,
     * in the same order.
     *
     * @param val arguments.
     */
    public static ExternalFileType buildFromArgs(String[] val) {
        if ((val == null) || (val.length < 4) || (val.length > 5)) {
            throw new IllegalArgumentException("Cannot construct ExternalFileType without four elements in String[] argument.");
        }
        String name = val[0];
        String extension = val[1];
        String openWith;
        String mimeType;
        String iconName;
        Icon icon;

        if (val.length == 4) {
            // Up to version 2.4b the mime type is not included:
            mimeType = "";
            openWith = val[2];
            iconName = val[3];
        } else {
            // When mime type is included, the array length should be 5:
            mimeType = val[2];
            openWith = val[3];
            iconName = val[4];
        }

        // set icon to default first
        icon = IconTheme.JabRefIcon.FILE.getSmallIcon();

        // check whether there is another icon defined for this file type
        for(ExternalFileType fileType : ExternalFileTypes.getDefaultExternalFileTypes()) {
            if(fileType.getName().equals(name)) {
                icon = fileType.icon;
                break;
            }
        }

        return new ExternalFileType(name, extension, mimeType, openWith, iconName, icon);
    }

    /**
     * Return a String array representing this file type. This is used for storage into
     * Preferences, and the same array can be used to construct the file type later,
     * using the String[] constructor.
     *
     * @return A String[] containing all information about this file type.
     */
    public String[] getStringArrayRepresentation() {
        return new String[]{name, extension, mimeType, openWith, iconName};
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        label.setToolTipText(this.name);
    }

    public String getExtension() {
        if (extension == null) {
            return "";
        }
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getMimeType() {
        if (mimeType == null) {
            return "";
        }
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Get the bibtex field name used to extension to this file type.
     * Currently we assume that field name equals filename extension.
     *
     * @return The field name.
     */
    public String getFieldName() {
        return extension;
    }

    public String getOpenWithApplication() {
        if (openWith == null) {
            return "";
        }
        return openWith;
    }

    public void setOpenWith(String openWith) {
        this.openWith = openWith;
    }

    /**
     * Set the string associated with this file type's icon.
     *
     * @param name The icon name to use.
     */
    public void setIconName(String name) {
        this.iconName = name;
    }

    /**
     * Obtain a JLabel instance set with this file type's icon. The same JLabel
     * is returned from each call of this method.
     *
     * @return the label.
     */
    public JLabel getIconLabel() {
        return label;
    }

    /**
     * Get the string associated with this file type's icon.
     *
     * @return The icon name.
     */
    public String getIconName() {
        return iconName;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        label.setIcon(this.icon);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(ExternalFileType o) {
        return getName().compareTo(o.getName());
    }

    public ExternalFileType copy() {
        return new ExternalFileType(name, extension, mimeType, openWith, iconName, icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, extension, mimeType, openWith, iconName);
    }

    /**
     * We define two file type objects as equal if their name, extension, openWith and
     * iconName are equal.
     *
     * @param object The file type to compare with.
     * @return true if the file types are equal.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof ExternalFileType) {
            ExternalFileType other = (ExternalFileType) object;
            return Objects.equals(name, other.name) && Objects.equals(extension, other.extension) &&
                    Objects.equals(mimeType, other.mimeType) && Objects.equals(openWith, other.openWith) && Objects.equals(iconName,  other.iconName);
        }
        return false;
    }
}
