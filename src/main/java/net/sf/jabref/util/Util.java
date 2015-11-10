/*  Copyright (C) 2003-2015 JabRef contributors.
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.*;
import net.sf.jabref.logic.util.date.MonthUtil;
import net.sf.jabref.logic.util.date.YearUtil;
import net.sf.jabref.logic.util.io.FileFinder;
import net.sf.jabref.logic.util.io.FileNameCleaner;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.logic.util.strings.UnicodeCharMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.logic.util.date.EasyDateFormat;
import net.sf.jabref.gui.EntryMarker;
import net.sf.jabref.Globals;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.OpenFileFilter;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.exporter.layout.Layout;
import net.sf.jabref.exporter.layout.LayoutHelper;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.RegExpFileSearch;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.logic.labelPattern.LabelPatternUtil;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;

/**
 * utility functions
 */
public class Util {

    private static final Log LOGGER = LogFactory.getLog(Util.class);

    public static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    private static final EasyDateFormat dateFormatter = new EasyDateFormat();

    public static final String ARXIV_LOOKUP_PREFIX = "http://arxiv.org/abs/";

    private static final String SEPARATING_CHARS_NOSPACE = ";,\n";

    private static final UnicodeCharMap UNICODE_CHAR_MAP = new UnicodeCharMap();


    /**
     * This method sets the location of a Dialog such that it is centered with regard to another window, but not outside
     * the screen on the left and the top.
     */
    public static void placeDialog(java.awt.Dialog diag, java.awt.Container win) {
        diag.setLocationRelativeTo(win);
    }

    /**
     * This method translates a field or string from Bibtex notation, with possibly text contained in " " or { }, and
     * string references, concatenated by '#' characters, into Bibkeeper notation, where string references are enclosed
     * in a pair of '#' characters.
     */
    public static String parseField(String content) {
        if (content.isEmpty()) {
            return content;
        }

        String[] strings = content.split("#");
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
            String s = string.trim();
            if (!s.isEmpty()) {
                char c = s.charAt(0);
                // String reference or not?
                if ((c == '{') || (c == '"')) {
                    result.append(StringUtil.shaveString(string));
                } else {
                    // This part should normally be a string reference, but if it's
                    // a pure number, it is not.
                    String s2 = StringUtil.shaveString(s);

                    try {
                        Integer.parseInt(s2);
                        result.append(s2);
                    } catch (NumberFormatException e) {
                        result.append('#').append(s2).append('#');
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * Will return the publication date of the given bibtex entry in conformance to ISO 8601, i.e. either YYYY or
     * YYYY-MM.
     *
     * @param entry
     * @return will return the publication date of the entry or null if no year was found.
     */
    // TODO: Should be instance method of BibTexEntry
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
     * This method returns a String similar to the one passed in, except that it is molded into a form that is
     * acceptable for bibtex.
     *
     * Watch-out that the returned string might be of length 0 afterwards.
     *
     * @param key mayBeNull
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
                if (!Character.isWhitespace(c) && (c != '{') && (c != '\\') && (c != '"') && (c != '}') && (c != ',')) {
                    newKey.append(c);
                }
            }
            return newKey.toString();

        }
        StringBuilder newKey = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isWhitespace(c) && (c != '#') && (c != '{') && (c != '\\') && (c != '"') && (c != '}') && (c != '~') && (c != ',') && (c != '^') && (c != '\'')) {
                newKey.append(c);
            }
        }

        // Replace non-english characters like umlauts etc. with a sensible
        // letter or letter combination that bibtex can accept.

        return Util.replaceSpecialCharacters(newKey.toString());
    }

    /**
     * Replace non-english characters like umlauts etc. with a sensible letter or letter combination that bibtex can
     * accept. The basis for replacement is the HashMap Globals.UNICODE_CHARS.
     */
    public static String replaceSpecialCharacters(String s) {
        for (Map.Entry<String, String> chrAndReplace : Util.UNICODE_CHAR_MAP.entrySet()) {
            s = s.replaceAll(chrAndReplace.getKey(), chrAndReplace.getValue());
        }
        return s;
    }

    public static TreeSet<String> findDeliminatedWordsInField(BibtexDatabase db, String field, String deliminator) {
        TreeSet<String> res = new TreeSet<>();

        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            Object o = be.getField(field);
            if (o != null) {
                String fieldValue = o.toString().trim();
                StringTokenizer tok = new StringTokenizer(fieldValue, deliminator);
                while (tok.hasMoreTokens()) {
                    res.add(StringUtil.capitalizeFirst(tok.nextToken().trim()));
                }
            }
        }
        return res;
    }

    /**
     * Returns a HashMap containing all words used in the database in the given field type. Characters in
     * <code>remove</code> are not included.
     *
     * @param db a <code>BibtexDatabase</code> value
     * @param field a <code>String</code> value
     * @param remove a <code>String</code> value
     * @return a <code>HashSet</code> value
     */
    public static TreeSet<String> findAllWordsInField(BibtexDatabase db, String field, String remove) {
        TreeSet<String> res = new TreeSet<>();
        StringTokenizer tok;
        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            Object o = be.getField(field);
            if (o != null) {
                tok = new StringTokenizer(o.toString(), remove, false);
                while (tok.hasMoreTokens()) {
                    res.add(StringUtil.capitalizeFirst(tok.nextToken().trim()));
                }
            }
        }
        return res;
    }

    /**
     * Finds all authors' last names in all the given fields for the given database.
     *
     * @param db The database.
     * @param fields The fields to look in.
     * @return a set containing the names.
     */
    public static Set<String> findAuthorLastNames(BibtexDatabase db, List<String> fields) {
        Set<String> res = new TreeSet<>();
        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            for (String field : fields) {
                String val = be.getField(field);
                if ((val != null) && !val.isEmpty()) {
                    AuthorList al = AuthorList.getAuthorList(val);
                    for (int i = 0; i < al.size(); i++) {
                        AuthorList.Author a = al.getAuthor(i);
                        String lastName = a.getLast();
                        if ((lastName != null) && !lastName.isEmpty()) {
                            res.add(lastName);
                        }
                    }
                }

            }
        }

        return res;
    }

    /**
     * Make sure an URL is "portable", in that it doesn't contain bad characters that break the open command in some
     * OSes.
     *
     * A call to this method will also remove \\url{} enclosings and clean Doi links.
     *
     * @param link :the URL to sanitize.
     * @return Sanitized URL
     */
    public static String sanitizeUrl(String link) {
        link = link.trim();

        // First check if it is enclosed in \\url{}. If so, remove the wrapper.
        if (link.startsWith("\\url{") && link.endsWith("}")) {
            link = link.substring(5, link.length() - 1);
        }

        // DOI cleanup
        // converts doi-only link to full http address
        // Morten Alver 6 Nov 2012: this extracts a nonfunctional Doi from some complete
        // http addresses (e.g. http://onlinelibrary.wiley.com/doi/10.1002/rra.999/abstract, where
        // the trailing "/abstract" is included but doesn't lead to a resolvable Doi).
        // To prevent mangling of working URLs I'm disabling this check if the link is already
        // a full http link:
        // TODO: not sure if this is allowed
        if (link.matches("^doi:/*.*")) {
            // Remove 'doi:'
            link = link.replaceFirst("^doi:/*", "");
            link = new DOI(link).getURLAsASCIIString();
        }

        Optional<DOI> doi = DOI.build(link);
        if (doi.isPresent() && !link.matches("^https?://.*")) {
            link = doi.get().getURLAsASCIIString();
        }

        // FIXME: everything below is really flawed atm
        link = link.replaceAll("\\+", "%2B");

        try {
            link = URLDecoder.decode(link, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            // Ignored
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

        ArrayList<String[]> result = new ArrayList<>();

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
                            while (((i + 1) < c.length) && !(!escaped && (c[i] == '"') && (c[i + 1] == ')'))) {
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


    /**
     * Takes a string that contains bracketed expression and expands each of these using getFieldAndFormat.
     *
     * Unknown Bracket expressions are silently dropped.
     *
     * @param bracketString
     * @param entry
     * @param database
     * @return
     */
    private static final Pattern squareBracketsPattern = Pattern.compile("\\[.*?\\]");


    public static String expandBrackets(String bracketString, BibtexEntry entry, BibtexDatabase database) {
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
     * Sets empty or non-existing owner fields of bibtex entries inside a List to a specified default value. Timestamp
     * field is also set. Preferences are checked to see if these options are enabled.
     *
     * @param bibs List of bibtex entries
     */
    public static void setAutomaticFields(Collection<BibtexEntry> bibs, boolean overwriteOwner, boolean overwriteTimestamp, boolean markEntries) {

        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);

        String defaultOwner = Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER);
        String timestamp = Util.dateFormatter.getCurrentDate();
        boolean globalSetOwner = Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER);
        boolean globalSetTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP);

        // Do not need to do anything if all options are disabled
        if (!(globalSetOwner || globalSetTimeStamp || markEntries)) {
            return;
        }

        // Iterate through all entries
        for (BibtexEntry curEntry : bibs) {
            boolean setOwner = globalSetOwner && (overwriteOwner || (curEntry.getField(BibtexFields.OWNER) == null));
            boolean setTimeStamp = globalSetTimeStamp && (overwriteTimestamp || (curEntry.getField(timeStampField) == null));
            Util.setAutomaticFields(curEntry, setOwner, defaultOwner, setTimeStamp, timeStampField, timestamp);
            if (markEntries) {
                EntryMarker.markEntry(curEntry, EntryMarker.IMPORT_MARK_LEVEL, false, new NamedCompound(""));
            }
        }
    }

    /**
     * Sets empty or non-existing owner fields of a bibtex entry to a specified default value. Timestamp field is also
     * set. Preferences are checked to see if these options are enabled.
     *
     * @param entry The entry to set fields for.
     * @param overwriteOwner Indicates whether owner should be set if it is already set.
     * @param overwriteTimestamp Indicates whether timestamp should be set if it is already set.
     */
    public static void setAutomaticFields(BibtexEntry entry, boolean overwriteOwner, boolean overwriteTimestamp) {
        String defaultOwner = Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER);
        String timestamp = Util.dateFormatter.getCurrentDate();
        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
        boolean setOwner = Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER) && (overwriteOwner || (entry.getField(BibtexFields.OWNER) == null));
        boolean setTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP) && (overwriteTimestamp || (entry.getField(timeStampField) == null));

        Util.setAutomaticFields(entry, setOwner, defaultOwner, setTimeStamp, timeStampField, timestamp);
    }

    private static void setAutomaticFields(BibtexEntry entry, boolean setOwner, String owner, boolean setTimeStamp, String timeStampField, String timeStamp) {

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
     * Collect file links from the given set of fields, and add them to the list contained in the field
     * GUIGlobals.FILE_FIELD.
     *
     * @param database The database to modify.
     * @param fields The fields to find links in.
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    public static NamedCompound upgradePdfPsToFile(BibtexDatabase database, String[] fields) {
        return Util.upgradePdfPsToFile(database.getEntryMap().values(), fields);
    }

    /**
     * Collect file links from the given set of fields, and add them to the list contained in the field
     * GUIGlobals.FILE_FIELD.
     *
     * @param entries The entries to modify.
     * @param fields The fields to find links in.
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    public static NamedCompound upgradePdfPsToFile(Collection<BibtexEntry> entries, String[] fields) {
        NamedCompound ce = new NamedCompound(Localization.lang("Move external links to 'file' field"));

        for (BibtexEntry entry : entries) {
            FileListTableModel tableModel = new FileListTableModel();
            // If there are already links in the file field, keep those on top:
            String oldFileContent = entry.getField(Globals.FILE_FIELD);
            if (oldFileContent != null) {
                tableModel.setContent(oldFileContent);
            }
            int oldRowCount = tableModel.getRowCount();
            for (String field : fields) {
                String o = entry.getField(field);
                if (o != null) {
                    if (!o.trim().isEmpty()) {
                        File f = new File(o);
                        FileListEntry flEntry = new FileListEntry(f.getName(), o, Globals.prefs.getExternalFileTypeByExt(field));
                        tableModel.addEntry(tableModel.getRowCount(), flEntry);

                        entry.clearField(field);
                        ce.addEdit(new UndoableFieldChange(entry, field, o, null));
                    }
                }
            }
            if (tableModel.getRowCount() != oldRowCount) {
                String newValue = tableModel.getStringRepresentation();
                entry.setField(Globals.FILE_FIELD, newValue);
                ce.addEdit(new UndoableFieldChange(entry, Globals.FILE_FIELD, oldFileContent, newValue));
            }
        }
        ce.end();
        return ce;
    }

    /**
     * Warns the user of undesired side effects of an explicit assignment/removal of entries to/from this group.
     * Currently there are four types of groups: AllEntriesGroup, SearchGroup - do not support explicit assignment.
     * ExplicitGroup - never modifies entries. KeywordGroup - only this modifies entries upon assignment/removal.
     * Modifications are acceptable unless they affect a standard field (such as "author") besides the "keywords" field.
     *
     * @param parent The Component used as a parent when displaying a confirmation dialog.
     * @return true if the assignment has no undesired side effects, or the user chose to perform it anyway. false
     *         otherwise (this indicates that the user has aborted the assignment).
     */
    public static boolean warnAssignmentSideEffects(AbstractGroup[] groups, BibtexEntry[] entries, BibtexDatabase db, Component parent) {
        Vector<String> affectedFields = new Vector<>();
        for (AbstractGroup group : groups) {
            if (group instanceof KeywordGroup) {
                KeywordGroup kg = (KeywordGroup) group;
                String field = kg.getSearchField().toLowerCase();
                if (field.equals("keywords")) {
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
        if (affectedFields.isEmpty()) {
            return true; // no side effects
        }

        // show a warning, then return
        StringBuffer message = // JZTODO lyrics...
                new StringBuffer("This action will modify the following field(s)\n" + "in at least one entry each:\n");
        for (int i = 0; i < affectedFields.size(); ++i) {
            message.append(affectedFields.elementAt(i)).append("\n");
        }
        message.append("This could cause undesired changes to " + "your entries, so it is\nrecommended that you change the grouping field " + "in your group\ndefinition to \"keywords\" or a non-standard name." + "\n\nDo you still want to continue?");
        int choice = JOptionPane.showConfirmDialog(parent, message, Localization.lang("Warning"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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
     * This method looks up what kind of external binding is used for the given field, and constructs on OpenFileFilter
     * suitable for browsing for an external file.
     *
     * @param fieldName The BibTeX field in question.
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
     * Set a given field to a given value for all entries in a Collection. This method DOES NOT update any UndoManager,
     * but returns a relevant CompoundEdit that should be registered by the caller.
     *
     * @param entries The entries to set the field for.
     * @param field The name of the field to set.
     * @param text The value to set. This value can be null, indicating that the field should be cleared.
     * @param overwriteValues Indicate whether the value should be set even if an entry already has the field set.
     * @return A CompoundEdit for the entire operation.
     */
    public static UndoableEdit massSetField(Collection<BibtexEntry> entries, String field, String text, boolean overwriteValues) {

        NamedCompound ce = new NamedCompound(Localization.lang("Set field"));
        for (BibtexEntry entry : entries) {
            String oldVal = entry.getField(field);
            // If we are not allowed to overwrite values, check if there is a
            // nonempty
            // value already for this entry:
            if (!overwriteValues && (oldVal != null) && !oldVal.isEmpty()) {
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
     *
     * @param entries The entries to do this operation for.
     * @param field The field to move contents from.
     * @param newField The field to move contents into.
     * @param overwriteValues If true, overwrites any existing values in the new field. If false, makes no change for
     *            entries with existing value in the new field.
     * @return A CompoundEdit for the entire operation.
     */
    public static UndoableEdit massRenameField(Collection<BibtexEntry> entries, String field, String newField, boolean overwriteValues) {
        NamedCompound ce = new NamedCompound(Localization.lang("Rename field"));
        for (BibtexEntry entry : entries) {
            String valToMove = entry.getField(field);
            // If there is no value, do nothing:
            if ((valToMove == null) || valToMove.isEmpty()) {
                continue;
            }
            // If we are not allowed to overwrite values, check if there is a
            // nonempy value already for this entry for the new field:
            String valInNewField = entry.getField(newField);
            if (!overwriteValues && (valInNewField != null) && !valInNewField.isEmpty()) {
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
     * Optimized method for converting a String into an Integer
     *
     * From http://stackoverflow.com/questions/1030479/most-efficient-way-of-converting-string-to-integer-in-java
     *
     * @param str the String holding an Integer value
     * @throws NumberFormatException if str cannot be parsed to an int
     * @return the int value of str
     */
    public static int intValueOf(String str) {
        int ival = 0;
        int idx = 0;
        int end;
        boolean sign = false;
        char ch;

        if ((str == null) || ((end = str.length()) == 0) || ((((ch = str.charAt(0)) < '0') || (ch > '9')) && (!(sign = ch == '-') || (++idx == end) || ((ch = str.charAt(idx)) < '0') || (ch > '9')))) {
            throw new NumberFormatException(str);
        }

        for (;; ival *= 10) {
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
     * Static equals that can also return the right result when one of the objects is null.
     *
     * @param one The object whose equals method is called if the first is not null.
     * @param two The object passed to the first one if the first is not null.
     * @return <code>one == null ? two == null : one.equals(two);</code>
     */
    public static boolean equals(Object one, Object two) {
        return one == null ? two == null : one.equals(two);
    }

    /**
     * Run an AbstractWorker's methods using Spin features to put each method on the correct thread.
     *
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
            Util.LOGGER.info("Wrong format " + e.getMessage(), e);
        }
        if (layout != null) {
            targetName = layout.doLayout(entry, database);
        }
        //Removes illegal characters from filename
        targetName = FileNameCleaner.cleanFileName(targetName);
        return targetName;
    }

    /**
     * @param keywords a String of keywords
     * @return an ArrayList containing the keywords. An emtpy list if keywords are null or empty
     */
    public static ArrayList<String> getSeparatedKeywords(String keywords) {
        ArrayList<String> res = new ArrayList<>();
        if (keywords == null) {
            return res;
        }
        // _NOSPACE is a hack to support keywords such as "choreography transactions"
        // a more intelligent algorithm would check for the separator chosen (SEPARATING_CHARS_NOSPACE)
        // if nothing is found, " " is likely to be the separating char.
        // solution by RisKeywords.java: s.split(",[ ]*")
        StringTokenizer tok = new StringTokenizer(keywords, Util.SEPARATING_CHARS_NOSPACE);
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
        if (!keywords.isEmpty()) {
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
        if ((oldValue == null) || !oldValue.equals(newValue)) {
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
        if (nullFieldIfValueIsTheSame && (oldValue != null) && oldValue.equals(newValue)) {
            // if oldValue == newValue then reset field if required by parameter
            newValue = null;
        }
        if ((oldValue == null) && (newValue == null)) {
            return;
        }
        if ((oldValue == null) || !oldValue.equals(newValue)) {
            be.setField(field, newValue);
            if (ce != null) {
                ce.addEdit(new UndoableFieldChange(be, field, oldValue, newValue));
            }
        }
    }

    /**
     * Binds ESC-Key to cancel button
     *
     * @param rootPane the pane to bind the action to. Typically, this variable is retrieved by this.getRootPane();
     * @param cancelAction the action to bind
     */
    // TODO: move to GUI
    public static void bindCloseDialogKeyToCancelAction(JRootPane rootPane, Action cancelAction) {
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rootPane.getActionMap();
        im.put(Globals.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);
    }

    /**
     * Download the URL and return contents as a String.
     *
     * @param source
     * @return
     * @throws IOException
     */
    public static String getResults(URL source) throws IOException {

        return Util.getResultsWithEncoding(source.openConnection(), null);
    }

    /**
     * Download the URL and return contents as a String.
     *
     * @param source
     * @return
     * @throws IOException
     */
    public static String getResults(URLConnection source) throws IOException {

        return Util.getResultsWithEncoding(source, null);
    }

    /**
     * Download the URL using specified encoding and return contents as a String.
     *
     * @param source encoding
     * @return
     * @throws IOException
     */
    public static String getResultsWithEncoding(URL source, String encoding) throws IOException {
        return Util.getResultsWithEncoding(source.openConnection(), encoding);
    }
    /**
     * Download the URL using specified encoding and return contents as a String.
     *
     * @param source encoding
     * @return
     * @throws IOException
     */
    public static String getResultsWithEncoding(URLConnection source, String encoding) throws IOException {

        // set user-agent to avoid being blocked as a crawler
        source.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

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

    /**
     * Read results from a file instead of an URL. Just for faster debugging.
     *
     * @param f
     * @return
     * @throws IOException
     */
    public String getResultsFromFile(File f) throws IOException {
        try(InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[256];
            while (true) {
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                for (int i = 0; i < bytesRead; i++) {
                    sb.append((char) buffer[i]);
                }
            }
            return sb.toString();
        }
    }

    public static boolean updateTimeStampIsSet() {
        return Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP) && Globals.prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP);
    }

    /**
     * Updates the timestamp of the given entry, nests the given undaoableEdit in a named compound, and returns that
     * named compound
     */
    public static NamedCompound doUpdateTimeStamp(BibtexEntry entry, AbstractUndoableEdit undoableEdit) {
        NamedCompound ce = new NamedCompound(undoableEdit.getPresentationName());
        ce.addEdit(undoableEdit);
        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
        String timestamp = Util.dateFormatter.getCurrentDate();
        Util.updateField(entry, timeStampField, timestamp, ce);
        return ce;
    }

    /**
     * Automatically add links for this set of entries, based on the globally stored list of external file types. The
     * entries are modified, and corresponding UndoEdit elements added to the NamedCompound given as argument.
     * Furthermore, all entries which are modified are added to the Set of entries given as an argument.
     *
     * The entries' bibtex keys must have been set - entries lacking key are ignored. The operation is done in a new
     * thread, which is returned for the caller to wait for if needed.
     *
     * @param entries A collection of BibtexEntry objects to find links for.
     * @param ce A NamedCompound to add UndoEdit elements to.
     * @param changedEntries MODIFIED, optional. A Set of BibtexEntry objects to which all modified entries is added.
     *            This is used for status output and debugging
     * @param singleTableModel UGLY HACK. The table model to insert links into. Already existing links are not
     *            duplicated or removed. This parameter has to be null if entries.count() != 1. The hack has been
     *            introduced as a bibtexentry does not (yet) support the function getListTableModel() and the
     *            FileListEntryEditor editor holds an instance of that table model and does not reconstruct it after the
     *            search has succeeded.
     * @param metaData The MetaData providing the relevant file directory, if any.
     * @param callback An ActionListener that is notified (on the event dispatch thread) when the search is finished.
     *            The ActionEvent has id=0 if no new links were added, and id=1 if one or more links were added. This
     *            parameter can be null, which means that no callback will be notified.
     * @param diag An instantiated modal JDialog which will be used to display the progress of the autosetting. This
     *            parameter can be null, which means that no progress update will be shown.
     * @return the thread performing the autosetting
     */
    public static Runnable autoSetLinks(final Collection<BibtexEntry> entries, final NamedCompound ce, final Set<BibtexEntry> changedEntries, final FileListTableModel singleTableModel, final MetaData metaData, final ActionListener callback, final JDialog diag) {
        final ExternalFileType[] types = Globals.prefs.getExternalFileTypeSelection();
        if (diag != null) {
            final JProgressBar prog = new JProgressBar(JProgressBar.HORIZONTAL, 0, types.length - 1);
            final JLabel label = new JLabel(Localization.lang("Searching for files"));
            prog.setIndeterminate(true);
            prog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            diag.setTitle(Localization.lang("Autosetting links"));
            diag.getContentPane().add(prog, BorderLayout.CENTER);
            diag.getContentPane().add(label, BorderLayout.SOUTH);

            diag.pack();
            diag.setLocationRelativeTo(diag.getParent());
        }

        Runnable r = new Runnable() {

            @Override
            public void run() {
                // determine directories to search in
                ArrayList<File> dirs = new ArrayList<>();
                String[] dirsS = metaData.getFileDirectory(Globals.FILE_FIELD);
                for (String dirs1 : dirsS) {
                    dirs.add(new File(dirs1));
                }

                // determine extensions
                Collection<String> extensions = new ArrayList<>();
                for (final ExternalFileType type : types) {
                    extensions.add(type.getExtension());
                }

                // Run the search operation:
                Map<BibtexEntry, java.util.List<File>> result;
                if (Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
                    String regExp = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
                    result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs, regExp);
                } else {
                    result = Util.findAssociatedFiles(entries, extensions, dirs);
                }

                boolean foundAny = false;
                // Iterate over the entries:
                for (BibtexEntry anEntry : result.keySet()) {
                    FileListTableModel tableModel;
                    String oldVal = anEntry.getField(Globals.FILE_FIELD);
                    if (singleTableModel == null) {
                        tableModel = new FileListTableModel();
                        if (oldVal != null) {
                            tableModel.setContent(oldVal);
                        }
                    } else {
                        assert entries.size() == 1;
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
                                type = Globals.prefs.getExternalFileTypeByExt(f.getPath().substring(index + 1).toLowerCase());
                            } else {
                                type = new UnknownExternalFileType("");
                            }
                            FileListEntry flEntry = new FileListEntry(f.getName(), f.getPath(), type);
                            tableModel.addEntry(tableModel.getRowCount(), flEntry);

                            String newVal = tableModel.getStringRepresentation();
                            if (newVal.isEmpty()) {
                                newVal = null;
                            }
                            if (ce != null) {
                                // store undo information
                                UndoableFieldChange change = new UndoableFieldChange(anEntry, Globals.FILE_FIELD, oldVal, newVal);
                                ce.addEdit(change);
                            }
                            // hack: if table model is given, do NOT modify entry
                            if (singleTableModel == null) {
                                anEntry.setField(Globals.FILE_FIELD, newVal);
                            }
                            if (changedEntries != null) {
                                changedEntries.add(anEntry);
                            }
                        }
                    }
                }

                // handle callbacks and dialog
                // FIXME: The ID signals if action was successful :/
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
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // show dialog which will be hidden when the task is done
                if (diag != null) {
                    diag.setVisible(true);
                }
            }
        });
        return r;
    }

    /**
     * Automatically add links for this entry to the table model given as an argument, based on the globally stored list
     * of external file types. The entry itself is not modified. The entry's bibtex key must have been set.
     *
     * @param entry The BibtexEntry to find links for.
     * @param singleTableModel The table model to insert links into. Already existing links are not duplicated or
     *            removed.
     * @param metaData The MetaData providing the relevant file directory, if any.
     * @param callback An ActionListener that is notified (on the event dispatch thread) when the search is finished.
     *            The ActionEvent has id=0 if no new links were added, and id=1 if one or more links were added. This
     *            parameter can be null, which means that no callback will be notified. The passed ActionEvent is
     *            constructed with (this, id, ""), where id is 1 if something has been done and 0 if nothing has been
     *            done.
     * @param diag An instantiated modal JDialog which will be used to display the progress of the autosetting. This
     *            parameter can be null, which means that no progress update will be shown.
     * @return the runnable able to perform the autosetting
     */
    public static Runnable autoSetLinks(final BibtexEntry entry, final FileListTableModel singleTableModel, final MetaData metaData, final ActionListener callback, final JDialog diag) {
        final Collection<BibtexEntry> entries = new ArrayList<>();
        entries.add(entry);

        return Util.autoSetLinks(entries, null, null, singleTableModel, metaData, callback, diag);
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
        ArrayList<File> res = new ArrayList<>();
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
        HashMap<BibtexEntry, List<File>> result = new HashMap<>();

        // First scan directories
        Set<File> filesWithExtension = FileFinder.findFiles(extensions, directories);

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
                if ((citeKey != null) && !citeKey.isEmpty()) {
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
                    if ((citeKey != null) && !citeKey.isEmpty()) {
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
     * Accepts a string like [author:lower] or [title:abbr] or [auth], whereas the first part signifies the bibtex-field
     * to get, or the key generator field marker to use, while the others are the modifiers that will be applied.
     *
     * @param fieldAndFormat
     * @param entry
     * @param database
     * @return
     */
    public static String getFieldAndFormat(String fieldAndFormat, BibtexEntry entry, BibtexDatabase database) {

        fieldAndFormat = StringUtil.stripBrackets(fieldAndFormat);

        int colon = fieldAndFormat.indexOf(':');

        String beforeColon;
        String afterColon;
        if (colon == -1) {
            beforeColon = fieldAndFormat;
            afterColon = null;
        } else {
            beforeColon = fieldAndFormat.substring(0, colon);
            afterColon = fieldAndFormat.substring(colon + 1);
        }
        beforeColon = beforeColon.trim();

        if (beforeColon.isEmpty()) {
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

        if ((afterColon == null) || afterColon.isEmpty()) {
            return fieldValue;
        }

        String[] parts = afterColon.split(":");
        fieldValue = LabelPatternUtil.applyModifiers(fieldValue, parts, 0);

        return fieldValue;
    }

    // Returns a reg exp pattern in the form (w1)|(w2)| ... wi are escaped if no regex search is enabled
    public static Pattern getPatternForWords(List<String> words) {
        if ((words == null) || words.isEmpty() || words.get(0).isEmpty()) {
            return Pattern.compile("");
        }

        boolean regExSearch = Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP);

        // compile the words to a regex in the form (w1) | (w2) | (w3)
        String searchPattern = "(".concat(regExSearch ? words.get(0) : Pattern.quote(words.get(0))).concat(")");
        for (int i = 1; i < words.size(); i++) {
            searchPattern = searchPattern.concat("|(").concat(regExSearch ? words.get(i) : Pattern.quote(words.get(i)))
                    .concat(")");
        }

        Pattern pattern;
        if (Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE)) {
            pattern = Pattern.compile(searchPattern);
        } else {
            pattern = Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE);
        }

        return pattern;
    }
}
