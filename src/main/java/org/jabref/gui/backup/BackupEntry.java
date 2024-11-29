package org.jabref.gui.backup;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BackupEntry {
    private final StringProperty name;
    private final StringProperty date;
    private final StringProperty size;
    private final IntegerProperty entries;

    public BackupEntry(String name, String date, String size, int entries) {
        this.name = new SimpleStringProperty(name);
        this.date = new SimpleStringProperty(date);
        this.size = new SimpleStringProperty(size);
        this.entries = new SimpleIntegerProperty(entries);
    }

    public String getDate() {
        return date.get();
    }

    public StringProperty dateProperty() {
        return date;
    }

    public String getSize() {
        return size.get();
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public int getEntries() {
        return entries.get();
    }

    public IntegerProperty entriesProperty() {
        return entries;
    }
}
