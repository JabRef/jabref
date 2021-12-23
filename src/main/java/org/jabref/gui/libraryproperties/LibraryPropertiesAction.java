package org.jabref.gui.libraryproperties;

import java.util.function.Supplier;

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
    private final Supplier<BibDatabaseContext> alternateDatabase;

    public LibraryPropertiesAction(StateManager stateManager) {
        this(null, stateManager);
        this.executable.bind(needsDatabase(stateManager));
    }

    public LibraryPropertiesAction(Supplier<BibDatabaseContext> databaseContext, StateManager stateManager) {
        this.stateManager = stateManager;
        this.alternateDatabase = databaseContext;
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);

        if (alternateDatabase != null) {
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
