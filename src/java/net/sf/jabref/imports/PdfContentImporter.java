/*
PdfContentImporter is part of JabRef. 
Copyright (C) 2011 Oliver Kopp

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
or see http://www.gnu.org/licenses/gpl-2.0.html
*/

package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.Util;

/**
 * PdfContentImporter parses data of the first page of the PDF and creates a BibTeX entry.
 * 
 * Currently, Springer and IEEE formats are supported.
 * 
 * Integrating XMP support is future work
 * 
 * @author koppor
 *
 */
public class PdfContentImporter extends ImportFormat {
	
	private static Logger logger = Logger.getLogger(PdfContentImporter.class.getName());
	
	// we can store the DOItoBibTeXFetcher as single reference as the fetcher doesn't hold internal state
	private static DOItoBibTeXFetcher doiToBibTeXFetcher = new DOItoBibTeXFetcher();

	/* global variables holding the state of the current parse run
	 * needed to be able to generate methods such as "fillCurStringWithNonEmptyLines"
	 */
	
	// input split into several lines
	private String[] split; 
	
	// current index in split
    private int i;
    
    // curent "line" in split.
    // sometimes, a "line" is several lines in split
    private String curString;
    
	private String year = null;

	@Override
	public boolean isRecognizedFormat(InputStream in) throws IOException {
		return false;
	}

	/**
	 * Removes all non-letter characters at the end
	 * 
	 * EXCEPTION: a closing bracket is NOT removed
	 * 
	 * @param input
	 * @return
	 * TODO Additionally repalce multiple subsequent spaces by one space
	 */
	private String removeNonLettersAtEnd(String input) {
		input = input.trim();
		if (input.length() == 0)
			return input;
		char lastC = input.charAt(input.length()-1);
		while (!Character.isLetter(lastC) &&  (lastC!=')')) {
			// if there is an asterix, a dot or something else at the end: remove it
			input = input.substring(0, input.length()-1);
			if (input.length()>0) {
				lastC = input.charAt(input.length()-1);
			} else {
				break;
			}
		}
		return input;
	}
	
	private String streamlineNames(String names) {
		String res;
		// supported formats:
		//   Matthias Schrepfer1, Johannes Wolf1, Jan Mendling1, and Hajo A. Reijers2
		if (names.contains(",")) {
			String[] splitNames = names.split(",");
			res = "";
			boolean isFirst = true;
			for (int i=0; i<splitNames.length; i++) {
				String curName = removeNonLettersAtEnd(splitNames[i]);
				if (curName.indexOf("and")==0) {
					// skip possible ands between names
					curName = curName.substring(3).trim();
				} else {
					int posAnd = curName.indexOf(" and "); 
					if (posAnd>=0) {
						String nameBefore = curName.substring(0, posAnd);
						// cannot be first name as "," is contained in the string
						res = res.concat(" and ").concat(removeNonLettersAtEnd(nameBefore));
						curName = curName.substring(posAnd+5);
					}
				}
					
				if (!curName.equals("")) {
					if (curName.equalsIgnoreCase("et al."))
						curName = "others";
					if (isFirst) {
						isFirst = false;
					} else {
						res = res.concat(" and ");
					}
					res = res.concat(curName);
				}
			}
		} else {
			// assumption: names separated by space
			
			String[] splitNames = names.split(" ");
			if (splitNames.length == 0) {
				// empty names... something was really wrong...
				return "";
			}
			
			boolean workedOnFirstOrMiddle = false;
			boolean isFirst = true;
			int i=0;
			res = "";
			do {
				if (!workedOnFirstOrMiddle) {
					if (splitNames[i].equalsIgnoreCase("and")) {
						// do nothing, just increment i at the end of this iteration
					} else {
						if (isFirst) {
							isFirst = false;
						} else {
							res = res.concat(" and ");
						}
						if ((splitNames[i].equalsIgnoreCase("et")) && (splitNames.length>i+1) && (splitNames[i+1].equalsIgnoreCase("al."))) {
							res = res.concat("others");
							break;
						} else {
							res = res.concat(splitNames[i]).concat(" ");
							workedOnFirstOrMiddle = true;
						}
					}
				} else {
					// last item was a first or a middle name
					// we have to check whether we are on a middle name
					// if not, just add the item as last name and add an "and"
					if (splitNames[i].contains(".")) {
						// we found a middle name
						res = res.concat(splitNames[i]).concat(" ");
					} else {
						// last name found
						res = res.concat(removeNonLettersAtEnd(splitNames[i]));
						
						if (splitNames[i].length() > 0 && Character.isLowerCase(splitNames[i].charAt(0))) {
							// it is probably be "van", "vom", ...
							// we just rely on the fact that these things are written in lower case letters
							// do NOT finish name
							res = res.concat(" ");
						} else {
							// finish this name
							workedOnFirstOrMiddle = false;
						}
					}
				}
				i++;
			} while (i<splitNames.length);
			
		}
		return res;
	}
	
	private String streamlineTitle(String title) {
		return removeNonLettersAtEnd(title);
	}
	
	private boolean isYear(String yearStr) {
		try {
			Integer.parseInt(yearStr);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public List<BibtexEntry> importEntries(InputStream in, OutputPrinter status) throws IOException {
		final ArrayList<BibtexEntry> res = new ArrayList<BibtexEntry>(1);
		
		PDDocument document = null;
		try {
			document = PDDocument.load(in);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not load document", e);
			return res;
		}

		try {
			if (document.isEncrypted()) {
				logger.log(Level.INFO,
						Globals.lang("Encrypted documents are not supported"));
				//return res;
			}

			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(1);
			stripper.setEndPage(1);
			stripper.setSortByPosition(true);
			stripper.setParagraphEnd(System.getProperty("line.separator"));
			StringWriter writer = new StringWriter();
			stripper.writeText(document, writer);
			String textResult = writer.toString();
			
			String doi = Util.getDOI(textResult);
			if (doi.length() < textResult.length()) {
				// A DOI was found in the text
				// We do NO parsing of the text, but use the DOI fetcher
				
				ImportInspector i = new ImportInspector() {
					@Override
					public void toFront() {
					}
					@Override
					public void setProgress(int current, int max) {
					}
					@Override
					public void addEntry(BibtexEntry entry) {
						// add the entry to the result object
						res.add(entry);
					}
				};
				doiToBibTeXFetcher.processQuery(doi, i, status);
				if (res.size() != 0) {
					// if something has been found, return the result
					return res;
				} else {
					// otherwise, we just parse the PDF
				}
			}

			String author = null;
			String editor = null;
			String institution = null;
			String abstractT = null;
			String keywords = null;
			String title = null;
			String conference = null;
			String DOI = null;
			String series = null;
			String volume = null;
			String number = null;
			String pages = null;
			// year is a class variable as the method extractYear() uses it;
			String publisher = null;
			BibtexEntryType type = BibtexEntryType.INPROCEEDINGS; 
			
			final String lineBreak = System.getProperty("line.separator");
			
			split = textResult.split(lineBreak);
			
			// idea: split[] contains the different lines
			// blocks are separated by empty lines
			// treat each block
			//   or do special treatment at authors (which are not broken)
			//   therefore, we do a line-based and not a block-based splitting
			// i points to the current line
			// curString (mostly) contains the current block
			//   the different lines are joined into one and thereby separated by " "
			
			proceedToNextNonEmptyLine();
			if (i>=split.length) {
				// PDF could not be parsed or is empty
				// return empty list
				return res;
			}
			curString = split[i];
			i = i+1;
			
			if (curString.length()>4) {
				// special case: possibly conference as first line on the page
				extractYear();
				if (curString.contains("Conference")) {
					fillCurStringWithNonEmptyLines();
					conference = curString;
					curString = "";
				} else {
					// e.g. Copyright (c) 1998 by the Genetics Society of America
					// future work: get year using RegEx
					String lower = curString.toLowerCase();
					if (lower.contains("copyright")) {
						fillCurStringWithNonEmptyLines();
						publisher = curString;
						curString = "";
					}
				}
			}
			
			// start: title
			fillCurStringWithNonEmptyLines();
			title = streamlineTitle(curString);
			curString = "";
			//i points to the next non-empty line
			
			// after title: authors
			author = null;
			while ((i<split.length)  && (!split[i].equals(""))) {
				// author names are unlikely to be split among different lines
				// treat them line by line
				curString = streamlineNames(split[i]);
				if (author==null) {
					author = curString;
				} else {
					if (curString.equals("")) {
						// if split[i] is "and" then "" is returned by streamlineNames -> do nothing
					} else {
						author = author.concat(" and ").concat(curString);
					}
				}
				i++;
			}			
			curString = "";
			i++;
			
			// then, abstract and keywords follow
			while (i<split.length) {
				curString = split[i];
				if ((curString.length()>="Abstract".length()) && (curString.substring(0, "Abstract".length()).equalsIgnoreCase("Abstract"))) {
					if (curString.length() == "Abstract".length()) {
						// only word "abstract" found -- skip line
						curString = "";
					} else {
						curString = curString.substring("Abstract".length()+1).trim().concat(lineBreak);
					}
					i++;
					// fillCurStringWithNonEmptyLines() cannot be used as that uses " " as line separator
					// whereas we need linebreak as separator
					while ((i<split.length)  && (!split[i].equals(""))) {
						curString = curString.concat(split[i]).concat(lineBreak);
						i++;
					}
					abstractT=curString;
					i++;
				} else if ((curString.length()>="Keywords".length()) && (curString.substring(0, "Keywords".length()).equalsIgnoreCase("Keywords"))) {
					if (curString.length() == "Keywords".length()) {
						// only word "Keywords" found -- skip line
						curString = "";
					} else {
						curString = curString.substring("Keywords".length()+1).trim();
					}
					i++;
					fillCurStringWithNonEmptyLines();
					keywords=removeNonLettersAtEnd(curString);
				} else {
					String lower = curString.toLowerCase();
					
					int pos = lower.indexOf("technical");
					if (pos>=0) {
						type = BibtexEntryType.TECHREPORT;
						pos = curString.trim().lastIndexOf(' ');
						if (pos>=0) {
							// assumption: last character of curString is NOT ' '
							//   otherwise pos+1 leads to an out-of-bounds exception
							number = curString.substring(pos+1);
						}
					}
					
					i++;
					proceedToNextNonEmptyLine();
				}
			}
			
			i = split.length-1;
			
			// last block: DOI, detailed information
			// sometimes, this information is in the third last block etc...
			// therefore, read until the beginning of the file 
			
			while (i>=0) {
				readLastBlock();
				// i now points to the block before or is -1
				// curString contains the last block, separated by " "
			
				extractYear();
				
				int pos = curString.indexOf("(Eds.)");
				if ((pos >= 0) && (publisher == null)) {
					// looks like a Springer last line
					// e.g: A. Persson and J. Stirna (Eds.): PoEM 2009, LNBIP 39, pp. 161-175, 2009.
					publisher = "Springer";
					editor = streamlineNames(curString.substring(0, pos - 1));
					curString = curString.substring(pos+"(Eds.)".length()+2); //+2 because of ":" after (Eds.) and the subsequent space
					String[] springerSplit = curString.split(", ");
					if (springerSplit.length >= 4) {
						conference = springerSplit[0];

						String seriesData = springerSplit[1];
						int lastSpace = seriesData.lastIndexOf(' ');
						series = seriesData.substring(0, lastSpace);
						volume = seriesData.substring(lastSpace + 1);
						
						pages = springerSplit[2].substring(4);
						
						if (springerSplit[3].length()>=4) {
							year = springerSplit[3].substring(0,4);
						}
					}
				} else {
					if (DOI==null) {
						pos = curString.indexOf("DOI");
						if (pos < 0) pos = curString.indexOf("doi");
						if (pos>=0) {
							pos += 3;
							char delimiter = curString.charAt(pos);
							if ((delimiter == ':') || (delimiter == ' ')) {
								pos++;
							}
							int nextSpace = curString.indexOf(' ', pos);
							if (nextSpace > 0)
								DOI = curString.substring(pos, nextSpace);
							else
								DOI = curString.substring(pos);
						}
					}
					
					if ((publisher==null) && (curString.indexOf("IEEE")>=0)) {
						// IEEE has the conference things at the end
						publisher = "IEEE";

						// year is extracted by extractYear
						// otherwise, we could it determine as follows: 
						// String yearStr = curString.substring(curString.length()-4);
						// if (isYear(yearStr)) {
						//	year = yearStr;
						// }
						
						if (conference == null) {
							pos = curString.indexOf('$');
							if (pos>0) {
								// we found the price
								// before the price, the ISSN is stated
								// skip that
								pos -= 2;
								while ((pos>=0) && (curString.charAt(pos) != ' '))
									pos--;
								if (pos>0) {
									conference = curString.substring(0,pos);
								}
							}
						}
					}
					
//					String lower = curString.toLowerCase();
//					if (institution == null) {
//						
//					}
					
				}
			}

			BibtexEntry entry = new BibtexEntry();
			entry.setType(type);
			
			if (author!=null) entry.setField("author", author);
			if (editor!=null) entry.setField("editor", editor);
			if (institution!=null) entry.setField("institution", institution);
			if (abstractT!=null) entry.setField("abstract", abstractT);
			if (keywords!=null) entry.setField("keywords", keywords);
			if (title!=null) entry.setField("title", title);
			if (conference!=null) entry.setField("booktitle", conference);
			if (DOI!=null) entry.setField("doi", DOI);
			if (series!=null) entry.setField("series", series);
			if (volume!=null) entry.setField("volume", volume);
			if (number!=null) entry.setField("number", number);
			if (pages!=null) entry.setField("pages", pages);
			if (year!=null) entry.setField("year", year);
			if (publisher!=null) entry.setField("publisher", publisher);
			
			entry.setField("review", textResult);

			res.add(entry);
		} catch (NoClassDefFoundError e) {
			if (e.getMessage().equals("org/bouncycastle/jce/provider/BouncyCastleProvider")) {
				status.showMessage(Globals.lang("Java Bouncy Castle library not found. Please download and install it. For more information see http://www.bouncycastle.org/."));
			} else {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		} finally {
			document.close();
		}
		
		return res;
	}

	
	
	/**
	 * Extract the year out of curString (if it is not yet defined)
	 */
	private void extractYear() {
		if (year != null)
			return;
		
		final Pattern p = Pattern.compile("\\d\\d\\d\\d");
		Matcher m = p.matcher(curString);
		if (m.find()) {
			year = curString.substring(m.start(), m.end());
		}
		
	}

	/**
	 * PDFTextStripper normally does NOT produce multiple empty lines 
	 * (besides at strange PDFs). These strange PDFs are handled here:
	 * proceed to next non-empty line
	 */
	private void proceedToNextNonEmptyLine() {
		while ((i<split.length) && (split[i].trim().equals(""))) {
			i++;
		}
	}
	

	/**
	 * Fill curString with lines until "" is found
	 * No trailing space is added
	 * i is advanced to the next non-empty line (ignoring white space)
	 * 
	 * Lines containing only white spaces are ignored,
	 * but NOT considered as ""
	 * 
	 * Uses GLOBAL variables split, curLine, i
	 */
	private void fillCurStringWithNonEmptyLines() {
		// ensure that curString does not end with " "
		curString = curString.trim();
		while ((i<split.length)  && (!split[i].equals(""))) {
			String curLine = split[i].trim();
			if (!curLine.equals("")) {
				if (curString.length()>0) {
					// insert separating space if necessary
					curString = curString.concat(" ");
				}
				curString = curString.concat(split[i]);
			}
			i++;
		}

		proceedToNextNonEmptyLine();
	}

	/**
	 * resets curString
	 * curString now contains the last block (until "" reached)
	 * Trailing space is added
	 * 
	 * invariant before/after: i points to line before the last handled block
	 */
	private void readLastBlock() {
		while ((i>=0) && (split[i].trim().equals(""))) {
			i--;
		}
		// i is now at the end of a block
		
		int end = i;
		
		// find beginning
		while ((i>=0) && (!split[i].equals(""))) {
			i--;
		}
		// i is now the line before the beginning of the block
		// this fulfills the invariant
		
		curString = "";
		for (int j = i+1; j<=end; j++) {
			curString = curString.concat(split[j].trim());
			if (j!=end) {
				curString = curString.concat(" ");
			}
		}
	}

	@Override
	public String getFormatName() {
		return "PDFcontent";
	}

}
