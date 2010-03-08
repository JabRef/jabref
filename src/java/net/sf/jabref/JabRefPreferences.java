/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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

package net.sf.jabref;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.swing.JTable;
import javax.swing.KeyStroke;

import net.sf.jabref.export.CustomExportList;
import net.sf.jabref.export.ExportComparator;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.gui.PersistenceTableColumnListener;
import net.sf.jabref.imports.CustomImportList;
import net.sf.jabref.labelPattern.DefaultLabelPatterns;
import net.sf.jabref.labelPattern.LabelPattern;

public class JabRefPreferences {

    public final String
        CUSTOM_TYPE_NAME = "customTypeName_",
        CUSTOM_TYPE_REQ = "customTypeReq_",
        CUSTOM_TYPE_OPT = "customTypeOpt_",
        CUSTOM_TAB_NAME = "customTabName_",
        CUSTOM_TAB_FIELDS = "customTabFields_";

    // This String is used in the encoded list in prefs of external file type
    // modifications, in order to indicate a removed default file type:
    public static final String FILE_TYPE_REMOVED_FLAG = "REMOVED";

    public String WRAPPED_USERNAME;

    Preferences prefs;
    public HashMap<String, Object> defaults = new HashMap<String, Object>();
    public HashMap<String, String>
        keyBinds = new HashMap<String, String>(),
        defKeyBinds = new HashMap<String, String>();
    private HashSet<String> putBracesAroundCapitalsFields = new HashSet<String>(4);
    private HashSet<String> nonWrappableFields = new HashSet<String>(5);
    private static final LabelPattern KEY_PATTERN = new DefaultLabelPatterns();
    private static LabelPattern keyPattern;

    // Object containing custom export formats:
    public CustomExportList customExports;

    /** Set with all custom {@link net.sf.jabref.imports.ImportFormat}s */
    public CustomImportList customImports;

    // Object containing info about customized entry editor tabs.
    private EntryEditorTabList tabList = null;

    // Map containing all registered external file types:
    private TreeSet<ExternalFileType> externalFileTypes = new TreeSet<ExternalFileType>();

    // The following field is used as a global variable during the export of a database.
    // By setting this field to the path of the database's default file directory, formatters
    // that should resolve external file paths can access this field. This is an ugly hack
    // to solve the problem of formatters not having access to any context except for the
    // string to be formatted and possible formatter arguments.
    public String fileDirForDatabase = null;

    // The following field is used as a global variable during the export of a database.
    // It is used to hold custom name formatters defined by a custom export filter.
    // It is set before the export starts:
    public HashMap<String,String> customExportNameFormatters = null;

    // The only instance of this class:
    private static JabRefPreferences singleton = null;

    public static JabRefPreferences getInstance() {
		if (singleton == null)
			singleton = new JabRefPreferences();
		return singleton;
	}

    // The constructor is made private to enforce this as a singleton class:
    private JabRefPreferences() {

        try {
            if (new File("jabref.xml").exists()){
                importPreferences("jabref.xml");
            }
        } catch (IOException e) {
            Globals.logger("Could not import preferences from jabref.xml:" + e.getLocalizedMessage());
        }
        
        prefs = Preferences.userNodeForPackage(JabRef.class);
        
        if (Globals.osName.equals(Globals.MAC)) {
			defaults.put("pdfviewer", "/Applications/Preview.app");
			defaults.put("psviewer", "/Applications/Preview.app");
			defaults.put("htmlviewer", "/Applications/Safari.app");
            defaults.put("fontFamily", "SansSerif");

		} else if (Globals.osName.toLowerCase().startsWith("windows")) {
			defaults.put("pdfviewer", "cmd.exe /c start /b");
			defaults.put("psviewer", "cmd.exe /c start /b");
			defaults.put("htmlviewer", "cmd.exe /c start /b");
			defaults.put("lookAndFeel", "com.jgoodies.looks.windows.WindowsLookAndFeel");
            defaults.put("winEdtPath", "C:\\Program Files\\WinEdt Team\\WinEdt\\WinEdt.exe");
            defaults.put("latexEditorPath", "C:\\Program Files\\LEd\\LEd.exe");
            defaults.put("fontFamily", "Arial");

        } else {
			defaults.put("pdfviewer", "evince");
			defaults.put("psviewer", "gv");
			defaults.put("htmlviewer", "firefox");
			defaults.put("lookAndFeel", "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel");
            defaults.put("fontFamily", "SansSerif");

		}
        defaults.put("useDefaultLookAndFeel", Boolean.TRUE);
        defaults.put("lyxpipe", System.getProperty("user.home")+File.separator+".lyx/lyxpipe");
        defaults.put("vim", "vim");
        defaults.put("vimServer", "vim");
        defaults.put("posX", new Integer(0));
        defaults.put("posY", new Integer(0));
        defaults.put("sizeX", new Integer(840));
        defaults.put("sizeY", new Integer(680));
        defaults.put("windowMaximised", Boolean.FALSE);
        defaults.put("rememberWindowLocation", Boolean.TRUE);
        defaults.put("autoResizeMode", new Integer(JTable.AUTO_RESIZE_ALL_COLUMNS));
        defaults.put("tableColorCodesOn", Boolean.TRUE);
        defaults.put("namesAsIs", Boolean.FALSE);
        defaults.put("namesFf", Boolean.FALSE);
        defaults.put("namesLf", Boolean.FALSE);
        defaults.put("namesNatbib", Boolean.TRUE);
        defaults.put("abbrAuthorNames", Boolean.TRUE);
        defaults.put("namesLastOnly", Boolean.TRUE);
        defaults.put("language", "en");
        defaults.put("showShort", Boolean.TRUE);
        defaults.put("priSort", "author");
        defaults.put("priDescending", Boolean.FALSE);
        defaults.put("priBinary", Boolean.FALSE);
        defaults.put("secSort", "year");
        defaults.put("secDescending", Boolean.TRUE);
        defaults.put("terSort", "author");
        defaults.put("terDescending", Boolean.FALSE);
        defaults.put("columnNames", "entrytype;author;title;year;journal;owner;timestamp;bibtexkey");
        defaults.put("columnWidths","75;280;400;60;100;100;100;100");
        defaults.put(PersistenceTableColumnListener.ACTIVATE_PREF_KEY, 
        		new Boolean(PersistenceTableColumnListener.DEFAULT_ENABLED));
        defaults.put("xmpPrivacyFilters", "pdf;timestamp;keywords;owner;note;review");
        defaults.put("useXmpPrivacyFilter", Boolean.FALSE);
        defaults.put("numberColWidth",new Integer(GUIGlobals.NUMBER_COL_LENGTH));
        defaults.put("workingDirectory", System.getProperty("user.home"));
        defaults.put("exportWorkingDirectory", System.getProperty("user.home"));
        defaults.put("importWorkingDirectory", System.getProperty("user.home"));
        defaults.put("fileWorkingDirectory", System.getProperty("user.home"));
        defaults.put("autoOpenForm", Boolean.TRUE);
        defaults.put("entryTypeFormHeightFactor", new Integer(1));
        defaults.put("entryTypeFormWidth", new Integer(1));
        defaults.put("backup", Boolean.TRUE);
        defaults.put("openLastEdited", Boolean.TRUE);
        defaults.put("lastEdited", null);
        defaults.put("stringsPosX", new Integer(0));
        defaults.put("stringsPosY", new Integer(0));
        defaults.put("stringsSizeX", new Integer(600));
        defaults.put("stringsSizeY", new Integer(400));
        defaults.put("defaultShowSource", Boolean.FALSE);
        defaults.put("showSource", Boolean.TRUE);
        defaults.put("defaultAutoSort", Boolean.FALSE);
        defaults.put("enableSourceEditing", Boolean.TRUE);
        defaults.put("caseSensitiveSearch", Boolean.FALSE);
        defaults.put("searchReq", Boolean.TRUE);
        defaults.put("searchOpt", Boolean.TRUE);
        defaults.put("searchGen", Boolean.TRUE);
        defaults.put("searchAll", Boolean.FALSE);
        defaults.put("incrementS", Boolean.FALSE);
        defaults.put("saveInStandardOrder", Boolean.TRUE);
        defaults.put("saveInOriginalOrder", Boolean.FALSE);
        defaults.put("exportInStandardOrder", Boolean.TRUE);
        defaults.put("exportInOriginalOrder", Boolean.FALSE);
        defaults.put("selectS", Boolean.FALSE);
        defaults.put("regExpSearch", Boolean.TRUE);
        defaults.put("searchPanePosX", new Integer(0));
        defaults.put("searchPanePosY", new Integer(0));
        defaults.put("autoComplete", Boolean.TRUE);
        defaults.put("autoCompleteFields", "author;editor;title;journal;publisher;keywords;crossref");
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
        defaults.put("groupKeywordSeparator", ", ");
        defaults.put("highlightGroupsMatchingAny", Boolean.FALSE);
        defaults.put("highlightGroupsMatchingAll", Boolean.FALSE);
        defaults.put("searchPanelVisible", Boolean.FALSE);
        defaults.put("defaultEncoding", System.getProperty("file.encoding"));
        defaults.put("groupsVisibleRows", new Integer(8));
        defaults.put("defaultOwner", System.getProperty("user.name"));
        defaults.put("preserveFieldFormatting", Boolean.FALSE);
        defaults.put("memoryStickMode", Boolean.FALSE);
        defaults.put("renameOnMoveFileToFileDir", Boolean.TRUE);

    // The general fields stuff is made obsolete by the CUSTOM_TAB_... entries.
        defaults.put("generalFields", "crossref;keywords;file;doi;url;urldate;citeseerurl;"+
                     "pdf;comment;owner");

        defaults.put("useCustomIconTheme", Boolean.FALSE);
        defaults.put("customIconThemeFile", "/home/alver/div/crystaltheme_16/Icons.properties");

    // Entry editor tab 0:
    defaults.put(CUSTOM_TAB_NAME+"_def0", Globals.lang("General"));
        defaults.put(CUSTOM_TAB_FIELDS+"_def0", "crossref;keywords;file;doi;url;citeseerurl;"+
                     "comment;owner;timestamp");

    // Entry editor tab 1:
        defaults.put(CUSTOM_TAB_FIELDS+"_def1", "abstract");
    defaults.put(CUSTOM_TAB_NAME+"_def1", Globals.lang("Abstract"));

  // Entry editor tab 2: Review Field - used for research comments, etc.
        defaults.put(CUSTOM_TAB_FIELDS+"_def2", "review");
    defaults.put(CUSTOM_TAB_NAME+"_def2", Globals.lang("Review"));

        //defaults.put("recentFiles", "/home/alver/Documents/bibk_dok/hovedbase.bib");
        defaults.put("historySize", new Integer(8));
        defaults.put("fontStyle", new Integer(java.awt.Font.PLAIN));
        defaults.put("fontSize", new Integer(12));
        defaults.put("overrideDefaultFonts", Boolean.FALSE);
        defaults.put("menuFontFamily", "Times");
        defaults.put("menuFontStyle", new Integer(java.awt.Font.PLAIN));
        defaults.put("menuFontSize", new Integer(11));
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
        defaults.put("markedEntryBackground", "255:255:180");
        defaults.put("incompleteEntryBackground", "250:175:175");

        defaults.put("antialias", Boolean.FALSE);
        defaults.put("ctrlClick", Boolean.FALSE);
        defaults.put("disableOnMultipleSelection", Boolean.FALSE);
        defaults.put("pdfColumn", Boolean.FALSE);
        defaults.put("urlColumn", Boolean.TRUE);
        defaults.put("fileColumn", Boolean.TRUE);
        defaults.put("citeseerColumn", Boolean.FALSE);
        defaults.put("useOwner", Boolean.TRUE);
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
        defaults.put("preview0", "<font face=\"arial\">"
                     +"<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                     +"\\end{bibtexkey}</b><br>__NEWLINE__"
                     +"\\begin{author} \\format[HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\author}<BR>\\end{author}__NEWLINE__"
                     +"\\begin{editor} \\format[HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\editor} "
                     +"<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                     +"\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                     +"\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                     +"\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                     // Include the booktitle field for @inproceedings, @proceedings, etc.
                     +"\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                     +"\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                     +"\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                     +"\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                     +"\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                     +"\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                     +"\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                     +"\\begin{review}<BR><BR><b>Review: </b> \\format[HTMLChars]{\\review} \\end{review}"
                     +"</dd>__NEWLINE__<p></p></font>");
        defaults.put("preview1", "<font face=\"arial\">"
                     +"<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                     +"\\end{bibtexkey}</b><br>__NEWLINE__"
                     +"\\begin{author} \\format[HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\author}<BR>\\end{author}__NEWLINE__"
                     +"\\begin{editor} \\format[HTMLChars,AuthorAbbreviator,AuthorAndsReplacer]{\\editor} "
                     +"<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                     +"\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                     +"\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                     +"\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                     // Include the booktitle field for @inproceedings, @proceedings, etc.
                     +"\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                     +"\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                     +"\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                     +"\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                     +"\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                     +"\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}"
                     +"</dd>__NEWLINE__<p></p></font>");


        // TODO: Currently not possible to edit this setting:
        defaults.put("previewPrintButton", Boolean.FALSE);
        defaults.put("autoDoubleBraces", Boolean.FALSE);
        defaults.put("doNotResolveStringsFor", "url");
        defaults.put("resolveStringsAllFields", Boolean.FALSE);
        defaults.put("putBracesAroundCapitals","");//"title;journal;booktitle;review;abstract");
        defaults.put("nonWrappableFields", "pdf;ps;url;doi;file");
        defaults.put("useImportInspectionDialog", Boolean.TRUE);
        defaults.put("useImportInspectionDialogForSingle", Boolean.TRUE);
        defaults.put("generateKeysAfterInspection", Boolean.TRUE);
        defaults.put("markImportedEntries", Boolean.TRUE);
        defaults.put("unmarkAllEntriesBeforeImporting", Boolean.TRUE);
        defaults.put("warnAboutDuplicatesInInspection", Boolean.TRUE);
        defaults.put("useTimeStamp", Boolean.TRUE);
        defaults.put("overwriteTimeStamp", Boolean.FALSE);
        defaults.put("timeStampFormat", "yyyy.MM.dd");
//        defaults.put("timeStampField", "timestamp");
        defaults.put("timeStampField", BibtexFields.TIMESTAMP);
        defaults.put("generateKeysBeforeSaving", Boolean.FALSE);

        defaults.put("useRemoteServer", Boolean.FALSE);
        defaults.put("remoteServerPort", new Integer(6050));

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
        defaults.put("sidePaneWidth", new Integer(-1));

        defaults.put("importInspectionDialogWidth", new Integer(650));
        defaults.put("importInspectionDialogHeight", new Integer(650));
        defaults.put("searchDialogWidth", new Integer(650));
        defaults.put("searchDialogHeight", new Integer(500));
        defaults.put("showFileLinksUpgradeWarning", Boolean.TRUE);
        defaults.put("autolinkExactKeyOnly", Boolean.TRUE);
        defaults.put("numericFields", "mittnum;author");
        defaults.put("runAutomaticFileSearch", Boolean.FALSE);
        defaults.put("useLockFiles", Boolean.TRUE);
        defaults.put("autoSave", Boolean.TRUE);
        defaults.put("autoSaveInterval", 5);
        defaults.put("promptBeforeUsingAutosave", Boolean.TRUE);

        defaults.put("deletePlugins", "");
        
        defaults.put("enforceLegalBibtexKey", Boolean.TRUE);

        defaults.put("biblatexMode", Boolean.FALSE);

        //defaults.put("lastAutodetectedImport", "");

        //defaults.put("autoRemoveExactDuplicates", Boolean.FALSE);
        //defaults.put("confirmAutoRemoveExactDuplicates", Boolean.TRUE);
        
        //defaults.put("tempDir", System.getProperty("java.io.tmpdir"));
        //Util.pr(System.getProperty("java.io.tempdir"));

        //defaults.put("keyPattern", new LabelPattern(KEY_PATTERN));

        restoreKeyBindings();

        customExports = new CustomExportList(new ExportComparator());
        customImports = new CustomImportList(this);

        //defaults.put("oooWarning", Boolean.TRUE);
        updateSpecialFieldHandling();
        WRAPPED_USERNAME = "["+get("defaultOwner")+"]";

        String defaultExpression = "**/.*[bibtexkey].*\\\\.[extension]";
        defaults.put(DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(USE_REG_EXP_SEARCH_KEY, Boolean.FALSE);
        defaults.put("useIEEEAbrv", Boolean.TRUE);
    }
    
    public static final String DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY = "defaultRegExpSearchExpression";
    public static final String REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    
    public boolean putBracesAroundCapitals(String fieldName) {
        return putBracesAroundCapitalsFields.contains(fieldName);
    }

    public void updateSpecialFieldHandling() {
        putBracesAroundCapitalsFields.clear();
        String fieldString = get("putBracesAroundCapitals");
        if (fieldString.length() > 0) {
            String[] fields = fieldString.split(";");
            for (int i=0; i<fields.length; i++)
                putBracesAroundCapitalsFields.add(fields[i].trim());
        }
        nonWrappableFields.clear();
        fieldString = get("nonWrappableFields");
        if (fieldString.length() > 0) {
            String[] fields = fieldString.split(";");
            for (int i=0; i<fields.length; i++)
                nonWrappableFields.add(fields[i].trim());
        }

    }

    /**
     * Check whether a key is set (differently from null).
     * @param key The key to check.
     * @return true if the key is set, false otherwise.
     */
    public boolean hasKey(String key) {
        return prefs.get(key, null) != null;
    }

    public String get(String key) {
        return prefs.get(key, (String)defaults.get(key));
    }

    public String get(String key, String def) {
        return prefs.get(key, def);
    }

    public boolean getBoolean(String key) {
        return prefs.getBoolean(key, getBooleanDefault(key));
    }
    
    public boolean getBooleanDefault(String key){
        return ((Boolean)defaults.get(key)).booleanValue();
    }

    public double getDouble(String key) {
        return prefs.getDouble(key, getDoubleDefault(key));
    }
    
    public double getDoubleDefault(String key){
        return ((Double)defaults.get(key)).doubleValue();
    }

    public int getInt(String key) {
        return prefs.getInt(key, getIntDefault(key));
    }

    public int getIntDefault(String key) {
        return ((Integer)defaults.get(key)).intValue();
    }
    
    public byte[] getByteArray(String key) {
        return prefs.getByteArray(key, getByteArrayDefault(key));
    }

    public byte[] getByteArrayDefault(String key){
        return (byte[])defaults.get(key);   
    }
    
    public void put(String key, String value) {
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
     * Puts a string array into the Preferences, by linking its elements
     * with ';' into a single string. Escape characters make the process
     * transparent even if strings contain ';'.
     */
    public void putStringArray(String key, String[] value) {
        if (value == null) {
            remove(key);
            return;
        }

        if (value.length > 0) {
            StringBuffer linked = new StringBuffer();
            for (int i=0; i<value.length-1; i++) {
                linked.append(makeEscape(value[i]));
                linked.append(";");
            }
            linked.append(makeEscape(value[value.length-1]));
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
        if (names == null)
            return null;

        StringReader rd = new StringReader(names);
        Vector<String> arr = new Vector<String>();
        String rs;
        try {
            while ((rs = getNextUnit(rd)) != null) {
                arr.add(rs);
            }
        } catch (IOException ex) {}
        String[] res = new String[arr.size()];
        for (int i=0; i<res.length; i++)
            res[i] = arr.elementAt(i);

        return res;
    }

    /**
     * Looks up a color definition in preferences, and returns the Color object.
     * @param key The key for this setting.
     * @return The color corresponding to the setting.
     */
    public Color getColor(String key) {
        String value = get(key);
        int[] rgb = getRgb(value);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    public Color getDefaultColor(String key) {
        String value = (String)defaults.get(key);
        int[] rgb = getRgb(value);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Set the default value for a key. This is useful for plugins that need to
     * add default values for the prefs keys they use.
     * @param key The preferences key.
     * @param value The default value.
     */
    public void putDefaultValue(String key, Object value) {
        defaults.put(key, value);
    }

    /**
     * Stores a color in preferences.
     * @param key The key for this setting.
     * @param color The Color to store.
     */
    public void putColor(String key, Color color) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(color.getRed()));
        sb.append(':');
        sb.append(String.valueOf(color.getGreen()));
        sb.append(':');
        sb.append(String.valueOf(color.getBlue()));
        put(key, sb.toString());
    }

    /**
     * Looks up a color definition in preferences, and returns an array containing the RGB values.
     * @param value The key for this setting.
     * @return The RGB values corresponding to this color setting.
     */
    public int[] getRgb(String value) {
        String[] elements = value.split(":");
        int[] values = new int[3];
        values[0] = Integer.parseInt(elements[0]);
        values[1] = Integer.parseInt(elements[1]);
        values[2] = Integer.parseInt(elements[2]);
        return values;
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the
     * defaults, or in the Preferences.
     */
    public KeyStroke getKey(String bindName) {

        String s = keyBinds.get(bindName);
        // If the current key bindings don't contain the one asked for,
        // we fall back on the default. This should only happen when a
        // user has his own set in Preferences, and has upgraded to a
        // new version where new bindings have been introduced.
        if (s == null) {
            s = defKeyBinds.get(bindName);
            // So, if this happens, we add the default value to the current
            // hashmap, so this doesn't happen again, and so this binding
            // will appear in the KeyBindingsDialog.
            keyBinds.put(bindName, s);
        }
        if (s == null) {
          Globals.logger("Could not get key binding for \"" + bindName + "\"");
        }

        if (Globals.ON_MAC)
          return getKeyForMac(KeyStroke.getKeyStroke(s));
        else
          return KeyStroke.getKeyStroke(s);
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the
     * defaults, or in the Preferences, but adapted for Mac users,
     * with the Command key preferred instead of Control.
     */
    private KeyStroke getKeyForMac(KeyStroke ks) {
      if (ks == null) return null;
      int keyCode = ks.getKeyCode();
      if ((ks.getModifiers() & KeyEvent.CTRL_MASK) == 0) {
        return ks;
      }
      else {
        if ((ks.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
          return KeyStroke.getKeyStroke(keyCode, Globals.getShortcutMask()+KeyEvent.SHIFT_MASK);
        }
        return KeyStroke.getKeyStroke(keyCode, Globals.getShortcutMask());
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
     * Calling this method will write all preferences into the preference store.
     */
    public void flush() {
        if (getBoolean("memoryStickMode")){
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
     * Stores new key bindings into Preferences, provided they
     * actually differ from the old ones.
     */
    public void setNewKeyBindings(HashMap<String, String> newBindings) {
        if (!newBindings.equals(keyBinds)) {
            // This confirms that the bindings have actually changed.
            String[] bindNames = new String[newBindings.size()],
                bindings = new String[newBindings.size()];
            int index = 0;
            for (Iterator<String> i=newBindings.keySet().iterator();
                 i.hasNext();) {
                String nm = i.next();
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


        public LabelPattern getKeyPattern(){

            keyPattern = new LabelPattern(KEY_PATTERN);
            Preferences pre = Preferences.userNodeForPackage
                (net.sf.jabref.labelPattern.LabelPattern.class);
            try {
                String[] keys = pre.keys();
            if (keys.length > 0) for (int i=0; i<keys.length; i++)
                keyPattern.addLabelPattern(keys[i], pre.get(keys[i], null));
            } catch (BackingStoreException ex) {
                Globals.logger("BackingStoreException in JabRefPreferences.getKeyPattern");
            }

            ///
            //keyPattern.addLabelPattern("article", "[author][year]");
            //putKeyPattern(keyPattern);
            ///

            return keyPattern;
        }

        public void putKeyPattern(LabelPattern pattern){
            keyPattern = pattern;
            LabelPattern parent = pattern.getParent();
            if (parent == null)
                return;

            // Store overridden definitions to Preferences.
            Preferences pre = Preferences.userNodeForPackage
                (net.sf.jabref.labelPattern.LabelPattern.class);
            try {
                pre.clear(); // We remove all old entries.
            } catch (BackingStoreException ex) {
                Globals.logger("BackingStoreException in JabRefPreferences.putKeyPattern");
            }

            for (String s: pattern.keySet()){
                if (!(pattern.get(s)).equals(parent.get(s)))
                    pre.put(s, pattern.getValue(s).get(0).toString());
            }
        }

    private void restoreKeyBindings() {
        // Define default keybindings.
        defineDefaultKeyBindings();

        // First read the bindings, and their names.
        String[] bindNames = getStringArray("bindNames"),
            bindings = getStringArray("bindings");

        // Then set up the key bindings HashMap.
        if ((bindNames == null) || (bindings == null)
            || (bindNames.length != bindings.length)) {
            // Nothing defined in Preferences, or something is wrong.
            setDefaultKeyBindings();
            return;
        }

        for (int i=0; i<bindNames.length; i++)
            keyBinds.put(bindNames[i], bindings[i]);
    }

    private void setDefaultKeyBindings() {
        keyBinds = defKeyBinds;
    }

    private void defineDefaultKeyBindings() {
        defKeyBinds.put("Push to application","ctrl L");
      defKeyBinds.put("Push to LyX","ctrl L");
      defKeyBinds.put("Push to WinEdt","ctrl shift W");
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
        defKeyBinds.put("Next tab", "ctrl PAGE_DOWN");
        defKeyBinds.put("Previous tab", "ctrl PAGE_UP");
        defKeyBinds.put("Replace string", "ctrl R");
        defKeyBinds.put("Delete", "DELETE");
        defKeyBinds.put("Open file", "F4");
        defKeyBinds.put("Open PDF or PS", "shift F5");
        defKeyBinds.put("Open URL or DOI", "F3");
        defKeyBinds.put("Open SPIRES entry", "ctrl F3");
        defKeyBinds.put("Toggle entry preview", "ctrl F9");
        defKeyBinds.put("Switch preview layout", "F9");
        defKeyBinds.put("Edit entry", "ctrl E");
        defKeyBinds.put("Mark entries", "ctrl M");
        defKeyBinds.put("Unmark entries", "ctrl shift M");
        defKeyBinds.put("Fetch Medline", "F5");
        defKeyBinds.put("Fetch CiteSeer", "F6");
        defKeyBinds.put("Search ScienceDirect", "ctrl F5");
        defKeyBinds.put("New from plain text", "ctrl shift N");
        defKeyBinds.put("Import Fields from CiteSeer", "ctrl shift C");
        defKeyBinds.put("Fetch citations from CiteSeer", "F7");
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
        defKeyBinds.put("Write XMP", "ctrl F4");
        defKeyBinds.put("New file link", "ctrl N");
        defKeyBinds.put("Fetch SPIRES", "ctrl F8");
        defKeyBinds.put("Back", "alt LEFT");
        defKeyBinds.put("Forward", "alt RIGHT");
        defKeyBinds.put("Import into current database", "ctrl I");
        defKeyBinds.put("Import into new database", "ctrl shift I");

        defKeyBinds.put("Increase table font size", "ctrl PLUS");
        defKeyBinds.put("Decrease table font size", "ctrl MINUS");

        defKeyBinds.put("Automatically link files", "alt F");
    }

    private String getNextUnit(Reader data) throws IOException {
        int c;
        boolean escape = false, done = false;
        StringBuffer res = new StringBuffer();
        while (!done && ((c = data.read()) != -1)) {
            if (c == '\\') {
                if (!escape)
                    escape = true;
                else {
                    escape = false;
                    res.append('\\');
                }
            } else {
                if (c == ';') {
                    if (!escape)
                        done = true;
                    else
                        res.append(';');
                } else {
                    res.append((char)c);
                }
                escape = false;
            }
        }
        if (res.length() > 0)
            return res.toString();
        else
            return null;
    }

    private String makeEscape(String s) {
        StringBuffer sb = new StringBuffer();
        int c;
        for (int i=0; i<s.length(); i++) {
            c = s.charAt(i);
            if ((c == '\\') || (c == ';'))
                sb.append('\\');
            sb.append((char)c);
        }
        return sb.toString();
    }

    /**
     * Stores all information about the entry type in preferences, with
     * the tag given by number.
     */
    public void storeCustomEntryType(CustomEntryType tp, int number) {
        String nr = ""+number;
        put(CUSTOM_TYPE_NAME+nr, tp.getName());
        putStringArray(CUSTOM_TYPE_REQ+nr, tp.getRequiredFields());
        putStringArray(CUSTOM_TYPE_OPT+nr, tp.getOptionalFields());

    }

    /**
     * Retrieves all information about the entry type in preferences,
     * with the tag given by number.
     */
    public CustomEntryType getCustomEntryType(int number) {
        String nr = ""+number;
        String
            name = get(CUSTOM_TYPE_NAME+nr);
        String[]
            req = getStringArray(CUSTOM_TYPE_REQ+nr),
            opt = getStringArray(CUSTOM_TYPE_OPT+nr);
        if (name == null)
            return null;
        else return new CustomEntryType
            (Util.nCase(name), req, opt);


    }



    public List<ExternalFileType> getDefaultExternalFileTypes() {
        List<ExternalFileType> list = new ArrayList<ExternalFileType>();
        list.add(new ExternalFileType("PDF", "pdf", "application/pdf", "evince", "pdfSmall"));
        list.add(new ExternalFileType("PostScript", "ps", "application/postscript", "evince", "psSmall"));
        list.add(new ExternalFileType("Word", "doc", "application/msword", "oowriter", "openoffice"));
        list.add(new ExternalFileType("OpenDocument text", "odt", "application/vnd.oasis.opendocument.text", "oowriter", "openoffice"));
        list.add(new ExternalFileType("Excel", "xls", "application/excel", "oocalc", "openoffice"));
        list.add(new ExternalFileType("OpenDocument spreadsheet", "ods", "application/vnd.oasis.opendocument.spreadsheet", "oocalc", "openoffice"));
        list.add(new ExternalFileType("PowerPoint", "ppt", "", "ooimpress", "openoffice"));
        list.add(new ExternalFileType("OpenDocument presentation", "odp", "application/vnd.oasis.opendocument.presentation", "ooimpress", "openoffice"));
        list.add(new ExternalFileType("Rich Text Format", "rtf", "application/rtf", "oowriter", "openoffice"));
        list.add(new ExternalFileType("PNG image", "png", "image/png", "gimp", "picture"));
        list.add(new ExternalFileType("GIF image", "gif", "image/gif", "gimp", "picture"));
        list.add(new ExternalFileType("JPG image", "jpg", "image/jpeg", "gimp", "picture"));
        list.add(new ExternalFileType("Djvu", "djvu", "", "evince", "psSmall"));
        list.add(new ExternalFileType("Text", "txt", "text/plain", "emacs", "emacs"));
        list.add(new ExternalFileType("LaTeX", "tex", "", "emacs", "emacs"));
        list.add(new ExternalFileType("CHM", "chm", "", "gnochm", "www"));
        list.add(new ExternalFileType("TIFF image", "tiff", "image/tiff", "gimp", "picture"));
        ExternalFileType tp = new ExternalFileType("URL", "html", "text/html", "firefox", "www");
        list.add(tp);

        // On all OSes there is a generic application available to handle file opening,
        // so we don't need the default application settings anymore:
        for (Iterator<ExternalFileType> iterator = list.iterator(); iterator.hasNext();) {
            ExternalFileType type = iterator.next();
            type.setOpenWith("");
        }
        

        return list;
    }

    public ExternalFileType[] getExternalFileTypeSelection() {
        return externalFileTypes.toArray
                (new ExternalFileType[externalFileTypes.size()]);
    }

    /**
     * Look up the external file type registered with this name, if any.
     * @param name The file type name.
     * @return The ExternalFileType registered, or null if none.
     */
    public ExternalFileType getExternalFileTypeByName(String name) {
        for (Iterator<ExternalFileType> iterator = externalFileTypes.iterator(); iterator.hasNext();) {
            ExternalFileType type = iterator.next();
            if (type.getName().equals(name))
                return type;
        }
        // Return an instance that signifies an unknown file type:
        return new UnknownExternalFileType(name);
    }

    /**
     * Look up the external file type registered for this extension, if any.
     * @param extension The file extension.
     * @return The ExternalFileType registered, or null if none.
     */
    public ExternalFileType getExternalFileTypeByExt(String extension) {
        for (Iterator<ExternalFileType> iterator = externalFileTypes.iterator(); iterator.hasNext();) {
            ExternalFileType type = iterator.next();
            if ((type.getExtension() != null) && type.getExtension().equalsIgnoreCase(extension))
                return type;
        }
        return null;
    }

    /**
     * Look up the external file type registered for this MIME type, if any.
     * @param mimeType The MIME type.
     * @return The ExternalFileType registered, or null if none.
     */
    public ExternalFileType getExternalFileTypeByMimeType(String mimeType) {
        for (Iterator<ExternalFileType> iterator = externalFileTypes.iterator(); iterator.hasNext();) {
            ExternalFileType type = iterator.next();
            if ((type.getMimeType() != null) && type.getMimeType().equals(mimeType))
                return type;
        }
        return null;
    }

    /**
     * Reset the List of external file types after user customization.
     * @param types The new List of external file types. This is the complete list, not
     *  just new entries.
     */
    public void setExternalFileTypes(List<ExternalFileType> types) {

        // First find a list of the default types:
        List<ExternalFileType> defTypes = getDefaultExternalFileTypes();
        // Make a list of types that are unchanged:
        List<ExternalFileType> unchanged = new ArrayList<ExternalFileType>();

        externalFileTypes.clear();
        for (Iterator<ExternalFileType> iterator = types.iterator(); iterator.hasNext();) {
            ExternalFileType type = iterator.next();
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
                if (found.equals(type))
                    unchanged.add(type);
                else {
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
        String[][] array = new String[types.size()+defTypes.size()][];
        int i=0;
        for (ExternalFileType type : types) {
            array[i] = type.getStringArrayRepresentation();
            i++;
        }
        for (ExternalFileType type : defTypes) {
            array[i] = new String[] {type.getName(), FILE_TYPE_REMOVED_FLAG};
            i++;
        }
        //System.out.println("Encoded: '"+Util.encodeStringArray(array)+"'");
        put("externalFileTypes", Util.encodeStringArray(array));
    }

    
    /**
     * Set up the list of external file types, either from default values, or from values
     * recorded in Preferences.
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
        for (int i = 0; i < vals.length; i++) {
            if ((vals[i].length == 2) && (vals[i][1].equals(FILE_TYPE_REMOVED_FLAG))) {
                // This entry indicates that a default entry type should be removed:
                ExternalFileType toRemove = null;
                for (ExternalFileType type : types) {
                    if (type.getName().equals(vals[i][0])) {
                        toRemove = type;
                        break;
                    }
                }
                // If we found it, remove it from the type list:
                if (toRemove != null)
                    types.remove(toRemove);
            }
            else {
                // A new or modified entry type. Construct it from the string array:
                ExternalFileType type = new ExternalFileType(vals[i]);
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
     * @param number or higher.
     */
    public void purgeCustomEntryTypes(int number) {
    purgeSeries(CUSTOM_TYPE_NAME, number);
    purgeSeries(CUSTOM_TYPE_REQ, number);
    purgeSeries(CUSTOM_TYPE_OPT, number);

        /*while (get(CUSTOM_TYPE_NAME+number) != null) {
            remove(CUSTOM_TYPE_NAME+number);
            remove(CUSTOM_TYPE_REQ+number);
            remove(CUSTOM_TYPE_OPT+number);
            number++;
            }*/
    }

    /**
     * Removes all entries keyed by prefix+number, where number
     * is equal to or higher than the given number.
     * @param number or higher.
     */
    public void purgeSeries(String prefix, int number) {
        while (get(prefix+number) != null) {
            remove(prefix+number);
            number++;
        }
    }

    public EntryEditorTabList getEntryEditorTabList() {
    if (tabList == null)
        updateEntryEditorTabList();
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
     * Determines whether the given field should be written without any sort of wrapping.
     * @param fieldName The field name.
     * @return true if the field should not be wrapped.
     */
    public boolean isNonWrappableField(String fieldName) {
        return nonWrappableFields.contains(fieldName);
    }
}
