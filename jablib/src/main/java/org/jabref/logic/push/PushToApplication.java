package org.jabref.logic.push;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.entry.BibEntry;

/**
 * Class that defines interaction with an external application in the form of "pushing" selected entries to it.
 */
public interface PushToApplication {

    /**
     * Gets the display name for the push operation. This name is used
     * in the GUI to represent the push action to the user.
     *
     * @return The display name for the push operation.
     */
    String getDisplayName();

    /**
     * The actual operation. This method will not be called on the event dispatch thread, so it should not do GUI
     * operations without utilizing invokeLater().
     */
    void pushEntries(List<BibEntry> entries);

    /**
     * Reporting etc., this method is called on the event dispatch thread after pushEntries() returns.
     */
    void onOperationCompleted();

    /// Used to route to either the GUI or the CLI - dependent on the usage
    void sendErrorNotification(String title, String message);

    /// Used to route to either the GUI or the CLI - dependent on the usage
    void sendErrorNotification(String message);

    /**
     * Check whether this operation requires citation keys to be set for the entries. If true is returned an error message
     * will be displayed if keys are missing.
     *
     * @return true if citation keys are required for this operation.
     */
    boolean requiresCitationKeys();

    /**
     * Get the delimiter used to separate citation keys.
     *
     * @return The delimiter as a String.
     */
    String getDelimiter();

    void jumpToLine(Path fileName, int line, int column);
}
