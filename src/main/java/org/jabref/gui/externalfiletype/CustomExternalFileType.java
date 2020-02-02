package org.jabref.gui.externalfiletype;

import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

/**
 * This class defines a type of external files that can be linked to from JabRef.
 * The class contains enough information to provide an icon, a standard extension
 * and a link to which application handles files of this type.
 */
public class CustomExternalFileType implements ExternalFileType {

    private StringProperty name;
    private StringProperty extension;
    private StringProperty openWith;
    private StringProperty iconName;
    private StringProperty mimeType;
    private JabRefIcon icon;

    public CustomExternalFileType(StringProperty name, StringProperty extension, StringProperty mimeType,
                                  StringProperty openWith, StringProperty iconName, JabRefIcon icon) {
        this.name = name;
        this.extension = extension;
        this.mimeType = mimeType;
        this.openWith = openWith;

        setIconName(iconName.getValue());
        setIcon(icon);
    }

    public CustomExternalFileType(ExternalFileType type) {
        this(type.getName(), type.getExtension(), type.getMimeType(), type.getOpenWithApplication(), new SimpleStringProperty(""), type.getIcon());
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
        JabRefIcon icon = IconTheme.JabRefIcons.FILE;

        // check whether there is another icon defined for this file type
        for (ExternalFileType fileType : ExternalFileTypes.getDefaultExternalFileTypes()) {
            if (fileType.getName().equals(name)) {
                icon = fileType.getIcon();
                break;
            }
        }

        return new CustomExternalFileType(new SimpleStringProperty(name), new SimpleStringProperty(extension), new SimpleStringProperty(mimeType), new SimpleStringProperty(openWith), new SimpleStringProperty(iconName), icon);
    }

    @Override
    public StringProperty getName() {
        return name;
    }

    @Override
    public String getNameAsString() {
        return name.getValue();
    }

    public void setName(String name) {
        this.name = new SimpleStringProperty(name);
    }

    @Override
    public StringProperty getExtension() {
        if (extension == null) {
            return new SimpleStringProperty("");
        }
        return extension;
    }

    @Override
    public String getExtensionAsString() {
        return extension.getValue();
    }

    public void setExtension(String extension) {
        this.extension = new SimpleStringProperty(extension);
    }

    @Override
    public StringProperty getMimeType() {
        if (mimeType == null) {
            return new SimpleStringProperty("");
        }
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = new SimpleStringProperty(mimeType);
    }

    @Override
    public StringProperty getOpenWithApplication() {
        if (openWith == null) {
            return new SimpleStringProperty("");
        }
        return openWith;
    }

    public void setOpenWith(String openWith) {
        this.openWith = new SimpleStringProperty(openWith);
    }

    /**
     * Get the string associated with this file type's icon.
     *
     * @return The icon name.
     */
    public String getIconName() {
        return iconName.getValue();
    }

    /**
     * Set the string associated with this file type's icon.
     *
     * @param name The icon name to use.
     */
    public void setIconName(String name) {
        this.iconName = new SimpleStringProperty(name);
    }

    @Override
    public JabRefIcon getIcon() {
        return icon;
    }

    public void setIcon(JabRefIcon icon) {
        Objects.requireNonNull(icon);
        this.icon = icon;
    }

    @Override
    public String toString() {
        return getName().toString();
    }

    public ExternalFileType copy() {
        return new CustomExternalFileType(name, extension, mimeType, openWith, iconName, icon);
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

        if (object instanceof CustomExternalFileType) {
            CustomExternalFileType other = (CustomExternalFileType) object;
            return Objects.equals(name, other.name) && Objects.equals(extension, other.extension) &&
                    Objects.equals(mimeType, other.mimeType) && Objects.equals(openWith, other.openWith) && Objects.equals(iconName, other.iconName);
        }
        return false;
    }
}
