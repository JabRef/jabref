package net.sf.jabref.imports;

import java.awt.BorderLayout;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.Util;

public class IEEEXploreFetcher implements EntryFetcher {

    ImportInspector dialog = null;
	OutputPrinter status;
    final HTMLConverter htmlConverter = new HTMLConverter();
    
    private JCheckBox absCheckBox = new JCheckBox(Globals.lang("Include abstracts"), false);
    private JRadioButton htmlButton = new JRadioButton(Globals.lang("HTML parser"));
    private JRadioButton bibButton = new JRadioButton(Globals.lang("BibTeX importer"));
    
    private static final int MAX_FETCH = 100;
    private int perPage = MAX_FETCH, hits = 0, unparseable = 0, parsed = 0;
    private int piv = 0;
    private boolean shouldContinue = false;
    private boolean includeAbstract = false;
    private boolean importBibtex = false;
    
    private String terms;
    private final String startUrl = "http://ieeexplore.ieee.org/search/freesearchresult.jsp?queryText=";
    private final String endUrl = "&rowsPerPage=" + Integer.toString(perPage) + "&pageNumber=";
    private String searchUrl;
    private final String importUrl = "http://ieeexplore.ieee.org/xpls/downloadCitations";
    
    private final Pattern hitsPattern = Pattern.compile("([0-9,]+) results");
    private final Pattern idPattern = Pattern.compile("<input name=\"\" type=\"checkbox\" value=\"\"\\s*" +
    		"id=\"([0-9]+)\"/>");
    private final Pattern typePattern = Pattern.compile("<span class=\"type\">\\s*(.+)");
    private HashMap<String, String> fieldPatterns = new HashMap<String, String>();
    private final Pattern absPattern = Pattern.compile("<p>\\s*(.+)");
    
    Pattern stdEntryPattern = Pattern.compile(".*<strong>(.+)</strong><br>"
			+ "\\s+(.+)");
    
    Pattern publicationPattern = Pattern.compile("(.*), \\d*\\.*\\s?(.*)");
    Pattern proceedingPattern = Pattern.compile("(.*?)\\.?\\s?Proceedings\\s?(.*)");
    Pattern abstractLinkPattern = Pattern.compile(
            "<a href=\"(.+)\" class=\"bodyCopySpaced\">Abstract</a>");
    String abrvPattern = ".*[^,] '?\\d+\\)?";

    Pattern ieeeArticleNumberPattern = Pattern.compile("<a href=\".*arnumber=(\\d+).*\">");
    
    public IEEEXploreFetcher() {
    	super();
    	
    	fieldPatterns.put("title", "<a\\s*href=[^<]+>\\s*(.+)\\s*</a>");
        fieldPatterns.put("author", "<p>\\s+(.+)");
        fieldPatterns.put("volume", "Volume:\\s*(\\d+)");
        fieldPatterns.put("number", "Issue:\\s*(\\d+)");
        //fieldPatterns.put("part", "Part (\\d+),&nbsp;(.+)");
        fieldPatterns.put("year", "Publication Year:\\s*(\\d{4})");
        fieldPatterns.put("pages", "Page\\(s\\):\\s*(\\d+)\\s*-\\s*(\\d*)");
        fieldPatterns.put("doi", "Digital Object Identifier:\\s*<a href=.*>(.+)</a>");
    }
    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        htmlButton.setSelected(true);
        htmlButton.setEnabled(false);
        bibButton.setEnabled(false);
        
        ButtonGroup group = new ButtonGroup();
        group.add(htmlButton);
        group.add(bibButton);
        pan.add(absCheckBox, BorderLayout.NORTH);
        pan.add(htmlButton, BorderLayout.CENTER);
        pan.add(bibButton, BorderLayout.EAST);
		
        return pan;
    }

    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        this.dialog = dialog;
        this.status = status;
        terms = query;
        piv = 0;
        shouldContinue = true;
        parsed = 0;
        unparseable = 0;
        int pageNumber = 1;
        
        searchUrl = makeUrl(pageNumber);//start at page 1
        
        try {
        	URL url = new URL(searchUrl);
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
            
            if (page.indexOf("No results were found.") >= 0) {
                status.showMessage(Globals.lang("No entries found for the search string '%0'",
                        terms),
                        Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            
            hits = getNumberOfHits(page, "display-status", hitsPattern);


            includeAbstract = absCheckBox.isSelected();
            importBibtex = bibButton.isSelected();
            
            if (hits > MAX_FETCH) {
            	status.showMessage(Globals.lang("%0 entries found. To reduce server load, "
                       +"only %1 will be downloaded.",
                                new String[] {String.valueOf(hits), String.valueOf(MAX_FETCH)}),
                        Globals.lang("Search IEEEXplore"), JOptionPane.INFORMATION_MESSAGE);
           		hits = MAX_FETCH;
            }

            parse(dialog, page, 0, 1);
            int firstEntry = perPage;
            while (shouldContinue && firstEntry < hits) {
            	pageNumber++;
                searchUrl = makeUrl(pageNumber);
                page = getResults(new URL(searchUrl));

                if (!shouldContinue)
                    break;

                parse(dialog, page, 0, firstEntry + 1);
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
        StringBuffer sb = new StringBuffer(startUrl);
        sb.append(terms.replaceAll(" ", "+"));
        sb.append(endUrl);
        sb.append(String.valueOf(startIndex));
        return sb.toString();
    }

    

    private void parse(ImportInspector dialog, String text, int startIndex, int firstEntryNumber) {
        piv = startIndex;
        int entryNumber = firstEntryNumber;
        
        if (importBibtex) {
			//TODO: Login
        	ArrayList<String> idSelected = new ArrayList<String>();
        	String id;
		 	while ((id = parseNextEntryId(text, piv)) != null && shouldContinue) {
	        	idSelected.add(id);
	        	entryNumber++;
	        }
		 	try {
		 		BibtexDatabase dbase = parseBibtexDatabase(idSelected, includeAbstract);
		 		Collection<BibtexEntry> items = dbase.getEntries();
		 		Iterator<BibtexEntry> iter = items.iterator();
		 		while (iter.hasNext()) {
		 			BibtexEntry entry = iter.next();
		 			dialog.addEntry(cleanup(entry));
	                dialog.setProgress(parsed + unparseable, hits);
	                parsed++;
		 		}
		 	} catch (IOException e) {
		 		e.printStackTrace();
		 	}
			//for
        } else {
        	BibtexEntry entry;
	        while (((entry = parseNextEntry(text, piv)) != null) && shouldContinue) {
	            if (entry.getField("title") != null) {
	                dialog.addEntry(entry);
	                dialog.setProgress(parsed + unparseable, hits);
	                parsed++;
	            }
	            entryNumber++;
	        }
        }
    }

    private BibtexDatabase parseBibtexDatabase(List<String> id, boolean abs) throws IOException {
    	if (id.isEmpty())
    		return null;
        URL url;
        URLConnection conn;
        try {
            url = new URL(importUrl);
            conn = url.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        conn.setRequestProperty("Referer", searchUrl);
        PrintWriter out = new PrintWriter(
                conn.getOutputStream());

		String recordIds = "";
		Iterator<String> iter = id.iterator();
		while (iter.hasNext()) { 
	    	recordIds += iter.next() + " ";
		}
		recordIds = recordIds.trim();
		String citation = abs ? "citation-abstract" : "citation-only";
		
		String content = "recordIds=" + recordIds.replaceAll(" ", "%20") + "&fromPageName=&citations-format=" + citation + "&download-format=download-bibtex";
		System.out.println(content);
        out.write(content);
        out.flush();
        out.close();

        BufferedReader bufr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer sb = new StringBuffer();
        char[] buffer = new char[256];
        while(true) {
            int bytesRead = bufr.read(buffer);
            if(bytesRead == -1) break;
            for (int i=0; i<bytesRead; i++)
                sb.append((char)buffer[i]);
        }
        System.out.println(sb.toString());
        
        ParserResult results = new BibtexParser(bufr).parse();
        bufr.close();
        return results.getDatabase();
    }

    private BibtexEntry cleanup(BibtexEntry entry) {
    	if (entry == null)
    		return null;
    	
    	// clean up author
    	String author = (String)entry.getField("author");
    	if (author != null) {
	    	author = author.replaceAll("\\.", ". ");
	    	author = author.replaceAll("  ", " ");
	    	author = author.replaceAll("\\. -", ".-");
	    	author = author.replaceAll("; ", " and ");
	    	author = author.replaceAll("[,;]$", "");
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
	    	//if (!date.isEmpty()) {
	    	entry.setField("month", date);
	    	//}
    	}
    	
    	// clean up pages
    	String field = "pages";
    	String pages = entry.getField(field);
    	if (pages != null) {
	    	String [] pageNumbers = pages.split("-");
	    	if (pageNumbers.length == 2) {
	    		if (pageNumbers[0].equals(pageNumbers[1])) {// single page
	    			entry.setField(field, pageNumbers[0]);
	    		} else {
	    			entry.setField(field, pages.replaceAll("-", "--"));
	    		}
	    	}
    	}
    	
    	// clean up publication field
    	BibtexEntryType type = entry.getType();
    	String sourceField = "";
		if (type.getName() == "Article") {
        	sourceField = "journal";
			entry.clearField("booktitle");
		} else if (type.getName() == "Inproceedings"){
            sourceField = "booktitle";
		}
        String fullName = entry.getField(sourceField);
        if (fullName != null) {
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
	        	fullName = fullName.replace("Conference Proceedings", "Proceedings").
	        			replace("Proceedings of", "Proceedings").replace("Proceedings.", "Proceedings");
	        	fullName = fullName.replaceAll("International", "Int.");
	        	fullName = fullName.replaceAll("Symposium", "Symp.");
	        	fullName = fullName.replaceAll("Conference", "Conf.");
	        	fullName = fullName.replaceAll(" on", " ").replace("  ", " ");
	        }
	        
	        Matcher m1 = publicationPattern.matcher(fullName);
			if (m1.find()) {
				String prefix = m1.group(2).trim();
				String postfix = m1.group(1).trim();
				String abrv = "";
				String[] parts = prefix.split("\\. ", 2);
				if (parts.length == 2) {
					if (parts[0].matches(abrvPattern)) {
						prefix = parts[1];
						abrv = parts[0];
					} else {
						prefix = parts[0];
						abrv = parts[1];
					}
				}
				if (prefix.matches(abrvPattern) == false) {
					fullName = prefix + " " + postfix + " " + abrv;
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
	        }
			if (type.getName() == "Inproceedings") {
	            Matcher m2 = proceedingPattern.matcher(fullName);
				if (m2.find()) {
					String prefix = m2.group(2); 
					String postfix = m2.group(1).replaceAll("\\.$", "");
					if (prefix.matches(abrvPattern) == false) {
						String abrv = "";
					
						String[] parts = postfix.split("\\. ", 2);
						if (parts.length == 2) {
							if (parts[0].matches(abrvPattern)) {
								postfix = parts[1];
								abrv = parts[0];
							} else {
								postfix = parts[0];
								abrv = parts[1];
							}
						}
						fullName = prefix.trim() + " " + postfix.trim() + " " + abrv;
						
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
        }
		return entry;
    }

    private String parseNextEntryId(String allText, int startIndex) {
	    int index = allText.indexOf("<div class=\"select", startIndex);
	    int endIndex = allText.indexOf("</div>", index);
	    
	    if (index >= 0 && endIndex > 0) {
	    	String text = allText.substring(index, endIndex);
	    	endIndex += 6;
	    	piv = endIndex;
	    	//parse id
	    	Matcher idMatcher = idPattern.matcher(text);
	    	//add id into a vector
	    	if (idMatcher.find()) {
	    		return idMatcher.group(1);
	    	}
	    }
	    return null;
    }
    
    private BibtexEntry parseNextEntry(String allText, int startIndex) {
        BibtexEntry entry = null;
        
     	int index = allText.indexOf("<div class=\"detail", piv);
        int endIndex = allText.indexOf("</div>", index);

        if (index >= 0 && endIndex > 0) {
        	endIndex += 6;
        	piv = endIndex;
        	String text = allText.substring(index, endIndex);
            
            BibtexEntryType type = null;
            String sourceField = null;
            
            String typeName = "";
            Matcher typeMatcher = typePattern.matcher(text);
            if (typeMatcher.find()) {
	            typeName = typeMatcher.group(1);
	            if (typeName.equalsIgnoreCase("IEEE Journals") || typeName.equalsIgnoreCase("IEEE Early Access") ||
	            		typeName.equalsIgnoreCase("IET Journals") || typeName.equalsIgnoreCase("AIP Journals") ||
					   	typeName.equalsIgnoreCase("AVS Journals") || typeName.equalsIgnoreCase("IBM Journals")) {
	                type = BibtexEntryType.getType("article");
	                sourceField = "journal";
	            } else if (typeName.equalsIgnoreCase("IEEE Conferences") || typeName.equalsIgnoreCase("IET Conferences")) {
	                type = BibtexEntryType.getType("inproceedings");
	                sourceField = "booktitle";
		        } else if (typeName.equalsIgnoreCase("IEEE Standards")) {
	                type = BibtexEntryType.getType("standard");
	                sourceField = "number";
		        } else if (typeName.equalsIgnoreCase("IEEE Educational Courses")) {
		        	type = BibtexEntryType.getType("Electronic");
		        	sourceField = "note";
		        } else if (typeName.equalsIgnoreCase("IEEE Book Chapter")) {
		        	type = BibtexEntryType.getType("inCollection");
		        	sourceField = "booktitle";
		        }
            }
            
            if (type == null) {
            	type = BibtexEntryType.getType("misc");
            	sourceField = "note";
                System.err.println("Type detection failed. Use MISC instead.");
                unparseable++;
                System.err.println(text);
            }
        
            entry = new BibtexEntry(Util.createNeutralId(), type);
            
            if (typeName.equalsIgnoreCase("IEEE Standards")) {
            	entry.setField("organization", "IEEE");
            }
            
            if (typeName.equalsIgnoreCase("IEEE Book Chapter")) {
            	entry.setField("publisher", "IEEE");
            }
            
            if (typeName.equalsIgnoreCase("IEEE Early Access")) {
            	entry.setField("note", "Early Access");
            }
            
            Set<String> fields = fieldPatterns.keySet();
            for (String field: fields) {
            	Matcher fieldMatcher = Pattern.compile(fieldPatterns.get(field)).matcher(text);
            	if (fieldMatcher.find()) {
            		entry.setField(field, htmlConverter.format(fieldMatcher.group(1)));
            		if (field.equals("title") && fieldMatcher.find()) {
            			String sec_title = htmlConverter.format(fieldMatcher.group(1));
            			if (entry.getType() == BibtexEntryType.getStandardType("standard")) {
            				sec_title = sec_title.replaceAll("IEEE Std ", "");
            			}
            			entry.setField(sourceField, sec_title);
            			
            		}
            		if (field.equals("pages") && fieldMatcher.groupCount() == 2) {
            			entry.setField(field, fieldMatcher.group(1) + "-" + fieldMatcher.group(2));
            		}
            	}
            }
            if (entry.getType() == BibtexEntryType.getStandardType("inproceedings") && entry.getField("author").equals("")) {
            	entry.setType(BibtexEntryType.getStandardType("proceedings"));
            }
        
            if (includeAbstract) {
            	index = allText.indexOf("<div class=\"abstract RevealContent", piv);
	            if (index >= 0) {
	            	endIndex = allText.indexOf("</div>", index) + 6;
		            piv = endIndex;
		            
	            	text = allText.substring(index, endIndex);
	            	Matcher absMatcher = absPattern.matcher(text);
	            	if (absMatcher.find()) {
	            		entry.setField("abstract", absMatcher.group(1));
	            	}
	            }
            }
        }
        
        if (entry == null) {
        	return null;
        } else {
            return cleanup(entry);
        }
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
        String substring = page.substring(ind, page.length());
        Matcher m = pattern.matcher(substring);
        if (m.find())
            return Integer.parseInt(m.group(1));
        else
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
}
