/* Aaron Chen
 * 08-28-2007
 * ACM Digital Library support
 */

package net.sf.jabref.imports;

//import net.sf.jabref.net.URLDownload;
import net.sf.jabref.*;
import net.sf.jabref.gui.ImportInspectionDialog;
//import net.sf.jabref.journals.JournalAbbreviations;

import javax.swing.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.io.*;
//import java.text.NumberFormat;
//import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.awt.*;
//import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 25, 2006
 * Time: 1:09:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ACMDigitalLibraryFetcher implements EntryFetcher {

	ImportInspector dialog = null;
	OutputPrinter status;
    HTMLConverter htmlConverter = new HTMLConverter();
//	JournalAbbreviations journalAbbrev = new JournalAbbreviations("/resource/AcmRisJournalList.txt");
    private String terms;
    String startUrl = "http://portal.acm.org/";
    String searchUrlPart = "results.cfm?query=";
    String searchUrlPartII = "&dl=";
    String endUrl = "&coll=ACM&short=1";//&start=";
    private int perPage = 20, hits = 0, unparseable = 0, parsed = 0;
    private boolean shouldContinue = false;
    private JRadioButton acmButton = new JRadioButton(Globals.lang("The ACM Digital Library"));
    private JRadioButton guideButton = new JRadioButton(Globals.lang("The Guide"));
    
    
    private JCheckBox fetchAstracts = new JCheckBox(Globals.lang("Include abstracts"), false);
    private boolean fetchingAbstracts = false;
    private boolean acmOrGuide = false;
//    private static final int MAX_ABSTRACT_FETCH = 5;
    private static final int MAX_FETCH = 20;

    //Pattern hitsPattern = Pattern.compile("Your search matched <strong>(\\d+)</strong>");
    Pattern hitsPattern = Pattern.compile(".*Found <b>(\\d+.*,*\\d+.*)</b> of.*");
    Pattern maxHitsPattern = Pattern.compile(".*<td>Results \\d+ - \\d+ of (\\d+,*\\d+)</td>.*");
    Pattern risPattern = Pattern.compile(".*(popBibTex.cfm.*)','BibTex'.*");
    Pattern absPattern = Pattern.compile(".*ABSTRACT</A></span>\\s+<p class=\"abstract\">\\s+(.*)");
    
    Pattern entryPattern1 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                +"\\s+<A href='(.+)'>(.+)</A><br>\\s+Volume (.+),&nbsp;\\s*"
                +"(.+)?\\s?(\\d\\d\\d\\d)\\s+Page\\(s\\):.*");

    Pattern entryPattern2 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                    +"\\s+<A href='(.+)'>(.+)</A><br>\\s+Volume (.+),&nbsp;\\s+.*Issue (\\d+).*,&nbsp;\\s*"
                    +"(.+)? (\\d\\d\\d\\d)\\s+Page\\(s\\):.*");


    Pattern entryPattern3 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                    +"\\s+<A href='(.+)'>(.+)</A><br>\\s+Volume (.+),&nbsp;\\s+Issue (\\d+),&nbsp;" +
                    "\\s+Part (\\d+),&nbsp;\\s*" //"[\\s-\\d]+"
                    +"(.+)? (\\d\\d\\d\\d)\\s+Page\\(s\\):.*");

    Pattern entryPattern4 = Pattern.compile(".*<strong>(.+)</strong><br>\\s+(.+)<br>"
                    +"\\s+<A href='(.+)'>(.+)</A><br>\\s*" //[\\s-\\da-z]+"
                    +"(.+)? (\\d\\d\\d\\d)\\s+Page\\(s\\):.*");

    Pattern fullCitationPattern =
        Pattern.compile("<A HREF=\"(citation.cfm.*)\" class.*");

    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());

        acmButton.setSelected(true);
        
        ButtonGroup group = new ButtonGroup();
        group.add(acmButton);
        group.add(guideButton);
        pan.add(fetchAstracts, BorderLayout.NORTH);
        pan.add(acmButton, BorderLayout.CENTER);
        pan.add(guideButton, BorderLayout.SOUTH);
        

        return pan;
    }

    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        this.dialog = dialog;
        this.status = status;
        this.terms = query;
        piv = 0;
        shouldContinue = true;
        parsed = 0;
        unparseable = 0;
        acmOrGuide = acmButton.isSelected();
        String address = makeUrl(0);
        try {
            URL url = new URL(address);
            // Fetch the search page and put the contents in a String:
            //String page = getResultsFromFile(new File("/home/alver/div/temp.txt"));
            //URLDownload ud = new URLDownload(new JPanel(), url, new File("/home/alver/div/temp.txt"));
            //ud.download();

            //dialog.setVisible(true);
            String page = getResults(url);
            //System.out.println(address);
            hits = getNumberOfHits(page, "Found", hitsPattern);
            //System.out.println(page);
            //System.out.printf("Hit %d\n", hits);
            
            if (hits == 0) {
                status.showMessage(Globals.lang("No entries found for the search string '%0'",
                        terms),
                        Globals.lang("Search ACM Digital Library"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            int maxHits = getNumberOfHits(page, "<td>Results", maxHitsPattern);
            //System.out.printf("maxHit %d\n", maxHits);
            //String page = getResultsFromFile(new File("/home/alver/div/temp50.txt"));

            //List entries = new ArrayList();
            //System.out.println("Number of hits: "+hits);
            //System.out.println("Maximum returned: "+maxHits);
            if (hits > maxHits)
                hits = maxHits;
            
            if (hits > MAX_FETCH) {
                status.showMessage(Globals.lang("%0 entries found. To reduce server load, "
                        +"only %1 will be downloaded.",
                                new String[] {String.valueOf(hits), String.valueOf(MAX_FETCH)}),
                        Globals.lang("Search ACM Digital Library"), JOptionPane.INFORMATION_MESSAGE);
                hits = MAX_FETCH;
            }
        
            fetchingAbstracts = fetchAstracts.isSelected();
            //parse(dialog, page, 0, 51);
            //dialog.setProgress(perPage/2, hits);
            parse(dialog, page, 0, 1);
            //System.out.println(page);
            int firstEntry = perPage;
            while (shouldContinue && (firstEntry < hits)) {
                //System.out.println("Fetching from: "+firstEntry);
                address = makeUrl(firstEntry);
                //System.out.println(address);
                page = getResults(new URL(address));
                
                //dialog.setProgress(firstEntry+perPage/2, hits);
                if (!shouldContinue)
                    break;

                parse(dialog, page, 0, 1+firstEntry);
                firstEntry += perPage;

            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            status.showMessage(Globals.lang("Connection to ACM Digital Library failed"),
                    Globals.lang("Search ACM Digital Library"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
        	status.showMessage(Globals.lang(e.getMessage()),
                    Globals.lang("Search ACM Digital Library"), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return false;
    }

    private String makeUrl(int startIndex) {
        StringBuffer sb = new StringBuffer(startUrl).append(searchUrlPart);
        sb.append(terms.replaceAll(" ", "%20"));
        sb.append(searchUrlPartII);
        if (acmOrGuide)
        	sb.append("ACM");
        else
        	sb.append("GUIDE");
        sb.append(endUrl);
        //sb.append(String.valueOf(startIndex));
        return sb.toString();
    }

    int piv = 0;

    private void parse(ImportInspector dialog, String text, int startIndex, int firstEntryNumber) {
        piv = startIndex;
        int entryNumber = firstEntryNumber;
        BibtexEntry entry;
        while (((entry = parseNextEntry(text, piv, entryNumber)) != null)
            && (shouldContinue)) {
            if (entry.getField("title") != null) {
                dialog.addEntry(entry);
                dialog.setProgress(parsed+unparseable, hits);
                parsed++;
            }
            entryNumber++;
            //break;
        }
    }

    private BibtexEntry parseEntryRis(String fullCitation, boolean abs)
        throws IOException
    {
        URL url;
        try {
            url = new URL(startUrl + fullCitation);
        	String page = getResults(url);
			Matcher bibtexAddr = risPattern.matcher(page);
			if (bibtexAddr.find()) {
				URL bibtexUrl = new URL(startUrl + bibtexAddr.group(1));
				BufferedReader in = new BufferedReader(new InputStreamReader(bibtexUrl.openStream()));
				ParserResult result = BibtexParser.parse(in);
				in.close();
				Collection item = result.getDatabase().getEntries();
				BibtexEntry entry = (BibtexEntry)item.iterator().next();
				if (abs == true) {
					Matcher absMatch = absPattern.matcher(page);
					if (absMatch.find()) {
						String absBlock = absMatch.group(1);
						/*
						absBlock = absBlock.replaceAll("<i>", "\\$");
						absBlock = absBlock.replaceAll("</i><sub>", "_");
						absBlock = absBlock.replaceAll("<sub>", "_");
						absBlock = absBlock.replaceAll("</i>", "\\$");
						absBlock = absBlock.replaceAll("</sub>", "");
						absBlock = absBlock.replaceAll("</?p.*>", "");
						*/
						//absBlock = absBlock.replaceAll("&gt;", ">");
						//absBlock = absBlock.replaceAll("&lt;", "<");
						//absBlock = absBlock.replaceAll("&#(\\d+);", "\\\\u$1");
						
						entry.setField("abstract", convertHTMLChars(absBlock).trim());
					} else {
						//System.out.println("No abstract matched.");
						//System.out.println(page);
					}
				}
				return entry;
			} else
				return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (ConnectException e) {
            e.printStackTrace();
            return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    }

    private BibtexEntry parseNextEntry(String allText, int startIndex, int entryNumber)
    {
        String toFind = new StringBuffer().append("<strong>")
                .append(entryNumber).append("</strong>").toString();
        int index = allText.indexOf(toFind, startIndex);
        int endIndex = allText.indexOf("</table>", index+1);
        //if (endIndex < 0)
            endIndex = allText.length();

        BibtexEntry entry = null;

        if (index >= 0) {
            piv = index+1;
            String text = allText.substring(index, endIndex);
            // Always try RIS import first
			Matcher fullCitation =
				fullCitationPattern.matcher(text);
			if (fullCitation.find()) {
				try {
					entry = parseEntryRis(fullCitation.group(1), fetchingAbstracts);
				} catch (IOException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			} else {
				System.out.printf("Citation Unmatched %d\n", entryNumber);
				System.out.printf(text);
			}
            if (entry != null) { // fetch successful
                return entry;
            }
        }
        //System.out.println(allText);
        //System.out.println(toFind);
        //System.out.println("Parse Failed");
        return null;
    }

    /**
     * This method must convert HTML style char sequences to normal characters.
     * @param text The text to handle.
     * @return The converted text.
     */
    private String convertHTMLChars(String text) {

        return htmlConverter.format(text);
    }


    /**
     * Find out how many hits were found.
     * @param page
     */
    private int getNumberOfHits(String page, String marker, Pattern pattern) throws IOException {
        int ind = page.indexOf(marker);
        if (ind < 0)
            throw new IOException(Globals.lang("Could not parse number of hits"));
        String substring = page.substring(ind, Math.min(ind + 42, page.length()));
        Matcher m = pattern.matcher(substring);
        if (!m.find()) {
        	System.out.println("Unmatched!");
        	//System.out.println(substring);
        }
        if (m.groupCount() >= 1) {
            try {
            	// get rid of ,
            	String number = m.group(1);
            	//NumberFormat nf = NumberFormat.getInstance();
            	//return nf.parse(number).intValue();
            	number = number.replaceAll(",", "");
            	//System.out.println(number);
                return Integer.parseInt(number);
            } catch (NumberFormatException ex) {
                throw new IOException(Globals.lang("Could not parse number of hits"));
            } catch (IllegalStateException e) {
                throw new IOException(Globals.lang("Could not parse number of hits"));
            }
        }
        throw new IOException(Globals.lang("Could not parse number of hits"));
    }

    /**
     * Download the URL and return contents as a String.
     * @param source
     * @return
     * @throws IOException
     */
    public String getResults(URL source) throws IOException {
        
        InputStream in = source.openStream();
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[256];
        while(true) {
            int bytesRead = in.read(buffer);
            if(bytesRead == -1) break;
            for (int i=0; i<bytesRead; i++)
                sb.append((char)buffer[i]);
        }
        return sb.toString();
    }

    /**
     * Read results from a file instead of an URL. Just for faster debugging.
     * @param f
     * @return
     * @throws IOException
     */
    public String getResultsFromFile(File f) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[256];
        while(true) {
            int bytesRead = in.read(buffer);
            if(bytesRead == -1) break;
            for (int i=0; i<bytesRead; i++)
                sb.append((char)buffer[i]);
        }
        return sb.toString();
    }

	public String getTitle() {
	    return Globals.menuTitle("Search ACM Digital Library");
	}
	
	
	public URL getIcon() {
	    return GUIGlobals.getIconUrl("www");
	}
	
	public String getHelpPage() {
	    return "ACMDigitalLibraryHelp.html";
	}
	
	public String getKeyName() {
	    return "Search ACM Digital Library";
	}
	
	// This method is called by the dialog when the user has cancelled the import.
	public void cancelled() {
	    shouldContinue = false;
	}
	
	// This method is called by the dialog when the user has selected the
	//wanted entries, and clicked Ok. The callback object can update status
	//line etc.
	public void done(int entriesImported) {
	    //System.out.println("Number of entries parsed: "+parsed);
	    //System.out.println("Parsing failed for "+unparseable+" entries");
	}
	
	// This method is called by the dialog when the user has cancelled or
	//signalled a stop. It is expected that any long-running fetch operations
	//will stop after this method is called.
	public void stopFetching() {
	    shouldContinue = false;
	}

    
}
