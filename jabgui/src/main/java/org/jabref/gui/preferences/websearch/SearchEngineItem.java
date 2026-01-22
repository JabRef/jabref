package org.jabref.gui.preferences.websearch;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SearchEngineItem {
    private final StringProperty name;
    private final StringProperty urlTemplate;

    public SearchEngineItem(String name, String urlTemplate) {
        this.name = new SimpleStringProperty(name);
        this.urlTemplate = new SimpleStringProperty(urlTemplate);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty urlTemplateProperty() {
        return urlTemplate;
    }

    public String getName() {
        return name.get();
    }

    public String getUrlTemplate() {
        return urlTemplate.get();
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate.set(urlTemplate);
    }
}
