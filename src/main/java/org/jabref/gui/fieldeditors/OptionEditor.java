package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Field editor that provides various pre-defined options as a drop-down combobox.
 */
public class OptionEditor<T> extends HBox implements FieldEditorFX {

    @FXML private OptionEditorViewModel<T> viewModel;
    @FXML private ComboBox<T> comboBox;

    public OptionEditor(OptionEditorViewModel<T> viewModel) {
        this.viewModel = viewModel;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        comboBox.setConverter(viewModel.getStringConverter());
        comboBox.setCellFactory(new ViewModelListCellFactory<T>().withText(viewModel::convertToDisplayText));
        comboBox.getItems().setAll(viewModel.getItems());
        comboBox.getEditor().textProperty().bindBidirectional(viewModel.textProperty());
    }

    public OptionEditorViewModel<T> getViewModel() {
        return viewModel;
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
