/*
 * Created on Jun 13, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sf.jabref.imports;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xml.sax.SAXException;

import net.sf.jabref.*;

/**
 * @author mspiegel
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CiteSeerFetcher extends SidePaneComponent {

	final String PREFIX_URL = "http://citeseer.ist.psu.edu/";
	final String PREFIX_IDENTIFIER = "oai:CiteSeerPSU:";
	final String OAI_HOST = "http://cs1.ist.psu.edu/";
	final String OAI_URL = OAI_HOST + "cgi-bin/oai.cgi?";
	final String OAI_ACTION = "verb=GetRecord";
	final String OAI_METADATAPREFIX ="metadataPrefix=oai_citeseer";
	protected SAXParserFactory parserFactory;
	protected SAXParser saxParser;
	protected HttpURLConnection citeseerCon;
	protected HttpClient citeseerHttpClient;
	boolean fetcherActive;

	BasePanel panel;
	JProgressBar progressBar;
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints con = new GridBagConstraints();
	SidePaneManager sidePaneManager;

	public CiteSeerFetcher(BasePanel panel_, SidePaneManager p0)  {
		super(p0);
		panel = panel_;
		sidePaneManager = p0;
		progressBar = new JProgressBar();
		progressBar.setMinimumSize(new Dimension(1,1));
		progressBar.setValue(0);
		progressBar.setMinimum(0);
		progressBar.setStringPainted(true);
//		I can't make this panel re-appear!
//      See comment "Ensure visible does not appear to be working" in JabRefFrame.java
//		sidePaneManager.hideAway(this);
		setLayout(gbl);
		SidePaneHeader header = new SidePaneHeader
			("CiteSeer Transfer", GUIGlobals.fetchHourglassIcon, this);
		con.gridwidth = GridBagConstraints.REMAINDER;
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.insets = new Insets(0, 0, 2,  0);
		gbl.setConstraints(header, con);
		add(header);
		con.gridwidth = 1;
		con.insets = new Insets(0, 0, 0,  0);
		con.fill = GridBagConstraints.HORIZONTAL;
		gbl.setConstraints(progressBar, con);
		add(progressBar);
		try {
			fetcherActive = false;
			parserFactory = SAXParserFactory.newInstance();
			saxParser = parserFactory.newSAXParser();
			citeseerHttpClient = new HttpClient();
			citeseerHttpClient.setConnectionTimeout(10000); // 10 seconds
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	/***********************************/
	/* Begin Inner Class Declarations */
	/* The inner classes are used to modify components, when not in the
	 * event-dispatching thread.  These are used to follow the "single-threaded
	 * rule", as defined here: http://java.sun.com/products/jfc/tsc/articles/threads/threads1.html
	 *
	 *
	 * I'm pretty sure the Dialog box invokers should remain as inner classes,
	 * but I can't decide whether or not to break the one-thread rule for the
	 * progress bar classes.  Because the search contains a locking-mechanism,
	 * activateFetcher() and deactivateFetcher(), there should only be at-most-one
	 * thread accessing the progress bar at any time.
	 */

	class ShowNoConnectionDialog implements Runnable {
		protected String targetURL = "";
		ShowNoConnectionDialog(String URL) {
				targetURL = URL;
		}
		public void run() {
				JOptionPane.showMessageDialog(panel.frame(),
				"I could not connect to host "+ targetURL +
				".  Please check your network connection " +
				"to this machine.",
				"CiteSeer Connection Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	class ShowBadURLDialog implements Runnable {
		protected String badURL = "";
		ShowBadURLDialog(String URL) {
			badURL = URL;
		}
		public void run() {
			JOptionPane.showMessageDialog(panel.frame(),
			"I couldn't parse the following URL: " + badURL +
			// How do I retrieve the index number? NOT the BibTex ID.
			" on entry number XXX.  " +
			" Please refer to the JabRef help manual on using the CiteSeer tools.",
			"CiteSeer Error",
			JOptionPane.ERROR_MESSAGE);
		}
	}

	class UpdateProgressBarMaximum implements Runnable {
		protected int maximum;
		UpdateProgressBarMaximum(int newValue) {
			maximum = newValue;
		}
		public void run() {
			progressBar.setMaximum(maximum);
		}
	}

	class UpdateProgressBarValue implements Runnable {
		protected int counter;
		UpdateProgressBarValue(int newValue) {
			counter = newValue;
		}
		public void run() {
			progressBar.setValue(counter);
		}
	}

	/* End Inner Class Declarations */
	/***********************************/

	synchronized public boolean activateFetcher() {
		if (fetcherActive == true) {
			return(false);
		}	else {
			fetcherActive = true;
			return(true);
		}
	}

	synchronized public void deactivateFetcher() {
		fetcherActive = false;
	}



	/**
	 * @param newDatabase
	 * @param targetDatabase
	 */
	public void populate(BibtexDatabase newDatabase, BibtexDatabase targetDatabase) {
		Iterator targetIterator = targetDatabase.getKeySet().iterator();
		String currentKey;
		BibtexEntry currentEntry;
		Enumeration newEntryEnum;
		Hashtable citationHashTable = new Hashtable();
		UpdateProgressBarValue updateValue = new UpdateProgressBarValue(0);
		SwingUtilities.invokeLater(updateValue);
		while (targetIterator.hasNext()) {
			currentKey = (String) targetIterator.next();
			currentEntry = targetDatabase.getEntryById(currentKey);
			generateIdentifierList(currentEntry, citationHashTable);
		}
		if (citationHashTable.size() > 0) {
			UpdateProgressBarMaximum updateMaximum = new UpdateProgressBarMaximum(citationHashTable.size());
			SwingUtilities.invokeLater(updateMaximum);
		}
		generateCitationList(citationHashTable, newDatabase);
		newEntryEnum = citationHashTable.elements();
		while (newEntryEnum.hasMoreElements()) {
			try {
				BibtexEntry nextEntry = (BibtexEntry) newEntryEnum.nextElement();
				newDatabase.insertEntry(nextEntry);
			} catch(KeyCollisionException ex) {
			}
		}
	}


	private Hashtable generateCitationList(Hashtable citationHashTable, BibtexDatabase database)
	 {
		try {
		if ((citationHashTable != null) && (citationHashTable.size() > 0)) {
			int citationCounter=0;
			for (Enumeration e = citationHashTable.keys() ; e.hasMoreElements() ;) {
				String key = (String) e.nextElement();
				String id = Util.createId(BibtexEntryType.ARTICLE, database);
				BibtexEntry newEntry = new BibtexEntry(id);
				StringBuffer citeseerURLString = new StringBuffer();
				citeseerURLString.append(OAI_URL);
				citeseerURLString.append(OAI_ACTION);
				citeseerURLString.append("&" + OAI_METADATAPREFIX);
				citeseerURLString.append("&" + "identifier=" + key);
				GetMethod citeseerMethod = new GetMethod(citeseerURLString.toString());
				citeseerHttpClient.executeMethod(citeseerMethod);
				saxParser.parse(citeseerMethod.getResponseBodyAsStream(), new CiteSeerImportHandler(newEntry));
				citeseerMethod.releaseConnection();
				citationHashTable.put(key, newEntry);
				citationCounter++;
				UpdateProgressBarValue updateValue = new UpdateProgressBarValue(citationCounter);
				SwingUtilities.invokeLater(updateValue);
			}
		}
		} catch (HttpException e) {
			System.out.println("HttpException: " + e.getReason());
			e.printStackTrace();
			} catch (SAXException e) {
				System.out.println("SAXException: " + e.getLocalizedMessage());
				e.printStackTrace();
			} catch (IOException e) {
				ShowNoConnectionDialog dialog = new ShowNoConnectionDialog(OAI_HOST);
				SwingUtilities.invokeLater(dialog);
			}
		return citationHashTable;
	}

	private void generateIdentifierList(BibtexEntry currentEntry, Hashtable citationHashTable)
		{
			String targetURL = (String) currentEntry.getField("url");
		try {
		if (targetURL != null && targetURL.startsWith(PREFIX_URL) &&
			(targetURL.length() > (PREFIX_URL.length() + 5))) {
				String id = targetURL.substring(PREFIX_URL.length(), targetURL.length() - 5);
				String identifier = PREFIX_IDENTIFIER + id;
				StringBuffer citeseerURLString = new StringBuffer();
				citeseerURLString.append(OAI_URL);
				citeseerURLString.append(OAI_ACTION);
				citeseerURLString.append("&" + OAI_METADATAPREFIX);
				citeseerURLString.append("&" + "identifier=" + identifier);
				GetMethod citeseerMethod = new GetMethod(citeseerURLString.toString());
				citeseerHttpClient.executeMethod(citeseerMethod);
				saxParser.parse(citeseerMethod.getResponseBodyAsStream(), new CiteSeerCitationHandler(citationHashTable));
				citeseerMethod.releaseConnection();
		} else {
			ShowBadURLDialog dialog = new ShowBadURLDialog(targetURL);
			SwingUtilities.invokeLater(dialog);
		}
		} catch(HttpException e) {
			System.out.println("HttpException: " + e.getReason());
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("SAXException: " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			ShowNoConnectionDialog dialog = new ShowNoConnectionDialog(OAI_HOST);
			SwingUtilities.invokeLater(dialog);
		}
	}

	/**
	 * @param be
	 *
	 * Reminder: this method runs in the EventDispatcher thread
	 */
	public boolean importCiteSeerEntry(BibtexEntry be) {
		boolean newValue = false;
		SAXParserFactory factory = SAXParserFactory.newInstance();
			String targetURL = (String) be.getField("url");
		try {
			if (targetURL != null && targetURL.startsWith(PREFIX_URL) &&
				(targetURL.length() > (PREFIX_URL.length() + 5))) {
					String id = targetURL.substring(PREFIX_URL.length(), targetURL.length() - 5);
					String identifier = PREFIX_IDENTIFIER + id;
					StringBuffer citeseerURLString = new StringBuffer();
					citeseerURLString.append(OAI_URL);
					citeseerURLString.append(OAI_ACTION);
					citeseerURLString.append("&" + OAI_METADATAPREFIX);
					citeseerURLString.append("&" + "identifier=" + identifier);
					GetMethod citeseerMethod = new GetMethod(citeseerURLString.toString());
					int response = citeseerHttpClient.executeMethod(citeseerMethod);
					saxParser.parse(citeseerMethod.getResponseBodyAsStream(), new CiteSeerImportHandler(be));
					citeseerMethod.releaseConnection();
					newValue = true;
				} else {
					ShowBadURLDialog dialog = new ShowBadURLDialog(targetURL);
					dialog.run();
				}
			} catch (HttpException e) {
				System.out.println("HttpException: " + e.getReason());
				e.printStackTrace();
			} catch (IOException e) {
					ShowNoConnectionDialog dialog = new ShowNoConnectionDialog(OAI_HOST);
					dialog.run();
			} catch (SAXException e) {
				System.out.println("SAXException: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
			return newValue;
		}
}
