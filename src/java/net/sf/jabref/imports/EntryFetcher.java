package net.sf.jabref.imports;

import java.net.URL;

import javax.swing.JPanel;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.ImportInspectionDialog;

/**
 * Implement this interface to add another fetcher (something that grabs records
 * from the Web for JabRef). Have a look at the existing implemenations
 * OAI2Fetcher, IEEEXploreFetcher, MedlineFetcher, JStorFetcher and
 * CiteSeerEntryFetcher.
 * 
 * Note: You also need to implement the method stopFetching from
 * ImportInspectionDialog.Callback
 */
public interface EntryFetcher extends ImportInspectionDialog.CallBack {

    /**
     * Handle a query entered by the user.
     * 
     * The method may block the caller if query takes some time.
     * 
     * The caller may not assume that the query is done when the call returns,
     * but should rather wait for the call "entryListComplete" to the import
     * inspection dialog.
     * 
     * @param query
     *            The query text.
     * @param dialog
     *            The dialog to add imported entries to.
     * @param frame
     *            The application frame.
     */
    public void processQuery(String query, ImportInspectionDialog dialog, JabRefFrame frame);

    /**
     * The title for this fetcher, displayed in the menu and in the side pane.
     * 
     * @return The title
     */
    public String getTitle();

    /**
     * Get the name of the key binding for this fetcher, if any.
     * 
     * @return The name of the key binding or null, if no keybinding should be
     *         created.
     */
    public String getKeyName();

    /**
     * Get the appropriate icon URL for this fetcher.
     * 
     * @return The icon URL
     */
    public URL getIcon();

    /**
     * Get the name of the help page for this fetcher.
     * 
     * If given, a question mark is displayed in the side pane which leads to
     * the help page.
     * 
     * @return The name of the help file or null if this fetcher does not have
     *         any help.
     */
    public String getHelpPage();

    /**
     * If this fetcher requires additional options, a panel for setting up these
     * should be returned in a JPanel by this method. This JPanel will be added
     * to the side pane component automatically.
     * 
     * @return Options panel for this fetcher or null if this fetcher does not
     *         have any options.
     */
    public JPanel getOptionsPanel();
}
