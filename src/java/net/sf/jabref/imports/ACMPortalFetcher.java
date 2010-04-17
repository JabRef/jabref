/* Aaron Chen
 * 08-28-2007
 * ACM Portal support
 */

package net.sf.jabref.imports;

import java.awt.GridLayout;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;

public class ACMPortalFetcher implements EntryFetcher {

	ImportInspector dialog = null;
	OutputPrinter status;
    final HTMLConverter htmlConverter = new HTMLConverter();
    private String terms;
    String startUrl = "http://portal.acm.org/";
    String searchUrlPart = "results.cfm?query=";
    String searchUrlPartII = "&dl=";
    String endUrl = "&coll=Portal&short=0";//&start=";

    private JRadioButton acmButton = new JRadioButton(Globals.lang("The ACM Digital Library"));
    private JRadioButton guideButton = new JRadioButton(Globals.lang("The Guide to Computing Literature"));
    private JCheckBox absCheckBox = new JCheckBox(Globals.lang("Include abstracts"), false);
    
    private static final int MAX_FETCH = 20; // 20 when short=0
    private int perPage = MAX_FETCH, hits = 0, unparseable = 0, parsed = 0;
    private boolean shouldContinue = false;
    private boolean fetchAbstract = false;
    private boolean acmOrGuide = false;

    Pattern hitsPattern = Pattern.compile(".*Found <b>(\\d+,*\\d*)</b> of.*");
    Pattern maxHitsPattern = Pattern.compile(".*Results \\d+ - \\d+ of (\\d+,*\\d*).*");
    Pattern bibPattern = Pattern.compile(".*(popBibTex.cfm.*)','BibTex'.*");
    Pattern absPattern = Pattern.compile(".*ABSTRACT</A></span>\\s+<p class=\"abstract\">\\s+(.*)");
    
    Pattern fullCitationPattern =
        Pattern.compile("<A HREF=\"(citation.cfm.*)\" class.*");

    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new GridLayout(0,1));

        guideButton.setSelected(true);
        
        ButtonGroup group = new ButtonGroup();
        group.add(acmButton);
        group.add(guideButton);
        
        pan.add(absCheckBox);
        pan.add(acmButton);
        pan.add(guideButton);
        
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

            //dialog.setVisible(true);
            String page = getResults(url);
            //System.out.println(address);
            hits = getNumberOfHits(page, "Found", hitsPattern);
			int index = page.indexOf("Found");
			if (index >= 0) {
            	page = page.substring(index + 5);
				index = page.indexOf("Found");
				if (index >= 0)
            		page = page.substring(index);
			}
            //System.out.println(page);
            //System.out.printf("Hit %d\n", hits);
            
            if (hits == 0) {
                status.showMessage(Globals.lang("No entries found for the search string '%0'",
                        terms),
                        Globals.lang("Search ACM Portal"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            int maxHits = getNumberOfHits(page, "Results", maxHitsPattern);
            //System.out.printf("maxHit %d\n", maxHits);
            //String page = getResultsFromFile(new File("/home/alver/div/temp50.txt"));

            //List entries = new ArrayList();
            //System.out.println("Number of hits: "+hits);
            //System.out.println("Maximum returned: "+maxHits);
            if (hits > maxHits)
                hits = maxHits;
            
            if (hits > MAX_FETCH) {
                status.showMessage(Globals.lang("%0 entries found. To reduce server load, "
                        +"only %1 will be downloaded. It will be very slow, in order to make ACM happy.",
                                new String[] {String.valueOf(hits), String.valueOf(MAX_FETCH)}),
                        Globals.lang("Search ACM Portal"), JOptionPane.INFORMATION_MESSAGE);
                hits = MAX_FETCH;
            }
        
            fetchAbstract = absCheckBox.isSelected();
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
            status.showMessage(Globals.lang("Connection to ACM Portal failed"),
                    Globals.lang("Search ACM Portal"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
        	status.showMessage(Globals.lang(e.getMessage()),
                    Globals.lang("Search ACM Portal"), JOptionPane.ERROR_MESSAGE);
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
                dialog.setProgress(parsed + unparseable, hits);
                parsed++;
            }
            entryNumber++;
            try {
            	Thread.sleep(10000);//wait between requests or you will be blocked by ACM
            } catch (InterruptedException e) {
            	System.err.println(e.getStackTrace());
            }
        }
    }

    private BibtexEntry parseEntryBibTeX(String fullCitation, boolean abs) throws IOException {
        URL url;
        try {
            url = new URL(startUrl + fullCitation);
        	String page = getResults(url);
			Thread.sleep(10000);//wait between requests or you will be blocked by ACM
			Matcher bibtexAddr = bibPattern.matcher(page);
			if (bibtexAddr.find()) {
				URL bibtexUrl = new URL(startUrl + bibtexAddr.group(1));
				BufferedReader in = new BufferedReader(new InputStreamReader(bibtexUrl.openStream()));
				ParserResult result = BibtexParser.parse(in);
				in.close();
				Collection<BibtexEntry> item = result.getDatabase().getEntries();
				BibtexEntry entry = item.iterator().next();
				if (abs == true) {
					Matcher absMatch = absPattern.matcher(page);
					if (absMatch.find()) {
						String absBlock = absMatch.group(1);
						entry.setField("abstract", convertHTMLChars(absBlock).trim());
					} else {
						System.out.println("No abstract matched.");
						//System.out.println(page);
					}
				}
				
				Thread.sleep(10000);//wait between requests or you will be blocked by ACM
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
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
    }

    private BibtexEntry parseNextEntry(String allText, int startIndex, int entryNumber) {
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
					Thread.sleep(10000);//wait between requests or you will be blocked by ACM
					entry = parseEntryBibTeX(fullCitation.group(1), fetchAbstract);
				} catch (Exception e) {
					e.printStackTrace();
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
        if (ind < 0) {
        	System.out.println(page);
            throw new IOException(Globals.lang("Could not parse number of hits"));
        }
        String substring = page.substring(ind, Math.min(ind + 42, page.length()));
        Matcher m = pattern.matcher(substring);
        if (!m.find()) {
        	System.out.println("Unmatched!");
        	System.out.println(substring);
        } else {
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
	    return Globals.menuTitle("Search ACM Portal");
	}
	
	
	public URL getIcon() {
	    return GUIGlobals.getIconUrl("www");
	}
	
	public String getHelpPage() {
	    return "ACMPortalHelp.html";
	}
	
	public String getKeyName() {
	    return "Search ACM Portal";
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
