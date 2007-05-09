package net.sf.jabref.external;

import net.sf.jabref.GUIGlobals;

import javax.swing.*;

/**
 * This class defines a type of external files that can be linked to from JabRef.
 * The class contains enough information to provide an icon, a standard extension
 * and a link to which application handles files of this type.
 */
public class ExternalFileType implements Comparable {

    protected String name, extension, openWith, iconName;
    protected ImageIcon icon;

    public ExternalFileType(String name, String extension, String openWith,
                            String iconName) {
        this.name = name;
        this.extension = extension;
        this.openWith = openWith;
        this.iconName = iconName;
        this.icon = GUIGlobals.getImage(iconName);
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
        this.extension = val[1];
        this.openWith = val[2];
        this.iconName = val[3];
        this.icon = GUIGlobals.getImage(val[3]);
    }

    /**
     * Return a String array representing this file type. This is used for storage into
     * Preferences, and the same array can be used to construct the file type later,
     * using the String[] constructor.
     *
     * @return A String[] containing all information about this file type.
     */
    public String[] getStringArrayRepresentation() {
        return new String[] {name, extension, openWith, iconName};
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
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

    public ImageIcon getIcon() {
        return icon;
    }

    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public String toString() {
        return getName();
    }

    public int compareTo(Object o) {
        return getName().compareTo(((ExternalFileType)o).getName());
    }
}
