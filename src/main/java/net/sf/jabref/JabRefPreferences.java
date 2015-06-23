/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JTable;
import javax.swing.KeyStroke;

import net.sf.jabref.export.CustomExportList;
import net.sf.jabref.export.ExportComparator;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.gui.CleanUpAction;
import net.sf.jabref.gui.PersistenceTableColumnListener;
import net.sf.jabref.imports.CustomImportList;
import net.sf.jabref.labelPattern.LabelPattern;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

public class JabRefPreferences {

    private final static String CUSTOM_TYPE_NAME = "customTypeName_";
    private final static String CUSTOM_TYPE_REQ = "customTypeReq_";
    private final static String CUSTOM_TYPE_OPT = "customTypeOpt_";
    private final static String CUSTOM_TYPE_PRIOPT = "customTypePriOpt_";
    public final static String CUSTOM_TAB_NAME = "customTabName_";
    public final static String CUSTOM_TAB_FIELDS = "customTabFields_";
    public final static String EMACS_PATH = "emacsPath";
    public final static String EMACS_ADDITIONAL_PARAMETERS = "emacsParameters";
    public final static String EMACS_23 = "emacsUseV23InsertString";
    public final static String EDIT_GROUP_MEMBERSHIP_MODE = "groupEditGroupMembershipMode";
    public final static String PDF_PREVIEW = "pdfPreview";
    public final static String SHOW_ONE_LETTER_HEADING_FOR_ICON_COLUMNS = "showOneLetterHeadingForIconColumns";
    public final static String EDITOR_EMACS_KEYBINDINGS = "editorEMACSkeyBindings";
    public final static String EDITOR_EMACS_KEYBINDINGS_REBIND_CA = "editorEMACSkeyBindingsRebindCA";
    public final static String SHORTEST_TO_COMPLETE = "shortestToComplete";
    public final static String AUTOCOMPLETE_FIRSTNAME_MODE = "autoCompFirstNameMode";
    public final static String// here are the possible values for _MODE:
            AUTOCOMPLETE_FIRSTNAME_MODE_BOTH = "both";
    public final static String AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL = "fullOnly";
    public final static String AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR = "abbrOnly";
    public final static String WRITEFIELD_ADDSPACES = "writeFieldAddSpaces";
    public final static String WRITEFIELD_CAMELCASENAME = "writeFieldCamelCase";
    public final static String UPDATE_TIMESTAMP = "updateTimestamp";
    public final static String PRIMARY_SORT_FIELD = "priSort";
    public final static String PRIMARY_SORT_DESCENDING = "priDescending";
    public final static String SECONDARY_SORT_FIELD = "secSort";
    public final static String SECONDARY_SORT_DESCENDING = "secDescending";
    public final static String TERTIARY_SORT_FIELD = "terSort";
    public final static String TERTIARY_SORT_DESCENDING = "terDescending";
    public final static String SAVE_IN_ORIGINAL_ORDER = "saveInOriginalOrder";
    public final static String SAVE_IN_SPECIFIED_ORDER = "saveInSpecifiedOrder";
    public final static String SAVE_PRIMARY_SORT_FIELD = "savePriSort";
    public final static String SAVE_PRIMARY_SORT_DESCENDING = "savePriDescending";
    public final static String SAVE_SECONDARY_SORT_FIELD = "saveSecSort";
    public final static String SAVE_SECONDARY_SORT_DESCENDING = "saveSecDescending";
    public final static String SAVE_TERTIARY_SORT_FIELD = "saveTerSort";
    public final static String SAVE_TERTIARY_SORT_DESCENDING = "saveTerDescending";
    public final static String EXPORT_IN_ORIGINAL_ORDER = "exportInOriginalOrder";
    public final static String EXPORT_IN_SPECIFIED_ORDER = "exportInSpecifiedOrder";
    public final static String EXPORT_PRIMARY_SORT_FIELD = "exportPriSort";
    public final static String EXPORT_PRIMARY_SORT_DESCENDING = "exportPriDescending";
    public final static String EXPORT_SECONDARY_SORT_FIELD = "exportSecSort";
    public final static String EXPORT_SECONDARY_SORT_DESCENDING = "exportSecDescending";
    public final static String EXPORT_TERTIARY_SORT_FIELD = "exportTerSort";
    public final static String EXPORT_TERTIARY_SORT_DESCENDING = "exportTerDescending";
    public final static String WRITEFIELD_SORTSTYLE = "writefieldSortStyle";
    public final static String WRITEFIELD_USERDEFINEDORDER = "writefieldUserdefinedOrder";
    public final static String WRITEFIELD_WRAPFIELD = "wrapFieldLine";

    // This String is used in the encoded list in prefs of external file type
    // modifications, in order to indicate a removed default file type:
    private static final String FILE_TYPE_REMOVED_FLAG = "REMOVED";

    private static final char[][] VALUE_DELIMITERS = new char[][] { {'"', '"'}, {'{', '}'}};
    public static final String XMP_PRIVACY_FILTERS = "xmpPrivacyFilters";
    private static final String USE_XMP_PRIVACY_FILTER = "useXmpPrivacyFilter";

    public static final String NEWLINE = "newline";

    public String WRAPPED_USERNAME;
    public final String MARKING_WITH_NUMBER_PATTERN;

    private final Preferences prefs;
    public final HashMap<String, Object> defaults = new HashMap<String, Object>();
    private HashMap<String, String> keyBinds = new HashMap<String, String>();
    private final HashMap<String, String> defKeyBinds = new HashMap<String, String>();
    private final HashSet<String> putBracesAroundCapitalsFields = new HashSet<String>(4);
    private final HashSet<String> nonWrappableFields = new HashSet<String>(5);
    private static LabelPattern keyPattern;

    // Object containing custom export formats:
    public final CustomExportList customExports;

    /**
     * Set with all custom {@link net.sf.jabref.imports.ImportFormat}s
     */
    public final CustomImportList customImports;

    // Object containing info about customized entry editor tabs.
    private EntryEditorTabList tabList = null;
    // Map containing all registered external file types:
    private final TreeSet<ExternalFileType> externalFileTypes = new TreeSet<ExternalFileType>();

    private final ExternalFileType HTML_FALLBACK_TYPE = new ExternalFileType("URL", "html", "text/html", "", "www");

    // The following field is used as a global variable during the export of a database.
    // By setting this field to the path of the database's default file directory, formatters
    // that should resolve external file paths can access this field. This is an ugly hack
    // to solve the problem of formatters not having access to any context except for the
    // string to be formatted and possible formatter arguments.
    public String[] fileDirForDatabase = null;

    // Similarly to the previous variable, this is a global that can be used during
    // the export of a database if the database filename should be output. If a database
    // is tied to a file on disk, this variable is set to that file before export starts:
    public File databaseFile = null;

    // The following field is used as a global variable during the export of a database.
    // It is used to hold custom name formatters defined by a custom export filter.
    // It is set before the export starts:
    public HashMap<String, String> customExportNameFormatters = null;

    // The only instance of this class:
    private static JabRefPreferences singleton = null;


    public static JabRefPreferences getInstance() {
        if (JabRefPreferences.singleton == null) {
            JabRefPreferences.singleton = new JabRefPreferences();
        }
        return JabRefPreferences.singleton;
    }

    // Upgrade the preferences for the current version
    // The old preference is kept in case an old version of JabRef is used with 
    // these preferences, but it is only used when the new preference does not 
    // exist
    private void upgradeOldPreferences() {
        if (prefs.get(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER, null) == null) {
            if (prefs.getBoolean("saveInStandardOrder", false)) {
                putBoolean(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER, true);
                put(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD, "author");
                put(JabRefPreferences.SAVE_SECONDARY_SORT_FIELD, "editor");
                put(JabRefPreferences.SAVE_TERTIARY_SORT_FIELD, "year");
                putBoolean(JabRefPreferences.SAVE_PRIMARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.SAVE_SECONDARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.SAVE_TERTIARY_SORT_DESCENDING, false);
            } else if (prefs.getBoolean("saveInTitleOrder", false)) {
                // saveInTitleOrder => title, author, editor
                putBoolean(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER, true);
                put(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD, "title");
                put(JabRefPreferences.SAVE_SECONDARY_SORT_FIELD, "author");
                put(JabRefPreferences.SAVE_TERTIARY_SORT_FIELD, "editor");
                putBoolean(JabRefPreferences.SAVE_PRIMARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.SAVE_SECONDARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.SAVE_TERTIARY_SORT_DESCENDING, false);
            }
        }

        if (prefs.get(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, null) == null) {
            if (prefs.getBoolean("exportInStandardOrder", false)) {
                putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, "author");
                put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, "editor");
                put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, "year");
                putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            } else if (prefs.getBoolean("exportInTitleOrder", false)) {
                // exportInTitleOrder => title, author, editor
                putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, "title");
                put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, "author");
                put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, "editor");
                putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            }
        }
    }

    // The constructor is made private to enforce this as a singleton class:
    private JabRefPreferences() {

        try {
            if (new File("jabref.xml").exists()) {
                importPreferences("jabref.xml");
            }
        } catch (IOException e) {
            Globals.logger("Could not import preferences from jabref.xml:" + e.getLocalizedMessage());
        }

        // load user preferences 
        prefs = Preferences.userNodeForPackage(JabRef.class);
        upgradeOldPreferences();

        if (Globals.osName.equals(Globals.MAC)) {
            //defaults.put("pdfviewer", "/Applications/Preview.app");
            //defaults.put("psviewer", "/Applications/Preview.app");
            //defaults.put("htmlviewer", "/Applications/Safari.app");
            defaults.put(JabRefPreferences.EMACS_PATH, "emacsclient");
            defaults.put(JabRefPreferences.EMACS_23, true);
            defaults.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, "-n -e");
            defaults.put("fontFamily", "SansSerif");

        } else if (Globals.osName.toLowerCase().startsWith("windows")) {
            //defaults.put("pdfviewer", "cmd.exe /c start /b");
            //defaults.put("psviewer", "cmd.exe /c start /b");
            //defaults.put("htmlviewer", "cmd.exe /c start /b");
            defaults.put("lookAndFeel", "com.jgoodies.looks.windows.WindowsLookAndFeel");
            defaults.put("winEdtPath", "C:\\Program Files\\WinEdt Team\\WinEdt\\WinEdt.exe");
            defaults.put("latexEditorPath", "C:\\Program Files\\LEd\\LEd.exe");
            defaults.put(JabRefPreferences.EMACS_PATH, "emacsclient.exe");
            defaults.put(JabRefPreferences.EMACS_23, true);
            defaults.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, "-n -e");
            defaults.put("fontFamily", "Arial");

        } else {
            //defaults.put("pdfviewer", "evince");
            //defaults.put("psviewer", "gv");
            //defaults.put("htmlviewer", "firefox");
            defaults.put("lookAndFeel", "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel");
            defaults.put("fontFamily", "SansSerif");

            // linux
            defaults.put(JabRefPreferences.EMACS_PATH, "gnuclient");
            defaults.put(JabRefPreferences.EMACS_23, false);
            defaults.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, "-batch -eval");
        }
        defaults.put("useProxy", Boolean.FALSE);
        defaults.put("proxyHostname", "my proxy host");
        defaults.put("proxyPort", "my proxy port");
        defaults.put(JabRefPreferences.PDF_PREVIEW, Boolean.FALSE);
        defaults.put("useDefaultLookAndFeel", Boolean.TRUE);
        defaults.put("lyxpipe", System.getProperty("user.home") + File.separator + ".lyx/lyxpipe");
        defaults.put("vim", "vim");
        defaults.put("vimServer", "vim");
        defaults.put("posX", 0);
        defaults.put("posY", 0);
        defaults.put("sizeX", 840);
        defaults.put("sizeY", 680);
        defaults.put("windowMaximised", Boolean.FALSE);
        defaults.put("autoResizeMode", JTable.AUTO_RESIZE_ALL_COLUMNS);
        defaults.put("previewPanelHeight", 200);
        defaults.put("entryEditorHeight", 400);
        defaults.put("tableColorCodesOn", Boolean.TRUE);
        defaults.put("namesAsIs", Boolean.FALSE); // "Show names unchanged"
        defaults.put("namesFf", Boolean.FALSE); // "Show 'Firstname Lastname'"
        defaults.put("namesLf", Boolean.FALSE); // "Show 'Lastname, Firstname'"
        defaults.put("namesNatbib", Boolean.TRUE); // "Natbib style"
        defaults.put("abbrAuthorNames", Boolean.TRUE); // "Abbreviate names"
        defaults.put("namesLastOnly", Boolean.TRUE); // "Show last names only"
        defaults.put("language", "en");
        defaults.put("showShort", Boolean.TRUE);

        // Sorting preferences
        defaults.put(JabRefPreferences.PRIMARY_SORT_FIELD, "author");
        defaults.put(JabRefPreferences.PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(JabRefPreferences.SECONDARY_SORT_FIELD, "year");
        defaults.put(JabRefPreferences.SECONDARY_SORT_DESCENDING, Boolean.TRUE);
        defaults.put(JabRefPreferences.TERTIARY_SORT_FIELD, "author");
        defaults.put(JabRefPreferences.TERTIARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(JabRefPreferences.SAVE_IN_ORIGINAL_ORDER, Boolean.FALSE);
        defaults.put(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER, Boolean.FALSE);
        defaults.put(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD, "bibtexkey");
        defaults.put(JabRefPreferences.SAVE_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(JabRefPreferences.SAVE_SECONDARY_SORT_FIELD, "author");
        defaults.put(JabRefPreferences.SAVE_SECONDARY_SORT_DESCENDING, Boolean.TRUE);
        defaults.put(JabRefPreferences.SAVE_TERTIARY_SORT_FIELD, "");
        defaults.put(JabRefPreferences.SAVE_TERTIARY_SORT_DESCENDING, Boolean.TRUE);
        defaults.put(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER, Boolean.FALSE);
        defaults.put(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, Boolean.FALSE);
        defaults.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, "bibtexkey");
        defaults.put(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, "author");
        defaults.put(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, Boolean.TRUE);
        defaults.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, "");
        defaults.put(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, Boolean.TRUE);

        defaults.put(JabRefPreferences.NEWLINE, System.getProperty("line.separator"));

        defaults.put("sidePaneComponentNames", "");
        defaults.put("sidePaneComponentPreferredPositions", "");

        defaults.put("columnNames", "entrytype;author;title;year;journal;owner;timestamp;bibtexkey");
        defaults.put("columnWidths", "75;280;400;60;100;100;100;100");
        defaults.put(PersistenceTableColumnListener.ACTIVATE_PREF_KEY,
                PersistenceTableColumnListener.DEFAULT_ENABLED);
        defaults.put(JabRefPreferences.XMP_PRIVACY_FILTERS, "pdf;timestamp;keywords;owner;note;review");
        defaults.put(JabRefPreferences.USE_XMP_PRIVACY_FILTER, Boolean.FALSE);
        defaults.put("numberColWidth", GUIGlobals.NUMBER_COL_LENGTH);
        defaults.put("workingDirectory", System.getProperty("user.home"));
        defaults.put("exportWorkingDirectory", System.getProperty("user.home"));
        defaults.put("importWorkingDirectory", System.getProperty("user.home"));
        defaults.put("fileWorkingDirectory", System.getProperty("user.home"));
        defaults.put("autoOpenForm", Boolean.TRUE);
        defaults.put("entryTypeFormHeightFactor", 1);
        defaults.put("entryTypeFormWidth", 1);
        defaults.put("backup", Boolean.TRUE);
        defaults.put("openLastEdited", Boolean.TRUE);
        defaults.put("lastEdited", null);
        defaults.put("stringsPosX", 0);
        defaults.put("stringsPosY", 0);
        defaults.put("stringsSizeX", 600);
        defaults.put("stringsSizeY", 400);
        defaults.put("defaultShowSource", Boolean.FALSE);
        defaults.put("showSource", Boolean.TRUE);
        defaults.put("defaultAutoSort", Boolean.FALSE);
        defaults.put("caseSensitiveSearch", Boolean.FALSE);
        defaults.put("searchReq", Boolean.TRUE);
        defaults.put("searchOpt", Boolean.TRUE);
        defaults.put("searchGen", Boolean.TRUE);
        defaults.put("searchAll", Boolean.FALSE);
        defaults.put("incrementS", Boolean.FALSE);
        defaults.put("searchAutoComplete", Boolean.TRUE);

        defaults.put("selectS", Boolean.FALSE);
        defaults.put("regExpSearch", Boolean.TRUE);
        defaults.put("highLightWords", Boolean.TRUE);
        defaults.put("searchPanePosX", 0);
        defaults.put("searchPanePosY", 0);
        defaults.put(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS, Boolean.FALSE);
        defaults.put(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS_REBIND_CA, Boolean.TRUE);
        defaults.put("autoComplete", Boolean.TRUE);
        defaults.put("autoCompleteFields", "author;editor;title;journal;publisher;keywords;crossref");
        defaults.put("autoCompFF", Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put("autoCompLF", Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(JabRefPreferences.SHORTEST_TO_COMPLETE, 2);
        defaults.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH);
        defaults.put("groupSelectorVisible", Boolean.TRUE);
        defaults.put("groupFloatSelections", Boolean.TRUE);
        defaults.put("groupIntersectSelections", Boolean.TRUE);
        defaults.put("groupInvertSelections", Boolean.FALSE);
        defaults.put("groupShowOverlapping", Boolean.FALSE);
        defaults.put("groupSelectMatches", Boolean.FALSE);
        defaults.put("groupsDefaultField", "keywords");
        defaults.put("groupShowIcons", Boolean.TRUE);
        defaults.put("groupShowDynamic", Boolean.TRUE);
        defaults.put("groupExpandTree", Boolean.TRUE);
        defaults.put("groupAutoShow", Boolean.TRUE);
        defaults.put("groupAutoHide", Boolean.TRUE);
        defaults.put(JabRefPreferences.GROUP_SHOW_NUMBER_OF_ELEMENTS, Boolean.FALSE);
        defaults.put("autoAssignGroup", Boolean.TRUE);
        defaults.put("groupKeywordSeparator", ", ");
        defaults.put(JabRefPreferences.EDIT_GROUP_MEMBERSHIP_MODE, Boolean.FALSE);
        defaults.put("highlightGroupsMatchingAny", Boolean.FALSE);
        defaults.put("highlightGroupsMatchingAll", Boolean.FALSE);
        defaults.put("toolbarVisible", Boolean.TRUE);
        defaults.put("searchPanelVisible", Boolean.FALSE);
        defaults.put("defaultEncoding", System.getProperty("file.encoding"));
        defaults.put("groupsVisibleRows", 8);
        defaults.put("defaultOwner", System.getProperty("user.name"));
        defaults.put("preserveFieldFormatting", Boolean.FALSE);
        defaults.put("memoryStickMode", Boolean.FALSE);
        defaults.put("renameOnMoveFileToFileDir", Boolean.TRUE);

        // The general fields stuff is made obsolete by the CUSTOM_TAB_... entries.
        defaults.put("generalFields", "crossref;keywords;file;doi;url;urldate;"
                + "pdf;comment;owner");

        defaults.put("useCustomIconTheme", Boolean.FALSE);
        defaults.put("customIconThemeFile", "/home/alver/div/crystaltheme_16/Icons.properties");

        //defaults.put("recentFiles", "/home/alver/Documents/bibk_dok/hovedbase.bib");
        defaults.put("historySize", 8);
        defaults.put("fontStyle", java.awt.Font.PLAIN);
        defaults.put("fontSize", 12);
        defaults.put("overrideDefaultFonts", Boolean.FALSE);
        defaults.put("menuFontFamily", "Times");
        defaults.put("menuFontStyle", java.awt.Font.PLAIN);
        defaults.put("menuFontSize", 11);
        defaults.put("tableRowPadding", GUIGlobals.TABLE_ROW_PADDING);
        defaults.put("tableShowGrid", Boolean.FALSE);
        // Main table color settings:
        defaults.put("tableBackground", "255:255:255");
        defaults.put("tableReqFieldBackground", "230:235:255");
        defaults.put("tableOptFieldBackground", "230:255:230");
        defaults.put("tableText", "0:0:0");
        defaults.put("gridColor", "210:210:210");
        defaults.put("grayedOutBackground", "210:210:210");
        defaults.put("grayedOutText", "40:40:40");
        defaults.put("veryGrayedOutBackground", "180:180:180");
        defaults.put("veryGrayedOutText", "40:40:40");
        defaults.put("markedEntryBackground0", "255:255:180");
        defaults.put("markedEntryBackground1", "255:220:180");
        defaults.put("markedEntryBackground2", "255:180:160");
        defaults.put("markedEntryBackground3", "255:120:120");
        defaults.put("markedEntryBackground4", "255:75:75");
        defaults.put("markedEntryBackground5", "220:255:220");
        defaults.put("validFieldBackgroundColor", "255:255:255");
        defaults.put("invalidFieldBackgroundColor", "255:0:0");
        defaults.put("activeFieldEditorBackgroundColor", "220:220:255");
        defaults.put("fieldEditorTextColor", "0:0:0");

        defaults.put("incompleteEntryBackground", "250:175:175");

        defaults.put("antialias", Boolean.FALSE);
        defaults.put("ctrlClick", Boolean.FALSE);
        defaults.put("disableOnMultipleSelection", Boolean.FALSE);
        defaults.put("pdfColumn", Boolean.FALSE);
        defaults.put("urlColumn", Boolean.TRUE);
        defaults.put("preferUrlDoi", Boolean.FALSE);
        defaults.put("fileColumn", Boolean.TRUE);
        defaults.put("arxivColumn", Boolean.FALSE);

        defaults.put("extraFileColumns", Boolean.FALSE);
        defaults.put("listOfFileColumns", "");

        defaults.put(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED, SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY, SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY, SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING, SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_RANKING_COMPACT, SpecialFieldsUtils.PREF_RANKING_COMPACT_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE, SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED, SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ, SpecialFieldsUtils.PREF_SHOWCOLUMN_READ_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS, SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS, SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS_DEFAULT);

        defaults.put(JabRefPreferences.SHOW_ONE_LETTER_HEADING_FOR_ICON_COLUMNS, Boolean.FALSE);

        defaults.put("useOwner", Boolean.FALSE);
        defaults.put("overwriteOwner", Boolean.FALSE);
        defaults.put("allowTableEditing", Boolean.FALSE);
        defaults.put("dialogWarningForDuplicateKey", Boolean.TRUE);
        defaults.put("dialogWarningForEmptyKey", Boolean.TRUE);
        defaults.put("displayKeyWarningDialogAtStartup", Boolean.TRUE);
        defaults.put("avoidOverwritingKey", Boolean.FALSE);
        defaults.put("warnBeforeOverwritingKey", Boolean.TRUE);
        defaults.put("confirmDelete", Boolean.TRUE);
        defaults.put("grayOutNonHits", Boolean.TRUE);
        defaults.put("floatSearch", Boolean.TRUE);
        defaults.put("showSearchInDialog", Boolean.FALSE);
        defaults.put("searchAllBases", Boolean.FALSE);
        defaults.put("defaultLabelPattern", "[auth][year]");
        defaults.put("previewEnabled", Boolean.TRUE);
        defaults.put("activePreview", 0);
        defaults.put("preview0", "<font face=\"arial\">"
                + "<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                + "\\end{bibtexkey}</b><br>__NEWLINE__"
                + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                // Include the booktitle field for @inproceedings, @proceedings, etc.
                + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                + "\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                + "\\begin{review}<BR><BR><b>Review: </b> \\format[HTMLChars]{\\review} \\end{review}"
                + "</dd>__NEWLINE__<p></p></font>");
        defaults.put("preview1", "<font face=\"arial\">"
                + "<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                + "\\end{bibtexkey}</b><br>__NEWLINE__"
                + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                // Include the booktitle field for @inproceedings, @proceedings, etc.
                + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}"
                + "</dd>__NEWLINE__<p></p></font>");

        // TODO: Currently not possible to edit this setting:
        defaults.put("previewPrintButton", Boolean.FALSE);
        defaults.put("autoDoubleBraces", Boolean.FALSE);
        defaults.put("doNotResolveStringsFor", "url");
        defaults.put("resolveStringsAllFields", Boolean.FALSE);
        defaults.put("putBracesAroundCapitals", "");//"title;journal;booktitle;review;abstract");
        defaults.put("nonWrappableFields", "pdf;ps;url;doi;file");
        defaults.put("useImportInspectionDialog", Boolean.TRUE);
        defaults.put("useImportInspectionDialogForSingle", Boolean.TRUE);
        defaults.put("generateKeysAfterInspection", Boolean.TRUE);
        defaults.put("markImportedEntries", Boolean.TRUE);
        defaults.put("unmarkAllEntriesBeforeImporting", Boolean.TRUE);
        defaults.put("warnAboutDuplicatesInInspection", Boolean.TRUE);
        defaults.put("useTimeStamp", Boolean.FALSE);
        defaults.put("overwriteTimeStamp", Boolean.FALSE);
        defaults.put("timeStampFormat", "yyyy.MM.dd");
        //        defaults.put("timeStampField", "timestamp");
        defaults.put("timeStampField", BibtexFields.TIMESTAMP);
        defaults.put(JabRefPreferences.UPDATE_TIMESTAMP, Boolean.FALSE);
        defaults.put("generateKeysBeforeSaving", Boolean.FALSE);

        // behavior of JabRef before 2.10: both: false
        defaults.put(JabRefPreferences.WRITEFIELD_ADDSPACES, Boolean.TRUE);
        defaults.put(JabRefPreferences.WRITEFIELD_CAMELCASENAME, Boolean.TRUE);

        //behavior of JabRef before LWang_AdjustableFieldOrder 1
        //0 sorted order (2.10 default), 1 unsorted order (2.9.2 default), 2 user defined
        defaults.put(JabRefPreferences.WRITEFIELD_SORTSTYLE, 0);
        defaults.put(JabRefPreferences.WRITEFIELD_USERDEFINEDORDER, "author;title;journal;year;volume;number;pages;month;note;volume;pages;part;eid");
        defaults.put(JabRefPreferences.WRITEFIELD_WRAPFIELD, Boolean.FALSE);

        defaults.put("useRemoteServer", Boolean.FALSE);
        defaults.put("remoteServerPort", 6050);

        defaults.put("personalJournalList", null);
        defaults.put("externalJournalLists", null);
        defaults.put("citeCommand", "cite"); // obsoleted by the app-specific ones
        defaults.put("citeCommandVim", "\\cite");
        defaults.put("citeCommandEmacs", "\\cite");
        defaults.put("citeCommandWinEdt", "\\cite");
        defaults.put("citeCommandLed", "\\cite");
        defaults.put("floatMarkedEntries", Boolean.TRUE);

        defaults.put("useNativeFileDialogOnMac", Boolean.FALSE);
        defaults.put("filechooserDisableRename", Boolean.TRUE);

        defaults.put("lastUsedExport", null);
        defaults.put("sidePaneWidth", -1);

        defaults.put("importInspectionDialogWidth", 650);
        defaults.put("importInspectionDialogHeight", 650);
        defaults.put("searchDialogWidth", 650);
        defaults.put("searchDialogHeight", 500);
        defaults.put("showFileLinksUpgradeWarning", Boolean.TRUE);
        defaults.put(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, Boolean.TRUE);
        defaults.put("numericFields", "mittnum;author");
        defaults.put("runAutomaticFileSearch", Boolean.FALSE);
        defaults.put("useLockFiles", Boolean.TRUE);
        defaults.put("autoSave", Boolean.TRUE);
        defaults.put("autoSaveInterval", 5);
        defaults.put("promptBeforeUsingAutosave", Boolean.TRUE);
        defaults.put("deletePlugins", "");
        defaults.put("enforceLegalBibtexKey", Boolean.TRUE);
        defaults.put("biblatexMode", Boolean.FALSE);
        // Curly brackets ({}) are the default delimiters, not quotes (") as these cause trouble when they appear within the field value:
        // Currently, JabRef does not escape them
        defaults.put("valueDelimiters", 1);
        defaults.put("includeEmptyFields", Boolean.FALSE);
        defaults.put("keyGenFirstLetterA", Boolean.TRUE);
        defaults.put("keyGenAlwaysAddLetter", Boolean.FALSE);
        defaults.put(JabRefPreferences.EMAIL_SUBJECT, Globals.lang("References"));
        defaults.put(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.FALSE);
        defaults.put("allowFileAutoOpenBrowse", Boolean.TRUE);
        defaults.put("webSearchVisible", Boolean.FALSE);
        defaults.put("selectedFetcherIndex", 0);
        defaults.put("bibLocationAsFileDir", Boolean.TRUE);
        defaults.put("bibLocAsPrimaryDir", Boolean.FALSE);
        defaults.put("dbConnectServerType", "MySQL");
        defaults.put("dbConnectHostname", "localhost");
        defaults.put("dbConnectDatabase", "jabref");
        defaults.put("dbConnectUsername", "root");
        CleanUpAction.putDefaults(defaults);

        // defaults for DroppedFileHandler UI
        defaults.put(DroppedFileHandler.DFH_LEAVE, Boolean.FALSE);
        defaults.put(DroppedFileHandler.DFH_COPY, Boolean.TRUE);
        defaults.put(DroppedFileHandler.DFH_MOVE, Boolean.FALSE);
        defaults.put(DroppedFileHandler.DFH_RENAME, Boolean.FALSE);

        //defaults.put("lastAutodetectedImport", "");
        //defaults.put("autoRemoveExactDuplicates", Boolean.FALSE);
        //defaults.put("confirmAutoRemoveExactDuplicates", Boolean.TRUE);
        //defaults.put("tempDir", System.getProperty("java.io.tmpdir"));
        //Util.pr(System.getProperty("java.io.tempdir"));
        //defaults.put("keyPattern", new LabelPattern(KEY_PATTERN));
        defaults.put(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE, Boolean.FALSE);
        defaults.put(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE, ImportSettingsTab.DEFAULT_STYLE);
        defaults.put(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN, ImportSettingsTab.DEFAULT_FILENAMEPATTERNS[0]);

        restoreKeyBindings();

        customExports = new CustomExportList(new ExportComparator());
        customImports = new CustomImportList(this);

        //defaults.put("oooWarning", Boolean.TRUE);
        updateSpecialFieldHandling();
        WRAPPED_USERNAME = '[' + get("defaultOwner") + ']';
        MARKING_WITH_NUMBER_PATTERN = "\\[" + get("defaultOwner").replaceAll("\\\\", "\\\\\\\\") + ":(\\d+)\\]";

        String defaultExpression = "**/.*[bibtexkey].*\\\\.[extension]";
        defaults.put(JabRefPreferences.DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(JabRefPreferences.USE_REG_EXP_SEARCH_KEY, Boolean.FALSE);
        defaults.put("useIEEEAbrv", Boolean.TRUE);
        defaults.put("useConvertToEquation", Boolean.FALSE);
        defaults.put("useCaseKeeperOnSearch", Boolean.TRUE);
        defaults.put("useUnitFormatterOnSearch", Boolean.TRUE);

        defaults.put("userFileDir", GUIGlobals.FILE_FIELD + "Directory");
        try {
            defaults.put("userFileDirInd_Legacy", GUIGlobals.FILE_FIELD + "Directory" + '-' + get("defaultOwner") + '@' + InetAddress.getLocalHost().getHostName()); // Legacy setting name - was a bug: @ not allowed inside BibTeX comment text. Retained for backward comp.
            defaults.put("userFileDirIndividual", GUIGlobals.FILE_FIELD + "Directory" + '-' + get("defaultOwner") + '-' + InetAddress.getLocalHost().getHostName()); // Valid setting name
        } catch (UnknownHostException ex) {
            Globals.logger("Hostname not found.");
            defaults.put("userFileDirInd_Legacy", GUIGlobals.FILE_FIELD + "Directory" + '-' + get("defaultOwner"));
            defaults.put("userFileDirIndividual", GUIGlobals.FILE_FIELD + "Directory" + '-' + get("defaultOwner"));
        }
    }

    public void setLanguageDependentDefaultValues() {

        // Entry editor tab 0:
        defaults.put(JabRefPreferences.CUSTOM_TAB_NAME + "_def0", Globals.lang("General"));
        defaults.put(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def0", "crossref;keywords;file;doi;url;"
                + "comment;owner;timestamp");

        // Entry editor tab 1:
        defaults.put(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def1", "abstract");
        defaults.put(JabRefPreferences.CUSTOM_TAB_NAME + "_def1", Globals.lang("Abstract"));

        // Entry editor tab 2: Review Field - used for research comments, etc.
        defaults.put(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def2", "review");
        defaults.put(JabRefPreferences.CUSTOM_TAB_NAME + "_def2", Globals.lang("Review"));

    }


    public static final String DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY = "defaultRegExpSearchExpression";
    public static final String REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";

    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";

    public static final String GROUP_SHOW_NUMBER_OF_ELEMENTS = "groupShowNumberOfElements";


    public boolean putBracesAroundCapitals(String fieldName) {
        return putBracesAroundCapitalsFields.contains(fieldName);
    }

    public void updateSpecialFieldHandling() {
        putBracesAroundCapitalsFields.clear();
        String fieldString = get("putBracesAroundCapitals");
        if (fieldString.length() > 0) {
            String[] fields = fieldString.split(";");
            for (String field : fields) {
                putBracesAroundCapitalsFields.add(field.trim());
            }
        }
        nonWrappableFields.clear();
        fieldString = get("nonWrappableFields");
        if (fieldString.length() > 0) {
            String[] fields = fieldString.split(";");
            for (String field : fields) {
                nonWrappableFields.add(field.trim());
            }
        }

    }

    public char getValueDelimiters(int index) {
        return getValueDelimiters()[index];
    }

    private char[] getValueDelimiters() {
        return JabRefPreferences.VALUE_DELIMITERS[getInt("valueDelimiters")];
    }

    /**
     * Check whether a key is set (differently from null).
     *
     * @param key The key to check.
     * @return true if the key is set, false otherwise.
     */
    public boolean hasKey(String key) {
        return prefs.get(key, null) != null;
    }

    public String get(String key) {
        //System.out.println("READ PREF [" + key + "]=" + result);
        return prefs.get(key, (String) defaults.get(key));
    }

    public String get(String key, String def) {
        return prefs.get(key, def);
    }

    public boolean getBoolean(String key) {
        return prefs.getBoolean(key, getBooleanDefault(key));
    }

    private boolean getBooleanDefault(String key) {
        return (Boolean) defaults.get(key);
    }

    public double getDouble(String key) {
        return prefs.getDouble(key, getDoubleDefault(key));
    }

    private double getDoubleDefault(String key) {
        return (Double) defaults.get(key);
    }

    public int getInt(String key) {
        return prefs.getInt(key, getIntDefault(key));
    }

    public int getIntDefault(String key) {
        return (Integer) defaults.get(key);
    }

    public byte[] getByteArray(String key) {
        return prefs.getByteArray(key, getByteArrayDefault(key));
    }

    private byte[] getByteArrayDefault(String key) {
        return (byte[]) defaults.get(key);
    }

    public void put(String key, String value) {
        //System.out.println("WRITE PREF [" + key + "]=" + value);
        prefs.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        prefs.putBoolean(key, value);
    }

    public void putDouble(String key, double value) {
        prefs.putDouble(key, value);
    }

    public void putInt(String key, int value) {
        prefs.putInt(key, value);
    }

    public void putByteArray(String key, byte[] value) {
        prefs.putByteArray(key, value);
    }

    public void remove(String key) {
        prefs.remove(key);
    }

    /**
     * Puts a string array into the Preferences, by linking its elements with
     * ';' into a single string. Escape characters make the process transparent
     * even if strings contain ';'.
     */
    public void putStringArray(String key, String[] value) {
        if (value == null) {
            remove(key);
            return;
        }

        if (value.length > 0) {
            StringBuilder linked = new StringBuilder();
            for (int i = 0; i < (value.length - 1); i++) {
                linked.append(makeEscape(value[i]));
                linked.append(';');
            }
            linked.append(makeEscape(value[value.length - 1]));
            put(key, linked.toString());
        } else {
            put(key, "");
        }
    }

    /**
     * Returns a String[] containing the chosen columns.
     */
    public String[] getStringArray(String key) {
        String names = get(key);
        if (names == null) {
            return null;
        }

        StringReader rd = new StringReader(names);
        Vector<String> arr = new Vector<String>();
        String rs;
        try {
            while ((rs = getNextUnit(rd)) != null) {
                arr.add(rs);
            }
        } catch (IOException ignored) {
        }
        String[] res = new String[arr.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = arr.elementAt(i);
        }

        return res;
    }

    /**
     * Looks up a color definition in preferences, and returns the Color object.
     *
     * @param key The key for this setting.
     * @return The color corresponding to the setting.
     */
    public Color getColor(String key) {
        String value = get(key);
        int[] rgb = getRgb(value);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    public Color getDefaultColor(String key) {
        String value = (String) defaults.get(key);
        int[] rgb = getRgb(value);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Set the default value for a key. This is useful for plugins that need to
     * add default values for the prefs keys they use.
     *
     * @param key The preferences key.
     * @param value The default value.
     */
    public void putDefaultValue(String key, Object value) {
        defaults.put(key, value);
    }

    /**
     * Stores a color in preferences.
     *
     * @param key The key for this setting.
     * @param color The Color to store.
     */
    public void putColor(String key, Color color) {
        String rgb = String.valueOf(color.getRed()) + ':' + String.valueOf(color.getGreen()) + ':' + String.valueOf(color.getBlue());
        put(key, rgb);
    }

    /**
     * Looks up a color definition in preferences, and returns an array
     * containing the RGB values.
     *
     * @param value The key for this setting.
     * @return The RGB values corresponding to this color setting.
     */
    private int[] getRgb(String value) {
        String[] elements = value.split(":");
        int[] values = new int[3];
        values[0] = Integer.parseInt(elements[0]);
        values[1] = Integer.parseInt(elements[1]);
        values[2] = Integer.parseInt(elements[2]);
        return values;
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in
     * the Preferences.
     */
    public KeyStroke getKey(String bindName) {

        String s = keyBinds.get(bindName);
        // If the current key bindings don't contain the one asked for,
        // we fall back on the default. This should only happen when a
        // user has his own set in Preferences, and has upgraded to a
        // new version where new bindings have been introduced.
        if (s == null) {
            s = defKeyBinds.get(bindName);
            if (s == null) {
                // there isn't even a default value
                // Output error
                Globals.logger("Could not get key binding for \"" + bindName + '"');
                // fall back to a default value
                s = "Not associated";
            }
            // So, if there is no configured key binding, we add the fallback value to the current
            // hashmap, so this doesn't happen again, and so this binding
            // will appear in the KeyBindingsDialog.
            keyBinds.put(bindName, s);
        }

        if (Globals.ON_MAC) {
            return getKeyForMac(KeyStroke.getKeyStroke(s));
        } else {
            return KeyStroke.getKeyStroke(s);
        }
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in
     * the Preferences, but adapted for Mac users, with the Command key
     * preferred instead of Control.
     */
    private KeyStroke getKeyForMac(KeyStroke ks) {
        if (ks == null) {
            return null;
        }
        int keyCode = ks.getKeyCode();
        if ((ks.getModifiers() & InputEvent.CTRL_MASK) == 0) {
            return ks;
        } else {
            int modifiers = 0;
            if ((ks.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                modifiers = modifiers | InputEvent.SHIFT_MASK;
            }
            if ((ks.getModifiers() & InputEvent.ALT_MASK) != 0) {
                modifiers = modifiers | InputEvent.ALT_MASK;
            }

            return KeyStroke.getKeyStroke(keyCode, Globals.getShortcutMask() + modifiers);
        }
    }

    /**
     * Returns the HashMap containing all key bindings.
     */
    public HashMap<String, String> getKeyBindings() {
        return keyBinds;
    }

    /**
     * Returns the HashMap containing default key bindings.
     */
    public HashMap<String, String> getDefaultKeys() {
        return defKeyBinds;
    }

    /**
     * Clear all preferences.
     *
     * @throws BackingStoreException
     */
    public void clear() throws BackingStoreException {
        prefs.clear();
    }

    public void clear(String key) {
        prefs.remove(key);
    }

    /**
     * Calling this method will write all preferences into the preference store.
     */
    public void flush() {
        if (getBoolean("memoryStickMode")) {
            try {
                exportPreferences("jabref.xml");
            } catch (IOException e) {
                Globals.logger("Could not save preferences for memory stick mode: " + e.getLocalizedMessage());
            }
        }
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stores new key bindings into Preferences, provided they actually differ
     * from the old ones.
     */
    public void setNewKeyBindings(HashMap<String, String> newBindings) {
        if (!newBindings.equals(keyBinds)) {
            // This confirms that the bindings have actually changed.
            String[] bindNames = new String[newBindings.size()], bindings = new String[newBindings.size()];
            int index = 0;
            for (String nm : newBindings.keySet()) {
                String bnd = newBindings.get(nm);
                bindNames[index] = nm;
                bindings[index] = bnd;
                index++;
            }
            putStringArray("bindNames", bindNames);
            putStringArray("bindings", bindings);
            keyBinds = newBindings;
        }
    }

    /**
     * Fetches key patterns from preferences Not cached
     *
     * @return LabelPattern containing all keys. Returned LabelPattern has no
     * parent
     */
    public LabelPattern getKeyPattern() {
        JabRefPreferences.keyPattern = new LabelPattern();
        Preferences pre = Preferences.userNodeForPackage(net.sf.jabref.labelPattern.LabelPattern.class);
        try {
            String[] keys = pre.keys();
            if (keys.length > 0) {
                for (String key : keys) {
                    JabRefPreferences.keyPattern.addLabelPattern(key, pre.get(key, null));
                }
            }
        } catch (BackingStoreException ex) {
            Globals.logger("BackingStoreException in JabRefPreferences.getKeyPattern");
        }
        return JabRefPreferences.keyPattern;
    }

    /**
     * Adds the given key pattern to the preferences
     *
     * @param pattern the pattern to store
     */
    public void putKeyPattern(LabelPattern pattern) {
        JabRefPreferences.keyPattern = pattern;

        // Store overridden definitions to Preferences.
        Preferences pre = Preferences.userNodeForPackage(net.sf.jabref.labelPattern.LabelPattern.class);
        try {
            pre.clear(); // We remove all old entries.
        } catch (BackingStoreException ex) {
            Globals.logger("BackingStoreException in JabRefPreferences.putKeyPattern");
        }

        for (Map.Entry<String, ArrayList<String>> stringArrayListEntry : pattern.entrySet()) {
            ArrayList<String> value = stringArrayListEntry.getValue();
            if (value != null) {
                // no default value
                // the first entry in the array is the full pattern
                // see net.sf.jabref.labelPattern.LabelPatternUtil.split(String)
                pre.put(stringArrayListEntry.getKey(), value.get(0));
            }
        }
    }

    private void restoreKeyBindings() {
        // Define default keybindings.
        defineDefaultKeyBindings();

        // First read the bindings, and their names.
        String[] bindNames = getStringArray("bindNames"), bindings = getStringArray("bindings");

        // Then set up the key bindings HashMap.
        if ((bindNames == null) || (bindings == null)
                || (bindNames.length != bindings.length)) {
            // Nothing defined in Preferences, or something is wrong.
            setDefaultKeyBindings();
            return;
        }

        for (int i = 0; i < bindNames.length; i++) {
            keyBinds.put(bindNames[i], bindings[i]);
        }
    }

    private void setDefaultKeyBindings() {
        keyBinds = defKeyBinds;
    }

    private void defineDefaultKeyBindings() {
        defKeyBinds.put("Push to application", "ctrl L");
        defKeyBinds.put("Push to LyX", "ctrl L");
        defKeyBinds.put("Push to WinEdt", "ctrl shift W");
        defKeyBinds.put("Quit JabRef", "ctrl Q");
        defKeyBinds.put("Open database", "ctrl O");
        defKeyBinds.put("Save database", "ctrl S");
        defKeyBinds.put("Save database as ...", "ctrl shift S");
        defKeyBinds.put("Save all", "ctrl alt S");
        defKeyBinds.put("Close database", "ctrl W");
        defKeyBinds.put("New entry", "ctrl N");
        defKeyBinds.put("Cut", "ctrl X");
        defKeyBinds.put("Copy", "ctrl C");
        defKeyBinds.put("Paste", "ctrl V");
        defKeyBinds.put("Undo", "ctrl Z");
        defKeyBinds.put("Redo", "ctrl Y");
        defKeyBinds.put("Help", "F1");
        defKeyBinds.put("New article", "ctrl shift A");
        defKeyBinds.put("New book", "ctrl shift B");
        defKeyBinds.put("New phdthesis", "ctrl shift T");
        defKeyBinds.put("New inbook", "ctrl shift I");
        defKeyBinds.put("New mastersthesis", "ctrl shift M");
        defKeyBinds.put("New proceedings", "ctrl shift P");
        defKeyBinds.put("New unpublished", "ctrl shift U");
        defKeyBinds.put("Edit strings", "ctrl T");
        defKeyBinds.put("Edit preamble", "ctrl P");
        defKeyBinds.put("Select all", "ctrl A");
        defKeyBinds.put("Toggle groups interface", "ctrl shift G");
        defKeyBinds.put("Autogenerate BibTeX keys", "ctrl G");
        defKeyBinds.put("Search", "ctrl F");
        defKeyBinds.put("Incremental search", "ctrl shift F");
        defKeyBinds.put("Repeat incremental search", "ctrl shift F");
        defKeyBinds.put("Close dialog", "ESCAPE");
        defKeyBinds.put("Close entry editor", "ESCAPE");
        defKeyBinds.put("Close preamble editor", "ESCAPE");
        defKeyBinds.put("Back, help dialog", "LEFT");
        defKeyBinds.put("Forward, help dialog", "RIGHT");
        defKeyBinds.put("Preamble editor, store changes", "alt S");
        defKeyBinds.put("Clear search", "ESCAPE");
        defKeyBinds.put("Entry editor, next panel", "ctrl TAB");//"ctrl PLUS");//"shift Right");
        defKeyBinds.put("Entry editor, previous panel", "ctrl shift TAB");//"ctrl MINUS");
        defKeyBinds.put("Entry editor, next panel 2", "ctrl PLUS");//"ctrl PLUS");//"shift Right");
        defKeyBinds.put("Entry editor, previous panel 2", "ctrl MINUS");//"ctrl MINUS");
        defKeyBinds.put("Entry editor, next entry", "ctrl shift DOWN");
        defKeyBinds.put("Entry editor, previous entry", "ctrl shift UP");
        defKeyBinds.put("Entry editor, store field", "alt S");
        defKeyBinds.put("String dialog, add string", "ctrl N");
        defKeyBinds.put("String dialog, remove string", "shift DELETE");
        defKeyBinds.put("String dialog, move string up", "ctrl UP");
        defKeyBinds.put("String dialog, move string down", "ctrl DOWN");
        defKeyBinds.put("Save session", "F11");
        defKeyBinds.put("Load session", "F12");
        defKeyBinds.put("Copy \\cite{BibTeX key}", "ctrl K");
        defKeyBinds.put("Copy BibTeX key", "ctrl shift K");
        defKeyBinds.put("Copy BibTeX key and title", "ctrl shift alt K");
        defKeyBinds.put("Next tab", "ctrl PAGE_DOWN");
        defKeyBinds.put("Previous tab", "ctrl PAGE_UP");
        defKeyBinds.put("Replace string", "ctrl R");
        defKeyBinds.put("Delete", "DELETE");
        defKeyBinds.put("Open file", "F4");
        defKeyBinds.put("Open folder", "ctrl shift O");
        defKeyBinds.put("Open PDF or PS", "shift F5");
        defKeyBinds.put("Open URL or DOI", "F3");
        defKeyBinds.put("Open SPIRES entry", "ctrl F3");
        defKeyBinds.put("Toggle entry preview", "ctrl F9");
        defKeyBinds.put("Switch preview layout", "F9");
        defKeyBinds.put("Edit entry", "ctrl E");
        defKeyBinds.put("Mark entries", "ctrl M");
        defKeyBinds.put("Unmark entries", "ctrl shift M");
        defKeyBinds.put("Fetch Medline", "F5");
        defKeyBinds.put("Search ScienceDirect", "ctrl F5");
        defKeyBinds.put("Search ADS", "ctrl shift F6");
        defKeyBinds.put("New from plain text", "ctrl shift N");
        defKeyBinds.put("Synchronize files", "ctrl F4");
        defKeyBinds.put("Synchronize PDF", "shift F4");
        defKeyBinds.put("Synchronize PS", "ctrl shift F4");
        defKeyBinds.put("Focus entry table", "ctrl shift E");

        defKeyBinds.put("Abbreviate", "ctrl alt A");
        defKeyBinds.put("Unabbreviate", "ctrl alt shift A");
        defKeyBinds.put("Search IEEEXplore", "alt F8");
        defKeyBinds.put("Search ACM Portal", "ctrl shift F8");
        defKeyBinds.put("Fetch ArXiv.org", "shift F8");
        defKeyBinds.put("Search JSTOR", "shift F9");
        defKeyBinds.put("Cleanup", "ctrl shift F7");
        defKeyBinds.put("Write XMP", "ctrl F7");
        defKeyBinds.put("New file link", "ctrl N");
        defKeyBinds.put("Fetch SPIRES", "ctrl F8");
        defKeyBinds.put("Fetch INSPIRE", "ctrl F2");
        defKeyBinds.put("Back", "alt LEFT");
        defKeyBinds.put("Forward", "alt RIGHT");
        defKeyBinds.put("Import into current database", "ctrl I");
        defKeyBinds.put("Import into new database", "ctrl alt I");
        defKeyBinds.put(FindUnlinkedFilesDialog.ACTION_KEYBINDING_ACTION, "shift F7");
        defKeyBinds.put("Increase table font size", "ctrl PLUS");
        defKeyBinds.put("Decrease table font size", "ctrl MINUS");
        defKeyBinds.put("Automatically link files", "alt F");
        defKeyBinds.put("Resolve duplicate BibTeX keys", "ctrl shift D");
        defKeyBinds.put("Refresh OO", "ctrl alt O");
        defKeyBinds.put("File list editor, move entry up", "ctrl UP");
        defKeyBinds.put("File list editor, move entry down", "ctrl DOWN");
        defKeyBinds.put("Minimize to system tray", "ctrl alt W");
        defKeyBinds.put("Hide/show toolbar", "ctrl alt T");
    }

    private String getNextUnit(Reader data) throws IOException {
        // character last read
        // -1 if end of stream
        // initialization necessary, because of Java compiler
        int c = -1;

        // last character was escape symbol
        boolean escape = false;

        // true if a ";" is found
        boolean done = false;

        StringBuffer res = new StringBuffer();
        while (!done && ((c = data.read()) != -1)) {
            if (c == '\\') {
                if (!escape) {
                    escape = true;
                } else {
                    escape = false;
                    res.append('\\');
                }
            } else {
                if (c == ';') {
                    if (!escape) {
                        done = true;
                    } else {
                        res.append(';');
                    }
                } else {
                    res.append((char) c);
                }
                escape = false;
            }
        }
        if (res.length() > 0) {
            return res.toString();
        } else if (c == -1) {
            // end of stream
            return null;
        } else {
            return "";
        }
    }

    private String makeEscape(String s) {
        StringBuffer sb = new StringBuffer();
        int c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if ((c == '\\') || (c == ';')) {
                sb.append('\\');
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    /**
     * Stores all information about the entry type in preferences, with the tag
     * given by number.
     */
    public void storeCustomEntryType(CustomEntryType tp, int number) {
        String nr = "" + number;
        put(JabRefPreferences.CUSTOM_TYPE_NAME + nr, tp.getName());
        put(JabRefPreferences.CUSTOM_TYPE_REQ + nr, tp.getRequiredFieldsString());//tp.getRequiredFields());
        putStringArray(JabRefPreferences.CUSTOM_TYPE_OPT + nr, tp.getOptionalFields());
        putStringArray(JabRefPreferences.CUSTOM_TYPE_PRIOPT + nr, tp.getPrimaryOptionalFields());

    }

    /**
     * Retrieves all information about the entry type in preferences, with the
     * tag given by number.
     */
    public CustomEntryType getCustomEntryType(int number) {
        String nr = "" + number;
        String name = get(JabRefPreferences.CUSTOM_TYPE_NAME + nr);
        String[] req = getStringArray(JabRefPreferences.CUSTOM_TYPE_REQ + nr), opt = getStringArray(JabRefPreferences.CUSTOM_TYPE_OPT + nr), priOpt = getStringArray(JabRefPreferences.CUSTOM_TYPE_PRIOPT + nr);
        if (name == null) {
            return null;
        }
        if (priOpt == null) {
            return new CustomEntryType(Util.nCase(name), req, opt);
        }
        ArrayList<String> secOpt = new ArrayList<String>();
        Collections.addAll(secOpt, opt);
        for (String aPriOpt : priOpt) {
            secOpt.remove(aPriOpt);
        }
        return new CustomEntryType(Util.nCase(name), req, priOpt,
                secOpt.toArray(new String[secOpt.size()]));

    }

    public List<ExternalFileType> getDefaultExternalFileTypes() {
        List<ExternalFileType> list = new ArrayList<ExternalFileType>();
        list.add(new ExternalFileType("PDF", "pdf", "application/pdf", "evince", "pdfSmall"));
        list.add(new ExternalFileType("PostScript", "ps", "application/postscript", "evince", "psSmall"));
        list.add(new ExternalFileType("Word", "doc", "application/msword", "oowriter", "openoffice"));
        list.add(new ExternalFileType("Word 2007+", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "oowriter", "openoffice"));
        list.add(new ExternalFileType("OpenDocument text", "odt", "application/vnd.oasis.opendocument.text", "oowriter", "openoffice"));
        list.add(new ExternalFileType("Excel", "xls", "application/excel", "oocalc", "openoffice"));
        list.add(new ExternalFileType("Excel 2007+", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "oocalc", "openoffice"));
        list.add(new ExternalFileType("OpenDocument spreadsheet", "ods", "application/vnd.oasis.opendocument.spreadsheet", "oocalc", "openoffice"));
        list.add(new ExternalFileType("PowerPoint", "ppt", "application/vnd.ms-powerpoint", "ooimpress", "openoffice"));
        list.add(new ExternalFileType("PowerPoint 2007+", "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "ooimpress", "openoffice"));
        list.add(new ExternalFileType("OpenDocument presentation", "odp", "application/vnd.oasis.opendocument.presentation", "ooimpress", "openoffice"));
        list.add(new ExternalFileType("Rich Text Format", "rtf", "application/rtf", "oowriter", "openoffice"));
        list.add(new ExternalFileType("PNG image", "png", "image/png", "gimp", "picture"));
        list.add(new ExternalFileType("GIF image", "gif", "image/gif", "gimp", "picture"));
        list.add(new ExternalFileType("JPG image", "jpg", "image/jpeg", "gimp", "picture"));
        list.add(new ExternalFileType("Djvu", "djvu", "", "evince", "psSmall"));
        list.add(new ExternalFileType("Text", "txt", "text/plain", "emacs", "emacs"));
        list.add(new ExternalFileType("LaTeX", "tex", "application/x-latex", "emacs", "emacs"));
        list.add(new ExternalFileType("CHM", "chm", "application/mshelp", "gnochm", "www"));
        list.add(new ExternalFileType("TIFF image", "tiff", "image/tiff", "gimp", "picture"));
        list.add(new ExternalFileType("URL", "html", "text/html", "firefox", "www"));
        list.add(new ExternalFileType("MHT", "mht", "multipart/related", "firefox", "www"));
        list.add(new ExternalFileType("ePUB", "epub", "application/epub+zip", "firefox", "www"));

        // On all OSes there is a generic application available to handle file opening,
        // so we don't need the default application settings anymore:
        for (ExternalFileType type : list) {
            type.setOpenWith("");
        }

        return list;
    }

    public ExternalFileType[] getExternalFileTypeSelection() {
        return externalFileTypes.toArray(new ExternalFileType[externalFileTypes.size()]);
    }

    /**
     * Look up the external file type registered with this name, if any.
     *
     * @param name The file type name.
     * @return The ExternalFileType registered, or null if none.
     */
    public ExternalFileType getExternalFileTypeByName(String name) {
        for (ExternalFileType type : externalFileTypes) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        // Return an instance that signifies an unknown file type:
        return new UnknownExternalFileType(name);
    }

    /**
     * Look up the external file type registered for this extension, if any.
     *
     * @param extension The file extension.
     * @return The ExternalFileType registered, or null if none.
     */
    public ExternalFileType getExternalFileTypeByExt(String extension) {
        for (ExternalFileType type : externalFileTypes) {
            if ((type.getExtension() != null) && type.getExtension().equalsIgnoreCase(extension)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Look up the external file type registered for this filename, if any.
     *
     * @param filename The name of the file whose type to look up.
     * @return The ExternalFileType registered, or null if none.
     */
    public ExternalFileType getExternalFileTypeForName(String filename) {
        int longestFound = -1;
        ExternalFileType foundType = null;
        for (ExternalFileType type : externalFileTypes) {
            if ((type.getExtension() != null) && filename.toLowerCase().
                    endsWith(type.getExtension().toLowerCase())) {
                if (type.getExtension().length() > longestFound) {
                    longestFound = type.getExtension().length();
                    foundType = type;
                }
            }
        }
        return foundType;
    }

    /**
     * Look up the external file type registered for this MIME type, if any.
     *
     * @param mimeType The MIME type.
     * @return The ExternalFileType registered, or null if none. For the mime
     * type "text/html", a valid file type is guaranteed to be returned.
     */
    public ExternalFileType getExternalFileTypeByMimeType(String mimeType) {
        for (ExternalFileType type : externalFileTypes) {
            if ((type.getMimeType() != null) && type.getMimeType().equals(mimeType)) {
                return type;
            }
        }
        if (mimeType.equals("text/html")) {
            return HTML_FALLBACK_TYPE;
        } else {
            return null;
        }
    }

    /**
     * Reset the List of external file types after user customization.
     *
     * @param types The new List of external file types. This is the complete
     * list, not just new entries.
     */
    public void setExternalFileTypes(List<ExternalFileType> types) {

        // First find a list of the default types:
        List<ExternalFileType> defTypes = getDefaultExternalFileTypes();
        // Make a list of types that are unchanged:
        List<ExternalFileType> unchanged = new ArrayList<ExternalFileType>();

        externalFileTypes.clear();
        for (ExternalFileType type : types) {
            externalFileTypes.add(type);

            // See if we can find a type with matching name in the default type list:
            ExternalFileType found = null;
            for (ExternalFileType defType : defTypes) {
                if (defType.getName().equals(type.getName())) {
                    found = defType;
                    break;
                }
            }
            if (found != null) {
                // Found it! Check if it is an exact match, or if it has been customized:
                if (found.equals(type)) {
                    unchanged.add(type);
                } else {
                    // It was modified. Remove its entry from the defaults list, since
                    // the type hasn't been removed:
                    defTypes.remove(found);
                }
            }
        }

        // Go through unchanged types. Remove them from the ones that should be stored,
        // and from the list of defaults, since we don't need to mention these in prefs:
        for (ExternalFileType type : unchanged) {
            defTypes.remove(type);
            types.remove(type);
        }

        // Now set up the array to write to prefs, containing all new types, all modified
        // types, and a flag denoting each default type that has been removed:
        String[][] array = new String[types.size() + defTypes.size()][];
        int i = 0;
        for (ExternalFileType type : types) {
            array[i] = type.getStringArrayRepresentation();
            i++;
        }
        for (ExternalFileType type : defTypes) {
            array[i] = new String[] {type.getName(), JabRefPreferences.FILE_TYPE_REMOVED_FLAG};
            i++;
        }
        //System.out.println("Encoded: '"+Util.encodeStringArray(array)+"'");
        put("externalFileTypes", Util.encodeStringArray(array));
    }

    /**
     * Set up the list of external file types, either from default values, or
     * from values recorded in Preferences.
     */
    public void updateExternalFileTypes() {
        // First get a list of the default file types as a starting point:
        List<ExternalFileType> types = getDefaultExternalFileTypes();
        // If no changes have been stored, simply use the defaults:
        if (prefs.get("externalFileTypes", null) == null) {
            externalFileTypes.clear();
            externalFileTypes.addAll(types);
            return;
        }
        // Read the prefs information for file types:
        String[][] vals = Util.decodeStringDoubleArray(prefs.get("externalFileTypes", ""));
        for (String[] val : vals) {
            if ((val.length == 2) && (val[1].equals(JabRefPreferences.FILE_TYPE_REMOVED_FLAG))) {
                // This entry indicates that a default entry type should be removed:
                ExternalFileType toRemove = null;
                for (ExternalFileType type : types) {
                    if (type.getName().equals(val[0])) {
                        toRemove = type;
                        break;
                    }
                }
                // If we found it, remove it from the type list:
                if (toRemove != null) {
                    types.remove(toRemove);
                }
            } else {
                // A new or modified entry type. Construct it from the string array:
                ExternalFileType type = new ExternalFileType(val);
                // Check if there is a default type with the same name. If so, this is a
                // modification of that type, so remove the default one:
                ExternalFileType toRemove = null;
                for (ExternalFileType defType : types) {
                    if (type.getName().equals(defType.getName())) {
                        toRemove = defType;
                        break;
                    }
                }
                // If we found it, remove it from the type list:
                if (toRemove != null) {
                    types.remove(toRemove);
                }

                // Then add the new one:
                types.add(type);
            }
        }

        // Finally, build the list of types based on the modified defaults list:
        for (ExternalFileType type : types) {
            externalFileTypes.add(type);
        }
    }

    /**
     * Removes all information about custom entry types with tags of
     *
     * @param number or higher.
     */
    public void purgeCustomEntryTypes(int number) {
        purgeSeries(JabRefPreferences.CUSTOM_TYPE_NAME, number);
        purgeSeries(JabRefPreferences.CUSTOM_TYPE_REQ, number);
        purgeSeries(JabRefPreferences.CUSTOM_TYPE_OPT, number);
        purgeSeries(JabRefPreferences.CUSTOM_TYPE_PRIOPT, number);
    }

    /**
     * Removes all entries keyed by prefix+number, where number is equal to or
     * higher than the given number.
     *
     * @param number or higher.
     */
    public void purgeSeries(String prefix, int number) {
        while (get(prefix + number) != null) {
            remove(prefix + number);
            number++;
        }
    }

    public EntryEditorTabList getEntryEditorTabList() {
        if (tabList == null) {
            updateEntryEditorTabList();
        }
        return tabList;
    }

    public void updateEntryEditorTabList() {
        tabList = new EntryEditorTabList();
    }

    /**
     * Exports Preferences to an XML file.
     *
     * @param filename String File to export to
     */
    public void exportPreferences(String filename) throws IOException {
        File f = new File(filename);
        OutputStream os = new FileOutputStream(f);
        try {
            prefs.exportSubtree(os);
        } catch (BackingStoreException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Imports Preferences from an XML file.
     *
     * @param filename String File to import from
     */
    public void importPreferences(String filename) throws IOException {
        File f = new File(filename);
        InputStream is = new FileInputStream(f);
        try {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Determines whether the given field should be written without any sort of
     * wrapping.
     *
     * @param fieldName The field name.
     * @return true if the field should not be wrapped.
     */
    public boolean isNonWrappableField(String fieldName) {
        return nonWrappableFields.contains(fieldName);
    }
}
