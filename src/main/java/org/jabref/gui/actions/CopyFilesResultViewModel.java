package org.jabref.gui.actions;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;

public class CopyFilesResultViewModel {

    private final StringProperty file = new SimpleStringProperty("");
    private final ObjectProperty<MaterialDesignIcon> icon = new SimpleObjectProperty<>(MaterialDesignIcon.ALERT);

    private final StringProperty message = new SimpleStringProperty("");

    public CopyFilesResultViewModel(String file, boolean success, String message) {
        this.file.setValue(file);
        this.message.setValue(message);
        if (success) {
            this.icon.setValue(MaterialDesignIcon.CHECK);
        }

    }

    public StringProperty getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file.setValue(file);
    }

    public StringProperty getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message.setValue(message);
    }

    public ObjectProperty<MaterialDesignIcon> getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return "CopyFilesResultViewModel [file=" + file.get() + ", message=" + message.get() + "]";
    }
}
