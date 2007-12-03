/*
 * Created on Jun 13, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sf.jabref.imports;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.KeyCollisionException;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.Util;
import net.sf.jabref.undo.NamedCompound;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
	
	boolean citationFetcherActive;
	boolean importFetcherActive;

	JProgressBar progressBar, progressBar2;
	JLabel citeSeerProgress;
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints con = new GridBagConstraints();
	SidePaneManager sidePaneManager;


	public CiteSeerFetcher(SidePaneManager p0)  {
		super(p0, GUIGlobals.getIconUrl("citeseer"), Globals.lang("CiteSeer Transfer"));

		sidePaneManager = p0;
		progressBar = new JProgressBar();
		progressBar2 = new JProgressBar();
		citeSeerProgress = new JLabel();
		progressBar.setValue(0);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setStringPainted(true);
		progressBar2.setValue(0);
		progressBar2.setMinimum(0);
		progressBar2.setMaximum(100);
		progressBar2.setStringPainted(true);
                JPanel main = new JPanel();
		main.setLayout(gbl);
		//SidePaneHeader header = new SidePaneHeader
		//	("CiteSeer Transfer", GUIGlobals.wwwCiteSeerIcon, this);
		con.gridwidth = GridBagConstraints.REMAINDER;			
		con.fill = GridBagConstraints.BOTH;
		con.weightx = 1;
		con.insets = new Insets(0, 0, 2,  0);
		//gbl.setConstraints(header, con);
		//add(header);
		con.insets = new Insets(0, 0, 0,  0);
		con.fill = GridBagConstraints.HORIZONTAL;		
		gbl.setConstraints(progressBar, con);
		main.add(progressBar);		
		gbl.setConstraints(progressBar2, con);
		main.add(progressBar2);
		gbl.setConstraints(citeSeerProgress, con);
		main.add(citeSeerProgress);		
                main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
                add(main, BorderLayout.CENTER);
		try {
			citationFetcherActive = false;
			importFetcherActive = false;
			parserFactory = SAXParserFactory.newInstance();
			saxParser = parserFactory.newSAXParser();

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

	class ShowEmptyFetchSetDialog implements Runnable {
	
		public void run() {
			JOptionPane.showMessageDialog(panel.frame(),
				Globals.lang("The CiteSeer fetch operation returned zero results" 
						+ "."),
				"CiteSeer",
				JOptionPane.INFORMATION_MESSAGE);
			deactivateCitationFetcher();
		}		
	}

	public ShowEmptyFetchSetDialog getEmptyFetchSetDialog() {
		return(new ShowEmptyFetchSetDialog());
	}
	
	class ShowNoConnectionDialog implements Runnable {
		protected String targetURL = "";
		ShowNoConnectionDialog(String URL) {
				targetURL = URL;
		}
		public void run() {
				JOptionPane.showMessageDialog(panel.frame(),
				Globals.lang("Could not connect to host") + " " + targetURL + ".  " +
				Globals.lang("Please check your network connection to this machine" + "."),
				Globals.lang("CiteSeer Error"),
				JOptionPane.ERROR_MESSAGE);
		}
	}

	class ShowBadIdentifiersDialog implements Runnable {

	    Hashtable<Integer, BibtexEntry> rejectedEntries;
	    
	    ShowBadIdentifiersDialog(Hashtable<Integer, BibtexEntry> entries) {
	        rejectedEntries = entries;
	    }
	    
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if (rejectedEntries.size() == 1) {
                
            } else if (rejectedEntries.size() > 1) {
            	int i;
                String rowNumbers = "";
                String oneRowOfNumbers = "";
                TreeSet<Integer> rowSet = new TreeSet<Integer>(rejectedEntries.keySet());
                int rowSize = rowSet.size();
                for(i=0; (i < rowSize - 1) && (i < 100); i++) {
                    Integer next = rowSet.first();
                    if (oneRowOfNumbers.equals(""))
                        oneRowOfNumbers = next.toString();
                    else {
                        oneRowOfNumbers = oneRowOfNumbers + ", ";
                        if (oneRowOfNumbers.length() > 50) {
                        	oneRowOfNumbers = oneRowOfNumbers + "\n";
                        	rowNumbers = rowNumbers + oneRowOfNumbers;
                        	oneRowOfNumbers = "";
                        }
                        oneRowOfNumbers = oneRowOfNumbers + next.toString();
                    }
                    rowSet.remove(next);
                }
            	rowNumbers = rowNumbers + oneRowOfNumbers;
            	if (i == 100) {
            		rowNumbers = rowNumbers + "..";
            	} else {
            		rowNumbers = rowNumbers + " "+Globals.lang("and")+" " + rowSet.first().toString();
            	}
                JOptionPane.showMessageDialog(panel.frame(),
                        Globals.lang("Couldn't parse the 'citeseerurl' field of the following entries") + ':' + '\n' + 
                        rowNumbers + ".\n" + 
                        Globals.lang("Please refer to the JabRef help manual on using the CiteSeer tools" + '.'),
                        Globals.lang("Warning"),
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
			Globals.lang("Couldn't find an entry associated with this URL") + ": \"" + badURL + '\"' +
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
			Globals.lang("Unable to parse the following URL") + ": \"" + badURL + '\"' +
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

	class UpdateProgressBarTwoMaximum implements Runnable {
		protected int maximum;
		UpdateProgressBarTwoMaximum(int newValue) {
			maximum = newValue;
		}
		public void run() {
			progressBar2.setMaximum(maximum);
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

	class InitializeProgressBarTwo implements Runnable {

		public void run() {
			progressBar2.setValue(0);
	        progressBar2.setMinimum(0);
			progressBar2.setMaximum(100);
		    progressBar2.setString(null);
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

	class UpdateProgressBarTwoValue implements Runnable {
		protected int counter;
		UpdateProgressBarTwoValue(int newValue) {
			counter = newValue;
		}
		public void run() {
			progressBar2.setValue(counter);
		}		
	}

	class UpdateProgressStatus implements Runnable {
		protected String status;
		UpdateProgressStatus(String newStatus) {
			status = newStatus;
		}
		public void run() {
			citeSeerProgress.setText(status);
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
	    progressBar2.setVisible(false);
	    citeSeerProgress.setText("");
	    sidePaneManager.show("CiteSeerProgress");
	}
	public void endImportCiteSeerProgress() {
	    progressBar.setIndeterminate(false);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setValue(100);
	}


	/**
	 * @param newDatabase
	 * @param targetDatabase
	 */
	public int populate(BibtexDatabase newDatabase, BibtexDatabase targetDatabase) {
		int errorCode = 0;
		Iterator<String> targetIterator = targetDatabase.getKeySet().iterator();
		boolean abortOperation = false;
		String currentKey;
		BibtexEntry currentEntry;
		Map<String, Boolean> citationHashTable = new HashMap<String, Boolean>();
		Hashtable<Integer, BibtexEntry> rejectedEntries = new Hashtable<Integer, BibtexEntry>();
		InitializeProgressBar initializeProgressBar = new InitializeProgressBar();
		InitializeProgressBarTwo initializeProgressBarTwo = new InitializeProgressBarTwo();
		UpdateProgressBarMaximum updateMaximum = new UpdateProgressBarMaximum(targetDatabase.getEntryCount());		
	    progressBar2.setVisible(true);		
		SwingUtilities.invokeLater(initializeProgressBar);
		SwingUtilities.invokeLater(initializeProgressBarTwo);
		SwingUtilities.invokeLater(updateMaximum);		
		int identifierCounter = 0;
		UpdateProgressStatus progressStatus = new UpdateProgressStatus(Globals.lang("Fetching Identifiers"));
		SwingUtilities.invokeLater(progressStatus);
		while (targetIterator.hasNext() && !abortOperation) {
			currentKey = targetIterator.next();
			currentEntry = targetDatabase.getEntryById(currentKey);
			abortOperation = generateIdentifierList(currentEntry, citationHashTable, rejectedEntries);
			UpdateProgressBarValue updateValue = new UpdateProgressBarValue(++identifierCounter);			
			SwingUtilities.invokeLater(updateValue);
		}
		if (rejectedEntries.size() > 0) {
			errorCode = -1;
		    ShowBadIdentifiersDialog badIdentifiersDialog = new ShowBadIdentifiersDialog(rejectedEntries);
		    SwingUtilities.invokeLater(badIdentifiersDialog);
		}
		if (citationHashTable.size() > 0) {
			UpdateProgressBarTwoMaximum update2Maximum = new UpdateProgressBarTwoMaximum(citationHashTable.size());
			SwingUtilities.invokeLater(update2Maximum);
		}
		progressStatus = new UpdateProgressStatus(Globals.lang("Fetching Citations"));
		SwingUtilities.invokeLater(progressStatus);		
		generateCitationList(citationHashTable, newDatabase);
		progressStatus = new UpdateProgressStatus(Globals.lang("Done"));
		SwingUtilities.invokeLater(progressStatus);
		if (abortOperation)
			errorCode = -2;
		return(errorCode);
	}


	private Map<String, Boolean> generateCitationList(Map<String, Boolean> citationHashTable, BibtexDatabase database)
	 {
		try {
			NamedCompound dummyNamedCompound = new NamedCompound(Globals.lang("Import Data from CiteSeer Database"));
			BooleanAssign dummyBoolean = new BooleanAssign(false);
			if ((citationHashTable != null) && (citationHashTable.size() > 0)) {
			    int citationCounter=0;
			    for (String key : citationHashTable.keySet()){
			    	String id = Util.createNeutralId();
					BibtexEntry newEntry = new BibtexEntry(id);
					StringBuffer citeseerURLString = new StringBuffer();
					citeseerURLString.append(OAI_URL);
					citeseerURLString.append(OAI_ACTION);
					citeseerURLString.append("&" + OAI_METADATAPREFIX);
                    citeseerURLString.append("&" + "identifier=").append(key);
					URL citeseerUrl = new URL( citeseerURLString.toString());
					HttpURLConnection citeseerConnection = (HttpURLConnection)citeseerUrl.openConnection();				
					saxParser.parse(citeseerConnection.getInputStream(), new CiteSeerUndoHandler(dummyNamedCompound, newEntry, panel, dummyBoolean));
					database.insertEntry(newEntry);
					citationCounter++;
					UpdateProgressBarTwoValue updateValue = new UpdateProgressBarTwoValue(citationCounter);
					SwingUtilities.invokeLater(updateValue);
			    }
			}
		} catch (SAXException e) {
				System.out.println("SAXException: " + e.getLocalizedMessage());
				e.printStackTrace();
		} catch (IOException e) {
				ShowNoConnectionDialog dialog = new ShowNoConnectionDialog(OAI_HOST);
				SwingUtilities.invokeLater(dialog);
		} catch (KeyCollisionException e) {
		    	System.out.println("KeyCollisionException: " + e.getLocalizedMessage());
                e.printStackTrace();
        }
		return citationHashTable;
	}

	public static String generateCanonicalNumber(BibtexEntry be) {
	    return(generateCanonicalNumber(be.getField("citeseerurl")));
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
        return(generateCanonicalURL(be.getField("citeseerurl")));
	}
	
	private boolean generateIdentifierList(BibtexEntry currentEntry, Map<String, Boolean> citationHashTable, Hashtable<Integer, BibtexEntry> rejectedEntries)
		{
                  boolean abortOperation = false;
                  String identifier = generateCanonicalIdentifier(currentEntry);                  
                  try {
                    if (identifier != null) {
                      StringBuffer citeseerURLString = new StringBuffer();
                      citeseerURLString.append(OAI_URL);
                      citeseerURLString.append(OAI_ACTION);
                      citeseerURLString.append("&" + OAI_METADATAPREFIX);
                        citeseerURLString.append("&" + "identifier=").append(identifier);
                      URL citeseerUrl = new URL( citeseerURLString.toString());
                      HttpURLConnection citeseerConnection = (HttpURLConnection)citeseerUrl.openConnection();				
                      saxParser.parse(citeseerConnection.getInputStream(), new CiteSeerCitationHandler(citationHashTable));
                    } else {
                      int row = panel.mainTable.findEntry(currentEntry);
                      rejectedEntries.put(new Integer(row+1),currentEntry);                     
                    }
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

	public boolean importCiteSeerEntries(int[] clickedOn, NamedCompound citeseerNamedCompound) {
	  	boolean newValues = false;
	  	boolean abortOperation = false;
	  	Vector<Integer> clickedVector = new Vector<Integer>();
		Hashtable<Integer, BibtexEntry> rejectedEntries = new Hashtable<Integer, BibtexEntry>();		
		for(int i=0; i < clickedOn.length; i++)
			clickedVector.add(new Integer(clickedOn[i]));
		Iterator<Integer> clickedIterator = clickedVector.iterator();
		BooleanAssign overwriteAll = new BooleanAssign(false);
		BooleanAssign overwriteNone = new BooleanAssign(false);

		while (clickedIterator.hasNext() && !abortOperation) {
			int currentIndex = clickedIterator.next().intValue();
			BooleanAssign newValue = new BooleanAssign(false);
			BibtexEntry be = panel.mainTable.getEntryAt(currentIndex);
			abortOperation = importCiteSeerEntry(be, citeseerNamedCompound, overwriteAll, overwriteNone, newValue, rejectedEntries);
			if (newValue.getValue())
				newValues = true;
		}
		if (rejectedEntries.size() > 0) {
		    ShowBadIdentifiersDialog badIdentifiersDialog = new ShowBadIdentifiersDialog(rejectedEntries);
		    SwingUtilities.invokeLater(badIdentifiersDialog);
		}
		return newValues;
	}
	
	
	
	/**
	 * @param be
	 * @param overwriteNone
	 * @param overwriteAll
	 * @param rejectedEntries
	 *
	 */
	public boolean importCiteSeerEntry(BibtexEntry be, NamedCompound citeseerNC, BooleanAssign overwriteAll, 
			BooleanAssign overwriteNone, BooleanAssign newValue, Hashtable<Integer, BibtexEntry> rejectedEntries) {
	    boolean abortOperation = false;
		
    	String identifier = generateCanonicalIdentifier(be);			 
		try {
			if (identifier != null) {
					StringBuffer citeseerURLString = new StringBuffer();
					citeseerURLString.append(OAI_URL);
					citeseerURLString.append(OAI_ACTION);
					citeseerURLString.append("&" + OAI_METADATAPREFIX);
                citeseerURLString.append("&" + "identifier=").append(identifier);
                    URL citeseerUrl = new URL( citeseerURLString.toString());
                    HttpURLConnection citeseerConnection = (HttpURLConnection)citeseerUrl.openConnection();									
					InputStream inputStream  = citeseerConnection.getInputStream();
					DefaultHandler handlerBase = new CiteSeerUndoHandler(citeseerNC, be, panel, newValue, overwriteAll, overwriteNone);

					saxParser.parse(inputStream, handlerBase);
				} else {
                    int row = panel.mainTable.findEntry(be);
                    rejectedEntries.put(new Integer(row+1), be);                
				}
			} catch (IOException e) {
					ShowNoConnectionDialog dialog = new ShowNoConnectionDialog(OAI_HOST);
					SwingUtilities.invokeLater(dialog);
					abortOperation = true;
			} catch (SAXException e) {
				System.out.println("SAXException: " + e.getLocalizedMessage());
				e.printStackTrace();
				abortOperation = true;				
			}
			return abortOperation;
		}



}
