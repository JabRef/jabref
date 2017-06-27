package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.gui.util.AutoCompletionTextInputBinding;
import org.jabref.logic.autocompleter.ContentAutoCompleters;
import org.jabref.model.entry.BibEntry;

public class SimpleEditor extends HBox implements FieldEditorFX {

    @FXML private final SimpleEditorViewModel viewModel;

    public SimpleEditor(String fieldName, ContentAutoCompleters autoCompleters) {
        this.viewModel = new SimpleEditorViewModel(fieldName, autoCompleters);

        EditorTextArea textArea = new EditorTextArea();
        HBox.setHgrow(textArea, Priority.ALWAYS);
        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        textArea.addToContextMenu(EditorMenus.getDefaultMenu(textArea));
        this.getChildren().add(textArea);

        AutoCompletionTextInputBinding.autoComplete(textArea, viewModel::complete);
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
