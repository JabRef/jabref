package org.jabref.gui.importer;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

/**
 * Create a new, empty, database.
 */
public class NewDatabaseAction extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final PreferencesService preferencesService;

    /**
     * Constructs a command to create a new library of the default type
     *
     * @param tabContainer       the ui container for libraries
     * @param preferencesService the preferencesService of JabRef
     */
    public NewDatabaseAction(LibraryTabContainer tabContainer, PreferencesService preferencesService) {
        this.tabContainer = tabContainer;
        this.preferencesService = preferencesService;
    }

    @Override
    public void execute() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setMode(preferencesService.getLibraryPreferences().getDefaultBibDatabaseMode());
        tabContainer.addTab(bibDatabaseContext, true);
    }
}
