package org.jabref.gui.copyfiles;

import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

public class CopyFilesResultItemViewModel {

    private final StringProperty file = new SimpleStringProperty("");
    private final ObjectProperty<JabRefIcon> icon = new SimpleObjectProperty<>(IconTheme.JabRefIcons.WARNING);
    private final StringProperty message = new SimpleStringProperty("");

    public CopyFilesResultItemViewModel(Path file, boolean success, String message) {
        this.file.setValue(file.toString());
        this.message.setValue(message);
        if (success) {
            this.icon.setValue(IconTheme.JabRefIcons.CHECK);
        }
    }

    public StringProperty getFile() {
        return file;
    }

    public StringProperty getMessage() {
        return message;
    }

    public ObjectProperty<JabRefIcon> getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return "CopyFilesResultItemViewModel [file=" + file.get() + ", message=" + message.get() + "]";
    }
}
