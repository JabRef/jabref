package net.sf.jabref.external;

import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 4, 2006
 * Time: 4:27:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExternalFileType {

    protected String name, extension, openWith;
    protected URL icon;

    public ExternalFileType(String name, String extension, String openWith,
                            URL icon) {
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

    public String getOpenWith() {
        return openWith;
    }

    public void setOpenWith(String openWith) {
        this.openWith = openWith;
    }

    public URL getIcon() {
        return icon;
    }

    public void setIcon(URL icon) {
        this.icon = icon;
    }
}
