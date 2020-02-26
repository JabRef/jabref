package org.jabref.gui.referencemetadata;

import com.google.gson.JsonArray;

public class IncompleteItem {
    String key;
    String title;
    JsonArray authors;

    public IncompleteItem(String key, String title, JsonArray authors) {
        this.key = key;
        this.title = title;
        this.authors = authors;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonArray getAuthors() {
        return authors;
    }

    public void setAuthors(JsonArray authors) {
        this.authors = authors;
    }
}
