package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ControlHelper;
import org.jabref.model.entry.BibEntry;

public class SimpleEditor extends HBox implements FieldEditorFX {

    protected final String fieldName;
    @FXML private EditorTextArea textArea;

    public SimpleEditor(String fieldName) {
        this.fieldName = fieldName;
        ControlHelper.loadFXMLForControl(this);
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        textArea.bindToEntry(fieldName, entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
