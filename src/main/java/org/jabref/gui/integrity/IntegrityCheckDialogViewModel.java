package org.jabref.gui.integrity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.integrity.IntegrityIssue;
import org.jabref.logic.integrity.IntegrityMessage;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final ListProperty<IntegrityMessage> columnsListProperty;
    private final ObservableList<IntegrityMessage> messages;
    private final ObservableSet<String> entryTypes;

    public IntegrityCheckDialogViewModel(List<IntegrityMessage> messages) {
        this.messages = FXCollections.observableArrayList(messages);

        Set<String> types = messages.stream()
                                    .map(IntegrityMessage::message)
                                    .collect(Collectors.toSet());
        this.entryTypes = FXCollections.observableSet(types);

        this.columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList(messages));
    }

    public ListProperty<IntegrityMessage> columnsListProperty() {
        return this.columnsListProperty;
    }

    public ObservableList<IntegrityMessage> getMessages() {
        return messages;
    }

    public Set<String> getEntryTypes() {
        return entryTypes;
    }

    public void removeFromEntryTypes(String entry) {
        entryTypes.remove(entry);
    }

    public void fix(IntegrityIssue issue, IntegrityMessage message) {
        // fixes
    }
}
