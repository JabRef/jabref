package org.jabref.gui.preferences;

import javafx.beans.property.SimpleStringProperty;

public class AppearanceThemeModel {
    private SimpleStringProperty name;
    private SimpleStringProperty path;

    public AppearanceThemeModel(String name, String path) {
        this.name = new SimpleStringProperty(name);
        this.path = new SimpleStringProperty(path);
    }

    public String getName() {
        return name.get();
    }

    public String getPath() {
        return path.get();
    }
}
