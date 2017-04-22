package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.FileFieldWriter;

public class LinkedFilesEditorViewModel extends AbstractEditorViewModel {
    private ListProperty<LinkedFileViewModel> files = new SimpleListProperty<>(FXCollections.observableArrayList());

    public LinkedFilesEditorViewModel() {
        BindingsHelper.bindContentBidirectional(
                files,
                text,
                this::getStringRepresentation,
                this::parseToFileViewModel
        );
    }

    private List<LinkedFileViewModel> parseToFileViewModel(String stringValue) {
        return FileFieldParser.parse(stringValue).stream()
                .map(LinkedFileViewModel::new)
                .collect(Collectors.toList());
    }

    private String getStringRepresentation(List<LinkedFileViewModel> filesValue) {
        return FileFieldWriter.getStringRepresentation(
                filesValue.stream().map(LinkedFileViewModel::getFile).collect(Collectors.toList()));
    }

    public ObservableList<LinkedFileViewModel> getFiles() {
        return files.get();
    }

    public ListProperty<LinkedFileViewModel> filesProperty() {
        return files;
    }
}
