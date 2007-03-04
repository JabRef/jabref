package net.sf.jabref.external;

import javax.swing.*;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 4, 2006
 * Time: 4:27:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExternalFileType implements Comparable {

    protected String name, extension, openWith;
    protected ImageIcon icon;

    public ExternalFileType(String name, String extension, String openWith,
                            ImageIcon icon) {
        this.name = name;
        this.extension = extension;
        this.openWith = openWith;
        this.icon = icon;
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

    /**
     * Get the bibtex field name used to link to this file type.
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
