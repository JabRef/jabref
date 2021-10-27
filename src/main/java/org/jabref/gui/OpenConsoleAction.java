package org.jabref.gui;

import java.io.IOException;
import java.util.Optional;

import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenConsoleAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenConsoleAction.class);
    private final BibDatabaseContext databaseContext;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public OpenConsoleAction(BibDatabaseContext databaseContext, StateManager stateManager, PreferencesService preferencesService) {
        this.databaseContext = databaseContext;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    /**
     * Using this constructor will result in executing the command on the active database
     */
    public OpenConsoleAction(StateManager stateManager, PreferencesService preferencesService) {
        this(null, stateManager, preferencesService);
    }

    @Override
    public void execute() {
        Optional.ofNullable(databaseContext).or(stateManager::getActiveDatabase).flatMap(BibDatabaseContext::getDatabasePath).ifPresent(path -> {
            try {
                JabRefDesktop.openConsole(path.toFile(), preferencesService);
            } catch (IOException e) {
                LOGGER.info("Could not open console", e);
            }
        });
    }
}
