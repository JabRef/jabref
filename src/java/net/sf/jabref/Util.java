/*
 Copyright (C) 2003 Morten O. Alver

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
// created by : Morten O. Alver 2003
//
// function : utility functions
//
// modified :  - r.nagel 20.04.2006
//               make the DateFormatter abstract and splitt the easyDate methode
//               (now we cannot change the dateformat dynamicly, sorry)
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEdit;

import net.sf.jabref.autocompleter.AbstractAutoCompleter;
import net.sf.jabref.export.SaveSession;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypeEntryEditor;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.groups.AbstractGroup;
import net.sf.jabref.groups.KeywordGroup;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.imports.CiteSeerFetcher;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Describe class <code>Util</code> here.
 * 
 * @author <a href="mailto:"> </a>
 * @version 1.0
 */
public class Util {

	/**
	 * A static Object for date formatting. Please do not create the object
	 * here, because there are some references from the Globals class.....
	 * 
	 */
	private static SimpleDateFormat dateFormatter = null;

	/*
	 * Colors are defined here.
	 * 
	 */
	public static Color fieldsCol = new Color(180, 180, 200);

	/*
	 * Integer values for indicating result of duplicate check (for entries):
	 * 
	 */
	final static int TYPE_MISMATCH = -1, NOT_EQUAL = 0, EQUAL = 1, EMPTY_IN_ONE = 2,
		EMPTY_IN_TWO = 3, EMPTY_IN_BOTH = 4;

	final static NumberFormat idFormat;

    public static Pattern remoteLinkPattern = Pattern.compile("[a-z]+://.*");

	static {
		idFormat = NumberFormat.getInstance();
		idFormat.setMinimumIntegerDigits(8);
		idFormat.setGroupingUsed(false);
	}

	public static int getMinimumIntegerDigits(){
		return idFormat.getMinimumIntegerDigits();
	}

	public static void bool(boolean b) {
		if (b)
			System.out.println("true");
		else
			System.out.println("false");
	}

	public static void pr(String s) {
		System.out.println(s);
	}

	public static void pr_(String s) {
		System.out.print(s);
	}

	public static String nCase(String s) {
		// Make first character of String uppercase, and the
		// rest lowercase.
		if (s.length() > 1)
			return s.substring(0, 1).toUpperCase() + s.substring(1, s.length()).toLowerCase();
		else
			return s.toUpperCase();

	}

	public static String checkName(String s) {
		// Append '.bib' to the string unless it ends with that.
		if (s.length() < 4 || !s.substring(s.length() - 4).equalsIgnoreCase(".bib")) {
			return s + ".bib";
		}
		return s;
	}

	private static int idCounter = 0;

	public synchronized static String createNeutralId() {
		return idFormat.format(idCounter++);
	}

	/**
	 * This method sets the location of a Dialog such that it is centered with
	 * regard to another window, but not outside the screen on the left and the
	 * top.
	 */
	public static void placeDialog(java.awt.Dialog diag, java.awt.Container win) {
        diag.setLocationRelativeTo(win);
	}

	/**
	 * This method translates a field or string from Bibtex notation, with
	 * possibly text contained in " " or { }, and string references,
	 * concatenated by '#' characters, into Bibkeeper notation, where string
	 * references are enclosed in a pair of '#' characters.
	 */
	public static String parseField(String content) {
		
		if (content.length() == 0)
			return content;
		
		String[] strings = content.split("#");
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < strings.length; i++){
			String s = strings[i].trim();
			if (s.length() > 0){
				char c = s.charAt(0);
				// String reference or not?
				if (c == '{' || c == '"'){
					result.append(shaveString(strings[i]));	
				} else {
					// This part should normally be a string reference, but if it's
					// a pure number, it is not.
					String s2 = shaveString(s);
					try {
						Integer.parseInt(s2);
						// If there's no exception, it's a number.
						result.append(s2);
					} catch (NumberFormatException ex) {
						// otherwise append with hashes...
						result.append("#").append(s2).append("#");
					}
				}
			}
		}
		return result.toString();
	}

	/**
	 * Will return the publication date of the given bibtex entry in conformance
	 * to ISO 8601, i.e. either YYYY or YYYY-MM.
	 * 
	 * @param entry
	 * @return will return the publication date of the entry or null if no year
	 *         was found.
	 */
	public static String getPublicationDate(BibtexEntry entry) {

		Object o = entry.getField("year");
		if (o == null)
			return null;

		String year = toFourDigitYear(o.toString());

		o = entry.getField("month");
		if (o != null) {
			int month = Util.getMonthNumber(o.toString());
			if (month != -1) {
				return year + "-" + (month + 1 < 10 ? "0" : "") + (month + 1);
			}
		}
		return year;
	}

	public static String shaveString(String s) {
		// returns the string, after shaving off whitespace at the beginning
		// and end, and removing (at most) one pair of braces or " surrounding
		// it.
		if (s == null)
			return null;
		char ch, ch2;
		int beg = 0, end = s.length();
		// We start out assuming nothing will be removed.
		boolean begok = false, endok = false;
		while (!begok) {
			if (beg < s.length()) {
				ch = s.charAt(beg);
				if (Character.isWhitespace(ch))
					beg++;
				else
					begok = true;
			} else
				begok = true;

		}
		while (!endok) {
			if (end > beg + 1) {
				ch = s.charAt(end - 1);
				if (Character.isWhitespace(ch))
					end--;
				else
					endok = true;
			} else
				endok = true;
		}

		if (end > beg + 1) {
			ch = s.charAt(beg);
			ch2 = s.charAt(end - 1);
			if (((ch == '{') && (ch2 == '}')) || ((ch == '"') && (ch2 == '"'))) {
				beg++;
				end--;
			}
		}
		s = s.substring(beg, end);
		return s;
	}

	/**
	 * This method returns a String similar to the one passed in, except that it
	 * is molded into a form that is acceptable for bibtex.
	 * 
	 * Watch-out that the returned string might be of length 0 afterwards.
	 * 
	 * @param key
	 *            mayBeNull
	 */
	public static String checkLegalKey(String key) {
		if (key == null)
			return null;
        if (!Globals.prefs.getBoolean("enforceLegalBibtexKey")) {
            // User doesn't want us to enforce legal characters. We must still look
            // for whitespace and some characters such as commas, since these would
            // interfere with parsing:
            StringBuilder newKey = new StringBuilder();
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (!Character.isWhitespace(c) && (c != '{') && (c != '\\') && (c != '"')
                    && (c != '}') && (c != ','))
                    newKey.append(c);
            }
            return newKey.toString();

        }
		StringBuilder newKey = new StringBuilder();
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!Character.isWhitespace(c) && (c != '#') && (c != '{') && (c != '\\') && (c != '"')
				&& (c != '}') && (c != '~') && (c != ',') && (c != '^'))
				newKey.append(c);
		}

		// Replace non-english characters like umlauts etc. with a sensible
		// letter or letter combination that bibtex can accept.
		String newKeyS = replaceSpecialCharacters(newKey.toString());

		return newKeyS;
	}

	/**
	 * Replace non-english characters like umlauts etc. with a sensible letter
	 * or letter combination that bibtex can accept. The basis for replacement
	 * is the HashMap GLobals.UNICODE_CHARS.
	 */
	public static String replaceSpecialCharacters(String s) {
		for (Map.Entry<String, String> chrAndReplace : Globals.UNICODE_CHARS.entrySet()){
			s = s.replaceAll(chrAndReplace.getKey(), chrAndReplace.getValue());
		}
		return s;
	}

	static public String _wrap2(String in, int wrapAmount) {
		// The following line cuts out all whitespace and replaces them with
		// single
		// spaces:
		// in = in.replaceAll("[ ]+"," ").replaceAll("[\\t]+"," ");
		// StringBuffer out = new StringBuffer(in);
		StringBuffer out = new StringBuffer(in.replaceAll("[ \\t\\r]+", " "));

		int p = in.length() - wrapAmount;
		int lastInserted = -1;
		while (p > 0) {
			p = out.lastIndexOf(" ", p);
			if (p <= 0 || p <= 20)
				break;
			int lbreak = out.indexOf("\n", p);
			System.out.println(lbreak + " " + lastInserted);
			if ((lbreak > p) && ((lastInserted >= 0) && (lbreak < lastInserted))) {
				p = lbreak - wrapAmount;
			} else {
				out.insert(p, "\n\t");
				lastInserted = p;
				p -= wrapAmount;
			}
		}
		return out.toString();
	}

	static public String wrap2(String in, int wrapAmount) {
		return net.sf.jabref.imports.FieldContentParser.wrap(in, wrapAmount);
	}

	static public String __wrap2(String in, int wrapAmount) {
		// The following line cuts out all whitespace except line breaks, and
		// replaces
		// with single spaces. Line breaks are padded with a tab character:
		StringBuffer out = new StringBuffer(in.replaceAll("[ \\t\\r]+", " "));

		int p = 0;
		// int lastInserted = -1;
		while (p < out.length()) {
			int q = out.indexOf(" ", p + wrapAmount);
			if ((q < 0) || (q >= out.length()))
				break;
			int lbreak = out.indexOf("\n", p);
			// System.out.println(lbreak);
			if ((lbreak > p) && (lbreak < q)) {
				p = lbreak + 1;
				int piv = lbreak + 1;
				if ((out.length() > piv) && !(out.charAt(piv) == '\t'))
					out.insert(piv, "\n\t");

			} else {
				// System.out.println(q+" "+out.length());
				out.deleteCharAt(q);
				out.insert(q, "\n\t");
				p = q + 1;
			}
		}
		return out.toString();// .replaceAll("\n", "\n\t");
	}

	public static TreeSet<String> findDeliminatedWordsInField(BibtexDatabase db, String field,
		String deliminator) {
		TreeSet<String> res = new TreeSet<String>();
		
		for (String s : db.getKeySet()){
			BibtexEntry be = db.getEntryById(s);
			Object o = be.getField(field);
			if (o != null) {
				String fieldValue = o.toString().trim();
				StringTokenizer tok = new StringTokenizer(fieldValue, deliminator);
				while (tok.hasMoreTokens())
					res.add(nCase(tok.nextToken().trim()));
			}
		}
		return res;
	}

	/**
	 * Returns a HashMap containing all words used in the database in the given
	 * field type. Characters in
	 * 
	 * @param remove
	 *            are not included.
	 * @param db
	 *            a <code>BibtexDatabase</code> value
	 * @param field
	 *            a <code>String</code> value
	 * @param remove
	 *            a <code>String</code> value
	 * @return a <code>HashSet</code> value
	 */
	public static TreeSet<String> findAllWordsInField(BibtexDatabase db, String field, String remove) {
		TreeSet<String> res = new TreeSet<String>();
		StringTokenizer tok;
		for (String s : db.getKeySet()){
			BibtexEntry be = db.getEntryById(s);
			Object o = be.getField(field);
			if (o != null) {
				tok = new StringTokenizer(o.toString(), remove, false);
				while (tok.hasMoreTokens())
					res.add(nCase(tok.nextToken().trim()));
			}
		}
		return res;
	}


    /**
     * Finds all authors' last names in all the given fields for the given database.
     * @param db The database.
     * @param fields The fields to look in.
     * @return a set containing the names.
     */
    public static Set<String> findAuthorLastNames(BibtexDatabase db, List<String> fields) {
		Set<String> res = new TreeSet<String>();
		for (String s : db.getKeySet()){
			BibtexEntry be = db.getEntryById(s);
            for (String field : fields) {
                String val = be.getField(field);
                if ((val != null) && (val.length() > 0)) {
                    AuthorList al = AuthorList.getAuthorList(val);
                    for (int i=0; i<al.size(); i++) {
                        AuthorList.Author a = al.getAuthor(i);
                        String lastName = a.getLast();
                        if ((lastName != null) && (lastName.length() > 0))
                            res.add(lastName);
                    }
                }

            }
		}

		return res;
	}
    

	/**
	 * Takes a String array and returns a string with the array's elements
	 * delimited by a certain String.
	 * 
	 * @param strs
	 *            String array to convert.
	 * @param delimiter
	 *            String to use as delimiter.
	 * @return Delimited String.
	 */
	public static String stringArrayToDelimited(String[] strs, String delimiter) {
		if ((strs == null) || (strs.length == 0))
			return "";
		if (strs.length == 1)
			return strs[0];
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < strs.length - 1; i++) {
			sb.append(strs[i]);
			sb.append(delimiter);
		}
		sb.append(strs[strs.length - 1]);
		return sb.toString();
	}

	/**
	 * Takes a delimited string, splits it and returns
	 * 
	 * @param names
	 *            a <code>String</code> value
	 * @return a <code>String[]</code> value
	 */
	public static String[] delimToStringArray(String names, String delimiter) {
		if (names == null)
			return null;
		return names.split(delimiter);
	}

	/**
	 * Open a http/pdf/ps viewer for the given link string.
	 */
	public static void openExternalViewer(MetaData metaData, String link, String fieldName)
		throws IOException {

        if (fieldName.equals("ps") || fieldName.equals("pdf")) {

            // Find the default directory for this field type:
			String dir = metaData.getFileDirectory(fieldName);

			File file = expandFilename(link, new String[] { dir, "." });

			// Check that the file exists:
			if ((file == null) || !file.exists()) {
				throw new IOException(Globals.lang("File not found") + " (" + fieldName + "): '"
					+ link + "'.");
			}
			link = file.getCanonicalPath();

			// Use the correct viewer even if pdf and ps are mixed up:
			String[] split = file.getName().split("\\.");
			if (split.length >= 2) {
				if (split[split.length - 1].equalsIgnoreCase("pdf"))
					fieldName = "pdf";
				else if (split[split.length - 1].equalsIgnoreCase("ps")
					|| (split.length >= 3 && split[split.length - 2].equalsIgnoreCase("ps")))
					fieldName = "ps";
			}

        } else if (fieldName.equals("doi")) {
			fieldName = "url";
			
			link = sanitizeUrl(link);
			
			// Check to see if link field already contains a well formated URL
			if (!link.startsWith("http://")) {
			    // Remove possible 'doi:'
			    if (link.matches("^doi:/*.*")){
	                link = link.replaceFirst("^doi:/*", "");
	            }
			    link = Globals.DOI_LOOKUP_PREFIX + link;
			}
		} else if (fieldName.equals("citeseerurl")) {
			fieldName = "url";

			String canonicalLink = CiteSeerFetcher.generateCanonicalURL(link);
			if (canonicalLink != null)
				link = canonicalLink;
		}

		String cmdArray[] = new String[2];
		if (fieldName.equals("url")) { // html
			try {
				link = sanitizeUrl(link);

				if (Globals.ON_MAC) {
					String[] cmd = { "/usr/bin/open", "-a", Globals.prefs.get("htmlviewer"), link };
					Runtime.getRuntime().exec(cmd);
				} else if (Globals.ON_WIN) {
					openFileOnWindows(link, false);
				} else {
					cmdArray[0] = Globals.prefs.get("htmlviewer");
					cmdArray[1] = link;
					Runtime.getRuntime().exec(cmdArray);
				}

			} catch (IOException e) {
				System.err.println("An error occured on the command: "
					+ Globals.prefs.get("htmlviewer") + " " + link);
			}
		} else if (fieldName.equals("ps")) {
			try {
				if (Globals.ON_MAC) {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("ps");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    String[] cmd = { "/usr/bin/open", "-a", viewer, link };
					Runtime.getRuntime().exec(cmd);
				} else if (Globals.ON_WIN) {
					openFileOnWindows(link, true);
					/*
					 * cmdArray[0] = Globals.prefs.get("psviewer"); cmdArray[1] =
					 * link; Process child = Runtime.getRuntime().exec(
					 * cmdArray[0] + " " + cmdArray[1]);
					 */
				} else {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("ps");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    cmdArray[0] = viewer;
					cmdArray[1] = link;
					Runtime.getRuntime().exec(cmdArray);
				}
			} catch (IOException e) {
				System.err.println("An error occured on the command: "
					+ Globals.prefs.get("psviewer") + " " + link);
			}
		} else if (fieldName.equals("pdf")) {
			try {
				if (Globals.ON_MAC) {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("pdf");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    String[] cmd = { "/usr/bin/open", "-a", viewer, link };
					Runtime.getRuntime().exec(cmd);
				} else if (Globals.ON_WIN) {
					openFileOnWindows(link, true);
					/*
					 * String[] spl = link.split("\\\\"); StringBuffer sb = new
					 * StringBuffer(); for (int i = 0; i < spl.length; i++) { if
					 * (i > 0) sb.append("\\"); if (spl[i].indexOf(" ") >= 0)
					 * spl[i] = "\"" + spl[i] + "\""; sb.append(spl[i]); }
					 * //pr(sb.toString()); link = sb.toString();
					 * 
					 * String cmd = "cmd.exe /c start " + link;
					 * 
					 * Process child = Runtime.getRuntime().exec(cmd);
					 */
				} else {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("pdf");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    cmdArray[0] = viewer;
					cmdArray[1] = link;
					// Process child = Runtime.getRuntime().exec(cmdArray[0]+"
					// "+cmdArray[1]);
					Runtime.getRuntime().exec(cmdArray);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("An error occured on the command: "
					+ Globals.prefs.get("pdfviewer") + " #" + link);
				System.err.println(e.getMessage());
			}
		} else {
			System.err
				.println("Message: currently only PDF, PS and HTML files can be opened by double clicking");
		}
	}

	/**
	 * Opens a file on a Windows system, using its default viewer.
	 * 
	 * @param link
	 *            The file name.
	 * @param localFile
	 *            true if it is a local file, not an URL.
	 * @throws IOException
	 */
	public static void openFileOnWindows(String link, boolean localFile) throws IOException {
		/*
		 * if (localFile) { String[] spl = link.split("\\\\"); StringBuffer sb =
		 * new StringBuffer(); for (int i = 0; i < spl.length; i++) { if (i > 0)
		 * sb.append("\\"); if (spl[i].indexOf(" ") >= 0) spl[i] = "\"" + spl[i] +
		 * "\""; sb.append(spl[i]); } link = sb.toString(); }
		 */
		link = link.replaceAll("&", "\"&\"").replaceAll(" ", "\" \"");

		// Bug fix for:
		// http://sourceforge.net/tracker/index.php?func=detail&aid=1489454&group_id=92314&atid=600306
		String cmd;
		if (Globals.osName.startsWith("Windows 9")) {
			cmd = "command.com /c start " + link;
		} else {
			cmd = "cmd.exe /c start " + link;
		}

        Runtime.getRuntime().exec(cmd);
	}

    /**
     * Opens a file on a Windows system, using the given application.
     *
     * @param link The file name.
     * @param application Link to the app that opens the file.
     * @throws IOException
     */
    public static void openFileWithApplicationOnWindows(String link, String application)
        throws IOException {

        link = link.replaceAll("&", "\"&\"").replaceAll(" ", "\" \"");

		Runtime.getRuntime().exec(application + " " + link);
    }

    /**
	 * Open an external file, attempting to use the correct viewer for it.
	 * 
	 * @param metaData
	 *            The MetaData for the database this file belongs to.
	 * @param link
	 *            The file name.
     * @return false if the link couldn't be resolved, true otherwise.
	 */
	public static boolean openExternalFileAnyFormat(final MetaData metaData, String link,
                                                 final ExternalFileType fileType) throws IOException {

        boolean httpLink = false;

        if (remoteLinkPattern.matcher(link.toLowerCase()).matches()) {
            httpLink = true;
        }
        /*if (link.toLowerCase().startsWith("file://")) {
            link = link.substring(7);
        }
        final String ln = link;
        if (remoteLinkPattern.matcher(link.toLowerCase()).matches()) {
            (new Thread(new Runnable() {
                public void run() {
                    openRemoteExternalFile(metaData, ln, fileType);
                }
            })).start();

            return true;
        }*/

        //boolean httpLink = link.toLowerCase().startsWith("http:")
        //        || link.toLowerCase().startsWith("ftp:");

        
        // For other platforms we'll try to find the file type:
		File file = new File(link);

		// We try to check the extension for the file:
		String name = file.getName();
		int pos = name.lastIndexOf('.');
		String extension = ((pos >= 0) && (pos < name.length() - 1)) ? name.substring(pos + 1)
			.trim().toLowerCase() : null;
		// Find the default directory for this field type, if any:
		String dir = metaData.getFileDirectory(extension);
		// Include the standard "file" directory:
        String fileDir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
        // Include the directory of the bib file:
        String[] dirs;
        if (metaData.getFile() != null) {
            String databaseDir = metaData.getFile().getParent();
            dirs = new String[] { dir, fileDir, databaseDir };
        }
        else
            dirs = new String[] { dir, fileDir };

        if (!httpLink) {
            File tmp = expandFilename(link, dirs);
            if (tmp != null)
                file = tmp;
        }

        // Check if we have arrived at a file type, and either an http link or an existing file:
		if ((httpLink || file.exists()) && (fileType != null)) {
            // Open the file:
			try {
                String filePath = httpLink ? link : file.getPath();
                if (Globals.ON_MAC) {
                    // Use "-a <application>" if the app is specified, and just "open <filename>" otherwise:
                    String[] cmd = ((fileType.getOpenWith() != null) && (fileType.getOpenWith().length() > 0)) ?
                            new String[] { "/usr/bin/open", "-a", fileType.getOpenWith(), filePath } :
                            new String[] { "/usr/bin/open", filePath };
					Runtime.getRuntime().exec(cmd);
				} else if (Globals.ON_WIN) {
                    if ((fileType.getOpenWith() != null) && (fileType.getOpenWith().length() > 0)) {
                        // Application is specified. Use it:
                        openFileWithApplicationOnWindows(filePath, fileType.getOpenWith());
                    } else
                        openFileOnWindows(filePath, true);
				} else {
                    // Use the given app if specified, and the universal "xdg-open" otherwise:
                    String[] openWith;
                    if ((fileType.getOpenWith() != null) && (fileType.getOpenWith().length() > 0))
                        openWith = fileType.getOpenWith().split(" ");
                    else
                        openWith = new String[] {"xdg-open"};
                    
                    String[] cmdArray = new String[openWith.length+1];
                    System.arraycopy(openWith, 0, cmdArray, 0, openWith.length);
                    cmdArray[cmdArray.length-1] = filePath;
                    Runtime.getRuntime().exec(cmdArray);
				}
                return true;
            } catch (IOException e) {
                throw e;
                /*e.printStackTrace();
				System.err.println("An error occured on the command: " + fileType.getOpenWith()
					+ " #" + link);
				System.err.println(e.getMessage());*/
			}

		} else {

            return false;
            // No file matched the name, or we didn't know the file type.
			// Perhaps it is an URL thing.

            /* TODO: find out if this fallback option of opening the link in a web browser is
               TODO: actually necessary. */
            /*
            link = sanitizeUrl(link);

			if (Globals.ON_MAC) {
				String[] cmd = { "/usr/bin/open", "-a", Globals.prefs.get("htmlviewer"), link };
				Runtime.getRuntime().exec(cmd);
			} else if (Globals.ON_WIN) {
				openFileOnWindows(link, false);
			} else {
				String[] openWith = Globals.prefs.get("htmlviewer").split(" ");
                String[] cmdArray = new String[openWith.length+1];
                System.arraycopy(openWith, 0, cmdArray, 0, openWith.length);
                cmdArray[cmdArray.length-1] = link;
                Runtime.getRuntime().exec(cmdArray);
            }
            */
		}


    }


    public static void openRemoteExternalFile(final MetaData metaData,
                                              final String link, final ExternalFileType fileType) {
        File temp = null;
        try {
            temp = File.createTempFile("jabref-link", "."+fileType.getExtension());
            temp.deleteOnExit();
            System.out.println("Downloading to '"+temp.getPath()+"'");
            URLDownload ud = new URLDownload(null, new URL(link), temp);
            ud.download();
            System.out.println("Done");
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        final String ln = temp.getPath();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    openExternalFileAnyFormat(metaData, ln, fileType);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

public static boolean openExternalFileUnknown(JabRefFrame frame, BibtexEntry entry, MetaData metaData,
                                           String link, UnknownExternalFileType fileType) throws IOException {

    String cancelMessage = Globals.lang("Unable to open file.");
    String[] options = new String[] {Globals.lang("Define '%0'", fileType.getName()),
            Globals.lang("Change file type"), Globals.lang("Cancel")};
    String defOption = options[0];
    int answer = JOptionPane.showOptionDialog(frame, Globals.lang("This external link is of the type '%0', which is undefined. What do you want to do?",
            fileType.getName()),
            Globals.lang("Undefined file type"), JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, defOption);
    if (answer == JOptionPane.CANCEL_OPTION) {
        frame.output(cancelMessage);
        return false;
    }
    else if (answer == JOptionPane.YES_OPTION) {
        // User wants to define the new file type. Show the dialog:
        ExternalFileType newType = new ExternalFileType(fileType.getName(), "", "", "", "new");
        ExternalFileTypeEntryEditor editor = new ExternalFileTypeEntryEditor(frame, newType);
        editor.setVisible(true);
        if (editor.okPressed()) {
            // Get the old list of types, add this one, and update the list in prefs:
            List<ExternalFileType> fileTypes = new ArrayList<ExternalFileType>();
            ExternalFileType[] oldTypes = Globals.prefs.getExternalFileTypeSelection();
            for (int i = 0; i < oldTypes.length; i++) {
                fileTypes.add(oldTypes[i]);
            }
            fileTypes.add(newType);
            Collections.sort(fileTypes);
            Globals.prefs.setExternalFileTypes(fileTypes);
            // Finally, open the file:
            return openExternalFileAnyFormat(metaData, link, newType);
        } else {
            // Cancelled:
            frame.output(cancelMessage);
            return false;
        }
    }
    else {
        // User wants to change the type of this link.
        // First get a model of all file links for this entry:
        FileListTableModel tModel = new FileListTableModel();
        String oldValue = entry.getField(GUIGlobals.FILE_FIELD);
        tModel.setContent(oldValue);
        FileListEntry flEntry = null;
        // Then find which one we are looking at:
        for (int i=0; i<tModel.getRowCount(); i++) {
            FileListEntry iEntry = tModel.getEntry(i);
            if (iEntry.getLink().equals(link)) {
                flEntry = iEntry;
                break;
            }
        }
        if (flEntry == null) {
            // This shouldn't happen, so I'm not sure what to put in here:
            throw new RuntimeException("Could not find the file list entry "+link+" in "+entry.toString());
        }

        FileListEntryEditor editor = new FileListEntryEditor(frame, flEntry, false, true, metaData);
        editor.setVisible(true, false);
        if (editor.okPressed()) {
            // Store the changes and add an undo edit:
            String newValue = tModel.getStringRepresentation();
            UndoableFieldChange ce = new UndoableFieldChange(entry, GUIGlobals.FILE_FIELD,
                    oldValue, newValue);
            entry.setField(GUIGlobals.FILE_FIELD, newValue);
            frame.basePanel().undoManager.addEdit(ce);
            frame.basePanel().markBaseChanged();
            // Finally, open the link:
            return openExternalFileAnyFormat(metaData, flEntry.getLink(), flEntry.getType());
        } else {
            // Cancelled:
            frame.output(cancelMessage);
            return false;
        }
    }
}
    /**
	 * Make sure an URL is "portable", in that it doesn't contain bad characters
	 * that break the open command in some OSes.
	 * 
	 * A call to this method will also remove \\url{} enclosings and clean doi links.
	 * 
	 * Old Version can be found in CVS version 114 of Util.java.
	 * 
	 * @param link
	 *            The URL to sanitize.
	 * @return Sanitized URL
	 */
	public static String sanitizeUrl(String link) {

	    // First check if it is enclosed in \\url{}. If so, remove
        // the wrapper.
        if (link.startsWith("\\url{") && link.endsWith("}"))
            link = link.substring(5, link.length() - 1);

        if (link.matches("^doi:/*.*")){
            // Remove 'doi:'
            link = link.replaceFirst("^doi:/*", "");
            link = Globals.DOI_LOOKUP_PREFIX + link;
        }
        
        /*
         * Poor man's DOI detection
         * 
         * Fixes
         * https://sourceforge.net/tracker/index.php?func=detail&aid=1709449&group_id=92314&atid=600306
         */
        if (link.startsWith("10.")) {
            link = Globals.DOI_LOOKUP_PREFIX + link;
        }
	    
		link = link.replaceAll("\\+", "%2B");

		try {
			link = URLDecoder.decode(link, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}

		/**
		 * Fix for: [ 1574773 ] sanitizeUrl() breaks ftp:// and file:///
		 * 
		 * http://sourceforge.net/tracker/index.php?func=detail&aid=1574773&group_id=92314&atid=600306
		 */
		try {
			return new URI(null, link, null).toASCIIString();
		} catch (URISyntaxException e) {
			return link;
		}
	}

	/**
	 * Searches the given directory and subdirectories for a pdf file with name
	 * as given + ".pdf"
	 */
	public static String findPdf(String key, String extension, String directory, OpenFileFilter off) {
		// String filename = key + "."+extension;

		/*
		 * Simon Fischer's patch for replacing a regexp in keys before
		 * converting to filename:
		 * 
		 * String regex = Globals.prefs.get("basenamePatternRegex"); if ((regex !=
		 * null) && (regex.trim().length() > 0)) { String replacement =
		 * Globals.prefs.get("basenamePatternReplacement"); key =
		 * key.replaceAll(regex, replacement); }
		 */
		if (!directory.endsWith(System.getProperty("file.separator")))
			directory += System.getProperty("file.separator");
		String found = findInDir(key, directory, off, 0);
		if (found != null)
			return found.substring(directory.length());
		else
			return null;
	}

	public static Map<BibtexEntry, List<File>> findAssociatedFiles(Collection<BibtexEntry> entries, Collection<String> extensions, Collection<File> directories){
		HashMap<BibtexEntry, List<File>> result = new HashMap<BibtexEntry, List<File>>();
	
		// First scan directories
		Set<File> filesWithExtension = findFiles(extensions, directories);
		
		// Initialize Result-Set
		for (BibtexEntry entry : entries){
			result.put(entry, new ArrayList<File>());
		}

        boolean exactOnly = Globals.prefs.getBoolean("autolinkExactKeyOnly");
        // Now look for keys
		nextFile:
		for (File file : filesWithExtension){
			
			String name = file.getName();
            int dot = name.lastIndexOf('.');
            // First, look for exact matches:
            for (BibtexEntry entry : entries){
                String citeKey = entry.getCiteKey();
                if ((citeKey != null) && (citeKey.length() > 0)) {
                    if (dot > 0) {
                        if (name.substring(0, dot).equals(citeKey)) {
                            result.get(entry).add(file);
                            continue nextFile;
                        }
                    }
                }
            }
            // If we get here, we didn't find any exact matches. If non-exact
            // matches are allowed, try to find one:
            if (!exactOnly) {
                for (BibtexEntry entry : entries){
                    String citeKey = entry.getCiteKey();
                    if ((citeKey != null) && (citeKey.length() > 0)) {
                        if (name.startsWith(citeKey)){
                            result.get(entry).add(file);
                            continue nextFile;
                        }
                    }
                }
            }
		}
		
		return result;
	}
	
	public static Set<File> findFiles(Collection<String> extensions, Collection<File> directories) {
		Set<File> result = new HashSet<File>();
		
		for (File directory : directories){
			result.addAll(findFiles(extensions, directory));
		}
		
		return result;
	}

	private static Collection<? extends File> findFiles(Collection<String> extensions, File directory) {
		Set<File> result = new HashSet<File>();
		
		File[] children = directory.listFiles();
		if (children == null)
			return result; // No permission?

		for (File child : children){
			if (child.isDirectory()) {
				result.addAll(findFiles(extensions, child));
			} else {
				
				String extension = getFileExtension(child);
					
				if (extension != null){
					if (extensions.contains(extension)){
						result.add(child);
					}
				}
			}
		}
		
		return result;
	}

	/**
	 * Returns the extension of a file or null if the file does not have one (no . in name).
	 * 
	 * @param file
	 * 
	 * @return The extension, trimmed and in lowercase.
	 */
	public static String getFileExtension(File file) {
		String name = file.getName();
		int pos = name.lastIndexOf('.');
		String extension = ((pos >= 0) && (pos < name.length() - 1)) ? name.substring(pos + 1)
			.trim().toLowerCase() : null;
		return extension;
	}

	/**
	 * New version of findPdf that uses findFiles.
	 * 
	 * The search pattern will be read from the preferences.
	 * 
	 * The [extension]-tags in this pattern will be replace by the given
	 * extension parameter.
	 * 
	 */
	public static String findPdf(BibtexEntry entry, String extension, String directory) {
		return findPdf(entry, extension, new String[] { directory });
	}

	/**
	 * Convenience method for findPDF. Can search multiple PDF directories.
	 */
	public static String findPdf(BibtexEntry entry, String extension, String[] directories) {

		String regularExpression;
		if (Globals.prefs.getBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY)) {
			regularExpression = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
		} else {
			regularExpression = Globals.prefs
				.get(JabRefPreferences.DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY);
		}
		regularExpression = regularExpression.replaceAll("\\[extension\\]", extension);

		return findFile(entry, null, directories, regularExpression, true);
	}

    /**
     * Convenience menthod for findPDF. Searches for a file of the given type.
     * @param entry The BibtexEntry to search for a link for.
     * @param fileType The file type to search for.
     * @return The link to the file found, or null if not found.
     */
    public static String findFile(BibtexEntry entry, ExternalFileType fileType, List<String> extraDirs) {

        List<String> dirs = new ArrayList<String>();
        dirs.addAll(extraDirs);
        if (Globals.prefs.hasKey(fileType.getExtension()+"Directory")) {
            dirs.add(Globals.prefs.get(fileType.getExtension()+"Directory"));
        }
        String [] directories = dirs.toArray(new String[dirs.size()]);
        return findPdf(entry, fileType.getExtension(), directories);
    }

    /**
	 * Searches the given directory and file name pattern for a file for the
	 * bibtexentry.
	 *
	 * Used to fix:
	 *
	 * http://sourceforge.net/tracker/index.php?func=detail&aid=1503410&group_id=92314&atid=600309
	 *
	 * Requirements:
	 *  - Be able to find the associated PDF in a set of given directories.
	 *  - Be able to return a relative path or absolute path.
	 *  - Be fast.
	 *  - Allow for flexible naming schemes in the PDFs.
	 *
	 * Syntax scheme for file:
	 * <ul>
	 * <li>* Any subDir</li>
	 * <li>** Any subDir (recursiv)</li>
	 * <li>[key] Key from bibtex file and database</li>
	 * <li>.* Anything else is taken to be a Regular expression.</li>
	 * </ul>
	 *
	 * @param entry
	 *            non-null
	 * @param database
	 *            non-null
	 * @param directory
	 *            A set of root directories to start the search from. Paths are
	 *            returned relative to these directories if relative is set to
	 *            true. These directories will not be expanded or anything. Use
	 *            the file attribute for this.
	 * @param file
	 *            non-null
	 *
	 * @param relative
	 *            whether to return relative file paths or absolute ones
	 *
	 * @return Will return the first file found to match the given criteria or
	 *         null if none was found.
	 */
	public static String findFile(BibtexEntry entry, BibtexDatabase database, String[] directory,
		String file, boolean relative) {

		for (int i = 0; i < directory.length; i++) {
			String result = findFile(entry, database, directory[i], file, relative);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Removes optional square brackets from the string s
	 *
	 * @param s
	 * @return
	 */
	public static String stripBrackets(String s) {
		int beginIndex = (s.startsWith("[") ? 1 : 0);
		int endIndex = (s.endsWith("]") ? s.length() - 1 : s.length());
		return s.substring(beginIndex, endIndex);
	}

	public static ArrayList<String[]> parseMethodsCalls(String calls) throws RuntimeException {

		ArrayList<String[]> result = new ArrayList<String[]>();

		char[] c = calls.toCharArray();

		int i = 0;

		while (i < c.length) {

			int start = i;
			if (Character.isJavaIdentifierStart(c[i])) {
				i++;
				while (i < c.length && (Character.isJavaIdentifierPart(c[i]) || c[i] == '.')) {
					i++;
				}
				if (i < c.length && c[i] == '(') {

					String method = calls.substring(start, i);

					// Skip the brace
					i++;

					if (i < c.length){
						if (c[i] == '"'){
							// Parameter is in format "xxx"

							// Skip "
							i++;

							int startParam = i;
							i++;
		                    boolean escaped = false;
							while (i + 1 < c.length &&
                                    !(!escaped && c[i] == '"' && c[i + 1] == ')')) {
                                if (c[i] == '\\') {
                                    escaped = !escaped;
                                }
                                else
                                    escaped = false;
                                i++;

                            }

							String param = calls.substring(startParam, i);
		
							result.add(new String[] { method, param });
						} else {
							// Parameter is in format xxx

							int startParam = i;

							while (i < c.length && c[i] != ')') {
								i++;
							}

							String param = calls.substring(startParam, i);

							result.add(new String[] { method, param });


						}
					} else {
						// Incorrecly terminated open brace
						result.add(new String[] { method });
					}
				} else {
					String method = calls.substring(start, i);
					result.add(new String[] { method });
				}
			}
			i++;
		}

		return result;
	}

	/**
	 * Accepts a string like [author:lower] or [title:abbr] or [auth],
	 * whereas the first part signifies the bibtex-field to get, or the key generator
     * field marker to use, while the others are the modifiers that will be applied.
	 *
	 * @param fieldAndFormat
	 * @param entry
	 * @param database
	 * @return
	 */
	public static String getFieldAndFormat(String fieldAndFormat, BibtexEntry entry,
		BibtexDatabase database) {

		fieldAndFormat = stripBrackets(fieldAndFormat);

		int colon = fieldAndFormat.indexOf(':');

		String beforeColon, afterColon;
		if (colon == -1) {
			beforeColon = fieldAndFormat;
			afterColon = null;
		} else {
			beforeColon = fieldAndFormat.substring(0, colon);
			afterColon = fieldAndFormat.substring(colon + 1);
		}
		beforeColon = beforeColon.trim();

		if (beforeColon.length() == 0) {
			return null;
		}

		String fieldValue = BibtexDatabase.getResolvedField(beforeColon, entry, database);

        // If no field value was found, try to interpret it as a key generator field marker:
        if (fieldValue == null)
            fieldValue =  LabelPatternUtil.makeLabel(entry, beforeColon);

		if (fieldValue == null)
			return null;

		if (afterColon == null || afterColon.length() == 0)
			return fieldValue;

        String[] parts = afterColon.split(":");
        fieldValue = LabelPatternUtil.applyModifiers(fieldValue, parts, 0);
        
		return fieldValue;
	}

	/**
	 * Convenience function for absolute search.
	 *
	 * Uses findFile(BibtexEntry, BibtexDatabase, (String)null, String, false).
	 */
	public static String findFile(BibtexEntry entry, BibtexDatabase database, String file) {
		return findFile(entry, database, (String) null, file, false);
	}

	/**
	 * Internal Version of findFile, which also accepts a current directory to
	 * base the search on.
	 *
	 */
	public static String findFile(BibtexEntry entry, BibtexDatabase database, String directory,
		String file, boolean relative) {

		File root;
		if (directory == null) {
			root = new File(".");
		} else {
			root = new File(directory);
		}
		if (!root.exists())
			return null;

		String found = findFile(entry, database, root, file);

		if (directory == null || !relative) {
			return found;
		}

		if (found != null) {
			try {
				/**
				 * [ 1601651 ] PDF subdirectory - missing first character
				 *
				 * http://sourceforge.net/tracker/index.php?func=detail&aid=1601651&group_id=92314&atid=600306
				 */
                // Changed by M. Alver 2007.01.04:
                // Remove first character if it is a directory separator character:
                String tmp = found.substring(root.getCanonicalPath().length());
                if ((tmp.length() > 1) && (tmp.charAt(0) == File.separatorChar))
                    tmp = tmp.substring(1);
                return tmp;
                //return found.substring(root.getCanonicalPath().length());
			} catch (IOException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * The actual work-horse. Will find absolute filepaths starting from the
	 * given directory using the given regular expression string for search.
	 */
	protected static String findFile(BibtexEntry entry, BibtexDatabase database, File directory,
		String file) {

		if (file.startsWith("/")) {
			directory = new File(".");
			file = file.substring(1);
		}

		// Escape handling...
		Matcher m = Pattern.compile("([^\\\\])\\\\([^\\\\])").matcher(file);
		StringBuffer s = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(s, m.group(1) + "/" + m.group(2));
		}
		m.appendTail(s);
		file = s.toString();
		String[] fileParts = file.split("/");

		if (fileParts.length == 0)
			return null;

		if (fileParts.length > 1) {

			for (int i = 0; i < fileParts.length - 1; i++) {

				String dirToProcess = fileParts[i];

				dirToProcess = expandBrackets(dirToProcess, entry, database);

				if (dirToProcess.matches("^.:$")) { // Windows Drive Letter
					directory = new File(dirToProcess + "/");
					continue;
				}
				if (dirToProcess.equals(".")) { // Stay in current directory
					continue;
				}
				if (dirToProcess.equals("..")) {
					directory = new File(directory.getParent());
					continue;
				}
				if (dirToProcess.equals("*")) { // Do for all direct subdirs

					File[] subDirs = directory.listFiles();
					if (subDirs == null)
						return null; // No permission?

					String restOfFileString = join(fileParts, "/", i + 1, fileParts.length);

					for (int sub = 0; sub < subDirs.length; sub++) {
						if (subDirs[sub].isDirectory()) {
							String result = findFile(entry, database, subDirs[sub],
								restOfFileString);
							if (result != null)
								return result;
						}
					}
					return null;
				}
				// Do for all direct and indirect subdirs
				if (dirToProcess.equals("**")) {
					List<File> toDo = new LinkedList<File>();
					toDo.add(directory);

					String restOfFileString = join(fileParts, "/", i + 1, fileParts.length);

					// Before checking the subdirs, we first check the current
					// dir
					String result = findFile(entry, database, directory, restOfFileString);
					if (result != null)
						return result;

					while (!toDo.isEmpty()) {

						// Get all subdirs of each of the elements found in toDo
						File[] subDirs = toDo.remove(0).listFiles();
						if (subDirs == null) // No permission?
							continue;

						toDo.addAll(Arrays.asList(subDirs));

						for (int sub = 0; sub < subDirs.length; sub++) {
							if (!subDirs[sub].isDirectory())
								continue;
							result = findFile(entry, database, subDirs[sub], restOfFileString);
							if (result != null)
								return result;
						}
					}
					// We already did the currentDirectory
					return null;
				}

				final Pattern toMatch = Pattern
					.compile(dirToProcess.replaceAll("\\\\\\\\", "\\\\"));

				File[] matches = directory.listFiles(new FilenameFilter() {
					public boolean accept(File arg0, String arg1) {
						return toMatch.matcher(arg1).matches();
					}
				});
				if (matches == null || matches.length == 0)
					return null;

				directory = matches[0];

				if (!directory.exists())
					return null;

			} // End process directory information
		}
		// Last step check if the given file can be found in this directory
		String filenameToLookFor = expandBrackets(fileParts[fileParts.length - 1], entry, database);

		final Pattern toMatch = Pattern.compile("^"
			+ filenameToLookFor.replaceAll("\\\\\\\\", "\\\\") + "$");

		File[] matches = directory.listFiles(new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return toMatch.matcher(arg1).matches();
			}
		});
		if (matches == null || matches.length == 0)
			return null;

		try {
			return matches[0].getCanonicalPath();
		} catch (IOException e) {
			return null;
		}
	}

	static Pattern squareBracketsPattern = Pattern.compile("\\[.*?\\]");

	/**
	 * Takes a string that contains bracketed expression and expands each of
	 * these using getFieldAndFormat.
	 *
	 * Unknown Bracket expressions are silently dropped.
	 *
	 * @param bracketString
	 * @param entry
	 * @param database
	 * @return
	 */
	public static String expandBrackets(String bracketString, BibtexEntry entry,
		BibtexDatabase database) {
		Matcher m = squareBracketsPattern.matcher(bracketString);
		StringBuffer s = new StringBuffer();
		while (m.find()) {
			String replacement = getFieldAndFormat(m.group(), entry, database);
			if (replacement == null)
				replacement = "";
			m.appendReplacement(s, replacement);
		}
		m.appendTail(s);

		return s.toString();
	}

	/**
	 * Concatenate all strings in the array from index 'from' to 'to' (excluding
	 * to) with the given separator.
	 * 
	 * Example:
	 * 
	 * String[] s = "ab/cd/ed".split("/"); join(s, "\\", 0, s.length) ->
	 * "ab\\cd\\ed"
	 * 
	 * @param strings
	 * @param separator
	 * @param from
	 * @param to
	 *            Excluding strings[to]
	 * @return
	 */
	public static String join(String[] strings, String separator, int from, int to) {
		if (strings.length == 0 || from >= to)
			return "";
		
		from = Math.max(from, 0);
		to = Math.min(strings.length, to);

		StringBuffer sb = new StringBuffer();
		for (int i = from; i < to - 1; i++) {
			sb.append(strings[i]).append(separator);
		}
		return sb.append(strings[to - 1]).toString();
	}

	/**
	 * Converts a relative filename to an absolute one, if necessary. Returns
	 * null if the file does not exist.
	 * 
	 * Will look in each of the given dirs starting from the beginning and
	 * returning the first found file to match if any.
	 */
	public static File expandFilename(String name, String[] dir) {

		for (int i = 0; i < dir.length; i++) {
            if (dir[i] != null) {
                File result = expandFilename(name, dir[i]);
                if (result != null) {
                    return result;
                }
            }
        }

		return null;
	}

	/**
	 * Converts a relative filename to an absolute one, if necessary. Returns
	 * null if the file does not exist.
	 */
	public static File expandFilename(String name, String dir) {

		File file = null;
		if (name == null || name.length() == 0)
			return null;
		else {
			file = new File(name);
		}

		if (!file.exists() && (dir != null)) {
            if (dir.endsWith(System.getProperty("file.separator")))
                name = dir + name;
            else
                name = dir + System.getProperty("file.separator") + name;

            // System.out.println("expanded to: "+name);
            // if (name.startsWith("ftp"))

            file = new File(name);

            if (file.exists())
                return file;
            // Ok, try to fix / and \ problems:
            if (Globals.ON_WIN) {
                // workaround for catching Java bug in regexp replacer
                // and, why, why, why ... I don't get it - wegner 2006/01/22
                try {
                    name = name.replaceAll("/", "\\\\");
                } catch (java.lang.StringIndexOutOfBoundsException exc) {
                    System.err
                        .println("An internal Java error was caused by the entry " +
                            "\"" + name + "\"");
                }
            } else
                name = name.replaceAll("\\\\", "/");
            // System.out.println("expandFilename: "+name);
            file = new File(name);
            if (!file.exists())
                file = null;
        }
        return file;
    }

	private static String findInDir(String key, String dir, OpenFileFilter off, int count) {
        if (count > 20)
            return null; // Make sure an infinite loop doesn't occur.
        File f = new File(dir);
		File[] all = f.listFiles();
		if (all == null)
			return null; // An error occured. We may not have
		// permission to list the files.

		int numFiles = all.length;

		for (int i = 0; i < numFiles; i++) {
			File curFile = all[i];

			if (curFile.isFile()) {
				String name = curFile.getName();
				if (name.startsWith(key + ".") && off.accept(name))
					return curFile.getPath();

			} else if (curFile.isDirectory()) {
				String found = findInDir(key, curFile.getPath(), off, count+1);
				if (found != null)
					return found;
			}
		}
		return null;
	}

    /**
     * This methods assures all words in the given entry are recorded in their
     * respective Completers, if any.
     */
    public static void updateCompletersForEntry(HashMap<String, AbstractAutoCompleter> autoCompleters, BibtexEntry bibtexEntry) {
    	for (Map.Entry<String, AbstractAutoCompleter> entry : autoCompleters.entrySet()){    		
            AbstractAutoCompleter comp = entry.getValue();
            comp.addBibtexEntry(bibtexEntry);
        }
    }


	/**
	 * Sets empty or non-existing owner fields of bibtex entries inside a List
	 * to a specified default value. Timestamp field is also set. Preferences
	 * are checked to see if these options are enabled.
	 * 
	 * @param bibs
	 *            List of bibtex entries
	 */
	public static void setAutomaticFields(Collection<BibtexEntry> bibs,
             boolean overwriteOwner, boolean overwriteTimestamp, boolean markEntries) {


		String timeStampField = Globals.prefs.get("timeStampField");

		String defaultOwner = Globals.prefs.get("defaultOwner");
		String timestamp = easyDateFormat();
		boolean globalSetOwner = Globals.prefs.getBoolean("useOwner"),
                globalSetTimeStamp = Globals.prefs.getBoolean("useTimeStamp");

        // Do not need to do anything if all options are disabled
		if (!(globalSetOwner || globalSetTimeStamp || markEntries))
			return;

        // Iterate through all entries
		for (BibtexEntry curEntry : bibs){
            boolean setOwner = globalSetOwner &&
                (overwriteOwner || (curEntry.getField(BibtexFields.OWNER)==null));
            boolean setTimeStamp = globalSetTimeStamp &&
                (overwriteTimestamp || (curEntry.getField(timeStampField)==null));
            setAutomaticFields(curEntry, setOwner, defaultOwner, setTimeStamp, timeStampField,
				timestamp);
            if (markEntries)
                Util.markEntry(curEntry, new NamedCompound(""));
		}

	}

	/**
	 * Sets empty or non-existing owner fields of a bibtex entry to a specified
	 * default value. Timestamp field is also set. Preferences are checked to
	 * see if these options are enabled.
	 * 
	 * @param entry
	 *            The entry to set fields for.
     * @param overwriteOwner
     *              Indicates whether owner should be set if it is already set.
     * @param overwriteTimestamp
     *              Indicates whether timestamp should be set if it is already set.
	 */
	public static void setAutomaticFields(BibtexEntry entry, boolean overwriteOwner,
                                          boolean overwriteTimestamp) {
		String defaultOwner = Globals.prefs.get("defaultOwner");
		String timestamp = easyDateFormat();
        String timeStampField = Globals.prefs.get("timeStampField");
        boolean setOwner = Globals.prefs.getBoolean("useOwner") &&
            (overwriteOwner || (entry.getField(BibtexFields.OWNER)==null));
        boolean setTimeStamp = Globals.prefs.getBoolean("useTimeStamp") &&
            (overwriteTimestamp || (entry.getField(timeStampField)==null));

		setAutomaticFields(entry, setOwner, defaultOwner, setTimeStamp, timeStampField, timestamp);
	}

	private static void setAutomaticFields(BibtexEntry entry, boolean setOwner, String owner,
		boolean setTimeStamp, String timeStampField, String timeStamp) {

		// Set owner field if this option is enabled:
		if (setOwner) {
			// No or empty owner field?
			// if (entry.getField(Globals.OWNER) == null
			// || ((String) entry.getField(Globals.OWNER)).length() == 0) {
			// Set owner field to default value
			entry.setField(BibtexFields.OWNER, owner);
			// }
		}

		if (setTimeStamp)
			entry.setField(timeStampField, timeStamp);
	}

	/**
	 * Copies a file.
	 * 
	 * @param source
	 *            File Source file
	 * @param dest
	 *            File Destination file
	 * @param deleteIfExists
	 *            boolean Determines whether the copy goes on even if the file
	 *            exists.
	 * @throws IOException
	 * @return boolean Whether the copy succeeded, or was stopped due to the
	 *         file already existing.
	 */
	public static boolean copyFile(File source, File dest, boolean deleteIfExists)
		throws IOException {

		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			// Check if the file already exists.
			if (dest.exists()) {
				if (!deleteIfExists)
					return false;
				// else dest.delete();
			}

			in = new BufferedInputStream(new FileInputStream(source));
			out = new BufferedOutputStream(new FileOutputStream(dest));
			int el;
			// int tell = 0;
			while ((el = in.read()) >= 0) {
				out.write(el);
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
			if (in != null)
				in.close();
		}
		return true;
	}

	/**
	 * This method is called at startup, and makes necessary adaptations to
	 * preferences for users from an earlier version of Jabref.
	 */
	public static void performCompatibilityUpdate() {

		// Make sure "abstract" is not in General fields, because
		// Jabref 1.55 moves the abstract to its own tab.
		String genFields = Globals.prefs.get("generalFields");
		// pr(genFields+"\t"+genFields.indexOf("abstract"));
		if (genFields.indexOf("abstract") >= 0) {
			// pr(genFields+"\t"+genFields.indexOf("abstract"));
			String newGen;
			if (genFields.equals("abstract"))
				newGen = "";
			else if (genFields.indexOf(";abstract;") >= 0) {
				newGen = genFields.replaceAll(";abstract;", ";");
			} else if (genFields.indexOf("abstract;") == 0) {
				newGen = genFields.replaceAll("abstract;", "");
			} else if (genFields.indexOf(";abstract") == genFields.length() - 9) {
				newGen = genFields.replaceAll(";abstract", "");
			} else
				newGen = genFields;
			// pr(newGen);
			Globals.prefs.put("generalFields", newGen);
		}

	}

    /**
     * Collect file links from the given set of fields, and add them to the list contained
     * in the field GUIGlobals.FILE_FIELD.
     * @param database The database to modify.
     * @param fields The fields to find links in.
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    public static NamedCompound upgradePdfPsToFile(BibtexDatabase database, String[] fields) {
        NamedCompound ce = new NamedCompound(Globals.lang("Move external links to 'file' field"));
        
        for (BibtexEntry entry : database.getEntryMap().values()){
            FileListTableModel tableModel = new FileListTableModel();
            // If there are already links in the file field, keep those on top:
            String oldFileContent = entry.getField(GUIGlobals.FILE_FIELD);
            if (oldFileContent != null) {
                tableModel.setContent(oldFileContent);
            }
            int oldRowCount = tableModel.getRowCount();
            for (int j = 0; j < fields.length; j++) {
                String o = entry.getField(fields[j]);
                if (o != null) {
                    String s = o;
                    if (s.trim().length() > 0) {
                        File f = new File(s);
                        FileListEntry flEntry = new FileListEntry(f.getName(), s,
                                Globals.prefs.getExternalFileTypeByExt(fields[j]));
                        tableModel.addEntry(tableModel.getRowCount(), flEntry);
                        
                        entry.clearField(fields[j]);
                        ce.addEdit(new UndoableFieldChange(entry, fields[j], o, null));
                    }
                }
            }
            if (tableModel.getRowCount() != oldRowCount) {
                String newValue = tableModel.getStringRepresentation();
                entry.setField(GUIGlobals.FILE_FIELD, newValue);
                ce.addEdit(new UndoableFieldChange(entry, GUIGlobals.FILE_FIELD, oldFileContent, newValue));
            }
        }
        ce.end();
        return ce;
    }

    // -------------------------------------------------------------------------------

	/**
	 * extends the filename with a default Extension, if no Extension '.x' could
	 * be found
	 */
	public static String getCorrectFileName(String orgName, String defaultExtension) {
		if (orgName == null)
			return "";

		String back = orgName;
		int t = orgName.indexOf(".", 1); // hidden files Linux/Unix (?)
		if (t < 1)
			back = back + "." + defaultExtension;

		return back;
	}

	/**
	 * Quotes each and every character, e.g. '!' as &#33;. Used for verbatim
	 * display of arbitrary strings that may contain HTML entities.
	 */
	public static String quoteForHTML(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); ++i) {
			sb.append("&#" + (int) s.charAt(i) + ";");
		}
		return sb.toString();
	}

	public static String quote(String s, String specials, char quoteChar) {
		return quote(s, specials, quoteChar, 0);
	}

	/**
	 * Quote special characters.
	 * 
	 * @param s
	 *            The String which may contain special characters.
	 * @param specials
	 *            A String containing all special characters except the quoting
	 *            character itself, which is automatically quoted.
	 * @param quoteChar
	 *            The quoting character.
	 * @param linewrap
	 *            The number of characters after which a linebreak is inserted
	 *            (this linebreak is undone by unquote()). Set to 0 to disable.
	 * @return A String with every special character (including the quoting
	 *         character itself) quoted.
	 */
	public static String quote(String s, String specials, char quoteChar, int linewrap) {
		StringBuffer sb = new StringBuffer();
		char c;
		int linelength = 0;
		boolean isSpecial;
		for (int i = 0; i < s.length(); ++i) {
			c = s.charAt(i);
			isSpecial = specials.indexOf(c) >= 0 || c == quoteChar;
			// linebreak?
			if (linewrap > 0
				&& (++linelength >= linewrap || (isSpecial && linelength >= linewrap - 1))) {
				sb.append(quoteChar);
				sb.append('\n');
				linelength = 0;
			}
			if (isSpecial) {
				sb.append(quoteChar);
				++linelength;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Unquote special characters.
	 * 
	 * @param s
	 *            The String which may contain quoted special characters.
	 * @param quoteChar
	 *            The quoting character.
	 * @return A String with all quoted characters unquoted.
	 */
	public static String unquote(String s, char quoteChar) {
		StringBuffer sb = new StringBuffer();
		char c;
		boolean quoted = false;
		for (int i = 0; i < s.length(); ++i) {
			c = s.charAt(i);
			if (quoted) { // append literally...
				if (c != '\n') // ...unless newline
					sb.append(c);
				quoted = false;
			} else if (c != quoteChar) {
				sb.append(c);
			} else { // quote char
				quoted = true;
			}
		}
		return sb.toString();
	}

	/**
	 * Quote all regular expression meta characters in s, in order to search for
	 * s literally.
	 */
	public static String quoteMeta(String s) {
		// work around a bug: trailing backslashes have to be quoted
		// individually
		int i = s.length() - 1;
		StringBuffer bs = new StringBuffer("");
		while ((i >= 0) && (s.charAt(i) == '\\')) {
			--i;
			bs.append("\\\\");
		}
		s = s.substring(0, i + 1);
		return "\\Q" + s.replaceAll("\\\\E", "\\\\E\\\\\\\\E\\\\Q") + "\\E" + bs.toString();
	}

	/*
	 * This method "tidies" up e.g. a keyword string, by alphabetizing the words
	 * and removing all duplicates.
	 */
	public static String sortWordsAndRemoveDuplicates(String text) {

		String[] words = text.split(", ");
		SortedSet<String> set = new TreeSet<String>();
		for (int i = 0; i < words.length; i++)
			set.add(words[i]);
		StringBuffer sb = new StringBuffer();
		for (Iterator<String> i = set.iterator(); i.hasNext();) {
			sb.append(i.next());
			sb.append(", ");
		}
		if (sb.length() > 2)
			sb.delete(sb.length() - 2, sb.length());
		String result = sb.toString();
		return result.length() > 2 ? result : "";
	}

	/**
	 * Warns the user of undesired side effects of an explicit
	 * assignment/removal of entries to/from this group. Currently there are
	 * four types of groups: AllEntriesGroup, SearchGroup - do not support
	 * explicit assignment. ExplicitGroup - never modifies entries. KeywordGroup -
	 * only this modifies entries upon assignment/removal. Modifications are
	 * acceptable unless they affect a standard field (such as "author") besides
	 * the "keywords" field.
	 * 
	 * @param parent
	 *            The Component used as a parent when displaying a confirmation
	 *            dialog.
	 * @return true if the assignment has no undesired side effects, or the user
	 *         chose to perform it anyway. false otherwise (this indicates that
	 *         the user has aborted the assignment).
	 */
	public static boolean warnAssignmentSideEffects(AbstractGroup[] groups, BibtexEntry[] entries,
		BibtexDatabase db, Component parent) {
		Vector<String> affectedFields = new Vector<String>();
		for (int k = 0; k < groups.length; ++k) {
			if (groups[k] instanceof KeywordGroup) {
				KeywordGroup kg = (KeywordGroup) groups[k];
				String field = kg.getSearchField().toLowerCase();
				if (field.equals("keywords"))
					continue; // this is not undesired
				for (int i = 0, len = BibtexFields.numberOfPublicFields(); i < len; ++i) {
					if (field.equals(BibtexFields.getFieldName(i))) {
						affectedFields.add(field);
						break;
					}
				}
			}
		}
		if (affectedFields.size() == 0)
			return true; // no side effects

		// show a warning, then return
		StringBuffer message = // JZTODO lyrics...
		new StringBuffer("This action will modify the following field(s)\n"
			+ "in at least one entry each:\n");
		for (int i = 0; i < affectedFields.size(); ++i)
			message.append(affectedFields.elementAt(i)).append("\n");
		message.append("This could cause undesired changes to "
			+ "your entries, so it is\nrecommended that you change the grouping field "
			+ "in your group\ndefinition to \"keywords\" or a non-standard name."
			+ "\n\nDo you still want to continue?");
		int choice = JOptionPane.showConfirmDialog(parent, message, Globals.lang("Warning"),
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		return choice != JOptionPane.NO_OPTION;

		// if (groups instanceof KeywordGroup) {
		// KeywordGroup kg = (KeywordGroup) groups;
		// String field = kg.getSearchField().toLowerCase();
		// if (field.equals("keywords"))
		// return true; // this is not undesired
		// for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; ++i) {
		// if (field.equals(GUIGlobals.ALL_FIELDS[i])) {
		// // show a warning, then return
		// String message = Globals // JZTODO lyrics...
		// .lang(
		// "This action will modify the \"%0\" field "
		// + "of your entries.\nThis could cause undesired changes to "
		// + "your entries, so it is\nrecommended that you change the grouping
		// field "
		// + "in your group\ndefinition to \"keywords\" or a non-standard name."
		// + "\n\nDo you still want to continue?",
		// field);
		// int choice = JOptionPane.showConfirmDialog(parent, message,
		// Globals.lang("Warning"), JOptionPane.YES_NO_OPTION,
		// JOptionPane.WARNING_MESSAGE);
		// return choice != JOptionPane.NO_OPTION;
		// }
		// }
		// }
		// return true; // found no side effects
	}

	// ========================================================
	// lot of abreviations in medline
	// PKC etc convert to {PKC} ...
	// ========================================================
	static Pattern titleCapitalPattern = Pattern.compile("[A-Z]+");

	/**
	 * Wrap all uppercase letters, or sequences of uppercase letters, in curly
	 * braces. Ignore letters within a pair of # character, as these are part of
	 * a string label that should not be modified.
	 * 
	 * @param s
	 *            The string to modify.
	 * @return The resulting string after wrapping capitals.
	 */
	public static String putBracesAroundCapitals(String s) {

		boolean inString = false, isBracing = false, escaped = false;
		int inBrace = 0;
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			// Update variables based on special characters:
			int c = s.charAt(i);
			if (c == '{')
				inBrace++;
			else if (c == '}')
				inBrace--;
			else if (!escaped && (c == '#'))
				inString = !inString;

			// See if we should start bracing:
			if ((inBrace == 0) && !isBracing && !inString && Character.isLetter((char) c)
				&& Character.isUpperCase((char) c)) {

				buf.append('{');
				isBracing = true;
			}

			// See if we should close a brace set:
			if (isBracing && !(Character.isLetter((char) c) && Character.isUpperCase((char) c))) {

				buf.append('}');
				isBracing = false;
			}

			// Add the current character:
			buf.append((char) c);

			// Check if we are entering an escape sequence:
			if ((c == '\\') && !escaped)
				escaped = true;
			else
				escaped = false;

		}
		// Check if we have an unclosed brace:
		if (isBracing)
			buf.append('}');

		return buf.toString();

		/*
		 * if (s.length() == 0) return s; // Protect against ArrayIndexOutOf....
		 * StringBuffer buf = new StringBuffer();
		 * 
		 * Matcher mcr = titleCapitalPattern.matcher(s.substring(1)); while
		 * (mcr.find()) { String replaceStr = mcr.group();
		 * mcr.appendReplacement(buf, "{" + replaceStr + "}"); }
		 * mcr.appendTail(buf); return s.substring(0, 1) + buf.toString();
		 */
	}

	static Pattern bracedTitleCapitalPattern = Pattern.compile("\\{[A-Z]+\\}");

	/**
	 * This method looks for occurences of capital letters enclosed in an
	 * arbitrary number of pairs of braces, e.g. "{AB}" or "{{T}}". All of these
	 * pairs of braces are removed.
	 * 
	 * @param s
	 *            The String to analyze.
	 * @return A new String with braces removed.
	 */
	public static String removeBracesAroundCapitals(String s) {
		String previous = s;
		while ((s = removeSingleBracesAroundCapitals(s)).length() < previous.length()) {
			previous = s;
		}
		return s;
	}

	/**
	 * This method looks for occurences of capital letters enclosed in one pair
	 * of braces, e.g. "{AB}". All these are replaced by only the capitals in
	 * between the braces.
	 * 
	 * @param s
	 *            The String to analyze.
	 * @return A new String with braces removed.
	 */
	public static String removeSingleBracesAroundCapitals(String s) {
		Matcher mcr = bracedTitleCapitalPattern.matcher(s);
		StringBuffer buf = new StringBuffer();
		while (mcr.find()) {
			String replaceStr = mcr.group();
			mcr.appendReplacement(buf, replaceStr.substring(1, replaceStr.length() - 1));
		}
		mcr.appendTail(buf);
		return buf.toString();
	}

	/**
	 * This method looks up what kind of external binding is used for the given
	 * field, and constructs on OpenFileFilter suitable for browsing for an
	 * external file.
	 * 
	 * @param fieldName
	 *            The BibTeX field in question.
	 * @return The file filter.
	 */
	public static OpenFileFilter getFileFilterForField(String fieldName) {
		String s = BibtexFields.getFieldExtras(fieldName);
		final String ext = "." + fieldName.toLowerCase();
		final OpenFileFilter off;
		if (s.equals("browseDocZip"))
			off = new OpenFileFilter(new String[] { ext, ext + ".gz", ext + ".bz2" });
		else
			off = new OpenFileFilter(new String[] { ext });
		return off;
	}

	/**
	 * This method can be used to display a "rich" error dialog which offers the
	 * entire stack trace for an exception.
	 * 
	 * @param parent
	 * @param e
	 */
	public static void showQuickErrorDialog(JFrame parent, String title, Exception e) {
		// create and configure a text area - fill it with exception text.
		final JPanel pan = new JPanel(), details = new JPanel();
		final CardLayout crd = new CardLayout();
		pan.setLayout(crd);
		final JTextArea textArea = new JTextArea();
		textArea.setFont(new Font("Sans-Serif", Font.PLAIN, 10));
		textArea.setEditable(false);
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		textArea.setText(writer.toString());
		JLabel lab = new JLabel(e.getMessage());
		JButton flip = new JButton(Globals.lang("Details"));

		FormLayout layout = new FormLayout("left:pref", "");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.append(lab);
		builder.nextLine();
		builder.append(Box.createVerticalGlue());
		builder.nextLine();
		builder.append(flip);
		final JPanel simple = builder.getPanel();

		// stuff it in a scrollpane with a controlled size.
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(350, 150));
		details.setLayout(new BorderLayout());
		details.add(scrollPane, BorderLayout.CENTER);

		flip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				crd.show(pan, "details");
			}
		});
		pan.add(simple, "simple");
		pan.add(details, "details");
		// pass the scrollpane to the joptionpane.
		JOptionPane.showMessageDialog(parent, pan, title, JOptionPane.ERROR_MESSAGE);
	}

	public static String wrapHTML(String s, final int lineWidth) {
		StringBuffer sb = new StringBuffer();
		StringTokenizer tok = new StringTokenizer(s);
		int charsLeft = lineWidth;
		while (tok.hasMoreTokens()) {
			String word = tok.nextToken();
			if (charsLeft == lineWidth) { // fresh line
				sb.append(word);
				charsLeft -= word.length();
				if (charsLeft <= 0) {
					sb.append("<br>\n");
					charsLeft = lineWidth;
				}
			} else { // continue previous line
				if (charsLeft < word.length() + 1) {
					sb.append("<br>\n");
					sb.append(word);
					if (word.length() >= lineWidth - 1) {
						sb.append("<br>\n");
						charsLeft = lineWidth;
					} else {
						sb.append(" ");
						charsLeft = lineWidth - word.length() - 1;
					}
				} else {
					sb.append(' ').append(word);
					charsLeft -= word.length() + 1;
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Creates a String containing the current date (and possibly time),
	 * formatted according to the format set in preferences under the key
	 * "timeStampFormat".
	 * 
	 * @return The date string.
	 */
	public static String easyDateFormat() {
		// Date today = new Date();
		return easyDateFormat(new Date());
	}

	/**
	 * Creates a readable Date string from the parameter date. The format is set
	 * in preferences under the key "timeStampFormat".
	 * 
	 * @return The formatted date string.
	 */
	public static String easyDateFormat(Date date) {
		// first use, create an instance
		if (dateFormatter == null) {
			String format = Globals.prefs.get("timeStampFormat");
			dateFormatter = new SimpleDateFormat(format);
		}
		return dateFormatter.format(date);
	}

	public static void markEntry(BibtexEntry be, NamedCompound ce) {
		Object o = be.getField(BibtexFields.MARKED);
		if ((o != null) && (o.toString().indexOf(Globals.prefs.WRAPPED_USERNAME) >= 0))
			return;
		String newValue;
		if (o == null) {
			newValue = Globals.prefs.WRAPPED_USERNAME;
		} else {
			StringBuffer sb = new StringBuffer(o.toString());
			// sb.append(' ');
			sb.append(Globals.prefs.WRAPPED_USERNAME);
			newValue = sb.toString();
		}
		ce.addEdit(new UndoableFieldChange(be, BibtexFields.MARKED, be
			.getField(BibtexFields.MARKED), newValue));
		be.setField(BibtexFields.MARKED, newValue);
	}

	public static void unmarkEntry(BibtexEntry be, BibtexDatabase database, NamedCompound ce) {
		Object o = be.getField(BibtexFields.MARKED);
		if (o != null) {
			String s = o.toString();
			if (s.equals("0")) {
				unmarkOldStyle(be, database, ce);
				return;
			}

			int piv = 0, hit;
			StringBuffer sb = new StringBuffer();
			while ((hit = s.indexOf(Globals.prefs.WRAPPED_USERNAME, piv)) >= 0) {
				if (hit > 0)
					sb.append(s.substring(piv, hit));
				piv = hit + Globals.prefs.WRAPPED_USERNAME.length();
			}
			if (piv < s.length() - 1) {
				sb.append(s.substring(piv));
			}
			String newVal = sb.length() > 0 ? sb.toString() : null;
			ce.addEdit(new UndoableFieldChange(be, BibtexFields.MARKED, be
				.getField(BibtexFields.MARKED), newVal));
			be.setField(BibtexFields.MARKED, newVal);
		}
	}

	/**
	 * An entry is marked with a "0", not in the new style with user names. We
	 * want to unmark it as transparently as possible. Since this shouldn't
	 * happen too often, we do it by scanning the "owner" fields of the entire
	 * database, collecting all user names. We then mark the entry for all users
	 * except the current one. Thus only the user who unmarks will see that it
	 * is unmarked, and we get rid of the old-style marking.
	 * 
	 * @param be
	 * @param ce
	 */
	private static void unmarkOldStyle(BibtexEntry be, BibtexDatabase database, NamedCompound ce) {
		TreeSet<Object> owners = new TreeSet<Object>();
		for (BibtexEntry entry : database.getEntries()){
			Object o = entry.getField(BibtexFields.OWNER);
			if (o != null)
				owners.add(o);
			// System.out.println("Owner: "+entry.getField(Globals.OWNER));
		}
		owners.remove(Globals.prefs.get("defaultOwner"));
		StringBuffer sb = new StringBuffer();
		for (Iterator<Object> i = owners.iterator(); i.hasNext();) {
			sb.append('[');
			sb.append(i.next().toString());
			sb.append(']');
		}
		String newVal = sb.toString();
		if (newVal.length() == 0)
			newVal = null;
		ce.addEdit(new UndoableFieldChange(be, BibtexFields.MARKED, be
			.getField(BibtexFields.MARKED), newVal));
		be.setField(BibtexFields.MARKED, newVal);

	}

	public static boolean isMarked(BibtexEntry be) {
		Object fieldVal = be.getField(BibtexFields.MARKED);
		if (fieldVal == null)
			return false;
		String s = (String) fieldVal;
		return (s.equals("0") || (s.indexOf(Globals.prefs.WRAPPED_USERNAME) >= 0));
	}

	/**
	 * Set a given field to a given value for all entries in a Collection. This
	 * method DOES NOT update any UndoManager, but returns a relevant
	 * CompoundEdit that should be registered by the caller.
	 * 
	 * @param entries
	 *            The entries to set the field for.
	 * @param field
	 *            The name of the field to set.
	 * @param text
	 *            The value to set. This value can be null, indicating that the
	 *            field should be cleared.
	 * @param overwriteValues
	 *            Indicate whether the value should be set even if an entry
	 *            already has the field set.
	 * @return A CompoundEdit for the entire operation.
	 */
	public static UndoableEdit massSetField(Collection<BibtexEntry> entries, String field, String text,
		boolean overwriteValues) {

		NamedCompound ce = new NamedCompound(Globals.lang("Set field"));
		for (BibtexEntry entry : entries){
			String oldVal = entry.getField(field);
			// If we are not allowed to overwrite values, check if there is a
			// nonempty
			// value already for this entry:
			if (!overwriteValues && (oldVal != null) && ((oldVal).length() > 0))
				continue;
			if (text != null)
				entry.setField(field, text);
			else
				entry.clearField(field);
			ce.addEdit(new UndoableFieldChange(entry, field, oldVal, text));
		}
		ce.end();
		return ce;
	}

    /**
     * Move contents from one field to another for a Collection of entries.
     * @param entries The entries to do this operation for.
     * @param field The field to move contents from.
     * @param newField The field to move contents into.
     * @param overwriteValues If true, overwrites any existing values in the new field.
     *          If false, makes no change for entries with existing value in the new field.
     * @return A CompoundEdit for the entire operation.
     */
    public static UndoableEdit massRenameField(Collection<BibtexEntry> entries, String field,
                String newField, boolean overwriteValues) {
        NamedCompound ce = new NamedCompound(Globals.lang("Rename field"));
		for (BibtexEntry entry : entries){
			String valToMove = entry.getField(field);
            // If there is no value, do nothing:
            if ((valToMove == null) || (valToMove.length() == 0))
                continue;
            // If we are not allowed to overwrite values, check if there is a
			// nonempy value already for this entry for the new field:
            String valInNewField = entry.getField(newField);
            if (!overwriteValues && (valInNewField != null) && (valInNewField.length() > 0))
                continue;

			entry.setField(newField, valToMove);
            ce.addEdit(new UndoableFieldChange(entry, newField, valInNewField,valToMove));
            entry.clearField(field);
            ce.addEdit(new UndoableFieldChange(entry, field, valToMove, null));
		}
		ce.end();
		return ce;
    }

	/**
	 * Make a list of supported character encodings that can encode all
	 * characters in the given String.
	 * 
	 * @param characters
	 *            A String of characters that should be supported by the
	 *            encodings.
	 * @return A List of character encodings
	 */
	public static List<String> findEncodingsForString(String characters) {
		List<String> encodings = new ArrayList<String>();
		for (int i = 0; i < Globals.ENCODINGS.length; i++) {
			CharsetEncoder encoder = Charset.forName(Globals.ENCODINGS[i]).newEncoder();
			if (encoder.canEncode(characters))
				encodings.add(Globals.ENCODINGS[i]);
		}
		return encodings;
	}

	/**
	 * Will convert a two digit year using the following scheme (describe at
	 * http://www.filemaker.com/help/02-Adding%20and%20view18.html):
	 * 
	 * If a two digit year is encountered they are matched against the last 69
	 * years and future 30 years.
	 * 
	 * For instance if it is the year 1992 then entering 23 is taken to be 1923
	 * but if you enter 23 in 1993 then it will evaluate to 2023.
	 * 
	 * @param year
	 *            The year to convert to 4 digits.
	 * @return
	 */
	public static String toFourDigitYear(String year) {
		if (thisYear == 0) {
			thisYear = Calendar.getInstance().get(Calendar.YEAR);
		}
		return toFourDigitYear(year, thisYear);
	}

	public static int thisYear;

	/**
	 * Will convert a two digit year using the following scheme (describe at
	 * http://www.filemaker.com/help/02-Adding%20and%20view18.html):
	 * 
	 * If a two digit year is encountered they are matched against the last 69
	 * years and future 30 years.
	 * 
	 * For instance if it is the year 1992 then entering 23 is taken to be 1923
	 * but if you enter 23 in 1993 then it will evaluate to 2023.
	 * 
	 * @param year
	 *            The year to convert to 4 digits.
	 * @return
	 */
	public static String toFourDigitYear(String year, int thisYear) {
		if (year.length() != 2)
			return year;
		try {
			int thisYearTwoDigits = thisYear % 100;
			int thisCentury = thisYear - thisYearTwoDigits;

			int yearNumber = Integer.parseInt(year);

			if (yearNumber == thisYearTwoDigits) {
				return String.valueOf(thisYear);
			}
			// 20 , 90
			// 99 > 30
			if ((yearNumber + 100 - thisYearTwoDigits) % 100 > 30) {
				if (yearNumber < thisYearTwoDigits) {
					return String.valueOf(thisCentury + yearNumber);
				} else {
					return String.valueOf(thisCentury - 100 + yearNumber);
				}
			} else {
				if (yearNumber < thisYearTwoDigits) {
					return String.valueOf(thisCentury + 100 + yearNumber);
				} else {
					return String.valueOf(thisCentury + yearNumber);
				}
			}
		} catch (NumberFormatException e) {
			return year;
		}
	}

	/**
	 * Will return an integer indicating the month of the entry from 0 to 11.
	 * 
	 * -1 signals a unknown month string.
	 * 
	 * This method accepts three types of months given:
	 *  - Single and Double Digit months from 1 to 12 (01 to 12)
	 *  - 3 Digit BibTex strings (jan, feb, mar...)
	 *  - Full English Month identifiers.
	 * 
	 * @param month
	 * @return
	 */
	public static int getMonthNumber(String month) {

		month = month.replaceAll("#", "").toLowerCase();

		for (int i = 0; i < Globals.MONTHS.length; i++) {
			if (month.startsWith(Globals.MONTHS[i])) {
				return i;
			}
		}

		try {
			return Integer.parseInt(month) - 1;
		} catch (NumberFormatException e) {
		}
		return -1;
	}


    /**
     * Encodes a two-dimensional String array into a single string, using ':' and
     * ';' as separators. The characters ':' and ';' are escaped with '\'.
     * @param values The String array.
     * @return The encoded String.
     */
    public static String encodeStringArray(String[][] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            sb.append(encodeStringArray(values[i]));
            if (i < values.length-1)
                sb.append(';');
        }
        return sb.toString();
    }

    /**
     * Encodes a String array into a single string, using ':' as separator.
     * The characters ':' and ';' are escaped with '\'.
     * @param entry The String array.
     * @return The encoded String.
     */
    public static String encodeStringArray(String[] entry) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entry.length; i++) {
            sb.append(encodeString(entry[i]));
            if (i < entry.length-1)
                sb.append(':');

        }
        return sb.toString();
    }

    /**
     * Decodes an encoded double String array back into array form. The array
     * is assumed to be square, and delimited by the characters ';' (first dim) and
     * ':' (second dim).
     * @param value The encoded String to be decoded.
     * @return The decoded String array.
     */
    public static String[][] decodeStringDoubleArray(String value) {
        ArrayList<ArrayList<String>> newList = new ArrayList<ArrayList<String>>();
        StringBuilder sb = new StringBuilder();
        ArrayList<String> thisEntry = new ArrayList<String>();
        boolean escaped = false;
        for (int i=0; i<value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && (c == '\\')) {
                escaped = true;
                continue;
            }
            else if (!escaped && (c == ':')) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
            }
            else if (!escaped && (c == ';')) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
                newList.add(thisEntry);
                thisEntry = new ArrayList<String>();
            }
            else sb.append(c);
            escaped = false;
        }
        if (sb.length() > 0)
            thisEntry.add(sb.toString());
        if (thisEntry.size() > 0)
            newList.add(thisEntry);

        // Convert to String[][]:
        String[][] res = new String[newList.size()][];
        for (int i = 0; i < res.length; i++) {
            res[i] = new String[newList.get(i).size()];
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = newList.get(i).get(j);
            }
        }
        return res;
    }

    private static String encodeString(String s) {
        if (s == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if ((c == ';') || (c == ':') || (c == '\\'))
                sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    /**
	 * Static equals that can also return the right result when one of the
	 * objects is null.
	 * 
	 * @param one
	 *            The object whose equals method is called if the first is not
	 *            null.
	 * @param two
	 *            The object passed to the first one if the first is not null.
	 * @return <code>one == null ? two == null : one.equals(two);</code>
	 */
	public static boolean equals(Object one, Object two) {
		return one == null ? two == null : one.equals(two);
	}

	/**
	 * Returns the given string but with the first character turned into an
	 * upper case character.
	 * 
	 * Example: testTest becomes TestTest
	 * 
	 * @param string
	 *            The string to change the first character to upper case to.
	 * @return A string has the first character turned to upper case and the
	 *         rest unchanged from the given one.
	 */
	public static String toUpperFirstLetter(String string){
		if (string == null)
			throw new IllegalArgumentException();
		
		if (string.length() == 0)
			return string;
		
		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}


    /**
     * Run an AbstractWorker's methods using Spin features to put each method
     * on the correct thread.
     * @param worker The worker to run.
     * @throws Throwable 
     */
    public static void runAbstractWorker(AbstractWorker worker) throws Throwable {
        // This part uses Spin's features:
        Worker wrk = worker.getWorker();
        // The Worker returned by getWorker() has been wrapped
        // by Spin.off(), which makes its methods be run in
        // a different thread from the EDT.
        CallBack clb = worker.getCallBack();

        worker.init(); // This method runs in this same thread, the EDT.
        // Useful for initial GUI actions, like printing a message.

        // The CallBack returned by getCallBack() has been wrapped
        // by Spin.over(), which makes its methods be run on
        // the EDT.
        wrk.run(); // Runs the potentially time-consuming action
        // without freezing the GUI. The magic is that THIS line
        // of execution will not continue until run() is finished.
        clb.update(); // Runs the update() method on the EDT.
    }

    /**
     * This method checks whether there is a lock file for the given file. If
     * there is, it waits for 500 ms. This is repeated until the lock is gone
     * or we have waited the maximum number of times.
     *
     * @param file The file to check the lock for.
     * @param maxWaitCount The maximum number of times to wait.
     * @return true if the lock file is gone, false if it is still there.
     */
    public static boolean waitForFileLock(File file, int maxWaitCount) {
        // Check if the file is locked by another JabRef user:
        int lockCheckCount = 0;
        while (Util.hasLockFile(file)) {

            System.out.println("File locked... waiting");
            if (lockCheckCount++ == maxWaitCount) {
                System.out.println("Giving up wait.");
                return false;
            }
            try { Thread.sleep(500); } catch (InterruptedException ex) {}
        }
        return true;
    }

    /**
     * Check whether a lock file exists for this file.
     * @param file The file to check.
     * @return true if a lock file exists, false otherwise.
     */
    public static boolean hasLockFile(File file) {
        File lock = new File(file.getPath()+ SaveSession.LOCKFILE_SUFFIX);
        return lock.exists();
    }

    /**
     * Find the lock file's last modified time, if it has a lock file.
     * @param file The file to check.
     * @return the last modified time if lock file exists, -1 otherwise.
     */
    public static long getLockFileTimeStamp(File file) {
        File lock = new File(file.getPath()+ SaveSession.LOCKFILE_SUFFIX);
        return lock.exists() ? lock.lastModified() : -1;
    }

        /**
     * Check if a lock file exists, and delete it if it does.
     * @return true if the lock file existed, false otherwise.
     * @throws IOException if something goes wrong.
     */
    public static boolean deleteLockFile(File file) {
        File lock = new File(file.getPath()+SaveSession.LOCKFILE_SUFFIX);
        if (!lock.exists()) {
            return false;
        }
        lock.delete();
        return true;
    }

    /**
     * Build a String array containing all those elements of all that are not
     * in subset.
     * @param all The array of all values.
     * @param subset The subset of values.
     * @return The remainder that is not part of the subset.
     */
    public static String[] getRemainder(String[] all, String[] subset) {
        ArrayList<String> al = new ArrayList<String>();
        for (int i = 0; i < all.length; i++) {
            boolean found = false;
            inner: for (int j = 0; j < subset.length; j++) {
                if (subset[j].equals(all[i])) {
                    found = true;
                    break inner;
                }
            }
            if (!found) al.add(all[i]);
        }
        return al.toArray(new String[al.size()]);
    }
}