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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    final static String CITESEER_HOST = "citeseer.ist.psu.edu";
	final static String PREFIX_URL = "http://" + CITESEER_HOST + "/";
	final static String PREFIX_IDENTIFIER = "oai:CiteSeerPSU:";
	final static String OAI_HOST = "http://cs1.ist.psu.edu/";
	final static String OAI_URL = OAI_HOST + "cgi-bin/oai.cgi?";
	final static String OAI_ACTION = "verb=GetRecord";
	final static String OAI_METADATAPREFIX ="metadataPrefix=oai_citeseer";
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
				Globals.lang("I could not connect to host") + " " + targetURL + ".  " +
				Globals.lang("Please check your network connection to this machine" + "."),
				Globals.lang("CiteSeer Error"),
				JOptionPane.ERROR_MESSAGE);
		}
	}

	class ShowBadIdentifiersDialog implements Runnable {

	    Hashtable rejectedEntries;
	    
	    ShowBadIdentifiersDialog(Hashtable entries) {
	        rejectedEntries = entries;
	    }
	    
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if (rejectedEntries.size() == 1) {
                
            } else if (rejectedEntries.size() > 1) {
                String rowNumbers = new String("");
                TreeSet rowSet = new TreeSet(rejectedEntries.keySet());
                for(int i=0; i < rowSet.size() - 1; i++) {
                    Integer next = (Integer) rowSet.first();
                    if (rowNumbers.equals(""))
                        rowNumbers = next.toString();
                    else
                        rowNumbers = rowNumbers + ", " + next.toString();
                    rowSet.remove(next);
                }
                rowNumbers = rowNumbers + " and " + ((Integer)rowSet.first()).toString();            
                JOptionPane.showMessageDialog(panel.frame(),
                        Globals.lang("I couldn't parse the CiteSeer URL field of the following entries") + '\n' + 
                        rowNumbers + ".\n" + 
                        Globals.lang("Please refer to the JabRef help manual on using the CiteSeer tools" + '.'),
                        Globals.lang("CiteSeer Warning"),
                        JOptionPane.WARNING_MESSAGE);
            }
        }
	}	
	
	class ShowBadIdentifierDialog implements Runnable {
		protected String badURL = "";
		protected int rowNumber;

		ShowBadIdentifierDialog(String URL, int row) {
			badURL = URL;
			rowNumber = row;
		}
		public void run() {
			JOptionPane.showMessageDialog(panel.frame(),
			Globals.lang("I couldn't seem to find an entry associated with this URL") + ": \"" + badURL + '\"' +
			Globals.lang(" on entry number ") + (rowNumber + 1) + ".  " +
			Globals.lang("Please refer to the JabRef help manual on using the CiteSeer tools."),
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

	class ShowMissingURLDialog implements Runnable {
		protected int rowNumber;

		ShowMissingURLDialog(int row) {
			rowNumber = row;
		}
		public void run() {
			JOptionPane.showMessageDialog(panel.frame(),
			Globals.lang("The URL field appears to be empty on entry number ") + (rowNumber + 1) + ".  " +
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
		Hashtable rejectedEntries = new Hashtable();
		InitializeProgressBar initializeProgressBar = new InitializeProgressBar();
		SwingUtilities.invokeLater(initializeProgressBar);
		while (targetIterator.hasNext() && !abortOperation) {
			currentKey = (String) targetIterator.next();
			currentEntry = targetDatabase.getEntryById(currentKey);
			abortOperation = generateIdentifierList(currentEntry, citationHashTable, rejectedEntries);
		}
		if (rejectedEntries.size() > 0) {
		    ShowBadIdentifiersDialog badIdentifiersDialog = new ShowBadIdentifiersDialog(rejectedEntries);
		    SwingUtilities.invokeLater(badIdentifiersDialog);
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
			BooleanAssign dummyBoolean = new BooleanAssign();
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
				saxParser.parse(citeseerMethod.getResponseBodyAsStream(), new CiteSeerUndoHandler(dummyNamedCompound, newEntry, panel, dummyBoolean));
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

	public static String generateCanonicalNumber(BibtexEntry be) {
	    return(generateCanonicalNumber((String) be.getField("citeseerurl")));
	}
	
    public static String generateCanonicalNumber(String link) {
        String IDnumber = null;
	    if (link != null) {
	        Pattern pattern = Pattern.compile("[0-9]+");
	        Matcher matcher = pattern.matcher(link);
	        if (matcher.find()) {
	            IDnumber = matcher.group();
	        }
	    }
        return IDnumber;
    }
	
	public String generateCanonicalIdentifier(BibtexEntry be) {
	    String canonID = null;
	    String IDnumber = generateCanonicalNumber(be);
	    if (IDnumber != null) {
	        canonID = PREFIX_IDENTIFIER + IDnumber;
	    }
	    return(canonID);
	}
	
	public static String generateCanonicalURL(String link) {
	    String canonURL = null;
	    String IDnumber = generateCanonicalNumber(link);
	    if (IDnumber != null) {
	        canonURL = PREFIX_URL + IDnumber + ".html";
	    }
	    return(canonURL);
	}	

    public static String generateCanonicalURL(BibtexEntry be) {
        return(generateCanonicalURL((String) be.getField("citeseerurl")));
	}
	
	private boolean generateIdentifierList(BibtexEntry currentEntry, Hashtable citationHashTable, Hashtable rejectedEntries)
		{
                  boolean abortOperation = false;
                  String identifier = generateCanonicalIdentifier(currentEntry);                  
                  try {
                    if (identifier != null) {
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
                      rejectedEntries.put(new Integer(row),currentEntry);                     
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
	 */
	public boolean importCiteSeerEntry(BibtexEntry be, NamedCompound citeseerNC) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		BooleanAssign newValue = new BooleanAssign();
    	String identifier = generateCanonicalIdentifier(be);			 
		try {
			if (identifier != null) {
					StringBuffer citeseerURLString = new StringBuffer();
					citeseerURLString.append(OAI_URL);
					citeseerURLString.append(OAI_ACTION);
					citeseerURLString.append("&" + OAI_METADATAPREFIX);
					citeseerURLString.append("&" + "identifier=" + identifier);
					GetMethod citeseerMethod = new GetMethod(citeseerURLString.toString());
					int response = citeseerHttpClient.executeMethod(citeseerMethod);		
					saxParser.parse(citeseerMethod.getResponseBodyAsStream(), new CiteSeerUndoHandler(citeseerNC, be, panel, newValue));
					citeseerMethod.releaseConnection();
					if (newValue.getValue() == false) {
					    int row = panel.getTableModel().getNumberFromName(be.getId());					    
					    ShowBadIdentifierDialog dialog = new ShowBadIdentifierDialog(generateCanonicalURL(be), row);
				        SwingUtilities.invokeLater(dialog);			    
					}
				} else {
				    int row = panel.getTableModel().getNumberFromName(be.getId());
				    if (be.getField("citeseerurl") == null) {
				        ShowMissingURLDialog dialog = new ShowMissingURLDialog(row);
				        SwingUtilities.invokeLater(dialog);
				    } else {
				        ShowBadURLDialog dialog = new ShowBadURLDialog((String) be.getField("citeseerurl"), row);
				        SwingUtilities.invokeLater(dialog);
				    }
				}
			} catch (HttpException e) {
				System.out.println("HttpException: " + e.getReason());
				e.printStackTrace();
			} catch (IOException e) {
					ShowNoConnectionDialog dialog = new ShowNoConnectionDialog(OAI_HOST);
					SwingUtilities.invokeLater(dialog);
			} catch (SAXException e) {
				System.out.println("SAXException: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
			return newValue.getValue();
		}



}
