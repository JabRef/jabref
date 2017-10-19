package org.jabref.gui.actions;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CopyFilesResult {

    private final StringProperty file = new SimpleStringProperty("");
    private final BooleanProperty success = new SimpleBooleanProperty(false);
    private final StringProperty message = new SimpleStringProperty("");

    public CopyFilesResult(String file, boolean success, String message)
    {
        this.file.setValue(file);
        this.success.setValue(success);
        this.message.setValue(message);
    }

    public StringProperty getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file.setValue(file);
    }

    public BooleanProperty getSucess() {
        return success;
    }

    public void setSucess(Boolean sucess) {
        this.success.setValue(sucess);
    }

    public StringProperty getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message.setValue(message);
    }

    @Override
    public String toString() {
        return "CopyFilesResult [file=" + file.get() + ", success=" + success.get() + ", message=" + message.get() + "]";
    }
}
