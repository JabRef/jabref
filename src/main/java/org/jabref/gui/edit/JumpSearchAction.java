package org.jabref.gui.edit;

import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.function.Supplier;

import javafx.scene.control.TextInputDialog;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.entryeditor.EntryEditor;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
//import org.jabref.gui.autocompleter.AutoCompleter;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.gui.preferences.PreferencesService;

public class JumpSearchAction extends SimpleCommand {
    //private final EntryEditor entryEditor;
    //private final AutoCompleter<String> autoCompleter;

    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    //private final Collection<BibEntryTypesManager>  bibentrytype;

    public JumpSearchAction( Supplier<LibraryTab> tabSupplier, StateManager stateManager, DialogService dialogService) {
        //this.entryEditor = entryEditor;
       //this.autoCompleter = new AutoCompleter<>(entryEditor.getAvailableFieldNames(), preferencesService);


        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        //this.bibentrytype = bibentrytype;
        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }


    @Override
    public void execute() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Jump to Field");
        dialog.setHeaderText("Type a field name:");
       // dialog.initOwner(new Stage());

        //autoCompleter.bindTo(dialog.getEditor());


        Optional<String> result = dialog.showAndWait();

//        result.ifPresent(fieldName -> {
//            if (!entryEditor.focusField(fieldName)) {
//                entryEditor.showErrorMessage("Field not found: " + fieldName);
//            }
//   });

        dialogService.showCustomDialogAndWait(new JumpSearchView(tabSupplier.get()));
    }
}
