/*
 * Created on Jun 13, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sf.jabref.imports;

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
import net.sf.jabref.undo.NamedCompound;

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
	boolean citationFetcherActive;
	boolean importFetcherActive;

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
		progressBar.setValue(0);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setStringPainted(true);
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
			citationFetcherActive = false;
			importFetcherActive = false;
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
				Globals.lang("I could not connect to host ") + targetURL + ".  " +
				Globals.lang("Please check your network connection to this machine."),
				Globals.lang("CiteSeer Error"),
				JOptionPane.ERROR_MESSAGE);	
		}		
	}
		
	class ShowBadURLDialog implements Runnable {
		protected String badURL = "";
		protected int rowNumber;
		
		ShowBadURLDialog(String URL, int row) {
			badURL = URL;			
			rowNumber = row;
		}
		public void run() {
			JOptionPane.showMessageDialog(panel.frame(),
			Globals.lang("I couldn't parse the following URL") + ": \"" + badURL + '\"' +
			Globals.lang(" on entry number ") + (rowNumber + 1) + ".  " + 
			Globals.lang("Please refer to the JabRef help manual on using the CiteSeer tools."),
			Globals.lang("CiteSeer Error"),
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

	class InitializeProgressBar implements Runnable {	    
	    public void run() {
			progressBar.setValue(0);		
	        progressBar.setMinimum(0);
			progressBar.setMaximum(100);	    
		    progressBar.setString(null);		        
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
	
	synchronized public boolean activateCitationFetcher() {
		if (citationFetcherActive == true) {
			return(false);	
		}	else {
			citationFetcherActive = true;
			return(true);	
		}
	}
	synchronized public void deactivateCitationFetcher() {
		citationFetcherActive = false;
	}

	synchronized public boolean activateImportFetcher() {
		if (importFetcherActive == true) {
			return(false);	
		}	else {
			importFetcherActive = true;
			return(true);	
		}
	}	
	synchronized public void deactivateImportFetcher() {
		importFetcherActive = false;
	}
	
	public void beginImportCiteSeerProgress() {
	    progressBar.setIndeterminate(true);
	    progressBar.setString("");
	    sidePaneManager.ensureVisible("CiteSeerProgress");		
	}
	public void endImportCiteSeerProgress() {
	    progressBar.setIndeterminate(false);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);	    
		progressBar.setValue(100);		
//	    progressBar.setString(null);	
	}
	

	/**
	 * @param newDatabase
	 * @param targetDatabase
	 */
	public void populate(BibtexDatabase newDatabase, BibtexDatabase targetDatabase) {
		Iterator targetIterator = targetDatabase.getKeySet().iterator();
		boolean abortOperation = false;
		String currentKey;
		BibtexEntry currentEntry;
		Enumeration newEntryEnum;
		Hashtable citationHashTable = new Hashtable();
		InitializeProgressBar initializeProgressBar = new InitializeProgressBar();
		SwingUtilities.invokeLater(initializeProgressBar);	
		while (targetIterator.hasNext() && !abortOperation) {
			currentKey = (String) targetIterator.next();
			currentEntry = targetDatabase.getEntryById(currentKey);
			abortOperation = generateIdentifierList(currentEntry, citationHashTable);
		}
		if (citationHashTable.size() > 0) {
			UpdateProgressBarMaximum updateMaximum = new UpdateProgressBarMaximum(citationHashTable.size());
			SwingUtilities.invokeLater(updateMaximum);
		}
		generateCitationList(citationHashTable, newDatabase);
		newEntryEnum = citationHashTable.elements();	
	}


	private Hashtable generateCitationList(Hashtable citationHashTable, BibtexDatabase database) 
	 {
		try {
			NamedCompound dummyNamedCompound = new NamedCompound("Import Data from CiteSeer Database");		    
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
				saxParser.parse(citeseerMethod.getResponseBodyAsStream(), new CiteSeerUndoHandler(dummyNamedCompound, newEntry, panel));
				citeseerMethod.releaseConnection();
				database.insertEntry(newEntry);
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
			} catch (KeyCollisionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		return citationHashTable;
	}

	private boolean generateIdentifierList(BibtexEntry currentEntry, Hashtable citationHashTable)
		{
	    	boolean abortOperation = false;
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
		    int row = panel.getTableModel().getNumberFromName(currentEntry.getId());
			ShowBadURLDialog dialog = new ShowBadURLDialog(targetURL, row);
			SwingUtilities.invokeLater(dialog);
		}
		} catch(HttpException e) {
			System.out.println("HttpException: " + e.getReason());			
			e.printStackTrace();			
		} catch (SAXException e) {
			System.out.println("SAXException: " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException: " + e.getLocalizedMessage());
		    ShowNoConnectionDialog dialog = new ShowNoConnectionDialog(OAI_HOST);
			abortOperation = true;
			SwingUtilities.invokeLater(dialog);
		}
		return(abortOperation);
	}

	/**
	 * @param be
	 * 
	 * Reminder: this method runs in the EventDispatcher thread
	 */
	public boolean importCiteSeerEntry(BibtexEntry be, NamedCompound citeseerNC) {
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
					saxParser.parse(citeseerMethod.getResponseBodyAsStream(), new CiteSeerUndoHandler(citeseerNC, be, panel));
					citeseerMethod.releaseConnection();				
					newValue = true;
				} else {	
				    int row = panel.getTableModel().getNumberFromName(be.getId());
					ShowBadURLDialog dialog = new ShowBadURLDialog(targetURL, row);				    
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
