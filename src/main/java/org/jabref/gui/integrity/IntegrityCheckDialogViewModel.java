package org.jabref.gui.integrity;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.integrity.IntegrityIssue;
import org.jabref.logic.integrity.IntegrityMessage;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final ListProperty<IntegrityMessage> columnsListProperty;
    private final ObservableList<String> issuesListProperty;
    private final ObservableList<IntegrityMessage> messages;

    public IntegrityCheckDialogViewModel(List<IntegrityMessage> messages) {
        this.messages = FXCollections.observableArrayList(messages);

        List<String> issuesList = messages.stream()
                                          .map(IntegrityMessage::message)
                                          .collect(Collectors.toList());
        this.issuesListProperty = FXCollections.observableArrayList(issuesList);

        this.columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList(messages));
    }

    public ObservableList<String> issueListProperty() {
        return issuesListProperty;
    }

    public ListProperty<IntegrityMessage> columnsListProperty() {
        return this.columnsListProperty;
    }

    public ObservableList<IntegrityMessage> getMessages() {
        return messages;
    }

    public void resolveIssue(IntegrityIssue issue, IntegrityMessage message) {
        // fixes
    }
}
