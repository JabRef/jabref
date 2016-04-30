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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.UIManager;

import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.exporter.CustomExportList;
import net.sf.jabref.exporter.ExportComparator;
import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.entryeditor.EntryEditorTabList;
import net.sf.jabref.gui.maintable.PersistenceTableColumnListener;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.importer.CustomImportList;
import net.sf.jabref.logic.autocompleter.AutoCompletePreferences;
import net.sf.jabref.logic.cleanup.CleanupPreset;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.GlobalLabelPattern;
import net.sf.jabref.logic.openoffice.OpenOfficePreferences;
import net.sf.jabref.logic.openoffice.StyleLoader;
import net.sf.jabref.logic.remote.RemotePreferences;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabRefPreferences {
    private static final Log LOGGER = LogFactory.getLog(JabRefPreferences.class);
    public static final String EXTERNAL_FILE_TYPES = "externalFileTypes";

    /**
     * HashMap that contains all preferences which are set by default
     */
    public final Map<String, Object> defaults = new HashMap<>();

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
    public static final String BIBLATEX_DEFAULT_MODE = "biblatexMode";
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
    public static final String PROXY_USE = "useProxy";
    public static final String PROXY_USERNAME = "proxyUsername";
    public static final String PROXY_PASSWORD = "proxyPassword";
    public static final String PROXY_USE_AUTHENTICATION = "useProxyAuthentication";
    public static final String TABLE_PRIMARY_SORT_FIELD = "priSort";
    public static final String TABLE_PRIMARY_SORT_DESCENDING = "priDescending";
    public static final String TABLE_SECONDARY_SORT_FIELD = "secSort";
    public static final String TABLE_SECONDARY_SORT_DESCENDING = "secDescending";
    public static final String TABLE_TERTIARY_SORT_FIELD = "terSort";
    public static final String TABLE_TERTIARY_SORT_DESCENDING = "terDescending";
    public static final String REFORMAT_FILE_ON_SAVE_AND_EXPORT = "reformatFileOnSaveAndExport";
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
    public static final String PREAMBLE_SIZE_Y = "preambleSizeY";
    public static final String PREAMBLE_SIZE_X = "preambleSizeX";
    public static final String PREAMBLE_POS_Y = "preamblePosY";
    public static final String PREAMBLE_POS_X = "preamblePosX";
    public static final String LAST_EDITED = "lastEdited";
    public static final String OPEN_LAST_EDITED = "openLastEdited";
    public static final String LAST_FOCUSED = "lastFocused";
    public static final String BACKUP = "backup";
    public static final String ENTRY_TYPE_FORM_WIDTH = "entryTypeFormWidth";
    public static final String ENTRY_TYPE_FORM_HEIGHT_FACTOR = "entryTypeFormHeightFactor";
    public static final String AUTO_OPEN_FORM = "autoOpenForm";
    public static final String FILE_WORKING_DIRECTORY = "fileWorkingDirectory";
    public static final String IMPORT_WORKING_DIRECTORY = "importWorkingDirectory";
    public static final String EXPORT_WORKING_DIRECTORY = "exportWorkingDirectory";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String NUMBER_COL_WIDTH = "numberColWidth";
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
    public static final String HIGHLIGHT_GROUPS_MATCHING = "highlightGroupsMatching";
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
    public static final String PREVIEW_PRINT_BUTTON = "previewPrintButton";
    public static final String PREVIEW_1 = "preview1";
    public static final String PREVIEW_0 = "preview0";
    public static final String ACTIVE_PREVIEW = "activePreview";
    public static final String PREVIEW_ENABLED = "previewEnabled";
    public static final String MERGE_ENTRIES_DIFF_MODE = "mergeEntriesDiffMode";

    public static final String CUSTOM_EXPORT_FORMAT = "customExportFormat";
    public static final String CUSTOM_IMPORT_FORMAT = "customImportFormat";
    public static final String BINDINGS = "bindings";
    public static final String BIND_NAMES = "bindNames";
    public static final String MARKED_ENTRY_BACKGROUND = "markedEntryBackground";
    public static final String KEY_PATTERN_REGEX = "KeyPatternRegex";
    public static final String KEY_PATTERN_REPLACEMENT = "KeyPatternReplacement";

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
    public static final String OVERWRITE_OWNER = "overwriteOwner";
    public static final String USE_OWNER = "useOwner";
    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";
    public static final String SHOW_FILE_LINKS_UPGRADE_WARNING = "showFileLinksUpgradeWarning";
    public static final String SEARCH_DIALOG_HEIGHT = "searchDialogHeight";
    public static final String SEARCH_DIALOG_WIDTH = "searchDialogWidth";
    public static final String IMPORT_INSPECTION_DIALOG_HEIGHT = "importInspectionDialogHeight";
    public static final String IMPORT_INSPECTION_DIALOG_WIDTH = "importInspectionDialogWidth";
    public static final String SIDE_PANE_WIDTH = "sidePaneWidth";
    public static final String LAST_USED_EXPORT = "lastUsedExport";
    public static final String LAST_USED_IMPORT = "lastUsedImport";
    public static final String FLOAT_MARKED_ENTRIES = "floatMarkedEntries";
    public static final String CITE_COMMAND = "citeCommand";
    public static final String EXTERNAL_JOURNAL_LISTS = "externalJournalLists";
    public static final String PERSONAL_JOURNAL_LIST = "personalJournalList";
    public static final String GENERATE_KEYS_BEFORE_SAVING = "generateKeysBeforeSaving";
    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";
    public static final String KEY_GEN_ALWAYS_ADD_LETTER = "keyGenAlwaysAddLetter";
    public static final String KEY_GEN_FIRST_LETTER_A = "keyGenFirstLetterA";
    public static final String VALUE_DELIMITERS2 = "valueDelimiters";
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
    public static final String SELECTED_FETCHER_INDEX = "selectedFetcherIndex";
    public static final String WEB_SEARCH_VISIBLE = "webSearchVisible";
    public static final String ALLOW_FILE_AUTO_OPEN_BROWSE = "allowFileAutoOpenBrowse";
    public static final String CUSTOM_TAB_NAME = "customTabName_";
    public static final String CUSTOM_TAB_FIELDS = "customTabFields_";
    public static final String USE_UNIT_FORMATTER_ON_SEARCH = "useUnitFormatterOnSearch";
    public static final String USE_CASE_KEEPER_ON_SEARCH = "useCaseKeeperOnSearch";
    public static final String USE_CONVERT_TO_EQUATION = "useConvertToEquation";
    public static final String USE_IEEE_ABRV = "useIEEEAbrv";

    public static final String AKS_AUTO_NAMING_PDFS_AGAIN = "AskAutoNamingPDFsAgain";
    public static final String CLEANUP_DOI = "CleanUpDOI";
    public static final String CLEANUP_MOVE_PDF = "CleanUpMovePDF";
    public static final String CLEANUP_MAKE_PATHS_RELATIVE = "CleanUpMakePathsRelative";
    public static final String CLEANUP_RENAME_PDF = "CleanUpRenamePDF";
    public static final String CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS = "CleanUpRenamePDFonlyRelativePaths";
    public static final String CLEANUP_UPGRADE_EXTERNAL_LINKS = "CleanUpUpgradeExternalLinks";
    public static final String CLEANUP_CONVERT_TO_BIBLATEX = "CleanUpConvertToBiblatex";
    public static final String CLEANUP_FIX_FILE_LINKS = "CleanUpFixFileLinks";
    public static final String CLEANUP_FORMATTERS = "CleanUpFormatters";
    public static final CleanupPreset CLEANUP_DEFAULT_PRESET;
    static {
        EnumSet<CleanupPreset.CleanupStep> deactivedJobs = EnumSet.of(
                CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS,
                CleanupPreset.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS,
                CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);

        List<FieldFormatterCleanup> activeFormatterCleanups = new ArrayList<>();
        activeFormatterCleanups.add(new FieldFormatterCleanup("pages", new NormalizePagesFormatter()));
        activeFormatterCleanups.add(new FieldFormatterCleanup("date", new NormalizeDateFormatter()));
        activeFormatterCleanups.add(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()));
        activeFormatterCleanups.add(new FieldFormatterCleanup("title", new ProtectTermsFormatter()));
        activeFormatterCleanups.add(new FieldFormatterCleanup("title", new UnitsToLatexFormatter()));
        activeFormatterCleanups.add(new FieldFormatterCleanup("title", new LatexCleanupFormatter()));
        activeFormatterCleanups.add(new FieldFormatterCleanup("title", new HtmlToLatexFormatter()));
        FieldFormatterCleanups formatterCleanups = new FieldFormatterCleanups(true, activeFormatterCleanups);
        CLEANUP_DEFAULT_PRESET = new CleanupPreset(EnumSet.complementOf(deactivedJobs), formatterCleanups);
    }

    public static final String PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE = "importDefaultPDFimportStyle";
    public static final String PREF_IMPORT_ALWAYSUSE = "importAlwaysUsePDFImportStyle";
    public static final String PREF_IMPORT_FILENAMEPATTERN = "importFileNamePattern";


    public static final String PUSH_TO_APPLICATION = "pushToApplication";

    /**
     * The OpenOffice/LibreOffice connection preferences are:
     * OO_PATH main directory for OO/LO installation, used to detect location on Win/OS X when using manual connect
     * OO_EXECUTABLE_PATH path to soffice-file
     * OO_JARS_PATH directory that contains juh.jar, jurt.jar, ridl.jar, unoil.jar
     * OO_SYNC_WHEN_CITING true if the reference list is updated when adding a new citation
     * OO_SHOW_PANEL true if the OO panel is shown on startup
     * OO_USE_ALL_OPEN_DATABASES true if all databases should be used when citing
     * OO_BIBLIOGRAPHY_STYLE_FILE path to the used style file
     * OO_EXTERNAL_STYLE_FILES list with paths to external style files
     * STYLES_*_* size and position of "Select style" dialog
     */
    public static final String OO_EXECUTABLE_PATH = "ooExecutablePath";
    public static final String OO_PATH = "ooPath";
    public static final String OO_JARS_PATH = "ooJarsPath";
    public static final String OO_SHOW_PANEL = "showOOPanel";
    public static final String OO_SYNC_WHEN_CITING = "syncOOWhenCiting";
    public static final String OO_USE_ALL_OPEN_BASES = "useAllOpenBases";
    public static final String OO_BIBLIOGRAPHY_STYLE_FILE = "ooBibliographyStyleFile";
    public static final String OO_EXTERNAL_STYLE_FILES = "ooExternalStyleFiles";
    public static final String STYLES_SIZE_Y = "stylesSizeY";
    public static final String STYLES_SIZE_X = "stylesSizeX";
    public static final String STYLES_POS_Y = "stylesPosY";
    public static final String STYLES_POS_X = "stylesPosX";

    //non-default preferences
    private static final String CUSTOM_TYPE_NAME = "customTypeName_";
    private static final String CUSTOM_TYPE_REQ = "customTypeReq_";
    private static final String CUSTOM_TYPE_OPT = "customTypeOpt_";
    private static final String CUSTOM_TYPE_PRIOPT = "customTypePriOpt_";

    private static final char[][] VALUE_DELIMITERS = new char[][] { {'"', '"'}, {'{', '}'}};


    public String WRAPPED_USERNAME;
    public final String MARKING_WITH_NUMBER_PATTERN;

    private final Preferences prefs;

    private final Set<String> putBracesAroundCapitalsFields = new HashSet<>(4);
    private final Set<String> nonWrappableFields = new HashSet<>(5);
    private GlobalLabelPattern keyPattern;

    // Object containing custom export formats:
    public final CustomExportList customExports;

    // Helper string
    private static final String USER_HOME = System.getProperty("user.home");

    /**
     * Set with all custom {@link ImportFormat}s
     */
    public final CustomImportList customImports;

    // Object containing info about customized entry editor tabs.
    private EntryEditorTabList tabList;

    // The following field is used as a global variable during the export of a database.
    // By setting this field to the path of the database's default file directory, formatters
    // that should resolve external file paths can access this field. This is an ugly hack
    // to solve the problem of formatters not having access to any context except for the
    // string to be formatted and possible formatter arguments.
    public List<String> fileDirForDatabase;

    // The following field is used as a global variable during the export of a database.
    // It is used to hold custom name formatters defined by a custom export filter.
    // It is set before the export starts:
    public Map<String, String> customExportNameFormatters;

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
        prefs = Preferences.userNodeForPackage(JabRefMain.class);

        defaults.put(TEXMAKER_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("texmaker", "Texmaker"));
        defaults.put(WIN_EDT_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("WinEdt", "WinEdt Team\\WinEdt"));
        defaults.put(LATEX_EDITOR_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("LEd", "LEd"));
        defaults.put(TEXSTUDIO_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("texstudio", "TeXstudio"));

        defaults.put(BIBLATEX_DEFAULT_MODE, false);

        if (OS.OS_X) {
            defaults.put(EMACS_PATH, "emacsclient");
            defaults.put(EMACS_23, true);
            defaults.put(EMACS_ADDITIONAL_PARAMETERS, "-n -e");
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(WIN_LOOK_AND_FEEL, UIManager.getSystemLookAndFeelClassName());

        } else if (OS.WINDOWS) {
            defaults.put(WIN_LOOK_AND_FEEL, "com.jgoodies.looks.windows.WindowsLookAndFeel");
            defaults.put(EMACS_PATH, "emacsclient.exe");
            defaults.put(EMACS_23, true);
            defaults.put(EMACS_ADDITIONAL_PARAMETERS, "-n -e");
            defaults.put(FONT_FAMILY, "Arial");

        } else {
            // Linux
            defaults.put(WIN_LOOK_AND_FEEL, "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel");
            defaults.put(FONT_FAMILY, "SansSerif");

            defaults.put(EMACS_PATH, "gnuclient");
            defaults.put(EMACS_23, false);
            defaults.put(EMACS_ADDITIONAL_PARAMETERS, "-batch -eval");
        }
        defaults.put(PUSH_TO_APPLICATION, "TeXstudio");

        defaults.put(RECENT_FILES, "");
        defaults.put(EXTERNAL_FILE_TYPES, "");
        defaults.put(KEY_PATTERN_REGEX, "");
        defaults.put(KEY_PATTERN_REPLACEMENT, "");

        // Proxy
        defaults.put(PROXY_USE, Boolean.FALSE);
        defaults.put(PROXY_HOSTNAME, "");
        defaults.put(PROXY_PORT, "80");
        defaults.put(PROXY_USE_AUTHENTICATION, Boolean.FALSE);
        defaults.put(PROXY_USERNAME, "");
        defaults.put(PROXY_PASSWORD, "");

        defaults.put(USE_DEFAULT_LOOK_AND_FEEL, Boolean.TRUE);
        defaults.put(LYXPIPE, USER_HOME + File.separator + ".lyx/lyxpipe");
        defaults.put(VIM, "vim");
        defaults.put(VIM_SERVER, "vim");
        defaults.put(POS_X, 0);
        defaults.put(POS_Y, 0);
        defaults.put(SIZE_X, 1024);
        defaults.put(SIZE_Y, 768);
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

        defaults.put(REFORMAT_FILE_ON_SAVE_AND_EXPORT, Boolean.FALSE);

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

        defaults.put(COLUMN_NAMES, "entrytype;author/editor;title;year;journal/booktitle;bibtexkey");
        defaults.put(COLUMN_WIDTHS, "75;300;470;60;130;100");
        defaults.put(PersistenceTableColumnListener.ACTIVATE_PREF_KEY,
                PersistenceTableColumnListener.DEFAULT_ENABLED);
        defaults.put(XMP_PRIVACY_FILTERS, "pdf;timestamp;keywords;owner;note;review");
        defaults.put(USE_XMP_PRIVACY_FILTER, Boolean.FALSE);
        defaults.put(NUMBER_COL_WIDTH, GUIGlobals.NUMBER_COL_LENGTH);
        defaults.put(WORKING_DIRECTORY, USER_HOME);
        defaults.put(EXPORT_WORKING_DIRECTORY, USER_HOME);
        // Remembers working directory of last import
        defaults.put(IMPORT_WORKING_DIRECTORY, USER_HOME);
        defaults.put(FILE_WORKING_DIRECTORY, USER_HOME);
        defaults.put(AUTO_OPEN_FORM, Boolean.TRUE);
        defaults.put(BACKUP, Boolean.TRUE);
        defaults.put(OPEN_LAST_EDITED, Boolean.TRUE);
        defaults.put(LAST_EDITED, "");
        defaults.put(LAST_FOCUSED, "");
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
        defaults.put(PREAMBLE_POS_X, 0);
        defaults.put(PREAMBLE_POS_Y, 0);
        defaults.put(PREAMBLE_SIZE_X, 600);
        defaults.put(PREAMBLE_SIZE_Y, 400);
        defaults.put(DEFAULT_SHOW_SOURCE, Boolean.FALSE);
        defaults.put(DEFAULT_AUTO_SORT, Boolean.FALSE);
        defaults.put(SEARCH_CASE_SENSITIVE, Boolean.FALSE);
        defaults.put(SEARCH_MODE_FILTER, Boolean.TRUE);

        defaults.put(SEARCH_REG_EXP, Boolean.FALSE);
        defaults.put(SEARCH_PANE_POS_X, 0);
        defaults.put(SEARCH_PANE_POS_Y, 0);

        defaults.put(MERGE_ENTRIES_DIFF_MODE, 2);

        defaults.put(EDITOR_EMACS_KEYBINDINGS, Boolean.FALSE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CA, Boolean.TRUE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CF, Boolean.TRUE);
        defaults.put(AUTO_COMPLETE, Boolean.TRUE);
        AutoCompletePreferences.putDefaults(defaults);
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
        defaults.put(HIGHLIGHT_GROUPS_MATCHING, "");
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
        defaults.put(URL_COLUMN, Boolean.TRUE);
        defaults.put(PREFER_URL_DOI, Boolean.FALSE);
        defaults.put(FILE_COLUMN, Boolean.TRUE);
        defaults.put(ARXIV_COLUMN, Boolean.FALSE);

        defaults.put(EXTRA_FILE_COLUMNS, Boolean.FALSE);
        defaults.put(LIST_OF_FILE_COLUMNS, "");

        // OpenOffice/LibreOffice
        if (OS.WINDOWS) {
            defaults.put(JabRefPreferences.OO_PATH, OpenOfficePreferences.DEFAULT_WINDOWS_PATH);
            defaults.put(JabRefPreferences.OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_WINDOWS_PATH
                    + OpenOfficePreferences.WINDOWS_EXECUTABLE_SUBPATH + OpenOfficePreferences.WINDOWS_EXECUTABLE);
            defaults.put(JabRefPreferences.OO_JARS_PATH,
                    OpenOfficePreferences.DEFAULT_WINDOWS_PATH + OpenOfficePreferences.WINDOWS_JARS_SUBPATH);
        } else if (OS.OS_X) {
            defaults.put(JabRefPreferences.OO_PATH, OpenOfficePreferences.DEFAULT_OSX_PATH);
            defaults.put(JabRefPreferences.OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_OSX_PATH
                    + OpenOfficePreferences.OSX_EXECUTABLE_SUBPATH + OpenOfficePreferences.OSX_EXECUTABLE);
            defaults.put(JabRefPreferences.OO_JARS_PATH,
                    OpenOfficePreferences.DEFAULT_OSX_PATH + OpenOfficePreferences.OSX_JARS_SUBPATH);
        } else { // Linux
            defaults.put(JabRefPreferences.OO_PATH, "/opt/openoffice.org3");
            defaults.put(JabRefPreferences.OO_EXECUTABLE_PATH, "/usr/lib/openoffice/program/soffice");
            defaults.put(JabRefPreferences.OO_JARS_PATH, "/opt/openoffice.org/basis3.0");
        }

        defaults.put(JabRefPreferences.OO_SYNC_WHEN_CITING, false);
        defaults.put(JabRefPreferences.OO_SHOW_PANEL, false);
        defaults.put(JabRefPreferences.OO_USE_ALL_OPEN_BASES, true);
        defaults.put(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE,
                StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        defaults.put(JabRefPreferences.OO_EXTERNAL_STYLE_FILES, "");
        defaults.put(STYLES_POS_X, 0);
        defaults.put(STYLES_POS_Y, 0);
        defaults.put(STYLES_SIZE_X, 600);
        defaults.put(STYLES_SIZE_Y, 400);

        defaults.put(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED, SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY, SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY, SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING, SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE, SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED, SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ, SpecialFieldsUtils.PREF_SHOWCOLUMN_READ_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS, SpecialFieldsUtils.PREF_AUTOSYNCSPECIALFIELDSTOKEYWORDS_DEFAULT);
        defaults.put(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS, SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS_DEFAULT);

        defaults.put(USE_OWNER, Boolean.FALSE);
        defaults.put(OVERWRITE_OWNER, Boolean.FALSE);
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
        defaults.put(PREVIEW_0,
                "<font face=\"sans-serif\">"
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
        defaults.put(PREVIEW_1,
                "<font face=\"sans-serif\">"
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

        defaults.put(TIME_STAMP_FIELD, InternalBibtexFields.TIMESTAMP);
        defaults.put(UPDATE_TIMESTAMP, Boolean.FALSE);

        defaults.put(GENERATE_KEYS_BEFORE_SAVING, Boolean.FALSE);

        defaults.put(RemotePreferences.USE_REMOTE_SERVER, Boolean.TRUE);
        defaults.put(RemotePreferences.REMOTE_SERVER_PORT, 6050);

        defaults.put(PERSONAL_JOURNAL_LIST, "");
        defaults.put(EXTERNAL_JOURNAL_LISTS, "");
        defaults.put(CITE_COMMAND, "\\cite"); // obsoleted by the app-specific ones (not any more?)
        defaults.put(FLOAT_MARKED_ENTRIES, Boolean.TRUE);

        defaults.put(LAST_USED_EXPORT, "");
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
        // Curly brackets ({}) are the default delimiters, not quotes (") as these cause trouble when they appear within the field value:
        // Currently, JabRef does not escape them
        defaults.put(VALUE_DELIMITERS2, 1);
        defaults.put(KEY_GEN_FIRST_LETTER_A, Boolean.TRUE);
        defaults.put(KEY_GEN_ALWAYS_ADD_LETTER, Boolean.FALSE);
        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
        defaults.put(OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.FALSE);
        defaults.put(ALLOW_FILE_AUTO_OPEN_BROWSE, Boolean.TRUE);
        defaults.put(WEB_SEARCH_VISIBLE, Boolean.FALSE);
        defaults.put(SELECTED_FETCHER_INDEX, 0);
        defaults.put(BIB_LOC_AS_PRIMARY_DIR, Boolean.FALSE);
        defaults.put(DB_CONNECT_SERVER_TYPE, "MySQL");
        defaults.put(DB_CONNECT_HOSTNAME, "localhost");
        defaults.put(DB_CONNECT_DATABASE, "jabref");
        defaults.put(DB_CONNECT_USERNAME, "root");

        defaults.put(AKS_AUTO_NAMING_PDFS_AGAIN, Boolean.TRUE);
        insertCleanupPreset(defaults, CLEANUP_DEFAULT_PRESET);

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

        defaults.put(PREF_IMPORT_ALWAYSUSE, Boolean.FALSE);
        defaults.put(PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE, ImportSettingsTab.DEFAULT_STYLE);

        // use BibTeX key appended with filename as default pattern
        defaults.put(PREF_IMPORT_FILENAMEPATTERN, ImportSettingsTab.DEFAULT_FILENAMEPATTERNS[1]);

        customExports = new CustomExportList(new ExportComparator());
        customImports = new CustomImportList(this);

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
    }

    public String getUser() {
        try {
            return get(DEFAULT_OWNER) + '-' + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            LOGGER.debug("Hostname not found.", ex);
            return get(DEFAULT_OWNER);
        }
    }

    public List<String> getCustomTabFieldNames() {
        List<String> customFields = new ArrayList<>();

        int defNumber = 0;
        while(true) {
            // saved as CUSTOMTABNAME_def{number} and ; separated
            String fields = (String) defaults.get(CUSTOM_TAB_FIELDS + "_def" + defNumber);

            if((fields == null) || fields.isEmpty()) {
                break;
            }

            customFields.addAll(Arrays.asList(fields.split(";")));
            defNumber++;
        }
        return customFields;
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

        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
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

    public int getInt(String key) {
        return prefs.getInt(key, getIntDefault(key));
    }

    public int getIntDefault(String key) {
        return (Integer) defaults.get(key);
    }

    public void put(String key, String value) {
        prefs.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        prefs.putBoolean(key, value);
    }

    public void putInt(String key, int value) {
        prefs.putInt(key, value);
    }

    public void remove(String key) {
        prefs.remove(key);
    }

    /**
     * Puts a list of strings into the Preferences, by linking its elements with ';' into a single string. Escape
     * characters make the process transparent even if strings contain ';'.
     */
    public void putStringList(String key, List<String> value) {
        if (value == null) {
            remove(key);
            return;
        }

        put(key, convertListToString(value));
    }

    private static String convertListToString(List<String> value) {
        return value.stream().map(val -> StringUtil.quote(val, ";", '\\')).collect(Collectors.joining(";"));
    }

    /**
     * Returns a List of Strings containing the chosen columns.
     */
    public List<String> getStringList(String key) {
        String names = get(key);
        if (names == null) {
            return new ArrayList<>();
        }

        StringReader rd = new StringReader(names);
        List<String> res = new ArrayList<>();
        String rs;
        try {
            while ((rs = getNextUnit(rd)) != null) {
                res.add(rs);
            }
        } catch (IOException ignored) {
            // Ignored
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
        String rgb = String.valueOf(color.getRed()) + ':' + color.getGreen() + ':' + color.getBlue();
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
            LOGGER.warn("Can not communicate with backing store", ex);
        }
    }

    /**
     * Fetches key patterns from preferences.
     * The implementation doesn't cache the results
     *
     * @return LabelPattern containing all keys. Returned LabelPattern has no parent
     */
    public GlobalLabelPattern getKeyPattern() {
        keyPattern = new GlobalLabelPattern();
        Preferences pre = Preferences.userNodeForPackage(GlobalLabelPattern.class);
        try {
            String[] keys = pre.keys();
            if (keys.length > 0) {
                for (String key : keys) {
                    keyPattern.addLabelPattern(key, pre.get(key, null));
                }
            }
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences.getKeyPattern", ex);
        }
        return keyPattern;
    }

    /**
     * Adds the given key pattern to the preferences
     *
     * @param pattern the pattern to store
     */
    public void putKeyPattern(GlobalLabelPattern pattern) {
        keyPattern = pattern;

        // Store overridden definitions to Preferences.
        Preferences pre = Preferences.userNodeForPackage(GlobalLabelPattern.class);
        try {
            pre.clear(); // We remove all old entries.
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences.putKeyPattern", ex);
        }

        Set<String> allKeys = pattern.getAllKeys();
        for (String key : allKeys) {
            if (!pattern.isDefaultValue(key)) {
                // no default value
                // the first entry in the array is the full pattern
                // see net.sf.jabref.logic.labelPattern.LabelPatternUtil.split(String)
                pre.put(key, pattern.getValue(key).get(0));
            }
        }
    }

    public Map<String, Object> getPreferences() {
        Map<String, Object> prefs = new HashMap<>();
        try {
            for(String key : this.prefs.keys()){
                Object value = getObject(key);
                prefs.put(key, value);
            }
        } catch (BackingStoreException e) {
            LOGGER.info("could not retrieve preference keys", e);
        }
        return prefs;
    }

    private Object getObject(String key) {
        try {
            return this.get(key);
        } catch (ClassCastException e) {
            try {
                return this.getBoolean(key);
            } catch (ClassCastException e2) {
                return this.getInt(key);
            }
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
                if (escape) {
                    escape = false;
                    res.append('\\');
                } else {
                    escape = true;
                }
            } else {
                if (c == ';') {
                    if (escape) {
                        res.append(';');
                    } else {
                        done = true;
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

    /**
     * Stores all information about the entry type in preferences, with the tag given by number.
     */
    public void storeCustomEntryType(CustomEntryType tp, int number) {
        String nr = String.valueOf(number);
        put(JabRefPreferences.CUSTOM_TYPE_NAME + nr, tp.getName());
        put(JabRefPreferences.CUSTOM_TYPE_REQ + nr, tp.getRequiredFieldsString());
        List<String> optionalFields = tp.getOptionalFields();
        putStringList(JabRefPreferences.CUSTOM_TYPE_OPT + nr, optionalFields);
        List<String> primaryOptionalFields = tp.getPrimaryOptionalFields();
        putStringList(JabRefPreferences.CUSTOM_TYPE_PRIOPT + nr, primaryOptionalFields);
    }

    /**
     * Retrieves all information about the entry type in preferences, with the tag given by number.
     */
    public CustomEntryType getCustomEntryType(int number) {
        String nr = String.valueOf(number);
        String name = get(JabRefPreferences.CUSTOM_TYPE_NAME + nr);
        if (name == null) {
            return null;
        }
        List<String> req = getStringList(JabRefPreferences.CUSTOM_TYPE_REQ + nr);
        List<String> opt = getStringList(JabRefPreferences.CUSTOM_TYPE_OPT + nr);
        List<String> priOpt = getStringList(JabRefPreferences.CUSTOM_TYPE_PRIOPT + nr);
        if (priOpt.isEmpty()) {
            return new CustomEntryType(EntryUtil.capitalizeFirst(name), req, opt);
        }
        List<String> secondary = new ArrayList<>(opt);
        secondary.removeAll(priOpt);

        return new CustomEntryType(EntryUtil.capitalizeFirst(name), req, priOpt, secondary);

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

    public void purgeCustomEntryTypes() {
        int number = 0;
        if(getCustomEntryType(number) != null) {
            number++;
        }

        for(int i = 0; i < number; i++) {
            purgeCustomEntryTypes(i);
        }
    }

    /**
     * Removes all entries keyed by prefix+number, where number is equal to or higher than the given number.
     *
     * @param number or higher.
     */
    public void purgeSeries(String prefix, int number) {
        int n = number;
        while (get(prefix + n) != null) {
            remove(prefix + n);
            n++;
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
     * ONLY FOR TESTING!
     *
     * Do not use in production code. Otherwise the singleton pattern is broken and preferences might get lost.
     *
     * @param owPrefs
     */
    public void overwritePreferences(JabRefPreferences owPrefs) {
        singleton = owPrefs;
    }

    public Charset getDefaultEncoding() {
        return Charset.forName(get(JabRefPreferences.DEFAULT_ENCODING));
    }

    public void setDefaultEncoding(Charset encoding) {
        put(JabRefPreferences.DEFAULT_ENCODING, encoding.name());
    }

    private static void insertCleanupPreset(Map<String, Object> storage, CleanupPreset preset) {
        storage.put(CLEANUP_DOI, preset.isCleanUpDOI());
        storage.put(CLEANUP_MOVE_PDF, preset.isMovePDF());
        storage.put(CLEANUP_MAKE_PATHS_RELATIVE, preset.isMakePathsRelative());
        storage.put(CLEANUP_RENAME_PDF, preset.isRenamePDF());
        storage.put(CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS, preset.isRenamePdfOnlyRelativePaths());
        storage.put(CLEANUP_UPGRADE_EXTERNAL_LINKS, preset.isCleanUpUpgradeExternalLinks());
        storage.put(CLEANUP_CONVERT_TO_BIBLATEX, preset.isConvertToBiblatex());
        storage.put(CLEANUP_FIX_FILE_LINKS, preset.isFixFileLinks());
        storage.put(CLEANUP_FORMATTERS, convertListToString(preset.getFormatterCleanups().convertToString()));
    }

}
