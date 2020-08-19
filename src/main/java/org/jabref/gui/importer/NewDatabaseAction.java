package org.jabref.gui.importer;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.PreferencesService;

/**
 * Create a new, empty, database.
 */
public class NewDatabaseAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final PreferencesService preferencesService;

    private final BibDatabaseMode mode;
    private final boolean shouldUseDefaultMode;

    /**
     * Constructs a command to create a new library of a specific type
     *
     * @param jabRefFrame the application frame of JabRef
     * @param preferencesService the preferencesService of JabRef
     * @param mode the mode the new library is created in (BibTeX or biblatex)
     */
    public NewDatabaseAction(JabRefFrame jabRefFrame, PreferencesService preferencesService, BibDatabaseMode mode) {
        this.jabRefFrame = jabRefFrame;
        this.preferencesService = preferencesService;

        this.mode = mode;
        this.shouldUseDefaultMode = false;
    }

    /**
     * Constructs a command to create a new library of the default type
     *
     * @param jabRefFrame the application frame of JabRef
     * @param preferencesService the preferencesService of JabRef
     */
    public NewDatabaseAction(JabRefFrame jabRefFrame, PreferencesService preferencesService) {
        this.jabRefFrame = jabRefFrame;
        this.preferencesService = preferencesService;

        this.mode = BibDatabaseMode.BIBTEX;
        this.shouldUseDefaultMode = true;
    }

    @Override
    public void execute() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        if (shouldUseDefaultMode) {
            bibDatabaseContext.setMode(preferencesService.getGeneralPreferences().getDefaultBibDatabaseMode());
        } else {
            bibDatabaseContext.setMode(mode);
        }
        jabRefFrame.addTab(bibDatabaseContext, true);
    }
}
