package org.jabref.gui.externalfiles;

import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

public class ImportFilesResultItemViewModel {

    private final StringProperty file = new SimpleStringProperty("");
    private final ObjectProperty<JabRefIcon> icon = new SimpleObjectProperty<>(IconTheme.JabRefIcons.WARNING);
    private final StringProperty message = new SimpleStringProperty("");

    public ImportFilesResultItemViewModel(Path file, boolean success, String message) {
        this.file.setValue(file.toString());
        this.message.setValue(message);
        if (success) {
            this.icon.setValue(IconTheme.JabRefIcons.CHECK.withColor(Color.GREEN));
        } else {
            this.icon.setValue(IconTheme.JabRefIcons.WARNING.withColor(Color.RED));
        }
    }

    public ObjectProperty<JabRefIcon> icon() {
        return this.icon;
    }

    public StringProperty file() {
        return this.file;
    }

    public StringProperty message() {
        return this.message;
    }

    @Override
    public String toString() {
        return "ImportFilesResultItemViewModel [file=" + file.get() + ", message=" + message.get() + "]";
    }
}
