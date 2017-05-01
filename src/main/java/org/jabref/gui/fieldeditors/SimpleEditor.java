package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.ControlHelper;
import org.jabref.model.entry.BibEntry;

public class SimpleEditor extends HBox implements FieldEditorFX {

    protected final String fieldName;
    @FXML private final SimpleEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;

    public SimpleEditor(String fieldName) {
        this.fieldName = fieldName;
        this.viewModel = new SimpleEditorViewModel();

        ControlHelper.loadFXMLForControl(this);

        textArea.textProperty().bindBidirectional(viewModel.textProperty());

        textArea.addToContextMenu(EditorMenus.getDefaultMenu(textArea));
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(fieldName, entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
