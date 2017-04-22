package org.jabref.gui.fieldeditors;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.model.entry.BibEntry;

public class LinkedFilesEditor extends HBox implements FieldEditorFX {

    private final String fieldName;
    @FXML private LinkedFilesEditorViewModel viewModel;
    @FXML private ListView<LinkedFileViewModel> listView;

    public LinkedFilesEditor(String fieldName) {
        this.fieldName = fieldName;
        this.viewModel = new LinkedFilesEditorViewModel();

        ControlHelper.loadFXMLForControl(this);

        ViewModelListCellFactory<LinkedFileViewModel> cellFactory = new ViewModelListCellFactory<LinkedFileViewModel>()
                .withText(LinkedFileViewModel::getLink)
                .withTooltip(LinkedFileViewModel::getDescription)
                .withIcon(LinkedFileViewModel::getTypeIcon);
        listView.setCellFactory(cellFactory);
        listView.itemsProperty().bind(viewModel.filesProperty());
    }

    public LinkedFilesEditorViewModel getViewModel() {
        return viewModel;
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
