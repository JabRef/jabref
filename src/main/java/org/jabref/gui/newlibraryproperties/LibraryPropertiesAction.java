package org.jabref.gui.newlibraryproperties;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class LibraryPropertiesAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryPropertiesAction.class);

    private final StateManager stateManager;
    private final Optional<BibDatabaseContext> alternateDatabase;

    public LibraryPropertiesAction(StateManager stateManager) {
        this.stateManager = stateManager;
        this.executable.bind(needsDatabase(stateManager));
        this.alternateDatabase = Optional.empty();
    }

    public LibraryPropertiesAction(BibDatabaseContext databaseContext, StateManager stateManager) {
        this.stateManager = stateManager;
        this.alternateDatabase = Optional.ofNullable(databaseContext);
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);

        if (alternateDatabase.isPresent()) {
            dialogService.showCustomDialogAndWait(new LibraryPropertiesView(alternateDatabase.get()));
        } else {
            if (stateManager.getActiveDatabase().isPresent()) {
                dialogService.showCustomDialogAndWait(new LibraryPropertiesView(stateManager.getActiveDatabase().get()));
            } else {
                LOGGER.warn("No database selected.");
            }
        }
    }
}
