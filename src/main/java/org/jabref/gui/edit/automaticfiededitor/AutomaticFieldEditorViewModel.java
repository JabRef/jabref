package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.edit.automaticfiededitor.editfieldvalue.EditFieldValueTabView;
import org.jabref.gui.edit.automaticfiededitor.renamefield.RenameFieldTabView;
import org.jabref.gui.edit.automaticfiededitor.twofields.TwoFieldsTabView;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AutomaticFieldEditorViewModel extends AbstractViewModel {
    private final ObservableList<AutomaticFieldEditorTab> fieldEditorTabs = FXCollections.observableArrayList();

    public AutomaticFieldEditorViewModel(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext) {
        fieldEditorTabs.addAll(
                new EditFieldValueTabView(selectedEntries, databaseContext),
                new TwoFieldsTabView(),
                new RenameFieldTabView()
        );
    }

    public ObservableList<AutomaticFieldEditorTab> getFieldEditorTabs() {
        return fieldEditorTabs;
    }
}
