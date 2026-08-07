package org.jabref.gui.texparser;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.injection.Injector;

public class ParseLatexAction extends SimpleCommand {

    private final StateManager stateManager;

    public ParseLatexAction(StateManager stateManager) {
        this.stateManager = stateManager;
        executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(NullPointerException::new);
        dialogService.showCustomDialogAndWait(new ParseLatexDialogView(database));
    }
}
