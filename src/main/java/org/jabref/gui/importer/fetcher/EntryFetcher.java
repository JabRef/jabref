package org.jabref.gui.importer.fetcher;

import javax.swing.JPanel;

import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;

/**
 * @Deprecated
 * Use {@link SearchBasedEntryFetcher} instead <br>
 * Implement this interface to add another activeFetcher (something that grabs records
 * from the Web for JabRef). Have a look at the existing implemenations
 * OAI2Fetcher, IEEEXploreFetcher, JStorFetcher and
 * CiteSeerEntryFetcher.
 *
 * Note: You also need to implement the method stopFetching from
 * ImportInspectionDialog.Callback
 *
 * A Fetcher should not execute any GUI Operations, because it might be run in
 * headless mode, but rather use the OutputPrinter for talking to the user.
 */
@Deprecated
public interface EntryFetcher extends ImportInspectionDialog.CallBack {

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
    boolean processQuery(String query, ImportInspector inspector, OutputPrinter status);

    /**
     * The title for this activeFetcher, displayed in the menu and in the side pane.
     *
     * @return The title
     */
    String getTitle();

    /**
     * Get the name of the help page for this activeFetcher.
     *
     * If given, a question mark is displayed in the side pane which leads to
     * the help page.
     *
     * @return The {@link HelpFile} enum constant for the help page
     */
    HelpFile getHelpPage();

    /**
     * If this activeFetcher requires additional options, a panel for setting up these
     * should be returned in a JPanel by this method. This JPanel will be added
     * to the side pane component automatically.
     *
     * @return Options panel for this activeFetcher or null if this activeFetcher does not
     *         have any options.
     */
    JPanel getOptionsPanel();
}
