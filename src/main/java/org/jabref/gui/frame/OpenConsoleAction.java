package org.jabref.gui.frame;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenConsoleAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenConsoleAction.class);
    private final Supplier<BibDatabaseContext> databaseContext;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final DialogService dialogService;

    /**
     * Creates a command that opens the console at the path of the supplied database,
     * or defaults to the active database. Use
     * {@link #OpenConsoleAction(StateManager, GuiPreferences, DialogService)} if not supplying
     * another database.
     */
    public OpenConsoleAction(Supplier<BibDatabaseContext> databaseContext, StateManager stateManager, GuiPreferences preferences, DialogService dialogService) {
        this.databaseContext = databaseContext;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.dialogService = dialogService;

        this.executable.bind(ActionHelper.needsSavedLocalDatabase(stateManager));
    }

    /**
     * Using this constructor will result in executing the command on the active database.
     */
    public OpenConsoleAction(StateManager stateManager, GuiPreferences preferences, DialogService dialogService) {
        this(() -> null, stateManager, preferences, dialogService);
    }

    @Override
    public void execute() {
        Optional.ofNullable(databaseContext.get()).or(stateManager::getActiveDatabase).flatMap(BibDatabaseContext::getDatabasePath).ifPresent(path -> {
            try {
                NativeDesktop.openConsole(path, preferences, dialogService);
            } catch (IOException e) {
                LOGGER.info("Could not open console", e);
            }
        });
    }
}
