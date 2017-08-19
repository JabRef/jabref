package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.fieldeditors.contextmenu.EditorMenus;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;

import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class PersonsEditor extends HBox implements FieldEditorFX {

    @FXML private final PersonsEditorViewModel viewModel;

    public PersonsEditor(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, AutoCompletePreferences autoCompletePreferences, FieldCheckers fieldCheckers) {
        this.viewModel = new PersonsEditorViewModel(fieldName, suggestionProvider, autoCompletePreferences, fieldCheckers);

        EditorTextArea textArea = new EditorTextArea();
        HBox.setHgrow(textArea, Priority.ALWAYS);
        textArea.textProperty().bindBidirectional(viewModel.textProperty());
        textArea.addToContextMenu(EditorMenus.getNameMenu(textArea));
        this.getChildren().add(textArea);

        AutoCompletionTextInputBinding.autoComplete(textArea, viewModel::complete, viewModel.getAutoCompletionConverter(), viewModel.getAutoCompletionStrategy());

        ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
        validationVisualizer.initVisualization(viewModel.getFieldValidator().getValidationStatus(), textArea);
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
