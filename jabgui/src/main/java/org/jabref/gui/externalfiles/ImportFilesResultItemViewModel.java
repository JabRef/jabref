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

    public enum Status {
        SUCCESS,
        WARNING,
        FAILURE
    }

    private final Status status;

    public ImportFilesResultItemViewModel(Path file, boolean success, String message) {
        this(file, success ? Status.SUCCESS : Status.FAILURE, message);
    }

    public ImportFilesResultItemViewModel(Path file, Status status, String message) {
        this.file.setValue(file.toString());
        this.message.setValue(message);
        this.status = status;
        switch (status) {
            case SUCCESS ->
                    this.icon.setValue(IconTheme.JabRefIcons.CHECK.withColor(Color.GREEN));
            case WARNING ->
                    this.icon.setValue(IconTheme.JabRefIcons.WARNING);
            case FAILURE ->
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

    public Status status() {
        return status;
    }

    public boolean isSuccess() {
        return status != Status.FAILURE;
    }

    @Override
    public String toString() {
        return "ImportFilesResultItemViewModel [file=" + file.get() + ", message=" + message.get() + "]";
    }
}
