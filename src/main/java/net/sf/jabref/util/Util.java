/*  Copyright (C) 2003-2012 JabRef contributors.
    Copyright (C) 2015 Oliver Kopp

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

// created by : Morten O. Alver 2003

package net.sf.jabref.util;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.CallBack;
import net.sf.jabref.EasyDateFormat;
import net.sf.jabref.EntryMarker;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.ImportSettingsTab;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.OpenFileFilter;
import net.sf.jabref.Worker;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypeEntryEditor;
import net.sf.jabref.external.RegExpFileSearch;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * utility functions
 */
public class Util {

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");
    
    private static final EasyDateFormat dateFormatter = new EasyDateFormat();

    public static void pr(String s) {
        Globals.logger(s);
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
        if (content.length() == 0) {
            return content;
        }

        String[] strings = content.split("#");
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
            String s = string.trim();
            if (s.length() > 0) {
                char c = s.charAt(0);
                // String reference or not?
                if ((c == '{') || (c == '"')) {
                    result.append(StringUtil.shaveString(string));
                } else {
                    // This part should normally be a string reference, but if it's
                    // a pure number, it is not.
                    String s2 = StringUtil.shaveString(s);
                    if(isInteger(s2)) {
                        result.append(s2);
                    } else {
                        result.append('#').append(s2).append('#');
                    }
                }
            }
        }
        return result.toString();
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
        if (o == null) {
            return null;
        }

        String year = YearUtil.toFourDigitYear(o.toString());

        o = entry.getField("month");
        if (o != null) {
            MonthUtil.Month month = MonthUtil.getMonth(o.toString());
            if (month.isValid()) {
                return year + "-" + month.twoDigitNumber;
            }
        }
        return year;
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
        if (key == null) {
            return null;
        }
        if (!JabRefPreferences.getInstance().getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY)) {
            // User doesn't want us to enforce legal characters. We must still look
            // for whitespace and some characters such as commas, since these would
            // interfere with parsing:
            StringBuilder newKey = new StringBuilder();
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (!Character.isWhitespace(c) && (c != '{') && (c != '\\') && (c != '"')
                        && (c != '}') && (c != ',')) {
                    newKey.append(c);
                }
            }
            return newKey.toString();

        }
        StringBuilder newKey = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isWhitespace(c) && (c != '#') && (c != '{') && (c != '\\') && (c != '"')
                    && (c != '}') && (c != '~') && (c != ',') && (c != '^') && (c != '\'')) {
                newKey.append(c);
            }
        }

        // Replace non-english characters like umlauts etc. with a sensible
        // letter or letter combination that bibtex can accept.

        return Util.replaceSpecialCharacters(newKey.toString());
    }

    /**
     * Replace non-english characters like umlauts etc. with a sensible letter
     * or letter combination that bibtex can accept. The basis for replacement
     * is the HashMap GLobals.UNICODE_CHARS.
     */
    public static String replaceSpecialCharacters(String s) {
        for (Map.Entry<String, String> chrAndReplace : Globals.UNICODE_CHARS.entrySet()) {
            s = s.replaceAll(chrAndReplace.getKey(), chrAndReplace.getValue());
        }
        return s;
    }

    public static TreeSet<String> findDeliminatedWordsInField(BibtexDatabase db, String field,
            String deliminator) {
        TreeSet<String> res = new TreeSet<String>();

        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            Object o = be.getField(field);
            if (o != null) {
                String fieldValue = o.toString().trim();
                StringTokenizer tok = new StringTokenizer(fieldValue, deliminator);
                while (tok.hasMoreTokens()) {
                    res.add(StringUtil.nCase(tok.nextToken().trim()));
                }
            }
        }
        return res;
    }

    /**
     * Returns a HashMap containing all words used in the database in the given
     * field type. Characters in <code>remove</code> are not included.
     * 
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
        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            Object o = be.getField(field);
            if (o != null) {
                tok = new StringTokenizer(o.toString(), remove, false);
                while (tok.hasMoreTokens()) {
                    res.add(StringUtil.nCase(tok.nextToken().trim()));
                }
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
        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            for (String field : fields) {
                String val = be.getField(field);
                if ((val != null) && (val.length() > 0)) {
                    AuthorList al = AuthorList.getAuthorList(val);
                    for (int i = 0; i < al.size(); i++) {
                        AuthorList.Author a = al.getAuthor(i);
                        String lastName = a.getLast();
                        if ((lastName != null) && (lastName.length() > 0)) {
                            res.add(lastName);
                        }
                    }
                }

            }
        }

        return res;
    }

    /**
     * Open a http/pdf/ps viewer for the given link string.
     */
    public static void openExternalViewer(MetaData metaData, String link, String fieldName)
            throws IOException {

        if (fieldName.equals("ps") || fieldName.equals("pdf")) {

            // Find the default directory for this field type:
            String[] dir = metaData.getFileDirectory(fieldName);

            File file = FileUtil.expandFilename(link, dir);

            // Check that the file exists:
            if ((file == null) || !file.exists()) {
                throw new IOException(Globals.lang("File not found") + " (" + fieldName + "): '"
                        + link + "'.");
            }
            link = file.getCanonicalPath();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.getName().split("\\.");
            if (split.length >= 2) {
                if (split[split.length - 1].equalsIgnoreCase("pdf")) {
                    fieldName = "pdf";
                } else if (split[split.length - 1].equalsIgnoreCase("ps")
                        || ((split.length >= 3) && split[split.length - 2].equalsIgnoreCase("ps"))) {
                    fieldName = "ps";
                }
            }

        } else if (fieldName.equals("doi")) {
            fieldName = "url";

            // sanitizing is done below at the treatment of "URL"
            // in sanatizeUrl a doi-link is correctly treated

        } else if (fieldName.equals("eprint")) {
            fieldName = "url";

            link = Util.sanitizeUrl(link);

            // Check to see if link field already contains a well formated URL
            if (!link.startsWith("http://")) {
                link = Globals.ARXIV_LOOKUP_PREFIX + link;
            }
        }

        if (fieldName.equals("url")) { // html
            try {
                Util.openBrowser(link);
            } catch (IOException e) {
                System.err.println(Globals.lang("Error_opening_file_'%0'.", link));
                e.printStackTrace();
            }
        } else if (fieldName.equals("ps")) {
            try {
                if (Globals.ON_MAC) {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("ps");
                    String viewer = type != null ? type.getOpenWith() : Globals.prefs.get("psviewer");
                    String[] cmd = {"/usr/bin/open", "-a", viewer, link};
                    Runtime.getRuntime().exec(cmd);
                } else if (Globals.ON_WIN) {
                    Util.openFileOnWindows(link, true);
                    /*
                     * cmdArray[0] = Globals.prefs.get("psviewer"); cmdArray[1] =
                     * link; Process child = Runtime.getRuntime().exec(
                     * cmdArray[0] + " " + cmdArray[1]);
                     */
                } else {
                    ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("ps");
                    String viewer = type != null ? type.getOpenWith() : "xdg-open";
                    String[] cmdArray = new String[2];
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
                    String[] cmd = {"/usr/bin/open", "-a", viewer, link};
                    Runtime.getRuntime().exec(cmd);
                } else if (Globals.ON_WIN) {
                    Util.openFileOnWindows(link, true);
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
                    String[] cmdArray = new String[2];
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
    private static void openFileOnWindows(String link, boolean localFile) throws IOException {
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
    private static void openFileWithApplicationOnWindows(String link, String application)
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

        if (Util.REMOTE_LINK_PATTERN.matcher(link.toLowerCase()).matches()) {
            httpLink = true;
        }
        /*if (link.toLowerCase().startsWith("file://")) {
            link = link.substring(7);
        }
        final String ln = link;
        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase()).matches()) {
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

        if (!httpLink) {
            File tmp = FileUtil.expandFilename(metaData, link);
            if (tmp != null) {
                file = tmp;
            }
        }

        // Check if we have arrived at a file type, and either an http link or an existing file:
        if ((httpLink || file.exists()) && (fileType != null)) {
            // Open the file:
            String filePath = httpLink ? link : file.getPath();
            Util.openExternalFilePlatformIndependent(fileType, filePath);
            return true;

        } else {

            return false;
            // No file matched the name, or we didn't know the file type.

        }

    }

    private static void openExternalFilePlatformIndependent(ExternalFileType fileType, String filePath) throws IOException {
        // For URLs, other solutions are
        //  * https://github.com/rajing/browserlauncher2, but it is not available in maven
        //  * a the solution combining http://stackoverflow.com/a/5226244/873282 and http://stackoverflow.com/a/28807079/873282
        if (Globals.ON_MAC) {
            // Use "-a <application>" if the app is specified, and just "open <filename>" otherwise:
            String[] cmd = ((fileType.getOpenWith() != null) && (fileType.getOpenWith().length() > 0)) ?
                    new String[] {"/usr/bin/open", "-a", fileType.getOpenWith(), filePath} :
                    new String[] {"/usr/bin/open", filePath};
            Runtime.getRuntime().exec(cmd);
        } else if (Globals.ON_WIN) {
            if ((fileType.getOpenWith() != null) && (fileType.getOpenWith().length() > 0)) {
                // Application is specified. Use it:
                Util.openFileWithApplicationOnWindows(filePath, fileType.getOpenWith());
            } else {
                Util.openFileOnWindows(filePath, true);
            }
        } else {
            // Use the given app if specified, and the universal "xdg-open" otherwise:
            String[] openWith;
            if ((fileType.getOpenWith() != null) && (fileType.getOpenWith().length() > 0)) {
                openWith = fileType.getOpenWith().split(" ");
            } else {
                openWith = new String[] {"xdg-open"};
            }

            String[] cmdArray = new String[openWith.length + 1];
            System.arraycopy(openWith, 0, cmdArray, 0, openWith.length);
            cmdArray[cmdArray.length - 1] = filePath;
            Runtime.getRuntime().exec(cmdArray);
        }
    }

    public static void openRemoteExternalFile(final MetaData metaData,
            final String link, final ExternalFileType fileType) {
        File temp = null;
        try {
            temp = File.createTempFile("jabref-link", "." + fileType.getExtension());
            temp.deleteOnExit();
            System.out.println("Downloading to '" + temp.getPath() + "'");
            new URLDownload(new URL(link)).downloadToFile(temp);
            System.out.println("Done");
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        final String ln = temp.getPath();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    Util.openExternalFileAnyFormat(metaData, ln, fileType);
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
                Collections.addAll(fileTypes, oldTypes);
                fileTypes.add(newType);
                Collections.sort(fileTypes);
                Globals.prefs.setExternalFileTypes(fileTypes);
                // Finally, open the file:
                return Util.openExternalFileAnyFormat(metaData, link, newType);
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
            for (int i = 0; i < tModel.getRowCount(); i++) {
                FileListEntry iEntry = tModel.getEntry(i);
                if (iEntry.getLink().equals(link)) {
                    flEntry = iEntry;
                    break;
                }
            }
            if (flEntry == null) {
                // This shouldn't happen, so I'm not sure what to put in here:
                throw new RuntimeException("Could not find the file list entry " + link + " in " + entry.toString());
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
                return Util.openExternalFileAnyFormat(metaData, flEntry.getLink(), flEntry.getType());
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
     * A call to this method will also remove \\url{} enclosings and clean DOI links.
	 * 
	 * @param link :the URL to sanitize.
	 * @return Sanitized URL
	 */
    public static String sanitizeUrl(String link) {
        link = link.trim();

        // First check if it is enclosed in \\url{}. If so, remove
// the wrapper.
        if (link.startsWith("\\url{") && link.endsWith("}")) {
            link = link.substring(5, link.length() - 1);
        }

        if (link.matches("^doi:/*.*")) {
            // Remove 'doi:'
            link = link.replaceFirst("^doi:/*", "");
            link = Globals.DOI_LOOKUP_PREFIX + link;
        }

        // converts doi-only link to full http address
        // Morten Alver 6 Nov 2012: this extracts a nonfunctional DOI from some complete
        // http addresses (e.g. http://onlinelibrary.wiley.com/doi/10.1002/rra.999/abstract, where
        // the trailing "/abstract" is included but doesn't lead to a resolvable DOI).
        // To prevent mangling of working URLs I'm disabling this check if the link is already
        // a full http link:
        if (DOIUtil.checkForPlainDOI(link) && !link.startsWith("http://")) {
            link = Globals.DOI_LOOKUP_PREFIX + DOIUtil.getDOI(link);
        }

        link = link.replaceAll("\\+", "%2B");

        try {
            link = URLDecoder.decode(link, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
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

    public static ArrayList<String[]> parseMethodsCalls(String calls) throws RuntimeException {

        ArrayList<String[]> result = new ArrayList<String[]>();

        char[] c = calls.toCharArray();

        int i = 0;

        while (i < c.length) {

            int start = i;
            if (Character.isJavaIdentifierStart(c[i])) {
                i++;
                while ((i < c.length) && (Character.isJavaIdentifierPart(c[i]) || (c[i] == '.'))) {
                    i++;
                }
                if ((i < c.length) && (c[i] == '(')) {

                    String method = calls.substring(start, i);

                    // Skip the brace
                    i++;

                    if (i < c.length) {
                        if (c[i] == '"') {
                            // Parameter is in format "xxx"

                            // Skip "
                            i++;

                            int startParam = i;
                            i++;
                            boolean escaped = false;
                            while (((i + 1) < c.length) &&
                                    !(!escaped && (c[i] == '"') && (c[i + 1] == ')'))) {
                                if (c[i] == '\\') {
                                    escaped = !escaped;
                                } else {
                                    escaped = false;
                                }
                                i++;

                            }

                            String param = calls.substring(startParam, i);

                            result.add(new String[] {method, param});
                        } else {
                            // Parameter is in format xxx

                            int startParam = i;

                            while ((i < c.length) && (c[i] != ')')) {
                                i++;
                            }

                            String param = calls.substring(startParam, i);

                            result.add(new String[] {method, param});

                        }
                    } else {
                        // Incorrecly terminated open brace
                        result.add(new String[] {method});
                    }
                } else {
                    String method = calls.substring(start, i);
                    result.add(new String[] {method});
                }
            }
            i++;
        }

        return result;
    }


    private static final Pattern squareBracketsPattern = Pattern.compile("\\[.*?\\]");


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
        Matcher m = Util.squareBracketsPattern.matcher(bracketString);
        StringBuffer s = new StringBuffer();
        while (m.find()) {
            String replacement = Util.getFieldAndFormat(m.group(), entry, database);
            if (replacement == null) {
                replacement = "";
            }
            m.appendReplacement(s, replacement);
        }
        m.appendTail(s);

        return s.toString();
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

        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);

        String defaultOwner = Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER);
        String timestamp = dateFormatter.getCurrentDate();
        boolean globalSetOwner = Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER), globalSetTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP);

        // Do not need to do anything if all options are disabled
        if (!(globalSetOwner || globalSetTimeStamp || markEntries)) {
            return;
        }

        // Iterate through all entries
        for (BibtexEntry curEntry : bibs) {
            boolean setOwner = globalSetOwner &&
                    (overwriteOwner || (curEntry.getField(BibtexFields.OWNER) == null));
            boolean setTimeStamp = globalSetTimeStamp &&
                    (overwriteTimestamp || (curEntry.getField(timeStampField) == null));
            Util.setAutomaticFields(curEntry, setOwner, defaultOwner, setTimeStamp, timeStampField,
                    timestamp);
            if (markEntries) {
                EntryMarker.markEntry(curEntry, EntryMarker.IMPORT_MARK_LEVEL, false, new NamedCompound(""));
            }
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
        String defaultOwner = Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER);
        String timestamp = dateFormatter.getCurrentDate();
        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
        boolean setOwner = Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER) &&
                (overwriteOwner || (entry.getField(BibtexFields.OWNER) == null));
        boolean setTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP) &&
                (overwriteTimestamp || (entry.getField(timeStampField) == null));

        Util.setAutomaticFields(entry, setOwner, defaultOwner, setTimeStamp, timeStampField, timestamp);
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

        if (setTimeStamp) {
            entry.setField(timeStampField, timeStamp);
        }
    }

    /**
     * This method is called at startup, and makes necessary adaptations to
     * preferences for users from an earlier version of Jabref.
     */
    public static void performCompatibilityUpdate() {

        // Make sure "abstract" is not in General fields, because
        // Jabref 1.55 moves the abstract to its own tab.
        String genFields = Globals.prefs.get(JabRefPreferences.GENERAL_FIELDS);
        // pr(genFields+"\t"+genFields.indexOf("abstract"));
        if (genFields.contains("abstract")) {
            // pr(genFields+"\t"+genFields.indexOf("abstract"));
            String newGen;
            if (genFields.equals("abstract")) {
                newGen = "";
            } else if (genFields.contains(";abstract;")) {
                newGen = genFields.replaceAll(";abstract;", ";");
            } else if (genFields.indexOf("abstract;") == 0) {
                newGen = genFields.replaceAll("abstract;", "");
            } else if (genFields.indexOf(";abstract") == (genFields.length() - 9)) {
                newGen = genFields.replaceAll(";abstract", "");
            } else {
                newGen = genFields;
            }
            // pr(newGen);
            Globals.prefs.put(JabRefPreferences.GENERAL_FIELDS, newGen);
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
        return Util.upgradePdfPsToFile(database.getEntryMap().values(), fields);
    }

    /**
     * Collect file links from the given set of fields, and add them to the list contained
     * in the field GUIGlobals.FILE_FIELD.
     * @param entries The entries to modify.
     * @param fields The fields to find links in.
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    public static NamedCompound upgradePdfPsToFile(Collection<BibtexEntry> entries, String[] fields) {
        NamedCompound ce = new NamedCompound(Globals.lang("Move external links to 'file' field"));

        for (BibtexEntry entry : entries) {
            FileListTableModel tableModel = new FileListTableModel();
            // If there are already links in the file field, keep those on top:
            String oldFileContent = entry.getField(GUIGlobals.FILE_FIELD);
            if (oldFileContent != null) {
                tableModel.setContent(oldFileContent);
            }
            int oldRowCount = tableModel.getRowCount();
            for (String field : fields) {
                String o = entry.getField(field);
                if (o != null) {
                    if (o.trim().length() > 0) {
                        File f = new File(o);
                        FileListEntry flEntry = new FileListEntry(f.getName(), o,
                                Globals.prefs.getExternalFileTypeByExt(field));
                        tableModel.addEntry(tableModel.getRowCount(), flEntry);

                        entry.clearField(field);
                        ce.addEdit(new UndoableFieldChange(entry, field, o, null));
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
        for (AbstractGroup group : groups) {
            if (group instanceof KeywordGroup) {
                KeywordGroup kg = (KeywordGroup) group;
                String field = kg.getSearchField().toLowerCase();
                if (field.equals("keywords"))
                 {
                    continue; // this is not undesired
                }
                for (int i = 0, len = BibtexFields.numberOfPublicFields(); i < len; ++i) {
                    if (field.equals(BibtexFields.getFieldName(i))) {
                        affectedFields.add(field);
                        break;
                    }
                }
            }
        }
        if (affectedFields.size() == 0)
         {
            return true; // no side effects
        }

        // show a warning, then return
        StringBuffer message = // JZTODO lyrics...
        new StringBuffer("This action will modify the following field(s)\n"
                + "in at least one entry each:\n");
        for (int i = 0; i < affectedFields.size(); ++i) {
            message.append(affectedFields.elementAt(i)).append("\n");
        }
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
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            // Update variables based on special characters:
            int c = s.charAt(i);
            if (c == '{') {
                inBrace++;
            } else if (c == '}') {
                inBrace--;
            } else if (!escaped && (c == '#')) {
                inString = !inString;
            }

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
            escaped = (c == '\\') && !escaped;

        }
        // Check if we have an unclosed brace:
        if (isBracing) {
            buf.append('}');
        }

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


    private static final Pattern BRACED_TITLE_CAPITAL_PATTERN = Pattern.compile("\\{[A-Z]+\\}");


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
        while ((s = Util.removeSingleBracesAroundCapitals(s)).length() < previous.length()) {
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
    private static String removeSingleBracesAroundCapitals(String s) {
        Matcher mcr = Util.BRACED_TITLE_CAPITAL_PATTERN.matcher(s);
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
        if ("browseDocZip".equals(s)) {
            off = new OpenFileFilter(new String[] {ext, ext + ".gz", ext + ".bz2"});
        } else {
            off = new OpenFileFilter(new String[] {ext});
        }
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

            @Override
            public void actionPerformed(ActionEvent event) {
                crd.show(pan, "details");
            }
        });
        pan.add(simple, "simple");
        pan.add(details, "details");
        // pass the scrollpane to the joptionpane.
        JOptionPane.showMessageDialog(parent, pan, title, JOptionPane.ERROR_MESSAGE);
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
        for (BibtexEntry entry : entries) {
            String oldVal = entry.getField(field);
            // If we are not allowed to overwrite values, check if there is a
            // nonempty
            // value already for this entry:
            if (!overwriteValues && (oldVal != null) && ((oldVal).length() > 0)) {
                continue;
            }
            if (text != null) {
                entry.setField(field, text);
            } else {
                entry.clearField(field);
            }
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
        for (BibtexEntry entry : entries) {
            String valToMove = entry.getField(field);
            // If there is no value, do nothing:
            if ((valToMove == null) || (valToMove.length() == 0)) {
                continue;
            }
            // If we are not allowed to overwrite values, check if there is a
            // nonempy value already for this entry for the new field:
            String valInNewField = entry.getField(newField);
            if (!overwriteValues && (valInNewField != null) && (valInNewField.length() > 0)) {
                continue;
            }

            entry.setField(newField, valToMove);
            ce.addEdit(new UndoableFieldChange(entry, newField, valInNewField, valToMove));
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
            if (encoder.canEncode(characters)) {
                encodings.add(Globals.ENCODINGS[i]);
            }
        }
        return encodings;
    }

    /**
     * From http://stackoverflow.com/questions/1030479/most-efficient-way-of-converting-string-to-integer-in-java
     *
     * @param str
     * @return
     */
    public static int intValueOf(String str) {
        int ival = 0, idx = 0, end;
        boolean sign = false;
        char ch;

        if ((str == null) || ((end = str.length()) == 0) ||
                ((((ch = str.charAt(0)) < '0') || (ch > '9'))
                && (!(sign = ch == '-') || (++idx == end) || (((ch = str.charAt(idx)) < '0') || (ch > '9'))))) {
            throw new NumberFormatException(str);
        }

        for (;; ival *= 10)
        {
            ival += '0' - ch;
            if (++idx == end) {
                return sign ? ival : -ival;
            }
            if (((ch = str.charAt(idx)) < '0') || (ch > '9')) {
                throw new NumberFormatException(str);
            }
        }
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
            sb.append(Util.encodeStringArray(values[i]));
            if (i < (values.length - 1)) {
                sb.append(';');
            }
        }
        return sb.toString();
    }

    /**
     * Encodes a String array into a single string, using ':' as separator.
     * The characters ':' and ';' are escaped with '\'.
     * @param entry The String array.
     * @return The encoded String.
     */
    private static String encodeStringArray(String[] entry) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entry.length; i++) {
            sb.append(Util.encodeString(entry[i]));
            if (i < (entry.length - 1)) {
                sb.append(':');
            }

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
        for (int i = 0; i < value.length(); i++) {
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
            } else {
                sb.append(c);
            }
            escaped = false;
        }
        if (sb.length() > 0) {
            thisEntry.add(sb.toString());
        }
        if (thisEntry.size() > 0) {
            newList.add(thisEntry);
        }

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

    public static String encodeString(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == ';') || (c == ':') || (c == '\\')) {
                sb.append('\\');
            }
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
     * Build a String array containing all those elements of all that are not
     * in subset.
     * @param all The array of all values.
     * @param subset The subset of values.
     * @return The remainder that is not part of the subset.
     */
    public static String[] getRemainder(String[] all, String[] subset) {
        ArrayList<String> al = new ArrayList<String>();
        for (String anAll : all) {
            boolean found = false;
            for (String aSubset : subset) {
                if (aSubset.equals(anAll)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                al.add(anAll);
            }
        }
        return al.toArray(new String[al.size()]);
    }

    /**
     * Determines filename provided by an entry in a database
     *
     * @param database the database, where the entry is located
     * @param entry the entry to which the file should be linked to
     * @return a suggested fileName
     */
    public static String getLinkedFileName(BibtexDatabase database, BibtexEntry entry) {
        String targetName = entry.getCiteKey() == null ? "default" : entry.getCiteKey();
        StringReader sr = new StringReader(Globals.prefs.get(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN));
        Layout layout = null;
        try {
            layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);
        } catch (Exception e) {
            Globals.logger(Globals.lang("Wrong Format").concat(" ").concat(e.toString()));
        }
        if (layout != null) {
            targetName = layout.doLayout(entry, database);
        }
        //Removes illegal characters from filename
        targetName = FileNameCleaner.cleanFileName(targetName);
        return targetName;
    }

    public static ArrayList<String> getSeparatedKeywords(String keywords) {
        ArrayList<String> res = new ArrayList<String>();
        if (keywords == null) {
            return res;
        }
        // _NOSPACE is a hack to support keywords such as "choreography transactions"
        // a more intelligent algorithm would check for the separator chosen (SEPARATING_CHARS_NOSPACE)
        // if nothing is found, " " is likely to be the separating char.
        // solution by RisKeywords.java: s.split(",[ ]*")
        StringTokenizer tok = new StringTokenizer(keywords, Globals.SEPARATING_CHARS_NOSPACE);
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken().trim();
            res.add(word);
        }
        return res;
    }

    public static ArrayList<String> getSeparatedKeywords(BibtexEntry be) {
        return Util.getSeparatedKeywords(be.getField("keywords"));
    }

    public static void putKeywords(BibtexEntry entry, ArrayList<String> keywords, NamedCompound ce) {
        // Set Keyword Field
        String oldValue = entry.getField("keywords");
        String newValue;
        if (keywords.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String keyword : keywords) {
                sb.append(keyword);
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            newValue = sb.toString();
        } else {
            newValue = null;
        }
        if ((oldValue == null) && (newValue == null)) {
            return;
        }
        if ((oldValue == null) || (!oldValue.equals(newValue))) {
            entry.setField("keywords", newValue);
            if (ce != null) {
                ce.addEdit(new UndoableFieldChange(entry, "keywords", oldValue, newValue));
            }
        }
    }

    /**
     * @param ce indicates the undo named compound. May be null
     */
    public static void updateField(BibtexEntry be, String field, String newValue, NamedCompound ce) {
        Util.updateField(be, field, newValue, ce, false);
    }

    /**
     * @param ce indicates the undo named compound. May be null
     */
    public static void updateField(BibtexEntry be, String field, String newValue, NamedCompound ce, Boolean nullFieldIfValueIsTheSame) {
        String oldValue = be.getField(field);
        if (nullFieldIfValueIsTheSame && (oldValue != null) && (oldValue.equals(newValue))) {
            // if oldValue == newValue then reset field if required by parameter
            newValue = null;
        }
        if ((oldValue == null) && (newValue == null)) {
            return;
        }
        if ((oldValue == null) || (!oldValue.equals(newValue))) {
            be.setField(field, newValue);
            if (ce != null) {
                ce.addEdit(new UndoableFieldChange(be, field, oldValue, newValue));
            }
        }
    }

    /**
     * Binds ESC-Key to cancel button
     * @param rootPane the pane to bind the action to. Typically, this variable is retrieved by this.getRootPane();
     * @param cancelAction the action to bind
     */
    public static void bindCloseDialogKeyToCancelAction(JRootPane rootPane,
            Action cancelAction) {
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rootPane.getActionMap();
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);
    }

    /**
     * Download the URL and return contents as a String.
     * @param source
     * @return
     * @throws IOException
     */
    public static String getResults(URLConnection source) throws IOException {

        return Util.getResultsWithEncoding(source, null);
    }

    /**
     * Download the URL using specified encoding and return contents as a String.
     * @param source
     * encoding
     * @return
     * @throws IOException
     */
    public static String getResultsWithEncoding(URLConnection source, String encoding) throws IOException {

        InputStreamReader in;
        if (encoding != null) {
            in = new InputStreamReader(source.getInputStream(), encoding);
        } else {
            in = new InputStreamReader(source.getInputStream());
        }

        StringBuilder sb = new StringBuilder();
        while (true) {
            int byteRead = in.read();
            if (byteRead == -1) {
                break;
            }
            sb.append((char) byteRead);
        }
        return sb.toString();
    }

    public static boolean updateTimeStampIsSet() {
        return (Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP) && Globals.prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP));
    }

    /**
     * Updates the timestamp of the given entry,
     * nests the given undaoableEdit in a named compound,
     * and returns that named compound
     */
    public static NamedCompound doUpdateTimeStamp(BibtexEntry entry, AbstractUndoableEdit undoableEdit) {
        NamedCompound ce = new NamedCompound(undoableEdit.getPresentationName());
        ce.addEdit(undoableEdit);
        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
        String timestamp = dateFormatter.getCurrentDate();
        Util.updateField(entry, timeStampField, timestamp, ce);
        return ce;
    }

    /**
     * Automatically add links for this set of entries, based on the globally stored list of
     * external file types. The entries are modified, and corresponding UndoEdit elements
     * added to the NamedCompound given as argument. Furthermore, all entries which are modified
     * are added to the Set of entries given as an argument.
     *
     * The entries' bibtex keys must have been set - entries lacking key are ignored.
     * The operation is done in a new thread, which is returned for the caller to wait for
     * if needed.
     *
     * @param entries A collection of BibtexEntry objects to find links for.
     * @param ce A NamedCompound to add UndoEdit elements to.
     * @param changedEntries MODIFIED, optional. A Set of BibtexEntry objects to which all modified entries is added. This is used for status output and debugging
     * @param singleTableModel UGLY HACK. The table model to insert links into. Already existing links are not duplicated or removed. This parameter has to be null if entries.count() != 1.
     *   The hack has been introduced as a bibtexentry does not (yet) support the function getListTableModel() and the FileListEntryEditor editor holds an instance of that table model and does not reconstruct it after the search has succeeded.
     * @param metaData The MetaData providing the relevant file directory, if any.
     * @param callback An ActionListener that is notified (on the event dispatch thread) when the search is
     *  finished. The ActionEvent has id=0 if no new links were added, and id=1 if one or more links were added.
     *  This parameter can be null, which means that no callback will be notified.
     * @param diag An instantiated modal JDialog which will be used to display the progress of the autosetting.
     *      This parameter can be null, which means that no progress update will be shown.
     * @return the thread performing the autosetting
     */
    public static Runnable autoSetLinks(final Collection<BibtexEntry> entries,
                                        final NamedCompound ce,
                                        final Set<BibtexEntry> changedEntries,
                                        final FileListTableModel singleTableModel,
                                        final MetaData metaData,
                                        final ActionListener callback,
                                        final JDialog diag) {
        final ExternalFileType[] types = Globals.prefs.getExternalFileTypeSelection();
        if (diag != null) {
            final JProgressBar prog = new JProgressBar(JProgressBar.HORIZONTAL, 0, types.length - 1);
            final JLabel label = new JLabel(Globals.lang("Searching for files"));
            prog.setIndeterminate(true);
            prog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            diag.setTitle(Globals.lang("Autosetting links"));
            diag.getContentPane().add(prog, BorderLayout.CENTER);
            diag.getContentPane().add(label, BorderLayout.SOUTH);

            diag.pack();
            diag.setLocationRelativeTo(diag.getParent());
        }

        Runnable r = new Runnable() {

            @Override
            public void run() {

                // determine directories to search in
                ArrayList<File> dirs = new ArrayList<File>();
                String[] dirsS = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
                for (String dirs1 : dirsS) {
                    dirs.add(new File(dirs1));
                }

                // determine extensions
                Collection<String> extensions = new ArrayList<String>();
                for (final ExternalFileType type : types) {
                    extensions.add(type.getExtension());
                }
                // Run the search operation:
                Map<BibtexEntry, java.util.List<File>> result;
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY)) {
                    String regExp = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
                    result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs, regExp);
                } else {
                    result = Util.findAssociatedFiles(entries, extensions, dirs);
                }

                boolean foundAny = false;

                // Iterate over the entries:
                for (BibtexEntry anEntry : result.keySet()) {
                    FileListTableModel tableModel;
                    String oldVal = anEntry.getField(GUIGlobals.FILE_FIELD);
                    if (singleTableModel == null) {
                        tableModel = new FileListTableModel();
                        if (oldVal != null) {
                            tableModel.setContent(oldVal);
                        }
                    } else {
                        assert (entries.size() == 1);
                        tableModel = singleTableModel;
                    }
                    List<File> files = result.get(anEntry);
                    for (File f : files) {
                        f = FileUtil.shortenFileName(f, dirsS);
                        boolean alreadyHas = false;
                        //System.out.println("File: "+f.getPath());
                        for (int j = 0; j < tableModel.getRowCount(); j++) {
                            FileListEntry existingEntry = tableModel.getEntry(j);
                            //System.out.println("Comp: "+existingEntry.getLink());
                            if (new File(existingEntry.getLink()).equals(f)) {
                                alreadyHas = true;
                                break;
                            }
                        }
                        if (!alreadyHas) {
                            foundAny = true;
                            ExternalFileType type;
                            int index = f.getPath().lastIndexOf('.');
                            if ((index >= 0) && (index < (f.getPath().length() - 1))) {
                                type = Globals.prefs.getExternalFileTypeByExt
                                        (f.getPath().substring(index + 1).toLowerCase());
                            } else {
                                type = new UnknownExternalFileType("");
                            }
                            FileListEntry flEntry = new FileListEntry(f.getName(), f.getPath(), type);
                            tableModel.addEntry(tableModel.getRowCount(), flEntry);

                            String newVal = tableModel.getStringRepresentation();
                            if (newVal.length() == 0) {
                                newVal = null;
                            }
                            if (ce != null) {
                                // store undo information
                                UndoableFieldChange change = new UndoableFieldChange(anEntry,
                                        GUIGlobals.FILE_FIELD, oldVal, newVal);
                                ce.addEdit(change);
                            }
                            // hack: if table model is given, do NOT modify entry
                            if (singleTableModel == null) {
                                anEntry.setField(GUIGlobals.FILE_FIELD, newVal);
                            }
                            if (changedEntries != null) {
                                changedEntries.add(anEntry);
                            }
                        }
                    }
                }

                // handle callbacks and dialog
                final int id = foundAny ? 1 : 0;
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (diag != null) {
                            diag.dispose();
                        }
                        if (callback != null) {
                            callback.actionPerformed(new ActionEvent(this, id, ""));
                        }
                    }
                });
            }
        };
        if (diag != null) {
            diag.setVisible(true);
        }
        return r;
    }

    /**
     * Automatically add links for this entry to the table model given as an argument, based on
     * the globally stored list of external file types. The entry itself is not modified. The entry's
     * bibtex key must have been set.
     *
     * @param entry The BibtexEntry to find links for.
     * @param singleTableModel The table model to insert links into. Already existing links are not duplicated or removed.
     * @param metaData The MetaData providing the relevant file directory, if any.
     * @param callback An ActionListener that is notified (on the event dispatch thread) when the search is
     *  finished. The ActionEvent has id=0 if no new links were added, and id=1 if one or more links were added.
     *  This parameter can be null, which means that no callback will be notified. The passed ActionEvent is constructed with
     *  (this, id, ""), where id is 1 if something has been done and 0 if nothing has been done.
     * @param diag An instantiated modal JDialog which will be used to display the progress of the autosetting.
     *      This parameter can be null, which means that no progress update will be shown.
     * @return the runnable able to perform the autosetting
     */
    public static Runnable autoSetLinks(
            final BibtexEntry entry,
            final FileListTableModel singleTableModel,
            final MetaData metaData,
            final ActionListener callback,
            final JDialog diag) {
        final Collection<BibtexEntry> entries = new ArrayList<BibtexEntry>();
        entries.add(entry);

        return Util.autoSetLinks(entries, null, null, singleTableModel, metaData, callback, diag);
    }

    /**
     * Opens a file browser of the folder of the given file. If possible, the file is selected
     * @param fileLink the location of the file
     * @throws IOException
     */
    public static void openFolderAndSelectFile(String fileLink) throws IOException {
        if (Globals.ON_WIN) {
            Util.openFolderAndSelectFileOnWindows(fileLink);
        } else if (Globals.ON_LINUX) {
            Util.openFolderAndSelectFileOnLinux(fileLink);
        } else {
            Util.openFolderAndSelectFileGeneric(fileLink);
        }
    }

    private static void openFolderAndSelectFileOnLinux(String fileLink) throws IOException {
        String desktopSession = System.getenv("DESKTOP_SESSION").toLowerCase();

        String cmd;

        if (desktopSession.contains("gnome")) {
            cmd = "nautilus " + fileLink;
        } else if (desktopSession.contains("kde")) {
            cmd = "dolphin --select " + fileLink;
        } else {
            cmd = "xdg-open " + fileLink.substring(0, fileLink.lastIndexOf(File.separator));
        }

        Runtime.getRuntime().exec(cmd);
    }

    private static void openFolderAndSelectFileGeneric(String fileLink) throws IOException {
        File f = new File(fileLink);
        Desktop.getDesktop().open(f.getParentFile());
    }

    private static void openFolderAndSelectFileOnWindows(String link) throws IOException {
        link = link.replace("&", "\"&\"");

        String cmd = "explorer.exe /select,\"" + link + "\"";

        Runtime.getRuntime().exec(cmd);
    }

    /**
     * Returns the list of linked files. The files have the absolute filename
     * 
     * @param bes list of BibTeX entries
     * @param fileDirs list of directories to try for expansion
     * 
     * @return list of files. May be empty
     */
    public static List<File> getListOfLinkedFiles(BibtexEntry[] bes, String[] fileDirs) {
        ArrayList<File> res = new ArrayList<File>();
        for (BibtexEntry entry : bes) {
            FileListTableModel tm = new FileListTableModel();
            tm.setContent(entry.getField("file"));
            for (int i = 0; i < tm.getRowCount(); i++) {
                FileListEntry flEntry = tm.getEntry(i);

                File f = FileUtil.expandFilename(flEntry.getLink(), fileDirs);
                if (f != null) {
                    res.add(f);
                }
            }
        }
        return res;
    }

    public static Map<BibtexEntry, List<File>> findAssociatedFiles(Collection<BibtexEntry> entries, Collection<String> extensions, Collection<File> directories) {
        HashMap<BibtexEntry, List<File>> result = new HashMap<BibtexEntry, List<File>>();

        // First scan directories
        Set<File> filesWithExtension = UtilFindFiles.findFiles(extensions, directories);

        // Initialize Result-Set
        for (BibtexEntry entry : entries) {
            result.put(entry, new ArrayList<File>());
        }

        boolean exactOnly = Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY);
        // Now look for keys
        nextFile: for (File file : filesWithExtension) {

            String name = file.getName();
            int dot = name.lastIndexOf('.');
            // First, look for exact matches:
            for (BibtexEntry entry : entries) {
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
                for (BibtexEntry entry : entries) {
                    String citeKey = entry.getCiteKey();
                    if ((citeKey != null) && (citeKey.length() > 0)) {
                        if (name.startsWith(citeKey)) {
                            result.get(entry).add(file);
                            continue nextFile;
                        }
                    }
                }
            }
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

        fieldAndFormat = StringUtil.stripBrackets(fieldAndFormat);

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
        if (fieldValue == null) {
            fieldValue = LabelPatternUtil.makeLabel(entry, beforeColon);
        }

        if (fieldValue == null) {
            return null;
        }

        if ((afterColon == null) || (afterColon.length() == 0)) {
            return fieldValue;
        }

        String[] parts = afterColon.split(":");
        fieldValue = LabelPatternUtil.applyModifiers(fieldValue, parts, 0);

        return fieldValue;
    }

    /**
     * Opens the given URL using the system browser
     * 
     * @param url the URL to open
     * @throws IOException
     */
    public static void openBrowser(String url) throws IOException {
        url = Util.sanitizeUrl(url);
        ExternalFileType fileType = Globals.prefs.getExternalFileTypeByExt("html");
        Util.openExternalFilePlatformIndependent(fileType, url);
    }

}
