package org.jabref.gui.push;

import java.util.List;

import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PushToApplicationPreferences;

/**
 * Class that defines interaction with an external application in the form of "pushing" selected entries to it.
 */
public interface PushToApplication {

    String getDisplayName();

    String getTooltip();

    JabRefIcon getApplicationIcon();

    /**
     * The actual operation. This method will not be called on the event dispatch thread, so it should not do GUI
     * operations without utilizing invokeLater().
     */
    void pushEntries(BibDatabaseContext database, List<BibEntry> entries, String keyString);

    /**
     * Reporting etc., this method is called on the event dispatch thread after pushEntries() returns.
     */
    void onOperationCompleted();

    /**
     * Check whether this operation requires citation keys to be set for the entries. If true is returned an error message
     * will be displayed if keys are missing.
     *
     * @return true if citation keys are required for this operation.
     */
    boolean requiresCitationKeys();

    Action getAction();

    PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences pushToApplicationPreferences);
}
