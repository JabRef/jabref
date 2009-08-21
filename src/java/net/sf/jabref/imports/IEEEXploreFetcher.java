package net.sf.jabref.imports;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.Util;


public class IEEEXploreFetcher implements EntryFetcher {

    ImportInspector dialog = null;
	OutputPrinter status;
    HTMLConverter htmlConverter = new HTMLConverter();
    private String terms;
    String startUrl = "http://ieeexplore.ieee.org";
    String searchUrlPart = "/search/freesearchresult.jsp?queryText=%28";
    String endUrl = "%29+%3Cin%3E+metadata&ResultCount=25&ResultStart=";
    String risUrl = "http://ieeexplore.ieee.org/xpls/citationAct";
    private int perPage = 25, hits = 0, unparseable = 0, parsed = 0;
    private boolean shouldContinue = false;
    private JCheckBox fetchAbstracts = new JCheckBox(Globals.lang("Include abstracts"), false);
    private boolean fetchingAbstracts = false;
    private JRadioButton htmlButton = new JRadioButton(Globals.lang("HTML parser"));
    private JRadioButton risButton = new JRadioButton(Globals.lang("RIS importer"));
    private boolean fetchingRIS = false;
    private static final int MAX_RIS_FETCH = 25;
    
    Pattern hitsPattern = Pattern.compile(".*Your search matched <strong>(\\d+)</strong>.*");
    Pattern maxHitsPattern = Pattern.compile(".*A maximum of <strong>(\\d+)</strong>.*");
    Pattern paperEntryPattern = Pattern.compile(".*<strong>(.+)</strong><br>"
    			+ "\\s+(.+)"
    			+ "\\s+<A href=.+>(.+)</A><br>"
    			+ "\\s+(.+)\\s+(.+)\\s+(.+)\\s+(.+).*");
    Pattern stdEntryPattern = Pattern.compile(".*<strong>(.+)</strong><br>"
    			+ "\\s+(.+)");
    Pattern volumePattern = Pattern.compile(".*Volume (\\d+),&nbsp;(.+)");
    Pattern numberPattern = Pattern.compile(".*Issue (\\d+)</a>,&nbsp;(.+)");
    Pattern partPattern = Pattern.compile(".*Part (\\d+),&nbsp;(.+)");
    Pattern datePattern = Pattern.compile("(.*)\\s?(\\d{4}).*");
    Pattern publicationPattern = Pattern.compile("(.*), \\d*\\.*\\s?(.*)");
    Pattern proceedingPattern = Pattern.compile("(.*?)\\.?\\s?Proceedings\\s?(.*)");
    Pattern abstractLinkPattern = Pattern.compile(
            "<a href=\"(.+)\" class=\"bodyCopySpaced\">Abstract</a>");
    String abbrvPattern = ".*[^,] '?\\d+\\)?";

    Pattern ieeeArticleNumberPattern =
        Pattern.compile("<a href=\".*arnumber=(\\d+).*\">");

    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        htmlButton.setSelected(true);
        
        ButtonGroup group = new ButtonGroup();
        group.add(htmlButton);
        group.add(risButton);
        pan.add(fetchAbstracts, BorderLayout.NORTH);
        pan.add(htmlButton, BorderLayout.CENTER);
        pan.add(risButton, BorderLayout.EAST);
		fetchAbstracts.addItemListener(new ItemListener(){
      		public void itemStateChanged (ItemEvent event) {
				if (fetchAbstracts.isSelected()) {
	        		risButton.setSelected(true);
        			risButton.setEnabled(false);
        			htmlButton.setEnabled(false);
				} else {
        			risButton.setEnabled(true);
        			htmlButton.setEnabled(true);
	        		htmlButton.setSelected(true);
				}
      		}
    	});
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
        String address = makeUrl(0);
        try {
            URL url = new URL(address);

            String page = getResults(url);

            if (page.indexOf("You have entered an invalid search") >= 0) {
                status.showMessage(Globals.lang("You have entered an invalid search '%0'.",
                        terms),
                        Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            
            if (page.indexOf("Bad request") >= 0) {
            	status.showMessage(Globals.lang("Bad Request '%0'.",
                        terms),
                        Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            
            if (page.indexOf("No results") >= 0) {
                status.showMessage(Globals.lang("No entries found for the search string '%0'",
                        terms),
                        Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            hits = getNumberOfHits(page, "Your search matched", hitsPattern);

            int maxHits = getNumberOfHits(page, "A maximum of", maxHitsPattern);

            fetchingAbstracts = fetchAbstracts.isSelected();
            fetchingRIS = risButton.isSelected();
            
            if (hits > maxHits && !fetchingRIS) {
            	status.showMessage(Globals.lang("Your search matched %0 entries. But "
                           +"only %1 results are displayed.",
                                    new String[] {String.valueOf(hits), String.valueOf(maxHits)}),
                            Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
                hits = maxHits;
            }
            
            if (fetchingRIS && hits > MAX_RIS_FETCH) {
            	status.showMessage(Globals.lang("%0 entries found. To reduce server load, "
                       +"only %1 will be downloaded. Choose the HTML parser for more results.",
                                new String[] {String.valueOf(hits), String.valueOf(MAX_RIS_FETCH)}),
                        Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
           		hits = MAX_RIS_FETCH;
            }

            parse(dialog, page, 0, 1);
            int firstEntry = perPage;
            while (shouldContinue && (firstEntry < hits)) {
                address = makeUrl(firstEntry);
                page = getResults(new URL(address));

                if (!shouldContinue)
                    break;

                parse(dialog, page, 0, 1+firstEntry);
                firstEntry += perPage;

            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            status.showMessage(Globals.lang("Connection to IEEEXplore failed"),
                    Globals.lang("Search IEEEXplore"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
        	status.showMessage(Globals.lang(e.getMessage()),
                    Globals.lang("Search IEEEXplore"), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return false;
    }

    public String getTitle() {
        return Globals.menuTitle("Search IEEEXplore");
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getHelpPage() {
        return "IEEEXploreHelp.html";
    }

    public String getKeyName() {
        return "Search IEEEXplore";
    }

    /**
     * This method is called by the dialog when the user has cancelled the import.
     */
    public void stopFetching() {
        shouldContinue = false;
    }

    private String makeUrl(int startIndex) {
        StringBuffer sb = new StringBuffer(startUrl).append(searchUrlPart);
        sb.append(terms.replaceAll(" ", "+"));
        sb.append(endUrl);
        sb.append(String.valueOf(startIndex));
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
        }
    }

    private BibtexEntry parseEntryRis(String number, boolean abs, boolean isStandard)
        throws IOException
    {
        URL url;
        URLConnection conn;
        try {
            url = new URL(risUrl);
            conn = url.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        PrintWriter out = new PrintWriter(
                conn.getOutputStream());
		String cite = "cite";
		if (abs == true)
			cite = "cite_abs";
        out.write("fileFormate=ris&dlSelect=" + cite + "&arnumber=" +
                URLEncoder.encode("<arnumber>" + number + "</arnumber>", "UTF-8"));
        out.flush();
        out.close();
        InputStream inp = conn.getInputStream();
        List<BibtexEntry> items = new RisImporter().importEntries(inp);
        inp.close();
        if (items.size() > 0) {
            BibtexEntry entry = items.get(0);
            if (isStandard == true) {
            	entry.setType(BibtexEntryType.getType("standard"));
            	entry.setField("organization", "IEEE");
            	String stdNumber = entry.getField("journal");
            	String[] parts = stdNumber.split("Std ");
            	if (parts.length == 2) {
            		stdNumber = parts[1];
            		parts = stdNumber.split(", ");
            		if (parts.length == 2) {
            			stdNumber = parts[0];
            			String date = parts[1];
            			parts = date.split(" ");
            			if (parts.length == 2) {
            				entry.setField("month", parts[0]);
            			}
            		}
            		entry.setField("number", stdNumber);
            	}
            	entry.clearField("journal");
            	entry.clearField("booktitle");
            	
            	String title = entry.getField("title");
            	entry.setField("title", title);
            }
            return entry;
        } else
            return null;
    }

    private BibtexEntry cleanup(BibtexEntry entry) {
    	if (entry == null)
    		return null;
    	if (entry.getType().getName() == "Standard")
    		return entry;
    	// clean up author
    	String author = (String)entry.getField("author");
    	if (author != null) {
	    	author = author.replaceAll("\\.", ". ");
	    	author = author.replaceAll("  ", " ");
	    	author = author.replaceAll("\\. -", ".-");
	    	//author = author.replaceAll(",$", "");
	    	entry.setField("author", author);
    	}
    	// clean up month
    	String month = (String)entry.getField("month");
    	if ((month != null) && (month.length() > 0)) {
	    	month = month.replaceAll("\\.", "");
	    	month = month.toLowerCase();

	    	Pattern monthPattern = Pattern.compile("(\\d*+)\\s*([a-z]*+)-*(\\d*+)\\s*([a-z]*+)");
	    	Matcher mm = monthPattern.matcher(month);
	    	String date = month;
	    	if (mm.find()) {
	    		if (mm.group(3).length() == 0) {
	    			if (mm.group(2).length() > 0) {
	    				date = "#" + mm.group(2).substring(0, 3) + "#";
	    				if (mm.group(1).length() > 0) {
	    					date += " " + mm.group(1) + ",";
	    				}
	    			} else {
	    				date = mm.group(1) + ",";
	    			}
	    		} else if (mm.group(2).length() == 0) {
	    			if (mm.group(4).length() > 0) {
	    				date = "#" + mm.group(4).substring(0, 3) + "# " + mm.group(1) + "--" + mm.group(3) + ",";
	    			} else
	    				date += ",";
	    		} else {
	    			date = "#" + mm.group(2).substring(0, 3) + "# " + mm.group(1) + "--#" + mm.group(4).substring(0, 3) + "# " + mm.group(3) + ",";
	    		}
	    	}
	    	//date = date.trim();
	    	// hash or map TODO
	    	//if (!date.isEmpty()) {
	    	entry.setField("month", date);
	    	//}
    	}
    	
    	// clean up pages
    	String pages = entry.getField("pages");
    	String [] pageNumbers = pages.split("--");
    	if (pageNumbers.length == 2) {
    		if (pageNumbers[0].equals(pageNumbers[1])) {// single page
    			entry.setField("pages", pageNumbers[0]);
    		}
    	}
    	
    	// clean up publication field
    	BibtexEntryType type = entry.getType();
    	String sourceField;
		if (type.getName() == "Article") {
        	sourceField = "journal";
			entry.clearField("booktitle");
		} else {
            sourceField = "booktitle";
		}
        String fullName = entry.getField(sourceField);
        if (fullName == null) {
        	System.err.println("Null publication");
        	return null;
        }
        
        if (type.getName() == "Article") {
        	int ind = fullName.indexOf(": Accepted for future publication");
			if (ind > 0) {
				fullName = fullName.substring(0, ind);
				entry.setField("year", "to be published");
				entry.clearField("month");
				entry.clearField("pages");
			}
	        String[] parts = fullName.split("[\\[\\]]"); //[see also...], [legacy...]
	        fullName = parts[0];
	        if (parts.length == 3) {
				fullName += parts[2];
			}
        } else {
        	fullName = fullName.replace("Conference Proceedings", "Proceedings").replace("Proceedings of", "Proceedings").replace("  ", ". ").replace("Proceedings.", "Proceedings");
        }
        
        Matcher m1 = publicationPattern.matcher(fullName);
		if (m1.find()) {
			String prefix = m1.group(2).trim();
			String postfix = m1.group(1).trim();
			String abbrv = "";
			String[] parts = prefix.split("\\. ", 2);
			if (parts.length == 2) {
				if (parts[0].matches(abbrvPattern)) {
					prefix = parts[1];
					abbrv = parts[0];
				} else {
					prefix = parts[0];
					abbrv = parts[1];
				}
			}
			if (prefix.matches(abbrvPattern) == false) {
				fullName = prefix + " " + postfix + " " + abbrv;
				fullName = fullName.trim();
			} else {
				fullName = postfix + " " + prefix;
			}
		}
		if (type.getName() == "Article") {
			fullName = fullName.replace("- ", "-"); //IEE Proceedings-
			
			fullName = fullName.trim();
			if (Globals.prefs.getBoolean("useIEEEAbrv")) {
				String id = Globals.journalAbbrev.getAbbreviatedName(fullName, false);
				if (id != null)
					fullName = id;
			}
        } else {
        	
            Matcher m2 = proceedingPattern.matcher(fullName);
			if (m2.find()) {
				String prefix = m2.group(2); 
				String postfix = m2.group(1).replaceAll("\\.$", "");
				if (prefix.matches(abbrvPattern) == false) {
					String abbrv = "";
				
					String[] parts = postfix.split("\\. ", 2);
					if (parts.length == 2) {
						if (parts[0].matches(abbrvPattern)) {
							postfix = parts[1];
							abbrv = parts[0];
						} else {
							postfix = parts[0];
							abbrv = parts[1];
						}
					}
					fullName = prefix.trim() + " " + postfix.trim() + " " + abbrv;
					
				} else {
					fullName = postfix.trim() + " " + prefix.trim();
				}
				
			}
			
			fullName = fullName.trim();
			
			fullName = fullName.replaceAll("^[tT]he ", "").replaceAll("^\\d{4} ", "").replaceAll("[,.]$", "");
			String year = entry.getField("year");
			fullName = fullName.replaceAll(", " + year + "\\.?", "");
			
        	if (fullName.contains("Abstract") == false && fullName.contains("Summaries") == false && fullName.contains("Conference Record") == false)
        		fullName = "Proc. " + fullName;
			
        }
		
		entry.setField(sourceField, fullName);
		return entry;
    }

    private BibtexEntry parseNextEntry(String allText, int startIndex, int entryNumber)
    {
        BibtexEntry entry = null;
        String toFind = new StringBuffer().append("<div align=\"left\"><strong>")
                .append(entryNumber).append(".</strong></div>").toString();
        int index = allText.indexOf(toFind, startIndex);
        int endIndex = allText.indexOf("</table>", index+1);
        if (endIndex < 0)
            endIndex = allText.length();

        if (index >= 0) {
            piv = index+1;
            String text = allText.substring(index, endIndex);
            BibtexEntryType type = null;
            String sourceField = null;
            if (text.indexOf("JNL") >= 0) {
                type = BibtexEntryType.getType("article");
                sourceField = "journal";
            } else if (text.indexOf("CNF") >= 0){
                type = BibtexEntryType.getType("inproceedings");
                sourceField = "booktitle";
            } else if (text.indexOf("STD") >= 0) {
                type = BibtexEntryType.getType("standard");
            } else {
                System.err.println("Type detection failed.");
            }
            if (fetchingRIS == true) {
				Matcher number =
					ieeeArticleNumberPattern.matcher(text);
				if (number.find()) {
					try {
						entry = parseEntryRis(number.group(1), fetchingAbstracts, type.getName() == "Standard");
					} catch (IOException e) {
						e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
					}
				}
	            if (entry != null) { // fetch successful
	                // we just need to add DOI, it is not included in RIS.
	                int pgInd = text.indexOf("Digital Object Identifier ");
	                if (pgInd >= 0) {
	                    int fieldEnd = text.indexOf("<br>", pgInd);
	                    if (fieldEnd >= 0) {
	                        entry.setField("doi",
	                            text.substring(pgInd+26, fieldEnd).trim());
	                    }
	                }
	                return cleanup(entry);
	            }
            }
            
            index = 0;
            entry = new BibtexEntry(Util.createNeutralId(), type);
            if (type.getName() == "Standard") {
            	Matcher mstd = stdEntryPattern.matcher(text);
            	if (mstd.find()) {
            		entry.setField("title", convertHTMLChars(mstd.group(1)));
            		entry.setField("year", convertHTMLChars(mstd.group(2)));
            		entry.setField("organization", "IEEE");
            		return entry;
            	}
            	System.err.println("Standard entry parsing failed.");
            }
            // Try to set doi:
            int pgInd = text.indexOf("Digital Object Identifier ");
            if (pgInd >= 0) {
                int fieldEnd = text.indexOf("<br>", pgInd);
                if (fieldEnd >= 0) {
                    entry.setField("doi", text.substring(pgInd + 26, fieldEnd).trim());
                }
                text = text.substring(0, pgInd);
            }
            Matcher m = paperEntryPattern.matcher(text);
            String tmp;
            String rest = "";
            if (m.find()) {
                // Title:
                entry.setField("title", convertHTMLChars(m.group(1)));
                // Author:
                tmp = convertHTMLChars(m.group(2));
                if (tmp.charAt(tmp.length()-1) == ';')
                    tmp= tmp.substring(0, tmp.length()-1);
                entry.setField("author", tmp.replaceAll(",;", ";").replaceAll("; ", " and ").replaceAll(",$", ""));
                // Publication:
                tmp = m.group(3);
				String fullName = convertHTMLChars(tmp);
				entry.setField(sourceField, fullName);
				// Volume, Issue, Part, Month, Year, Pages
				String misc = m.group(4);
				for (int i = 5; i < 8; i++) {
					tmp = m.group(i);
					if (tmp.startsWith("Page") == false)
						misc += tmp; 
					else
						break;
				}
                Matcher ms1 = volumePattern.matcher(misc);
                if (ms1.find()) {
                	// Volume:
                	entry.setField("volume", convertHTMLChars(ms1.group(1)));
                	misc = ms1.group(2);
                }
                
                Matcher ms2 = numberPattern.matcher(misc);
                if (ms2.find()) {
                	// Number:
                	entry.setField("number", convertHTMLChars(ms2.group(1)));
                	misc = ms2.group(2);
                }
                //System.out.println(misc);
                Matcher ms3 = partPattern.matcher(misc);
                if (ms3.find()) {
                	entry.setField("part", ms3.group(1));
                	misc = ms3.group(2);
                }
                Matcher ms4 = datePattern.matcher(misc);
                if (ms4.find()) {
                	// Month:
                    String month = convertHTMLChars(ms4.group(1)).replaceAll("-", "--");
                    // Year
                    String year = ms4.group(2);
        	    	if (year.length() > 0) {
        		    	month = month.replaceAll(year, "");
        		    	entry.setField("year", year);
        	    	}
        	    	entry.setField("month", month.trim());
                } else {
                  	Matcher ms5 = datePattern.matcher(fullName);
                	if (ms5.find()) {
                		entry.setField("year", ms5.group(2));
                	}
                }
            } else {
                System.err.println("---no structure match---");
                System.err.println(text);
                unparseable++;
            }
            if (entry == null) {
            	System.err.println("Parse failed");
                System.err.println(text);
            	return null;
            }
            pgInd = text.indexOf("Page(s):");
            if (pgInd >= 0) {
                // Try to set pages:
                rest = text.substring(pgInd+8);
                pgInd = rest.indexOf("<br>");
                if (pgInd >= 0) {
                    tmp = rest.substring(0, pgInd);
                	pgInd = tmp.indexOf("vol");
	                if (pgInd >= 0)
						tmp = tmp.substring(0,pgInd);
                	pgInd = tmp.indexOf("Vol");
	                if (pgInd >= 0)
						tmp = tmp.substring(0,pgInd);
                    entry.setField("pages", tmp.replaceAll(" - ","--").replaceAll("\\s+", ""));
                }
                
            }
            return cleanup(entry);
        }
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
        String substring = page.substring(ind, Math.min(ind+42, page.length()));
        Matcher m = pattern.matcher(substring);
        if (!m.find())
            return 0;
        if (m.groupCount() >= 1) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ex) {
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

    /**
     * Download and parse the web page containing an entry's Abstract:
     * @param link
     * @return
     * @throws IOException
     */
    public String fetchAbstract(String link) throws IOException {
        URL url = new URL(link);
        String page = getResults(url);

        String marker = "Abstract</span><br>";
        int index = page.indexOf(marker);
        int endIndex = page.indexOf("</td>", index + 1);
        if ((index >= 0) && (endIndex > index)) {
            return new String(page.substring(index + marker.length(), endIndex).trim());
        }

        return null;
    }

}
