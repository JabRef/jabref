package net.sf.jabref.imports;

import java.net.URL;

import javax.swing.JPanel;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.ImportInspectionDialog;

/**
 * Implement this interface to add another fetcher (something that grabs records
 * from the Web for JabRef). Have a look at the existing implemenations
 * OAI2Fetcher and IEEEXploreFetcher.
 */
public interface EntryFetcher extends ImportInspectionDialog.CallBack {

	/**
	 * Handle a query entered by the user.
	 * 
	 * @param query
	 *            The query text.
	 * @param dialog
	 *            The dialog to add imported entries to.
	 * @param frame
	 *            The application frame.
	 */
	public void processQuery(String query, ImportInspectionDialog dialog,
			JabRefFrame frame);

	/**
	 * The title for this fetcher
	 * 
	 * @return The title
	 */
	public String getTitle();

	/**
	 * Get the name of the key binding for this fetcher, if any.
	 * 
	 * @return The name of the key binding
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
	 * @return The name of the help file
	 */
	public String getHelpPage();

	/**
	 * If this fetcher requires additional options, a panel for setting up these
	 * should be returned in a JPanel by this method. This JPanel will be added
	 * to the side pane component automatically.
	 * 
	 * @return Options panel for this fetcher
	 */
	public JPanel getOptionsPanel();
}
