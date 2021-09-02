package org.jabref.gui;

import java.io.IOException;

import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenConsoleAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenConsoleAction.class);
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public OpenConsoleAction(StateManager stateManager, PreferencesService preferencesService) {
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().flatMap(BibDatabaseContext::getDatabasePath).ifPresent(path -> {
            try {
                JabRefDesktop.openConsole(path.toFile(), preferencesService);
            } catch (IOException e) {
                LOGGER.info("Could not open console", e);
            }
        });
    }
}
