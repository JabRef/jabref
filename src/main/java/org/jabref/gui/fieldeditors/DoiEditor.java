package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ControlHelper;
import org.jabref.model.entry.BibEntry;

public class DoiEditor extends HBox implements FieldEditorFX {

    private final String fieldName;
    @FXML private EditorTextArea textArea;

    public DoiEditor(String fieldName) {
        this.fieldName = fieldName;
        ControlHelper.loadFXMLForControl(this);
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        textArea.setText(entry.getField(fieldName).orElse(""));
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
