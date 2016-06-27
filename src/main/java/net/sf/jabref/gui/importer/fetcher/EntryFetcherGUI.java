package net.sf.jabref.gui.importer.fetcher;

import javax.swing.JPanel;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.EntryFetcher;

/**
 * Extends the original EntryFetcher and handles the GUI imports as well as the functional tasks.
 */
public interface EntryFetcherGUI extends EntryFetcher {

    /**
     * Handle a query entered by the user.
     *
     * The method is expected to block the caller until all entries have been
     * reported to the inspector.
     *
     * @param query
     *            The query text.
     * @param inspector
     *            The dialog to add imported entries to.
     * @param status
     *            An OutputPrinter passed to the activeFetcher for reporting about the
     *            status of the fetching.
     *
     * @return True if the query was completed successfully, false if an error
     *         occurred.
     */
    @Override
    boolean processQuery(String query, ImportInspector inspector, OutputPrinter status);

    /**
     * Get the name of the help page for this activeFetcher.
     *
     * If given, a question mark is displayed in the side pane which leads to
     * the help page.
     *
     * @return The {@link HelpFiles} enum constant for the help page
     */
    HelpFiles getHelpPage();

    /**
     * If this activeFetcher requires additional options, a panel for setting up these
     * should be returned in a JPanel by this method. This JPanel will be added
     * to the side pane component automatically.
     *
     * @return Options panel for this activeFetcher or null if this activeFetcher does not
     *         have any options.
     */
    default JPanel getOptionsPanel() {
        return null;
    }

}
