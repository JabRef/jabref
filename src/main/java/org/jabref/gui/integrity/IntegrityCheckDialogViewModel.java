package org.jabref.gui.integrity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final ObservableList<IntegrityMessage> messages;
    private final ObservableSet<String> entryTypes;

    public IntegrityCheckDialogViewModel(List<IntegrityMessage> messages) {
        this.messages = FXCollections.observableArrayList(messages);

        Set<String> types = messages.stream()
                                    .map(item -> item.field().getDisplayName())
                                    .collect(Collectors.toSet());
        this.entryTypes = FXCollections.observableSet(types);
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

    public void fixTitle(IntegrityMessage message) {
        String title = message.entry().getTitle().get();
        StringBuilder result = new StringBuilder();
        for (char ch : title.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append("{").append(ch).append("}");
            } else {
                result.append(ch);
            }
        }
        Map<Field, String> fields = new HashMap<>();
        fields.put(StandardField.TITLE, result.toString());
        message.entry().setField(fields);
    }
}
