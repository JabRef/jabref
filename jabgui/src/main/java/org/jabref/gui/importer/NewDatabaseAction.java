package org.jabref.gui.importer;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

/**
 * Create a new, empty, database.
 */
public class NewDatabaseAction extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final CliPreferences preferences;

    /**
     * Constructs a command to create a new library of the default type
     *
     * @param tabContainer the ui container for libraries
     * @param preferences  the preferencesService of JabRef
     */
    public NewDatabaseAction(LibraryTabContainer tabContainer, CliPreferences preferences) {
        this.tabContainer = tabContainer;
        this.preferences = preferences;
    }

    @Override
    public void execute() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setMode(preferences.getLibraryPreferences().getDefaultBibDatabaseMode());
        tabContainer.addTab(bibDatabaseContext, true);
    }
}
