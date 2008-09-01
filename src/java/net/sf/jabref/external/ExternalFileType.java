package net.sf.jabref.external;

import javax.swing.*;

import net.sf.jabref.GUIGlobals;

/**
 * This class defines a type of external files that can be linked to from JabRef.
 * The class contains enough information to provide an icon, a standard extension
 * and a link to which application handles files of this type.
 */
public class ExternalFileType implements Comparable<ExternalFileType> {

    protected String name, extension, openWith, iconName, mimeType;
    protected ImageIcon icon;
    protected JLabel label = new JLabel();

    public ExternalFileType(String name, String extension, String mimeType,
                            String openWith, String iconName) {
        label.setText(null);
        this.name = name;
        label.setToolTipText(this.name);
        this.extension = extension;
        this.mimeType = mimeType;
        this.openWith = openWith;
        setIconName(iconName);
    }

    /**
     * Construct an ExternalFileType from a String array. This constructor is used when
     * reading file type definitions from Preferences, where the available data types are
     * limited. We assume that the array contains the same values as the main constructor,
     * in the same order.
     *
     * TODO: The icon argument needs special treatment. At the moment, we assume that the fourth
     * element of the array contains the icon keyword to be looked up in the current icon theme.
     * To support icons found elsewhere on the file system we simply need to prefix the icon name
     * with a marker. 
     *
     * @param val Constructor arguments.
     */
    public ExternalFileType(String[] val) {
        if ((val == null) || (val.length < 4))
            throw new IllegalArgumentException("Cannot contruct ExternalFileType without four elements in String[] argument.");
        this.name = val[0];
        label.setToolTipText(this.name);
        this.extension = val[1];
        label.setText(null);
        // Up to version 2.4b the mime type is not included:
        if (val.length == 4) {
            this.openWith = val[2];
            setIconName(val[3]);
        }
        // When mime type is included, the array length should be 5:
        else if (val.length == 5) {
            this.mimeType = val[2];
            this.openWith = val[3];
            setIconName(val[4]);
        }
    }

    /**
     * Return a String array representing this file type. This is used for storage into
     * Preferences, and the same array can be used to construct the file type later,
     * using the String[] constructor.
     *
     * @return A String[] containing all information about this file type.
     */
    public String[] getStringArrayRepresentation() {
        return new String[] {name, extension, mimeType, openWith, iconName};
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        label.setToolTipText(this.name);
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Get the bibtex field name used to extension to this file type.
     * Currently we assume that field name equals filename extension.
     * @return The field name.
     */
    public String getFieldName() {
        return extension;
    }

    public String getOpenWith() {
        return openWith;
    }

    public void setOpenWith(String openWith) {
        this.openWith = openWith;
    }

    /**
     * Set the string associated with this file type's icon. The string is used
     * to get the actual icon by the method GUIGlobals.getIcon(String)
     * @param name The icon name to use.
     */
    public void setIconName(String name) {
        this.iconName = name;
        try {
            this.icon = GUIGlobals.getImage(iconName);
        } catch (NullPointerException ex) {
            // Loading the icon failed. This could be because the icons have not been
            // initialized, which will be the case if we are operating from the command
            // line and the graphical interface hasn't been initialized. In that case
            // we will do without the icon:
            this.icon = null;
        }
        label.setIcon(this.icon);
    }

    /**
     * Obtain a JLabel instance set with this file type's icon. The same JLabel
     * is returned from each call of this method.
     * @return the label.
     */
    public JLabel getIconLabel() {
        return label;
    }

    /**
     * Get the string associated with this file type's icon. The string is used
     * to get the actual icon by the method GUIGlobals.getIcon(String)
     * @return The icon name.
     */
    public String getIconName() {
        return iconName;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public String toString() {
        return getName();
    }

    public int compareTo(ExternalFileType o) {
        return getName().compareTo(o.getName());
    }

    public ExternalFileType copy() {
        return new ExternalFileType(name, extension, mimeType, openWith, iconName);
    }


    public int hashCode() {
        return name.hashCode();
    }

    /**
     * We define two file type objects as equal if their name, extension, openWith and
     * iconName are equal.
     *
     * @param object The file type to compare with.
     * @return true if the file types are equal.
     */
    public boolean equals(Object object) {
        ExternalFileType other = (ExternalFileType)object;
        if (other == null)
            return false;
        return (name == null ? other.name == null : name.equals(other.name))
                && (extension == null ? other.extension == null : extension.equals(other.extension))
                && (mimeType == null ? other.mimeType == null : mimeType.equals(other.mimeType))
                && (openWith== null ? other.openWith == null : openWith.equals(other.openWith))
                && (iconName== null ? other.iconName == null : iconName.equals(other.iconName));
    }
}
