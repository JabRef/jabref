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
import java.awt.Toolkit;
import java.awt.event.InputEvent;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.swing.*;

import net.sf.jabref.gui.*;
import net.sf.jabref.gui.actions.CleanUpAction;
import net.sf.jabref.gui.entryeditor.EntryEditorTabList;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelPattern.GlobalLabelPattern;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.CustomEntryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.exporter.CustomExportList;
import net.sf.jabref.exporter.ExportComparator;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.importer.CustomImportList;
import net.sf.jabref.logic.remote.RemotePreferences;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import net.sf.jabref.logic.util.strings.StringUtil;

public class JabRefPreferences {
    private static final Log LOGGER = LogFactory.getLog(JabRefPreferences.class);

    /**
     * HashMap that contains all preferences which are set by default
     */
    public final HashMap<String, Object> defaults = new HashMap<>();

    /* contents of the defaults HashMap that are defined in this class.
     * There are more default parameters in this map which belong to separate preference classes.
    */
    public static final String EMACS_PATH = "emacsPath";
    public static final String EMACS_ADDITIONAL_PARAMETERS = "emacsParameters";
    public static final String EMACS_23 = "emacsUseV23InsertString";
    public static final String FONT_FAMILY = "fontFamily";
    public static final String WIN_LOOK_AND_FEEL = "lookAndFeel";
    public static final String LATEX_EDITOR_PATH = "latexEditorPath";
    public static final String TEXSTUDIO_PATH = "TeXstudioPath";
    public static final String WIN_EDT_PATH = "winEdtPath";
    public static final String TEXMAKER_PATH = "texmakerPath";
    public static final String LANGUAGE = "language";
    public static final String NAMES_LAST_ONLY = "namesLastOnly";
    public static final String ABBR_AUTHOR_NAMES = "abbrAuthorNames";
    public static final String NAMES_NATBIB = "namesNatbib";
    public static final String NAMES_FIRST_LAST = "namesFf";
    public static final String NAMES_AS_IS = "namesAsIs";
    public static final String TABLE_COLOR_CODES_ON = "tableColorCodesOn";
    public static final String ENTRY_EDITOR_HEIGHT = "entryEditorHeight";
    public static final String PREVIEW_PANEL_HEIGHT = "previewPanelHeight";
    public static final String AUTO_RESIZE_MODE = "autoResizeMode";
    public static final String WINDOW_MAXIMISED = "windowMaximised";
    public static final String SIZE_Y = "mainWindowSizeY";
    public static final String SIZE_X = "mainWindowSizeX";
    public static final String POS_Y = "mainWindowPosY";
    public static final String POS_X = "mainWindowPosX";
    public static final String VIM_SERVER = "vimServer";
    public static final String VIM = "vim";
    public static final String LYXPIPE = "lyxpipe";
    public static final String USE_DEFAULT_LOOK_AND_FEEL = "useDefaultLookAndFeel";
    public static final String PROXY_PORT = "proxyPort";
    public static final String PROXY_HOSTNAME = "proxyHostname";
    public static final String USE_PROXY = "useProxy";
    public static final String PROXY_USERNAME = "proxyUsername";
    public static final String PROXY_PASSWORD = "proxyPassword";
    public static final String USE_PROXY_AUTHENTICATION = "useProxyAuthentication";
    public static final String TABLE_PRIMARY_SORT_FIELD = "priSort";
    public static final String TABLE_PRIMARY_SORT_DESCENDING = "priDescending";
    public static final String TABLE_SECONDARY_SORT_FIELD = "secSort";
    public static final String TABLE_SECONDARY_SORT_DESCENDING = "secDescending";
    public static final String TABLE_TERTIARY_SORT_FIELD = "terSort";
    public static final String TABLE_TERTIARY_SORT_DESCENDING = "terDescending";
    public static final String SAVE_IN_ORIGINAL_ORDER = "saveInOriginalOrder";
    public static final String SAVE_IN_SPECIFIED_ORDER = "saveInSpecifiedOrder";
    public static final String SAVE_PRIMARY_SORT_FIELD = "savePriSort";
    public static final String SAVE_PRIMARY_SORT_DESCENDING = "savePriDescending";
    public static final String SAVE_SECONDARY_SORT_FIELD = "saveSecSort";
    public static final String SAVE_SECONDARY_SORT_DESCENDING = "saveSecDescending";
    public static final String SAVE_TERTIARY_SORT_FIELD = "saveTerSort";
    public static final String SAVE_TERTIARY_SORT_DESCENDING = "saveTerDescending";
    public static final String EXPORT_IN_ORIGINAL_ORDER = "exportInOriginalOrder";
    public static final String EXPORT_IN_SPECIFIED_ORDER = "exportInSpecifiedOrder";
    public static final String EXPORT_PRIMARY_SORT_FIELD = "exportPriSort";
    public static final String EXPORT_PRIMARY_SORT_DESCENDING = "exportPriDescending";
    public static final String EXPORT_SECONDARY_SORT_FIELD = "exportSecSort";
    public static final String EXPORT_SECONDARY_SORT_DESCENDING = "exportSecDescending";
    public static final String EXPORT_TERTIARY_SORT_FIELD = "exportTerSort";
    public static final String EXPORT_TERTIARY_SORT_DESCENDING = "exportTerDescending";
    public static final String NEWLINE = "newline";
    public static final String COLUMN_WIDTHS = "columnWidths";
    public static final String COLUMN_NAMES = "columnNames";
    public static final String SIDE_PANE_COMPONENT_PREFERRED_POSITIONS = "sidePaneComponentPreferredPositions";
    public static final String SIDE_PANE_COMPONENT_NAMES = "sidePaneComponentNames";
    public static final String XMP_PRIVACY_FILTERS = "xmpPrivacyFilters";
    public static final String USE_XMP_PRIVACY_FILTER = "useXmpPrivacyFilter";
    public static final String SEARCH_MODE_FILTER = "searchModeFilter";
    public static final String SEARCH_CASE_SENSITIVE = "caseSensitiveSearch";
    public static final String DEFAULT_AUTO_SORT = "defaultAutoSort";
    public static final String DEFAULT_SHOW_SOURCE = "defaultShowSource";
    public static final String STRINGS_SIZE_Y = "stringsSizeY";
    public static final String STRINGS_SIZE_X = "stringsSizeX";
    public static final String STRINGS_POS_Y = "stringsPosY";
    public static final String STRINGS_POS_X = "stringsPosX";
    public static final String DUPLICATES_SIZE_Y = "duplicatesSizeY";
    public static final String DUPLICATES_SIZE_X = "duplicatesSizeX";
    public static final String DUPLICATES_POS_Y = "duplicatesPosY";
    public static final String DUPLICATES_POS_X = "duplicatesPosX";
    public static final String MERGEENTRIES_SIZE_Y = "mergeEntriesSizeY";
    public static final String MERGEENTRIES_SIZE_X = "mergeEntriesSizeX";
    public static final String MERGEENTRIES_POS_Y = "mergeEntriesPosY";
    public static final String MERGEENTRIES_POS_X = "mergeEntriesPosX";
    public static final String LAST_EDITED = "lastEdited";
    public static final String OPEN_LAST_EDITED = "openLastEdited";
    public static final String BACKUP = "backup";
    public static final String ENTRY_TYPE_FORM_WIDTH = "entryTypeFormWidth";
    public static final String ENTRY_TYPE_FORM_HEIGHT_FACTOR = "entryTypeFormHeightFactor";
    public static final String AUTO_OPEN_FORM = "autoOpenForm";
    public static final String FILE_WORKING_DIRECTORY = "fileWorkingDirectory";
    public static final String IMPORT_WORKING_DIRECTORY = "importWorkingDirectory";
    public static final String EXPORT_WORKING_DIRECTORY = "exportWorkingDirectory";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String NUMBER_COL_WIDTH = "numberColWidth";
    public static final String SHORTEST_TO_COMPLETE = "shortestToComplete";
    public static final String AUTOCOMPLETE_FIRSTNAME_MODE = "autoCompFirstNameMode";
    public static final String AUTOCOMPLETE_FIRSTNAME_MODE_BOTH = "both"; // here are the possible values for _MODE:
    public static final String AUTO_COMP_LAST_FIRST = "autoCompLF";
    public static final String AUTO_COMP_FIRST_LAST = "autoCompFF";
    public static final String AUTO_COMPLETE_FIELDS = "autoCompleteFields";
    public static final String AUTO_COMPLETE = "autoComplete";
    public static final String SEARCH_PANE_POS_Y = "searchPanePosY";
    public static final String SEARCH_PANE_POS_X = "searchPanePosX";
    public static final String SEARCH_REG_EXP = "regExpSearch";
    public static final String EDITOR_EMACS_KEYBINDINGS = "editorEMACSkeyBindings";
    public static final String EDITOR_EMACS_KEYBINDINGS_REBIND_CA = "editorEMACSkeyBindingsRebindCA";
    public static final String EDITOR_EMACS_KEYBINDINGS_REBIND_CF = "editorEMACSkeyBindingsRebindCF";
    public static final String GROUP_SHOW_NUMBER_OF_ELEMENTS = "groupShowNumberOfElements";
    public static final String GROUP_AUTO_HIDE = "groupAutoHide";
    public static final String GROUP_AUTO_SHOW = "groupAutoShow";
    public static final String GROUP_EXPAND_TREE = "groupExpandTree";
    public static final String GROUP_SHOW_DYNAMIC = "groupShowDynamic";
    public static final String GROUP_SHOW_ICONS = "groupShowIcons";
    public static final String GROUPS_DEFAULT_FIELD = "groupsDefaultField";
    public static final String GROUP_SELECT_MATCHES = "groupSelectMatches";
    public static final String GROUP_SHOW_OVERLAPPING = "groupShowOverlapping";
    public static final String GROUP_INVERT_SELECTIONS = "groupInvertSelections";
    public static final String GROUP_INTERSECT_SELECTIONS = "groupIntersectSelections";
    public static final String GROUP_FLOAT_SELECTIONS = "groupFloatSelections";
    public static final String EDIT_GROUP_MEMBERSHIP_MODE = "groupEditGroupMembershipMode";
    public static final String GROUP_KEYWORD_SEPARATOR = "groupKeywordSeparator";
    public static final String AUTO_ASSIGN_GROUP = "autoAssignGroup";
    public static final String LIST_OF_FILE_COLUMNS = "listOfFileColumns";
    public static final String EXTRA_FILE_COLUMNS = "extraFileColumns";
    public static final String ARXIV_COLUMN = "arxivColumn";
    public static final String FILE_COLUMN = "fileColumn";
    public static final String PREFER_URL_DOI = "preferUrlDoi";
    public static final String URL_COLUMN = "urlColumn";
    public static final String PDF_COLUMN = "pdfColumn";
    public static final String DISABLE_ON_MULTIPLE_SELECTION = "disableOnMultipleSelection";
    public static final String CTRL_CLICK = "ctrlClick";
    public static final String INCOMPLETE_ENTRY_BACKGROUND = "incompleteEntryBackground";
    public static final String FIELD_EDITOR_TEXT_COLOR = "fieldEditorTextColor";
    public static final String ACTIVE_FIELD_EDITOR_BACKGROUND_COLOR = "activeFieldEditorBackgroundColor";
    public static final String INVALID_FIELD_BACKGROUND_COLOR = "invalidFieldBackgroundColor";
    public static final String VALID_FIELD_BACKGROUND_COLOR = "validFieldBackgroundColor";
    public static final String MARKED_ENTRY_BACKGROUND5 = "markedEntryBackground5";
    public static final String MARKED_ENTRY_BACKGROUND4 = "markedEntryBackground4";
    public static final String MARKED_ENTRY_BACKGROUND3 = "markedEntryBackground3";
    public static final String MARKED_ENTRY_BACKGROUND2 = "markedEntryBackground2";
    public static final String MARKED_ENTRY_BACKGROUND1 = "markedEntryBackground1";
    public static final String MARKED_ENTRY_BACKGROUND0 = "markedEntryBackground0";
    public static final String VERY_GRAYED_OUT_TEXT = "veryGrayedOutText";
    public static final String VERY_GRAYED_OUT_BACKGROUND = "veryGrayedOutBackground";
    public static final String GRAYED_OUT_TEXT = "grayedOutText";
    public static final String GRAYED_OUT_BACKGROUND = "grayedOutBackground";
    public static final String GRID_COLOR = "gridColor";
    public static final String TABLE_TEXT = "tableText";
    public static final String TABLE_OPT_FIELD_BACKGROUND = "tableOptFieldBackground";
    public static final String TABLE_REQ_FIELD_BACKGROUND = "tableReqFieldBackground";
    public static final String TABLE_BACKGROUND = "tableBackground";
    public static final String TABLE_SHOW_GRID = "tableShowGrid";
    public static final String TABLE_ROW_PADDING = "tableRowPadding";
    public static final String MENU_FONT_SIZE = "menuFontSize";
    public static final String OVERRIDE_DEFAULT_FONTS = "overrideDefaultFonts";
    public static final String FONT_SIZE = "fontSize";
    public static final String FONT_STYLE = "fontStyle";
    public static final String RECENT_FILES = "recentFiles";
    public static final String GENERAL_FIELDS = "generalFields";
    public static final String RENAME_ON_MOVE_FILE_TO_FILE_DIR = "renameOnMoveFileToFileDir";
    public static final String MEMORY_STICK_MODE = "memoryStickMode";
    public static final String DEFAULT_OWNER = "defaultOwner";
    public static final String GROUPS_VISIBLE_ROWS = "groupsVisibleRows";
    public static final String DEFAULT_ENCODING = "defaultEncoding";
    public static final String TOOLBAR_VISIBLE = "toolbarVisible";
    public static final String HIGHLIGHT_GROUPS_MATCHING_ALL = "highlightGroupsMatchingAll";
    public static final String HIGHLIGHT_GROUPS_MATCHING_ANY = "highlightGroupsMatchingAny";
    public static final String SHOW_ONE_LETTER_HEADING_FOR_ICON_COLUMNS = "showOneLetterHeadingForIconColumns";
    public static final String UPDATE_TIMESTAMP = "updateTimestamp";
    public static final String TIME_STAMP_FIELD = "timeStampField";
    public static final String TIME_STAMP_FORMAT = "timeStampFormat";
    public static final String OVERWRITE_TIME_STAMP = "overwriteTimeStamp";
    public static final String USE_TIME_STAMP = "useTimeStamp";
    public static final String WARN_ABOUT_DUPLICATES_IN_INSPECTION = "warnAboutDuplicatesInInspection";
    public static final String UNMARK_ALL_ENTRIES_BEFORE_IMPORTING = "unmarkAllEntriesBeforeImporting";
    public static final String MARK_IMPORTED_ENTRIES = "markImportedEntries";
    public static final String GENERATE_KEYS_AFTER_INSPECTION = "generateKeysAfterInspection";
    public static final String NON_WRAPPABLE_FIELDS = "nonWrappableFields";
    public static final String PUT_BRACES_AROUND_CAPITALS = "putBracesAroundCapitals";
    public static final String RESOLVE_STRINGS_ALL_FIELDS = "resolveStringsAllFields";
    public static final String DO_NOT_RESOLVE_STRINGS_FOR = "doNotResolveStringsFor";
    public static final String AUTO_DOUBLE_BRACES = "autoDoubleBraces";
    public static final String PREVIEW_PRINT_BUTTON = "previewPrintButton";
    public static final String PREVIEW_1 = "preview1";
    public static final String PREVIEW_0 = "preview0";
    public static final String ACTIVE_PREVIEW = "activePreview";
    public static final String PREVIEW_ENABLED = "previewEnabled";

    // Currently, it is not possible to specify defaults for specific entry types
    // When this should be made possible, the code to inspect is net.sf.jabref.gui.preftabs.LabelPatternPrefTab.storeSettings() -> LabelPattern keypatterns = getLabelPattern(); etc
    public static final String DEFAULT_LABEL_PATTERN = "defaultLabelPattern";

    public static final String SEARCH_MODE_FLOAT = "floatSearch";
    public static final String GRAY_OUT_NON_HITS = "grayOutNonHits";
    public static final String CONFIRM_DELETE = "confirmDelete";
    public static final String WARN_BEFORE_OVERWRITING_KEY = "warnBeforeOverwritingKey";
    public static final String AVOID_OVERWRITING_KEY = "avoidOverwritingKey";
    public static final String DISPLAY_KEY_WARNING_DIALOG_AT_STARTUP = "displayKeyWarningDialogAtStartup";
    public static final String DIALOG_WARNING_FOR_EMPTY_KEY = "dialogWarningForEmptyKey";
    public static final String DIALOG_WARNING_FOR_DUPLICATE_KEY = "dialogWarningForDuplicateKey";
    public static final String ALLOW_TABLE_EDITING = "allowTableEditing";
    public static final String OVERWRITE_OWNER = "overwriteOwner";
    public static final String USE_OWNER = "useOwner";
    public static final String WRITEFIELD_ADDSPACES = "writeFieldAddSpaces";
    public static final String WRITEFIELD_CAMELCASENAME = "writeFieldCamelCase";
    public static final String WRITEFIELD_SORTSTYLE = "writefieldSortStyle";
    public static final String WRITEFIELD_USERDEFINEDORDER = "writefieldUserdefinedOrder";
    public static final String WRITEFIELD_WRAPFIELD = "wrapFieldLine";
    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";
    public static final String SHOW_FILE_LINKS_UPGRADE_WARNING = "showFileLinksUpgradeWarning";
    public static final String SEARCH_DIALOG_HEIGHT = "searchDialogHeight";
    public static final String SEARCH_DIALOG_WIDTH = "searchDialogWidth";
    public static final String IMPORT_INSPECTION_DIALOG_HEIGHT = "importInspectionDialogHeight";
    public static final String IMPORT_INSPECTION_DIALOG_WIDTH = "importInspectionDialogWidth";
    public static final String SIDE_PANE_WIDTH = "sidePaneWidth";
    public static final String LAST_USED_EXPORT = "lastUsedExport";
    public static final String FLOAT_MARKED_ENTRIES = "floatMarkedEntries";
    public static final String CITE_COMMAND = "citeCommand";
    public static final String EXTERNAL_JOURNAL_LISTS = "externalJournalLists";
    public static final String PERSONAL_JOURNAL_LIST = "personalJournalList";
    public static final String GENERATE_KEYS_BEFORE_SAVING = "generateKeysBeforeSaving";
    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";
    public static final String KEY_GEN_ALWAYS_ADD_LETTER = "keyGenAlwaysAddLetter";
    public static final String KEY_GEN_FIRST_LETTER_A = "keyGenFirstLetterA";
    public static final String INCLUDE_EMPTY_FIELDS = "includeEmptyFields";
    public static final String VALUE_DELIMITERS2 = "valueDelimiters";
    public static final String BIBLATEX_MODE = "biblatexMode";
    public static final String ENFORCE_LEGAL_BIBTEX_KEY = "enforceLegalBibtexKey";
    public static final String PROMPT_BEFORE_USING_AUTOSAVE = "promptBeforeUsingAutosave";
    public static final String AUTO_SAVE_INTERVAL = "autoSaveInterval";
    public static final String AUTO_SAVE = "autoSave";
    public static final String USE_LOCK_FILES = "useLockFiles";
    public static final String RUN_AUTOMATIC_FILE_SEARCH = "runAutomaticFileSearch";
    public static final String NUMERIC_FIELDS = "numericFields";
    public static final String DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY = "defaultRegExpSearchExpression";
    public static final String REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String AUTOLINK_USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    public static final String DB_CONNECT_USERNAME = "dbConnectUsername";
    public static final String DB_CONNECT_DATABASE = "dbConnectDatabase";
    public static final String DB_CONNECT_HOSTNAME = "dbConnectHostname";
    public static final String DB_CONNECT_SERVER_TYPE = "dbConnectServerType";
    public static final String BIB_LOC_AS_PRIMARY_DIR = "bibLocAsPrimaryDir";
    public static final String BIB_LOCATION_AS_FILE_DIR = "bibLocationAsFileDir";
    public static final String SELECTED_FETCHER_INDEX = "selectedFetcherIndex";
    public static final String WEB_SEARCH_VISIBLE = "webSearchVisible";
    public static final String ALLOW_FILE_AUTO_OPEN_BROWSE = "allowFileAutoOpenBrowse";
    public static final String CUSTOM_TAB_NAME = "customTabName_";
    public static final String CUSTOM_TAB_FIELDS = "customTabFields_";
    public static final String USER_FILE_DIR_INDIVIDUAL = "userFileDirIndividual";
    public static final String USER_FILE_DIR_IND_LEGACY = "userFileDirInd_Legacy";
    public static final String USER_FILE_DIR = "userFileDir";
    public static final String USE_UNIT_FORMATTER_ON_SEARCH = "useUnitFormatterOnSearch";
    public static final String USE_CASE_KEEPER_ON_SEARCH = "useCaseKeeperOnSearch";
    public static final String USE_CONVERT_TO_EQUATION = "useConvertToEquation";
    public static final String USE_IEEE_ABRV = "useIEEEAbrv";

    public static final String PUSH_TO_APPLICATION = "pushToApplication";

    //non-default preferences
    private static final String CUSTOM_TYPE_NAME = "customTypeName_";
    private static final String CUSTOM_TYPE_REQ = "customTypeReq_";
    private static final String CUSTOM_TYPE_OPT = "customTypeOpt_";
    private static final String CUSTOM_TYPE_PRIOPT = "customTypePriOpt_";
    public static final String PDF_PREVIEW = "pdfPreview";
    public static final String AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL = "fullOnly";
    public static final String AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR = "abbrOnly";

    // This String is used in the encoded list in prefs of external file type
    // modifications, in order to indicate a removed default file type:
    private static final String FILE_TYPE_REMOVED_FLAG = "REMOVED";

    private static final char[][] VALUE_DELIMITERS = new char[][] { {'"', '"'}, {'{', '}'}};

    public String WRAPPED_USERNAME;
    public final String MARKING_WITH_NUMBER_PATTERN;

    private int SHORTCUT_MASK = -1;

    private final Preferences prefs;

    private KeyBinds keyBinds = new KeyBinds();
    private KeyBinds defaultKeyBinds = new KeyBinds();

    private final HashSet<String> putBracesAroundCapitalsFields = new HashSet<>(4);
    private final HashSet<String> nonWrappableFields = new HashSet<>(5);
    private static GlobalLabelPattern keyPattern;

    // Object containing custom export formats:
    public final CustomExportList customExports;

    /**
     * Set with all custom {@link ImportFormat}s
     */
    public final CustomImportList customImports;

    // Object containing info about customized entry editor tabs.
    private EntryEditorTabList tabList;
    // Map containing all registered external file types:
    private final TreeSet<ExternalFileType> externalFileTypes = new TreeSet<>();

    private final ExternalFileType HTML_FALLBACK_TYPE = new ExternalFileType("URL", "html", "text/html", "", "www", IconTheme.JabRefIcon.WWW.getSmallIcon());

    // The following field is used as a global variable during the export of a database.
    // By setting this field to the path of the database's default file directory, formatters
    // that should resolve external file paths can access this field. This is an ugly hack
    // to solve the problem of formatters not having access to any context except for the
    // string to be formatted and possible formatter arguments.
    public String[] fileDirForDatabase;

    // Similarly to the previous variable, this is a global that can be used during
    // the export of a database if the database filename should be output. If a database
    // is tied to a file on disk, this variable is set to that file before export starts:
    public File databaseFile;

    // The following field is used as a global variable during the export of a database.
    // It is used to hold custom name formatters defined by a custom export filter.
    // It is set before the export starts:
    public HashMap<String, String> customExportNameFormatters;

    // The only instance of this class:
    private static JabRefPreferences singleton;


    public static JabRefPreferences getInstance() {
        if (JabRefPreferences.singleton == null) {
            JabRefPreferences.singleton = new JabRefPreferences();
        }
        return JabRefPreferences.singleton;
    }





    // The constructor is made private to enforce this as a singleton class:
    private JabRefPreferences() {

        try {
            if (new File("jabref.xml").exists()) {
                importPreferences("jabref.xml");
            }
        } catch (JabRefException e) {
            LOGGER.warn("Could not import preferences from jabref.xml: " + e.getMessage(), e);
        }

        // load user preferences
        prefs = Preferences.userNodeForPackage(JabRef.class);

        defaults.put(TEXMAKER_PATH, OS.guessProgramPath("texmaker", "Texmaker"));
        defaults.put(WIN_EDT_PATH, OS.guessProgramPath("WinEdt", "WinEdt Team\\WinEdt"));
        defaults.put(LATEX_EDITOR_PATH, OS.guessProgramPath("LEd", "LEd"));
        defaults.put(TEXSTUDIO_PATH, OS.guessProgramPath("texstudio", "TeXstudio"));

        if (OS.OS_X) {
            //defaults.put("pdfviewer", "/Applications/Preview.app");
            //defaults.put("psviewer", "/Applications/Preview.app");
            //defaults.put("htmlviewer", "/Applications/Safari.app");
            defaults.put(EMACS_PATH, "emacsclient");
            defaults.put(EMACS_23, true);
            defaults.put(EMACS_ADDITIONAL_PARAMETERS, "-n -e");
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(WIN_LOOK_AND_FEEL, UIManager.getSystemLookAndFeelClassName());

        } else if (OS.WINDOWS) {
            //defaults.put("pdfviewer", "cmd.exe /c start /b");
            //defaults.put("psviewer", "cmd.exe /c start /b");
            //defaults.put("htmlviewer", "cmd.exe /c start /b");
            defaults.put(WIN_LOOK_AND_FEEL, "com.jgoodies.looks.windows.WindowsLookAndFeel");
            defaults.put(EMACS_PATH, "emacsclient.exe");
            defaults.put(EMACS_23, true);
            defaults.put(EMACS_ADDITIONAL_PARAMETERS, "-n -e");
            defaults.put(FONT_FAMILY, "Arial");

        } else {
            //defaults.put("pdfviewer", "evince");
            //defaults.put("psviewer", "gv");
            //defaults.put("htmlviewer", "firefox");
            defaults.put(WIN_LOOK_AND_FEEL, "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel");
            defaults.put(FONT_FAMILY, "SansSerif");

            // linux
            defaults.put(EMACS_PATH, "gnuclient");
            defaults.put(EMACS_23, false);
            defaults.put(EMACS_ADDITIONAL_PARAMETERS, "-batch -eval");

        }
        defaults.put(PUSH_TO_APPLICATION, "TeXstudio");
        defaults.put(USE_PROXY, Boolean.FALSE);
        defaults.put(PROXY_HOSTNAME, "");
        defaults.put(PROXY_PORT, "80");
        defaults.put(USE_PROXY_AUTHENTICATION, Boolean.FALSE);
        defaults.put(PROXY_USERNAME, "");
        defaults.put(PROXY_PASSWORD, "");
        defaults.put(PDF_PREVIEW, Boolean.FALSE);
        defaults.put(USE_DEFAULT_LOOK_AND_FEEL, Boolean.TRUE);
        defaults.put(LYXPIPE, System.getProperty("user.home") + File.separator + ".lyx/lyxpipe");
        defaults.put(VIM, "vim");
        defaults.put(VIM_SERVER, "vim");
        defaults.put(POS_X, 0);
        defaults.put(POS_Y, 0);
        defaults.put(SIZE_X, 840);
        defaults.put(SIZE_Y, 680);
        defaults.put(WINDOW_MAXIMISED, Boolean.FALSE);
        defaults.put(AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_ALL_COLUMNS);
        defaults.put(PREVIEW_PANEL_HEIGHT, 200);
        defaults.put(ENTRY_EDITOR_HEIGHT, 400);
        defaults.put(TABLE_COLOR_CODES_ON, Boolean.FALSE);
        defaults.put(NAMES_AS_IS, Boolean.FALSE); // "Show names unchanged"
        defaults.put(NAMES_FIRST_LAST, Boolean.FALSE); // "Show 'Firstname Lastname'"
        defaults.put(NAMES_NATBIB, Boolean.TRUE); // "Natbib style"
        defaults.put(ABBR_AUTHOR_NAMES, Boolean.TRUE); // "Abbreviate names"
        defaults.put(NAMES_LAST_ONLY, Boolean.TRUE); // "Show last names only"
        // system locale as default
        defaults.put(LANGUAGE, Locale.getDefault().getLanguage());

        // Sorting preferences
        defaults.put(TABLE_PRIMARY_SORT_FIELD, "author");
        defaults.put(TABLE_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(TABLE_SECONDARY_SORT_FIELD, "year");
        defaults.put(TABLE_SECONDARY_SORT_DESCENDING, Boolean.TRUE);
        defaults.put(TABLE_TERTIARY_SORT_FIELD, "title");
        defaults.put(TABLE_TERTIARY_SORT_DESCENDING, Boolean.FALSE);

        // if both are false, then the entries are saved in table order
        defaults.put(SAVE_IN_ORIGINAL_ORDER, Boolean.FALSE);
        defaults.put(SAVE_IN_SPECIFIED_ORDER, Boolean.TRUE);

        // save order: if SAVE_IN_SPECIFIED_ORDER, then use following criteria
        defaults.put(SAVE_PRIMARY_SORT_FIELD, "bibtexkey");
        defaults.put(SAVE_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(SAVE_SECONDARY_SORT_FIELD, "author");
        defaults.put(SAVE_SECONDARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(SAVE_TERTIARY_SORT_FIELD, "title");
        defaults.put(SAVE_TERTIARY_SORT_DESCENDING, Boolean.FALSE);

        // export order
        defaults.put(EXPORT_IN_ORIGINAL_ORDER, Boolean.FALSE);
        defaults.put(EXPORT_IN_SPECIFIED_ORDER, Boolean.FALSE);

        // export order: if EXPORT_IN_SPECIFIED_ORDER, then use following criteria
        defaults.put(EXPORT_PRIMARY_SORT_FIELD, "bibtexkey");
        defaults.put(EXPORT_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(EXPORT_SECONDARY_SORT_FIELD, "author");
        defaults.put(EXPORT_SECONDARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(EXPORT_TERTIARY_SORT_FIELD, "title");
        defaults.put(EXPORT_TERTIARY_SORT_DESCENDING, Boolean.TRUE);

        defaults.put(NEWLINE, System.lineSeparator());

        defaults.put(SIDE_PANE_COMPONENT_NAMES, "");
        defaults.put(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, "");

        defaults.put(COLUMN_NAMES, "entrytype;author;title;year;journal;bibtexkey");
        defaults.put(COLUMN_WIDTHS, "75;300;470;60;130;100");
        defaults.put(PersistenceTableColumnListener.ACTIVATE_PREF_KEY,
                PersistenceTableColumnListener.DEFAULT_ENABLED);
        defaults.put(XMP_PRIVACY_FILTERS, "pdf;timestamp;keywords;owner;note;review");
        defaults.put(USE_XMP_PRIVACY_FILTER, Boolean.FALSE);
        defaults.put(NUMBER_COL_WIDTH, GUIGlobals.NUMBER_COL_LENGTH);
        defaults.put(WORKING_DIRECTORY, System.getProperty("user.home"));
        defaults.put(EXPORT_WORKING_DIRECTORY, System.getProperty("user.home"));
        defaults.put(IMPORT_WORKING_DIRECTORY, System.getProperty("user.home"));
        defaults.put(FILE_WORKING_DIRECTORY, System.getProperty("user.home"));
        defaults.put(AUTO_OPEN_FORM, Boolean.TRUE);
        defaults.put(ENTRY_TYPE_FORM_HEIGHT_FACTOR, 1);
        defaults.put(ENTRY_TYPE_FORM_WIDTH, 1);
        defaults.put(BACKUP, Boolean.TRUE);
        defaults.put(OPEN_LAST_EDITED, Boolean.TRUE);
        defaults.put(LAST_EDITED, null);
        defaults.put(STRINGS_POS_X, 0);
        defaults.put(STRINGS_POS_Y, 0);
        defaults.put(STRINGS_SIZE_X, 600);
        defaults.put(STRINGS_SIZE_Y, 400);
        defaults.put(DUPLICATES_POS_X, 0);
        defaults.put(DUPLICATES_POS_Y, 0);
        defaults.put(DUPLICATES_SIZE_X, 800);
        defaults.put(DUPLICATES_SIZE_Y, 600);
        defaults.put(MERGEENTRIES_POS_X, 0);
        defaults.put(MERGEENTRIES_POS_Y, 0);
        defaults.put(MERGEENTRIES_SIZE_X, 800);
        defaults.put(MERGEENTRIES_SIZE_Y, 600);
        defaults.put(DEFAULT_SHOW_SOURCE, Boolean.FALSE);
        defaults.put(DEFAULT_AUTO_SORT, Boolean.FALSE);
        defaults.put(SEARCH_CASE_SENSITIVE, Boolean.FALSE);
        defaults.put(SEARCH_MODE_FILTER, Boolean.TRUE);

        defaults.put(SEARCH_REG_EXP, Boolean.FALSE);
        defaults.put(SEARCH_PANE_POS_X, 0);
        defaults.put(SEARCH_PANE_POS_Y, 0);
        defaults.put(EDITOR_EMACS_KEYBINDINGS, Boolean.FALSE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CA, Boolean.TRUE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CF, Boolean.TRUE);
        defaults.put(AUTO_COMPLETE, Boolean.TRUE);
        defaults.put(AUTO_COMPLETE_FIELDS, "author;editor;title;journal;publisher;keywords;crossref");
        defaults.put(AUTO_COMP_FIRST_LAST, Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put(AUTO_COMP_LAST_FIRST, Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(SHORTEST_TO_COMPLETE, 1);
        defaults.put(AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH);
        defaults.put(GROUP_FLOAT_SELECTIONS, Boolean.TRUE);
        defaults.put(GROUP_INTERSECT_SELECTIONS, Boolean.TRUE);
        defaults.put(GROUP_INVERT_SELECTIONS, Boolean.FALSE);
        defaults.put(GROUP_SHOW_OVERLAPPING, Boolean.FALSE);
        defaults.put(GROUP_SELECT_MATCHES, Boolean.FALSE);
        defaults.put(GROUPS_DEFAULT_FIELD, "keywords");
        defaults.put(GROUP_SHOW_ICONS, Boolean.TRUE);
        defaults.put(GROUP_SHOW_DYNAMIC, Boolean.TRUE);
        defaults.put(GROUP_EXPAND_TREE, Boolean.TRUE);
        defaults.put(GROUP_AUTO_SHOW, Boolean.TRUE);
        defaults.put(GROUP_AUTO_HIDE, Boolean.TRUE);
        defaults.put(GROUP_SHOW_NUMBER_OF_ELEMENTS, Boolean.FALSE);
        defaults.put(AUTO_ASSIGN_GROUP, Boolean.TRUE);
        defaults.put(GROUP_KEYWORD_SEPARATOR, ", ");
        defaults.put(EDIT_GROUP_MEMBERSHIP_MODE, Boolean.FALSE);
        defaults.put(HIGHLIGHT_GROUPS_MATCHING_ANY, Boolean.FALSE);
        defaults.put(HIGHLIGHT_GROUPS_MATCHING_ALL, Boolean.FALSE);
        defaults.put(TOOLBAR_VISIBLE, Boolean.TRUE);
        defaults.put(DEFAULT_ENCODING, StandardCharsets.UTF_8.name());
        defaults.put(GROUPS_VISIBLE_ROWS, 8);
        defaults.put(DEFAULT_OWNER, System.getProperty("user.name"));
        defaults.put(MEMORY_STICK_MODE, Boolean.FALSE);
        defaults.put(RENAME_ON_MOVE_FILE_TO_FILE_DIR, Boolean.TRUE);

        // The general fields stuff is made obsolete by the CUSTOM_TAB_... entries.
        defaults.put(GENERAL_FIELDS, "crossref;keywords;file;doi;url;urldate;"
                + "pdf;comment;owner");

        defaults.put(FONT_STYLE, java.awt.Font.PLAIN);
        defaults.put(FONT_SIZE, 12);
        defaults.put(OVERRIDE_DEFAULT_FONTS, Boolean.FALSE);
        defaults.put(MENU_FONT_SIZE, 11);
        defaults.put(TABLE_ROW_PADDING, GUIGlobals.TABLE_ROW_PADDING);
        defaults.put(TABLE_SHOW_GRID, Boolean.FALSE);
        // Main table color settings:
        defaults.put(TABLE_BACKGROUND, "255:255:255");
        defaults.put(TABLE_REQ_FIELD_BACKGROUND, "230:235:255");
        defaults.put(TABLE_OPT_FIELD_BACKGROUND, "230:255:230");
        defaults.put(TABLE_TEXT, "0:0:0");
        defaults.put(GRID_COLOR, "210:210:210");
        defaults.put(GRAYED_OUT_BACKGROUND, "210:210:210");
        defaults.put(GRAYED_OUT_TEXT, "40:40:40");
        defaults.put(VERY_GRAYED_OUT_BACKGROUND, "180:180:180");
        defaults.put(VERY_GRAYED_OUT_TEXT, "40:40:40");
        defaults.put(MARKED_ENTRY_BACKGROUND0, "255:255:180");
        defaults.put(MARKED_ENTRY_BACKGROUND1, "255:220:180");
        defaults.put(MARKED_ENTRY_BACKGROUND2, "255:180:160");
        defaults.put(MARKED_ENTRY_BACKGROUND3, "255:120:120");
        defaults.put(MARKED_ENTRY_BACKGROUND4, "255:75:75");
        defaults.put(MARKED_ENTRY_BACKGROUND5, "220:255:220");
        defaults.put(VALID_FIELD_BACKGROUND_COLOR, "255:255:255");
        defaults.put(INVALID_FIELD_BACKGROUND_COLOR, "255:0:0");
        defaults.put(ACTIVE_FIELD_EDITOR_BACKGROUND_COLOR, "220:220:255");
        defaults.put(FIELD_EDITOR_TEXT_COLOR, "0:0:0");

        defaults.put(INCOMPLETE_ENTRY_BACKGROUND, "250:175:175");

        defaults.put(CTRL_CLICK, Boolean.FALSE);
        defaults.put(DISABLE_ON_MULTIPLE_SELECTION, Boolean.FALSE);
        defaults.put(PDF_COLUMN, Boolean.FALSE);
        defaults.put(URL_COLUMN, Boolean.TRUE);
        defaults.put(PREFER_URL_DOI, Boolean.FALSE);
        defaults.put(FILE_COLUMN, Boolean.TRUE);
        defaults.put(ARXIV_COLUMN, Boolean.FALSE);

        defaults.put(EXTRA_FILE_COLUMNS, Boolean.FALSE);
        defaults.put(LIST_OF_FILE_COLUMNS, "");

        defaults.put(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED, SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY, SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY, SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING, SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE, SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED, SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ, SpecialFieldsUtils.PREF_SHOWCOLUMN_READ_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS, SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS, SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS_DEFAULT);

        defaults.put(SHOW_ONE_LETTER_HEADING_FOR_ICON_COLUMNS, Boolean.FALSE);

        defaults.put(USE_OWNER, Boolean.FALSE);
        defaults.put(OVERWRITE_OWNER, Boolean.FALSE);
        defaults.put(ALLOW_TABLE_EDITING, Boolean.FALSE);
        defaults.put(DIALOG_WARNING_FOR_DUPLICATE_KEY, Boolean.TRUE);
        defaults.put(DIALOG_WARNING_FOR_EMPTY_KEY, Boolean.TRUE);
        defaults.put(DISPLAY_KEY_WARNING_DIALOG_AT_STARTUP, Boolean.TRUE);
        defaults.put(AVOID_OVERWRITING_KEY, Boolean.FALSE);
        defaults.put(WARN_BEFORE_OVERWRITING_KEY, Boolean.TRUE);
        defaults.put(CONFIRM_DELETE, Boolean.TRUE);
        defaults.put(GRAY_OUT_NON_HITS, Boolean.TRUE);
        defaults.put(SEARCH_MODE_FLOAT, Boolean.FALSE);
        defaults.put(DEFAULT_LABEL_PATTERN, "[authors3][year]");
        defaults.put(PREVIEW_ENABLED, Boolean.TRUE);
        defaults.put(ACTIVE_PREVIEW, 0);
        defaults.put(PREVIEW_0, "<font face=\"arial\">"
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
        defaults.put(PREVIEW_1, "<font face=\"arial\">"
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
        defaults.put(PREVIEW_PRINT_BUTTON, Boolean.FALSE);
        defaults.put(AUTO_DOUBLE_BRACES, Boolean.FALSE);
        defaults.put(DO_NOT_RESOLVE_STRINGS_FOR, "url");
        defaults.put(RESOLVE_STRINGS_ALL_FIELDS, Boolean.FALSE);
        defaults.put(PUT_BRACES_AROUND_CAPITALS, "");//"title;journal;booktitle;review;abstract");
        defaults.put(NON_WRAPPABLE_FIELDS, "pdf;ps;url;doi;file");
        defaults.put(GENERATE_KEYS_AFTER_INSPECTION, Boolean.TRUE);
        defaults.put(MARK_IMPORTED_ENTRIES, Boolean.TRUE);
        defaults.put(UNMARK_ALL_ENTRIES_BEFORE_IMPORTING, Boolean.TRUE);
        defaults.put(WARN_ABOUT_DUPLICATES_IN_INSPECTION, Boolean.TRUE);
        defaults.put(USE_TIME_STAMP, Boolean.FALSE);
        defaults.put(OVERWRITE_TIME_STAMP, Boolean.FALSE);

        // default time stamp follows ISO-8601. Reason: https://xkcd.com/1179/
        defaults.put(TIME_STAMP_FORMAT, "yyyy-MM-dd");

        defaults.put(TIME_STAMP_FIELD, BibtexFields.TIMESTAMP);
        defaults.put(UPDATE_TIMESTAMP, Boolean.FALSE);

        defaults.put(GENERATE_KEYS_BEFORE_SAVING, Boolean.FALSE);

        // behavior of JabRef before 2.10: both: false
        defaults.put(WRITEFIELD_ADDSPACES, Boolean.TRUE);
        defaults.put(WRITEFIELD_CAMELCASENAME, Boolean.TRUE);

        //behavior of JabRef before LWang_AdjustableFieldOrder 1
        //0 sorted order (2.10 default), 1 unsorted order (2.9.2 default), 2 user defined
        defaults.put(WRITEFIELD_SORTSTYLE, 0);
        defaults.put(WRITEFIELD_USERDEFINEDORDER, "author;title;journal;year;volume;number;pages;month;note;volume;pages;part;eid");
        defaults.put(WRITEFIELD_WRAPFIELD, Boolean.FALSE);

        defaults.put(RemotePreferences.USE_REMOTE_SERVER, Boolean.TRUE);
        defaults.put(RemotePreferences.REMOTE_SERVER_PORT, 6050);

        defaults.put(PERSONAL_JOURNAL_LIST, null);
        defaults.put(EXTERNAL_JOURNAL_LISTS, null);
        defaults.put(CITE_COMMAND, "\\cite"); // obsoleted by the app-specific ones (not any more?)
        defaults.put(FLOAT_MARKED_ENTRIES, Boolean.TRUE);

        defaults.put(LAST_USED_EXPORT, null);
        defaults.put(SIDE_PANE_WIDTH, -1);

        defaults.put(IMPORT_INSPECTION_DIALOG_WIDTH, 650);
        defaults.put(IMPORT_INSPECTION_DIALOG_HEIGHT, 650);
        defaults.put(SEARCH_DIALOG_WIDTH, 650);
        defaults.put(SEARCH_DIALOG_HEIGHT, 500);
        defaults.put(SHOW_FILE_LINKS_UPGRADE_WARNING, Boolean.TRUE);
        defaults.put(AUTOLINK_EXACT_KEY_ONLY, Boolean.FALSE);
        defaults.put(NUMERIC_FIELDS, "mittnum;author");
        defaults.put(RUN_AUTOMATIC_FILE_SEARCH, Boolean.FALSE);
        defaults.put(USE_LOCK_FILES, Boolean.TRUE);
        defaults.put(AUTO_SAVE, Boolean.TRUE);
        defaults.put(AUTO_SAVE_INTERVAL, 5);
        defaults.put(PROMPT_BEFORE_USING_AUTOSAVE, Boolean.TRUE);
        defaults.put(ENFORCE_LEGAL_BIBTEX_KEY, Boolean.TRUE);
        defaults.put(BIBLATEX_MODE, Boolean.FALSE);
        // Curly brackets ({}) are the default delimiters, not quotes (") as these cause trouble when they appear within the field value:
        // Currently, JabRef does not escape them
        defaults.put(VALUE_DELIMITERS2, 1);
        defaults.put(INCLUDE_EMPTY_FIELDS, Boolean.FALSE);
        defaults.put(KEY_GEN_FIRST_LETTER_A, Boolean.TRUE);
        defaults.put(KEY_GEN_ALWAYS_ADD_LETTER, Boolean.FALSE);
        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
        defaults.put(OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.FALSE);
        defaults.put(ALLOW_FILE_AUTO_OPEN_BROWSE, Boolean.TRUE);
        defaults.put(WEB_SEARCH_VISIBLE, Boolean.FALSE);
        defaults.put(SELECTED_FETCHER_INDEX, 0);
        defaults.put(BIB_LOCATION_AS_FILE_DIR, Boolean.TRUE);
        defaults.put(BIB_LOC_AS_PRIMARY_DIR, Boolean.FALSE);
        defaults.put(DB_CONNECT_SERVER_TYPE, "MySQL");
        defaults.put(DB_CONNECT_HOSTNAME, "localhost");
        defaults.put(DB_CONNECT_DATABASE, "jabref");
        defaults.put(DB_CONNECT_USERNAME, "root");
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
        LOGGER.debug("Temporary directory: " + System.getProperty("java.io.tempdir"));
        //defaults.put("keyPattern", new LabelPattern(KEY_PATTERN));
        defaults.put(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE, Boolean.FALSE);
        defaults.put(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE, ImportSettingsTab.DEFAULT_STYLE);

        // use BibTeX key appended with filename as default pattern
        defaults.put(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN, ImportSettingsTab.DEFAULT_FILENAMEPATTERNS[1]);

        restoreKeyBindings();

        customExports = new CustomExportList(new ExportComparator());
        customImports = new CustomImportList(this);

        //defaults.put("oooWarning", Boolean.TRUE);
        updateSpecialFieldHandling();
        WRAPPED_USERNAME = '[' + get(DEFAULT_OWNER) + ']';
        MARKING_WITH_NUMBER_PATTERN = "\\[" + get(DEFAULT_OWNER).replaceAll("\\\\", "\\\\\\\\") + ":(\\d+)\\]";

        String defaultExpression = "**/.*[bibtexkey].*\\\\.[extension]";
        defaults.put(DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(AUTOLINK_USE_REG_EXP_SEARCH_KEY, Boolean.FALSE);
        defaults.put(USE_IEEE_ABRV, Boolean.FALSE);
        defaults.put(USE_CONVERT_TO_EQUATION, Boolean.FALSE);
        defaults.put(USE_CASE_KEEPER_ON_SEARCH, Boolean.TRUE);
        defaults.put(USE_UNIT_FORMATTER_ON_SEARCH, Boolean.TRUE);

        defaults.put(USER_FILE_DIR, Globals.FILE_FIELD + "Directory");
        try {
            defaults.put(USER_FILE_DIR_IND_LEGACY, Globals.FILE_FIELD + "Directory" + '-' + get(DEFAULT_OWNER) + '@' + InetAddress.getLocalHost().getHostName()); // Legacy setting name - was a bug: @ not allowed inside BibTeX comment text. Retained for backward comp.
            defaults.put(USER_FILE_DIR_INDIVIDUAL, Globals.FILE_FIELD + "Directory" + '-' + get(DEFAULT_OWNER) + '-' + InetAddress.getLocalHost().getHostName()); // Valid setting name
        } catch (UnknownHostException ex) {
            LOGGER.info("Hostname not found.", ex);
            defaults.put(USER_FILE_DIR_IND_LEGACY, Globals.FILE_FIELD + "Directory" + '-' + get(DEFAULT_OWNER));
            defaults.put(USER_FILE_DIR_INDIVIDUAL, Globals.FILE_FIELD + "Directory" + '-' + get(DEFAULT_OWNER));
        }
    }

    public void setLanguageDependentDefaultValues() {
        // Entry editor tab 0:
        defaults.put(CUSTOM_TAB_NAME + "_def0", Localization.lang("General"));
        defaults.put(CUSTOM_TAB_FIELDS + "_def0", "crossref;keywords;file;doi;url;"
                + "comment;owner;timestamp");

        // Entry editor tab 1:
        defaults.put(CUSTOM_TAB_FIELDS + "_def1", "abstract");
        defaults.put(CUSTOM_TAB_NAME + "_def1", Localization.lang("Abstract"));

        // Entry editor tab 2: Review Field - used for research comments, etc.
        defaults.put(CUSTOM_TAB_FIELDS + "_def2", "review");
        defaults.put(CUSTOM_TAB_NAME + "_def2", Localization.lang("Review"));

    }

    public boolean putBracesAroundCapitals(String fieldName) {
        return putBracesAroundCapitalsFields.contains(fieldName);
    }

    public void updateSpecialFieldHandling() {
        putBracesAroundCapitalsFields.clear();
        String fieldString = get(PUT_BRACES_AROUND_CAPITALS);
        if (!fieldString.isEmpty()) {
            String[] fields = fieldString.split(";");
            for (String field : fields) {
                putBracesAroundCapitalsFields.add(field.trim());
            }
        }
        nonWrappableFields.clear();
        fieldString = get(NON_WRAPPABLE_FIELDS);
        if (!fieldString.isEmpty()) {
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
        return JabRefPreferences.VALUE_DELIMITERS[getInt(VALUE_DELIMITERS2)];
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
        return prefs.get(key, (String) defaults.get(key));
    }

    public String get(String key, String def) {
        return prefs.get(key, def);
    }

    public boolean getBoolean(String key) {
        return prefs.getBoolean(key, getBooleanDefault(key));
    }

    public boolean getBoolean(String key, boolean def) {
        return prefs.getBoolean(key, def);
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
     * Puts a string array into the Preferences, by linking its elements with ';' into a single string. Escape
     * characters make the process transparent even if strings contain ';'.
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
        Vector<String> arr = new Vector<>();
        String rs;
        try {
            while ((rs = getNextUnit(rd)) != null) {
                arr.add(rs);
            }
        } catch (IOException ignored) {
            // Ignored
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
     * Set the default value for a key. This is useful for plugins that need to add default values for the prefs keys
     * they use.
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
     * Looks up a color definition in preferences, and returns an array containing the RGB values.
     *
     * @param value The key for this setting.
     * @return The RGB values corresponding to this color setting.
     */
    private static int[] getRgb(String value) {
        int[] values = new int[3];

        if ((value != null) && !value.isEmpty()) {
            String[] elements = value.split(":");
            values[0] = Integer.parseInt(elements[0]);
            values[1] = Integer.parseInt(elements[1]);
            values[2] = Integer.parseInt(elements[2]);
        } else {
            values[0] = 0;
            values[1] = 0;
            values[2] = 0;
        }
        return values;
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in the Preferences.
     */
    public KeyStroke getKey(String bindName) {

        String s = keyBinds.get(bindName);
        // If the current key bindings don't contain the one asked for,
        // we fall back on the default. This should only happen when a
        // user has his own set in Preferences, and has upgraded to a
        // new version where new bindings have been introduced.
        if (s == null) {
            s = defaultKeyBinds.get(bindName);
            if (s == null) {
                // there isn't even a default value
                // Output error
                LOGGER.info("Could not get key binding for \"" + bindName + '"');
                // fall back to a default value
                s = "Not associated";
            }
            // So, if there is no configured key binding, we add the fallback value to the current
            // hashmap, so this doesn't happen again, and so this binding
            // will appear in the KeyBindingsDialog.
            keyBinds.put(bindName, s);
        }

        if (OS.OS_X) {
            return getKeyForMac(KeyStroke.getKeyStroke(s));
        } else {
            return KeyStroke.getKeyStroke(s);
        }
    }

    /**
     * Returns the KeyStroke for this binding, as defined by the defaults, or in the Preferences, but adapted for Mac
     * users, with the Command key preferred instead of Control.
     * TODO: Move to OS.java? Or replace with portable Java key codes, i.e. KeyEvent
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

            if (SHORTCUT_MASK == -1) {
                try {
                    SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                } catch (Throwable ignored) {
                    // Ignored
                }
            }

            return KeyStroke.getKeyStroke(keyCode, SHORTCUT_MASK + modifiers);
        }
    }

    /**
     * Returns the HashMap containing all key bindings.
     */
    public HashMap<String, String> getKeyBindings() {
        return keyBinds.getKeyBindings();
    }

    /**
     * Returns the HashMap containing default key bindings.
     */
    public HashMap<String, String> getDefaultKeys() {
        return defaultKeyBinds.getKeyBindings();
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
        if (getBoolean(MEMORY_STICK_MODE)) {
            try {
                exportPreferences("jabref.xml");
            } catch (JabRefException e) {
                LOGGER.warn("Could not export preferences for memory stick mode: " + e.getMessage(), e);
            }
        }
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stores new key bindings into Preferences, provided they actually differ from the old ones.
     */
    public void setNewKeyBindings(HashMap<String, String> newBindings) {
        if (!newBindings.equals(keyBinds.getKeyBindings())) {
            // This confirms that the bindings have actually changed.
            String[] bindNames = new String[newBindings.size()];
            String[] bindings = new String[newBindings.size()];
            int index = 0;
            for (Map.Entry<String, String> keyBinding : newBindings.entrySet()) {
                bindNames[index] = keyBinding.getKey();
                bindings[index] = keyBinding.getValue();
                index++;
            }
            putStringArray("bindNames", bindNames);
            putStringArray("bindings", bindings);
            keyBinds.overwriteBindings(newBindings);
        }
    }

    /**
     * Fetches key patterns from preferences.
     * The implementation doesn't cache the results
     *
     * @return LabelPattern containing all keys. Returned LabelPattern has no parent
     */
    public GlobalLabelPattern getKeyPattern() {
        JabRefPreferences.keyPattern = new GlobalLabelPattern();
        Preferences pre = Preferences.userNodeForPackage(GlobalLabelPattern.class);
        try {
            String[] keys = pre.keys();
            if (keys.length > 0) {
                for (String key : keys) {
                    JabRefPreferences.keyPattern.addLabelPattern(key, pre.get(key, null));
                }
            }
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences.getKeyPattern", ex);
        }
        return JabRefPreferences.keyPattern;
    }

    /**
     * Adds the given key pattern to the preferences
     *
     * @param pattern the pattern to store
     */
    public void putKeyPattern(GlobalLabelPattern pattern) {
        JabRefPreferences.keyPattern = pattern;

        // Store overridden definitions to Preferences.
        Preferences pre = Preferences.userNodeForPackage(GlobalLabelPattern.class);
        try {
            pre.clear(); // We remove all old entries.
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences.putKeyPattern", ex);
        }

        Enumeration<String> allKeys = pattern.getAllKeys();
        while (allKeys.hasMoreElements()) {
            String key = allKeys.nextElement();
            if (!pattern.isDefaultValue(key)) {
                ArrayList<String> value = pattern.getValue(key);
                // no default value
                // the first entry in the array is the full pattern
                // see net.sf.jabref.logic.labelPattern.LabelPatternUtil.split(String)
                pre.put(key, value.get(0));
            }
        }
    }

    private void restoreKeyBindings() {
        // Define default keybindings.
        defaultKeyBinds = new KeyBinds();

        // First read the bindings, and their names.
        String[] bindNames = getStringArray("bindNames");
        String[] bindings = getStringArray("bindings");

        // Then set up the key bindings HashMap.
        if ((bindNames == null) || (bindings == null)
                || (bindNames.length != bindings.length)) {
            // Nothing defined in Preferences, or something is wrong.
            keyBinds = new KeyBinds();
            return;
        }

        for (int i = 0; i < bindNames.length; i++) {
            keyBinds.put(bindNames[i], bindings[i]);
        }
    }

    private static String getNextUnit(Reader data) throws IOException {
        // character last read
        // -1 if end of stream
        // initialization necessary, because of Java compiler
        int c = -1;

        // last character was escape symbol
        boolean escape = false;

        // true if a ";" is found
        boolean done = false;

        StringBuilder res = new StringBuilder();
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

    private static String makeEscape(String s) {
        StringBuilder sb = new StringBuilder();
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
     * Stores all information about the entry type in preferences, with the tag given by number.
     */
    public void storeCustomEntryType(CustomEntryType tp, int number) {
        String nr = "" + number;
        put(JabRefPreferences.CUSTOM_TYPE_NAME + nr, tp.getName());
        put(JabRefPreferences.CUSTOM_TYPE_REQ + nr, tp.getRequiredFieldsString());
        putStringArray(JabRefPreferences.CUSTOM_TYPE_OPT + nr, tp.getOptionalFields().toArray(new String[0]));
        putStringArray(JabRefPreferences.CUSTOM_TYPE_PRIOPT + nr, tp.getPrimaryOptionalFields().toArray(new String[0]));
    }

    /**
     * Retrieves all information about the entry type in preferences, with the tag given by number.
     */
    public CustomEntryType getCustomEntryType(int number) {
        String nr = "" + number;
        String name = get(JabRefPreferences.CUSTOM_TYPE_NAME + nr);
        String[] req    = getStringArray(JabRefPreferences.CUSTOM_TYPE_REQ + nr);
        String[] opt    = getStringArray(JabRefPreferences.CUSTOM_TYPE_OPT + nr);
        String[] priOpt = getStringArray(JabRefPreferences.CUSTOM_TYPE_PRIOPT + nr);
        if (name == null) {
            return null;
        }
        if (priOpt == null) {
            return new CustomEntryType(EntryUtil.capitalizeFirst(name), Arrays.asList(req), Arrays.asList(opt));
        }
        List<String> secondary = EntryUtil.getRemainder(Arrays.asList(opt), Arrays.asList(priOpt));
        String[] secOpt = secondary.toArray(new String[secondary.size()]);
        return new CustomEntryType(EntryUtil.capitalizeFirst(name), Arrays.asList(req), Arrays.asList(priOpt), Arrays.asList(secOpt));

    }

    public List<ExternalFileType> getDefaultExternalFileTypes() {
        List<ExternalFileType> list = new ArrayList<>();
        list.add(new ExternalFileType("PDF", "pdf", "application/pdf", "evince", "pdfSmall", IconTheme.JabRefIcon.PDF_FILE.getSmallIcon()));
        list.add(new ExternalFileType("PostScript", "ps", "application/postscript", "evince", "psSmall", IconTheme.JabRefIcon.FILE.getSmallIcon()));
        list.add(new ExternalFileType("Word", "doc", "application/msword", "oowriter", "openoffice", IconTheme.JabRefIcon.FILE_WORD.getSmallIcon()));
        list.add(new ExternalFileType("Word 2007+", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "oowriter", "openoffice", IconTheme.JabRefIcon.FILE_WORD.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("OpenDocument text"), "odt",
                "application/vnd.oasis.opendocument.text", "oowriter", "openoffice", IconTheme.getImage("openoffice")));
        list.add(new ExternalFileType("Excel", "xls", "application/excel", "oocalc", "openoffice", IconTheme.JabRefIcon.FILE_EXCEL.getSmallIcon()));
        list.add(new ExternalFileType("Excel 2007+", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "oocalc", "openoffice", IconTheme.JabRefIcon.FILE_EXCEL.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("OpenDocument spreadsheet"), "ods",
                "application/vnd.oasis.opendocument.spreadsheet", "oocalc", "openoffice",
                IconTheme.getImage("openoffice")));
        list.add(new ExternalFileType("PowerPoint", "ppt", "application/vnd.ms-powerpoint", "ooimpress", "openoffice", IconTheme.JabRefIcon.FILE_POWERPOINT.getSmallIcon()));
        list.add(new ExternalFileType("PowerPoint 2007+", "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "ooimpress", "openoffice", IconTheme.JabRefIcon.FILE_POWERPOINT.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("OpenDocument presentation"), "odp",
                "application/vnd.oasis.opendocument.presentation", "ooimpress", "openoffice",
                IconTheme.getImage("openoffice")));
        list.add(new ExternalFileType("Rich Text Format", "rtf", "application/rtf", "oowriter", "openoffice", IconTheme.JabRefIcon.FILE_TEXT.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "PNG"), "png", "image/png", "gimp", "picture", IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "GIF"), "gif", "image/gif", "gimp", "picture", IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "JPG"), "jpg", "image/jpeg", "gimp", "picture", IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType("Djvu", "djvu", "", "evince", "psSmall", IconTheme.JabRefIcon.FILE.getSmallIcon()));
        list.add(new ExternalFileType("Text", "txt", "text/plain", "emacs", "emacs", IconTheme.JabRefIcon.FILE_TEXT.getSmallIcon()));
        list.add(new ExternalFileType("LaTeX", "tex", "application/x-latex", "emacs", "emacs", IconTheme.JabRefIcon.FILE_TEXT.getSmallIcon()));
        list.add(new ExternalFileType("CHM", "chm", "application/mshelp", "gnochm", "www", IconTheme.JabRefIcon.WWW.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "TIFF"), "tiff", "image/tiff", "gimp", "picture",
                IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType("URL", "html", "text/html", "firefox", "www", IconTheme.JabRefIcon.WWW.getSmallIcon()));
        list.add(new ExternalFileType("MHT", "mht", "multipart/related", "firefox", "www", IconTheme.JabRefIcon.WWW.getSmallIcon()));
        list.add(new ExternalFileType("ePUB", "epub", "application/epub+zip", "firefox", "www", IconTheme.JabRefIcon.WWW.getSmallIcon()));

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
     * @return The ExternalFileType registered, or null if none. For the mime type "text/html", a valid file type is
     *         guaranteed to be returned.
     */
    public ExternalFileType getExternalFileTypeByMimeType(String mimeType) {
        for (ExternalFileType type : externalFileTypes) {
            if ((type.getMimeType() != null) && type.getMimeType().equals(mimeType)) {
                return type;
            }
        }
        if ("text/html".equals(mimeType)) {
            return HTML_FALLBACK_TYPE;
        } else {
            return null;
        }
    }

    /**
     * Reset the List of external file types after user customization.
     *
     * @param types The new List of external file types. This is the complete list, not just new entries.
     */
    public void setExternalFileTypes(List<ExternalFileType> types) {

        // First find a list of the default types:
        List<ExternalFileType> defTypes = getDefaultExternalFileTypes();
        // Make a list of types that are unchanged:
        List<ExternalFileType> unchanged = new ArrayList<>();

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
        put("externalFileTypes", StringUtil.encodeStringArray(array));
    }

    /**
     * Set up the list of external file types, either from default values, or from values recorded in Preferences.
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
        String[][] vals = StringUtil.decodeStringDoubleArray(prefs.get("externalFileTypes", ""));
        for (String[] val : vals) {
            if ((val.length == 2) && val[1].equals(JabRefPreferences.FILE_TYPE_REMOVED_FLAG)) {
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
     * Removes all entries keyed by prefix+number, where number is equal to or higher than the given number.
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
    public void exportPreferences(String filename) throws JabRefException {
        File f = new File(filename);
        try (OutputStream os = new FileOutputStream(f)) {
            prefs.exportSubtree(os);
        } catch (BackingStoreException | IOException ex) {
            throw new JabRefException("Could not export preferences", Localization.lang("Could not export preferences"), ex);
        }
    }

    /**
     * Imports Preferences from an XML file.
     *
     * @param filename String File to import from
     * @throws JabRefException thrown if importing the preferences failed due to an InvalidPreferencesFormatException
     *                         or an IOException
     */
    public void importPreferences(String filename) throws JabRefException {
        File f = new File(filename);
        try (InputStream is = new FileInputStream(f)) {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException | IOException ex) {
            throw new JabRefException("Could not import preferences", Localization.lang("Could not import preferences"),
                    ex);
        }
    }

    /**
     * Determines whether the given field should be written without any sort of wrapping.
     *
     * @param fieldName The field name.
     * @return true if the field should not be wrapped.
     */
    public boolean isNonWrappableField(String fieldName) {
        return nonWrappableFields.contains(fieldName);
    }

    /**
     * ONLY FOR TESTING!
     *
     * Do not use in production code. Otherwise the singleton pattern is broken and preferences might get lost.
     *
     * @param owPrefs
     */
    void overwritePreferences(JabRefPreferences owPrefs) {
        singleton = owPrefs;
    }

    public Charset getDefaultEncoding() {
        return Charset.forName(get(JabRefPreferences.DEFAULT_ENCODING));
    }

    public void setDefaultEncoding(Charset encoding) {
        put(JabRefPreferences.DEFAULT_ENCODING, encoding.name());
    }
}
