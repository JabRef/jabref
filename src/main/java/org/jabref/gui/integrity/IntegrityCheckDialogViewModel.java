package org.jabref.gui.integrity;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.integrity.IntegrityMessage;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final ObservableList<IntegrityMessage> messages;

    public IntegrityCheckDialogViewModel(List<IntegrityMessage> messages) {
        this.messages = FXCollections.observableArrayList(messages);
    }

    public ObservableList<IntegrityMessage> getMessages() {
        return messages;
    }
}
