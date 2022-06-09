package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.edit.automaticfiededitor.editfieldvalue.EditFieldValueTabView;
import org.jabref.gui.edit.automaticfiededitor.renamefield.RenameFieldTabView;
import org.jabref.gui.edit.automaticfiededitor.twofields.TwoFieldsTabView;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class AutomaticFieldEditorViewModel extends AbstractViewModel {
    public static final String NAMED_COMPOUND_EDITS = "EDIT_FIELDS";
    private final ObservableList<AutomaticFieldEditorTab> fieldEditorTabs = FXCollections.observableArrayList();
    private NamedCompound edits = new NamedCompound(NAMED_COMPOUND_EDITS);

    public AutomaticFieldEditorViewModel(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext) {
        fieldEditorTabs.addAll(
                new EditFieldValueTabView(selectedEntries, databaseContext, edits),
                new TwoFieldsTabView(),
                new RenameFieldTabView()
        );
    }

    public ObservableList<AutomaticFieldEditorTab> getFieldEditorTabs() {
        return fieldEditorTabs;
    }
}
