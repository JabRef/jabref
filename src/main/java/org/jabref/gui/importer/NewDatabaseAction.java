package org.jabref.gui.importer;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

/**
 * Create a new, empty, database.
 */
public class NewDatabaseAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final PreferencesService preferencesService;

    /**
     * Constructs a command to create a new library of the default type
     *
     * @param jabRefFrame        the application frame of JabRef
     * @param preferencesService the preferencesService of JabRef
     */
    public NewDatabaseAction(JabRefFrame jabRefFrame, PreferencesService preferencesService) {
        this.jabRefFrame = jabRefFrame;
        this.preferencesService = preferencesService;
    }

    @Override
    public void execute() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setMode(preferencesService.getGeneralPreferences().getDefaultBibDatabaseMode());
        jabRefFrame.addTab(bibDatabaseContext, true);
    }
}
