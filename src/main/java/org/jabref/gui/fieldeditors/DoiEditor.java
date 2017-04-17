package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;


public class DoiEditor extends HBox implements FieldEditorFX {

    private final String fieldName;
    @FXML private DoiEditorViewModel viewModel;
    @FXML private EditorTextArea textArea;
    @FXML private Button fetchByDoiButton;
    private Optional<BibEntry> entry;

    public DoiEditor(String fieldName, TaskExecutor taskExecutor, DialogService dialogService) {
        this.fieldName = fieldName;
        this.viewModel = new DoiEditorViewModel(taskExecutor, dialogService);

        ControlHelper.loadFXMLForControl(this);

        viewModel.doiIsNotPresentProperty().bind(textArea.textProperty().isEmpty());

        //fetchByDoiButton.setTooltip(
        //        new Tooltip(Localization.lang("Get BibTeX data from %0", FieldName.getDisplayName(FieldName.DOI))));
    }

    public DoiEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        this.entry = Optional.of(entry);
        textArea.setText(entry.getField(fieldName).orElse(""));
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @FXML
    private void fetchByDoi(ActionEvent event) {
        entry.ifPresent(bibEntry -> viewModel.fetchByDoi(bibEntry));
    }

    @FXML
    private void lookupDoi(ActionEvent event) {
        entry.ifPresent(bibEntry -> viewModel.lookupDoi(bibEntry));
    }

    @FXML
    private void openDoi(ActionEvent event) {
        viewModel.openDoi(textArea.getText());
    }


}
