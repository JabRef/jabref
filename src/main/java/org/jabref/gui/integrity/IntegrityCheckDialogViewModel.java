package org.jabref.gui.integrity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.citationkeypattern.GenerateCitationKeyAction;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final CliPreferences preferences;
    private final UndoManager undoManager;

    private final ObservableList<IntegrityMessage> messages;
    private final ObservableSet<String> entryTypes;

    public IntegrityCheckDialogViewModel(List<IntegrityMessage> messages,
                                         Supplier<LibraryTab> tabSupplier,
                                         DialogService dialogService,
                                         StateManager stateManager,
                                         TaskExecutor taskExecutor,
                                         CliPreferences preferences,
                                         UndoManager undoManager) {
        this.messages = FXCollections.observableArrayList(messages);
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.undoManager = undoManager;

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

    public void fix(Field field, IntegrityMessage message, String text) {
        boolean fixed = true;

        switch (field) {
            case StandardField.TITLE:
                fixTitle(message);
                break;
            case StandardField.URLDATE:
                fixBiblatexFieldOnly(message);
                break;
            case InternalField.KEY_FIELD:
                new GenerateCitationKeyAction(tabSupplier, dialogService, stateManager, taskExecutor, preferences, undoManager).execute();
                break;
            default:
                fixed = false;
                break;
        }

        if (fixed && text != null) {
            dialogService.notify(text);
        }
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

    public void fixBiblatexFieldOnly(IntegrityMessage message) {
        Map<Field, String> fields = new HashMap<>();
        fields.put(StandardField.URLDATE, "");
        message.entry().setField(fields);
    }
}
