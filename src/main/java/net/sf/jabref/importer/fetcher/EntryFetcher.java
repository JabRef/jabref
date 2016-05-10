/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer.fetcher;

import javax.swing.JPanel;

import net.sf.jabref.gui.ImportInspectionDialog;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;

/**
 * Implement this interface to add another activeFetcher (something that grabs records
 * from the Web for JabRef). Have a look at the existing implemenations
 * OAI2Fetcher, IEEEXploreFetcher, MedlineFetcher, JStorFetcher and
 * CiteSeerEntryFetcher.
 *
 * Note: You also need to implement the method stopFetching from
 * ImportInspectionDialog.Callback
 *
 * A Fetcher should not execute any GUI Operations, because it might be run in
 * headless mode, but rather use the OutputPrinter for talking to the user.
 */
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
    JPanel getOptionsPanel();
}
