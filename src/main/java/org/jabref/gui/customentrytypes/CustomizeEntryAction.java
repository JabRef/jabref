package org.jabref.gui.customentrytypes;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class CustomizeEntryAction extends SimpleCommand {

    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;

    public CustomizeEntryAction(StateManager stateManager, BibEntryTypesManager entryTypesManager) {
        this.stateManager = stateManager;
        this.executable.bind(needsDatabase(this.stateManager));
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new CustomizeEntryTypeDialogView(database, entryTypesManager));
    }
}
