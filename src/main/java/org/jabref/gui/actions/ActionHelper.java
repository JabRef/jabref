package org.jabref.gui.actions;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class ActionHelper {

    public static BooleanExpression needsDatabase(StateManager stateManager) {
        return stateManager.activeDatabaseProperty().isPresent();
    }

    public static BooleanExpression needsEntriesSelected(StateManager stateManager) {
        return Bindings.isNotEmpty(stateManager.getSelectedEntries());
    }

    public static BooleanExpression needsEntriesSelected(int numberOfEntries, StateManager stateManager) {
        return Bindings.createBooleanBinding(() -> stateManager.getSelectedEntries().size() == numberOfEntries,
                                             stateManager.getSelectedEntries());
    }

    public static BooleanExpression isFieldSetForSelectedEntry(Field field, StateManager stateManager) {
        return isAnyFieldSetForSelectedEntry(Collections.singletonList(field), stateManager);
    }

    public static BooleanExpression isAnyFieldSetForSelectedEntry(List<Field> fields, StateManager stateManager) {
        ObservableList<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        Binding<Boolean> fieldsAreSet = EasyBind.wrapNullable(Bindings.valueAt(selectedEntries, 0))
                                                .mapObservable(entry -> Bindings.createBooleanBinding(() -> {
                                                           return entry.getFields().stream().anyMatch(fields::contains);
                                                       }, entry.getFieldsObservable()))
                                                .orElse(false);
        return BooleanExpression.booleanExpression(fieldsAreSet);
    }

    public static BooleanExpression isFilePresentForSelectedEntry(StateManager stateManager, PreferencesService preferencesService) {

        ObservableList<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        Binding<Boolean> fileIsPresent = EasyBind.wrapNullable(Bindings.valueAt(selectedEntries, 0)).map(entry -> {
            List<LinkedFile> files = entry.getFiles();

            if ((entry.getFiles().size() > 0) && stateManager.getActiveDatabase().isPresent()) {
                Optional<Path> filename = FileHelper.find(
                                                          stateManager.getActiveDatabase().get(),
                                                          files.get(0).getLink(),
                                                          preferencesService.getFilePreferences());
                return filename.isPresent();
            } else {
                return false;
            }

        }).orElse(false);

        return BooleanExpression.booleanExpression(fileIsPresent);
    }
}
