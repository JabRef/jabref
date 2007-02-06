package net.sf.jabref.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.gui.ImportInspectionDialog;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * This class can be used to access any archive offering an OAI2 interface. By
 * default it will access ArXiv.org
 * 
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 * 
 * @version $Revision$ ($Date$)
 * 
 */
public class OAI2Fetcher implements EntryFetcher, Runnable {

	public static final String OAI2_ARXIV_PREFIXIDENTIFIER = "oai%3AarXiv.org%3A";

	public static final String OAI2_ARXIV_HOST = "arxiv.org";

	public static final String OAI2_ARXIV_SCRIPT = "oai2";

	public static final String OAI2_ARXIV_METADATAPREFIX = "arXiv";

	public static final String OAI2_ARXIV_ARCHIVENAME = "ArXiv.org";

	public static final String OAI2_IDENTIFIER_FIELD = "oai2identifier";

	private SAXParserFactory parserFactory;

	private SAXParser saxParser;

	private String oai2Host;

	private String oai2Script;

	private String oai2MetaDataPrefix;

	private String oai2PrefixIdentifier;

	private String oai2ArchiveName;

	private boolean shouldContinue = true;

	private String query;

	private ImportInspectionDialog dialog;

	private JabRefFrame frame;

	/* some archives - like arxive.org - might expect of you to wait some time */

	private boolean shouldWait() {
		return waitTime > 0;
	}

	private long waitTime = -1;

	private Date lastCall;

	/**
	 * 
	 * 
	 * @param oai2Host
	 *            the host to query without leading http:// and without trailing /
	 * @param oai2Script
	 *            the relative location of the oai2 interface without leading
	 *            and trailing /
	 * @param oai2Metadataprefix
	 *            the urlencoded metadataprefix
	 * @param oai2Prefixidentifier
	 *            the urlencoded prefix identifier
	 * @param waitTimeMs
	 *            Time to wait in milliseconds between query-requests.
	 */
	public OAI2Fetcher(String oai2Host, String oai2Script, String oai2Metadataprefix,
		String oai2Prefixidentifier, String oai2ArchiveName, long waitTimeMs) {
		this.oai2Host = oai2Host;
		this.oai2Script = oai2Script;
		this.oai2MetaDataPrefix = oai2Metadataprefix;
		this.oai2PrefixIdentifier = oai2Prefixidentifier;
		this.oai2ArchiveName = oai2ArchiveName;
		this.waitTime = waitTimeMs;
		try {
			parserFactory = SAXParserFactory.newInstance();
			saxParser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Default Constructor. The archive queried will be ArXiv.org
	 * 
	 */
	public OAI2Fetcher() {
		this(OAI2_ARXIV_HOST, OAI2_ARXIV_SCRIPT, OAI2_ARXIV_METADATAPREFIX,
			OAI2_ARXIV_PREFIXIDENTIFIER, OAI2_ARXIV_ARCHIVENAME, 20000L);
	}

	/**
	 * Construct the query URL
	 * 
	 * @param key
	 *            The key of the OAI2 entry that the url should poitn to.
	 *            
	 * @return a String denoting the query URL
	 */
	public String constructUrl(String key) {
		String identifier = "";
		try {
			identifier = URLEncoder.encode((String) key, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
		StringBuffer sb = new StringBuffer("http://").append(oai2Host).append("/");
		sb.append(oai2Script).append("?");
		sb.append("verb=GetRecord");
		sb.append("&identifier=");
		sb.append(oai2PrefixIdentifier);
		sb.append(identifier);
		sb.append("&metadataPrefix=").append(oai2MetaDataPrefix);
		return sb.toString();
	}
	
	/**
	 * Strip subccategories from ArXiv key.
	 * 
	 * @param key The key to fix.
	 * @return Fixed key.
	 */
	public static String fixKey(String key){
		int dot = key.indexOf('.');
		int slash = key.indexOf('/');
		
		if (dot > -1 && dot < slash)
			key = key.substring(0, dot) + key.substring(slash, key.length());
	
		return key;
	}

	public static String correctLineBreaks(String s){
		s = s.replaceAll("\\n(?!\\s*\\n)", " ");
		s = s.replaceAll("\\s*\\n\\s*", "\n");
		return s.replaceAll(" {2,}", " ").replaceAll("(^\\s*|\\s+$)", "");
	}
	
	/**
	 * Import an entry from an OAI2 archive. The BibtexEntry provided has to
	 * have the field OAI2_IDENTIFIER_FIELD set to the search string.
	 * 
	 * @param key
	 *            The OAI2 key to fetch from ArXiv.
	 * @return The imnported BibtexEntry or null if none.
	 */
	public BibtexEntry importOai2Entry(String key) {
		/**
		 * Fix for problem reported in mailing-list: 
		 *   https://sourceforge.net/forum/message.php?msg_id=4087158
		 */
		key = fixKey(key);
		
		String url = constructUrl(key);
		try {
			URL oai2Url = new URL(url);
			HttpURLConnection oai2Connection = (HttpURLConnection) oai2Url.openConnection();
			oai2Connection.setRequestProperty("User-Agent", "Jabref");
			InputStream inputStream = oai2Connection.getInputStream();
	
			/* create an empty BibtexEntry and set the oai2identifier field */
			BibtexEntry be = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.ARTICLE);
			be.setField(OAI2_IDENTIFIER_FIELD, key);
			DefaultHandler handlerBase = new OAI2Handler(be);
			/* parse the result */
			saxParser.parse(inputStream, handlerBase);
			
			/* Correct line breaks and spacing */
			Object[] fields = be.getAllFields();
			for (int i = 0; i < fields.length; i++){
				String name = fields[i].toString();
				
				be.setField(name, correctLineBreaks(be.getField(name).toString()));
			}
			return be;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame, Globals.lang(
				"An Exception ocurred while accessing '%0'", url)
				+ "\n\n" + e.toString(), Globals.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
		} catch (SAXException e) {
			JOptionPane.showMessageDialog(frame, Globals.lang(
				"An SAXException ocurred while parsing '%0':", url)
				+ "\n\n" + e.getMessage(), Globals.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
		} catch (RuntimeException e){
			JOptionPane.showMessageDialog(frame, Globals.lang(
				"An Error occurred while fetching from OAI2 source (%0):", url)
				+ "\n\n" + e.getMessage(), Globals.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
		} 
		return null;
	}

	public String getHelpPage() {
		// there is no helppage
		return null;
	}

	public URL getIcon() {
		return GUIGlobals.getIconUrl("www");
	}

	public String getKeyName() {
		return "Fetch " + oai2ArchiveName;
	}

	public JPanel getOptionsPanel() {
		// we have no additional options
		return null;
	}

	public String getTitle() {
		return Globals.menuTitle(getKeyName());
	}

	public void processQuery(String query, ImportInspectionDialog dialog, JabRefFrame frame) {
		this.query = query;
		this.dialog = dialog;
		this.frame = frame;
		(new Thread(this)).start();
	}

	public void cancelled() {
		shouldContinue = false;
	}

	public void done(int entriesImported) {
		// do nothing
	}

	public void stopFetching() {
		shouldContinue = false;
	}

	public void run() {
		try {
			dialog.setVisible(true);
			shouldContinue = true;
			/* multiple keys can be delimited by ; or space */
			query = query.replaceAll(" ", ";");
			String[] keys = query.split(";");
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				/*
				 * some archives - like arxive.org - might expect of you to wait
				 * some time
				 */
				if (shouldWait() && lastCall != null) {

					long elapsed = new Date().getTime() - lastCall.getTime();

					while (elapsed < waitTime) {
						frame.output("Waiting for ArXiv..." + ((waitTime - elapsed) / 1000) + " s");
						Thread.sleep(1000);
						elapsed = new Date().getTime() - lastCall.getTime();
					}
				}

				frame.output("Processing " + key);

				/* the cancel button has been hit */
				if (!shouldContinue)
					break;
				
				/* query the archive and load the results into the BibtexEntry */
				BibtexEntry be = importOai2Entry(key);

				if (shouldWait())
					lastCall = new Date();
				
				/* add the entry to the inspection dialog */
				if (be != null)
					dialog.addEntry(be);

				/* update the dialogs progress bar */
				dialog.setProgress(i + 1, keys.length);
			}
			/* inform the inspection dialog, that we're done */
			dialog.entryListComplete();
			frame.output("");
		} catch (Exception e) {
			frame.output("Error while fetching from OIA2: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
