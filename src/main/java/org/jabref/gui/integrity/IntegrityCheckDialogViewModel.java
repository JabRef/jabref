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
import org.jabref.model.entry.field.StandardField;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final CliPreferences preferences;
    private final UndoManager undoManager;

    private final ObservableList<IntegrityMessage> messages;
    private final ObservableSet<Field> entryTypes;

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

        Set<Field> types = messages.stream()
                                    .map(IntegrityMessage::field)
                                    .collect(Collectors.toSet());
        this.entryTypes = FXCollections.observableSet(types);
    }

    public ObservableList<IntegrityMessage> getMessages() {
        return messages;
    }

    public Set<Field> getEntryTypes() {
        return entryTypes;
    }

    public void removeFromEntryTypes(String entry) {
        entryTypes.remove(entry);
    }

    public void fix(IntegrityIssue issue, IntegrityMessage message) {
        boolean fixed = true;

        switch (issue) {
            case CAPITAL_LETTER_ARE_NOT_MASKED_USING_CURLY_BRACKETS:
                if (issue.getField().equals(StandardField.TITLE)) {
                    fixTitle(message);
                    return;
                }
                break;
            case BIBTEX_FIELD_ONLY_KEY, BIBTEX_FIELD_ONLY_CROSS_REF:
                removeField(message, issue.getField());
                break;
            case CITATION_KEY_DEVIATES_FROM_GENERATED_KEY:
                new GenerateCitationKeyAction(tabSupplier, dialogService, stateManager, taskExecutor, preferences, undoManager).execute();
                break;
            case INCORRECT_FORMAT:
                if (issue.getField().equals(StandardField.ISSN)) {
                    dialogService.notify("fixed issn");
                    return;
                }
                break;
            default:
                fixed = false;
                break;
        }

         if (fixed) {
            dialogService.notify("Fixed the issue");
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

    public void removeField(IntegrityMessage message, Field
        field) {
        Map<Field, String> fields = new HashMap<>();
        fields.put(field, "");
        message.entry().setField(fields);
    }
}
