package org.jabref.preferences;

import java.awt.Color;
import java.awt.Font;
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.UIManager;

import org.jabref.JabRefException;
import org.jabref.JabRefMain;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.entryeditor.EntryEditorTabList;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preftabs.ImportSettingsTab;
import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.StyleLoader;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.UpdateFieldPreferences;
import org.jabref.logic.util.Version;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefPreferences implements PreferencesService {

    // Push to application preferences
    public static final String EMACS_PATH = "emacsPath";
    public static final String EMACS_ADDITIONAL_PARAMETERS = "emacsParameters";

    /* contents of the defaults HashMap that are defined in this class.
     * There are more default parameters in this map which belong to separate preference classes.
    */
    public static final String TEXSTUDIO_PATH = "TeXstudioPath";
    public static final String WIN_EDT_PATH = "winEdtPath";
    public static final String TEXMAKER_PATH = "texmakerPath";
    public static final String VIM_SERVER = "vimServer";
    public static final String VIM = "vim";
    public static final String LYXPIPE = "lyxpipe";
    public static final String EXTERNAL_FILE_TYPES = "externalFileTypes";
    public static final String FONT_FAMILY = "fontFamily";
    public static final String WIN_LOOK_AND_FEEL = "lookAndFeel";
    public static final String FX_FONT_RENDERING_TWEAK = "fxFontRenderingTweak";
    public static final String LANGUAGE = "language";
    public static final String NAMES_LAST_ONLY = "namesLastOnly";
    public static final String ABBR_AUTHOR_NAMES = "abbrAuthorNames";
    public static final String NAMES_NATBIB = "namesNatbib";
    public static final String NAMES_FIRST_LAST = "namesFf";
    public static final String BIBLATEX_DEFAULT_MODE = "biblatexMode";
    public static final String NAMES_AS_IS = "namesAsIs";
    public static final String ENTRY_EDITOR_HEIGHT = "entryEditorHeight";
    public static final String AUTO_RESIZE_MODE = "autoResizeMode";
    public static final String WINDOW_MAXIMISED = "windowMaximised";
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
    public static final String DEFAULT_AUTO_SORT = "defaultAutoSort";
    public static final String DEFAULT_SHOW_SOURCE = "defaultShowSource";
    // Window sizes
    public static final String SIZE_Y = "mainWindowSizeY";
    public static final String SIZE_X = "mainWindowSizeX";
    public static final String POS_Y = "mainWindowPosY";
    public static final String POS_X = "mainWindowPosX";
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
    public static final String TERMS_SIZE_Y = "termsSizeY";
    public static final String TERMS_SIZE_X = "termsSizeX";
    public static final String TERMS_POS_Y = "termsPosY";
    public static final String TERMS_POS_X = "termsPosX";
    public static final String SEARCH_DIALOG_HEIGHT = "searchDialogHeight";
    public static final String SEARCH_DIALOG_WIDTH = "searchDialogWidth";
    public static final String IMPORT_INSPECTION_DIALOG_HEIGHT = "importInspectionDialogHeight";
    public static final String IMPORT_INSPECTION_DIALOG_WIDTH = "importInspectionDialogWidth";
    public static final String LAST_EDITED = "lastEdited";
    public static final String OPEN_LAST_EDITED = "openLastEdited";
    public static final String LAST_FOCUSED = "lastFocused";
    public static final String BACKUP = "backup";
    public static final String AUTO_OPEN_FORM = "autoOpenForm";
    public static final String IMPORT_WORKING_DIRECTORY = "importWorkingDirectory";
    public static final String EXPORT_WORKING_DIRECTORY = "exportWorkingDirectory";
    public static final String PREFS_EXPORT_PATH = "prefsExportPath";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String NUMBER_COL_WIDTH = "numberColWidth";
    public static final String EDITOR_EMACS_KEYBINDINGS = "editorEMACSkeyBindings";
    public static final String EDITOR_EMACS_KEYBINDINGS_REBIND_CA = "editorEMACSkeyBindingsRebindCA";
    public static final String EDITOR_EMACS_KEYBINDINGS_REBIND_CF = "editorEMACSkeyBindingsRebindCF";
    public static final String GROUPS_DEFAULT_FIELD = "groupsDefaultField";
    public static final String GROUP_INTERSECT_SELECTIONS = "groupIntersectSelections";
    public static final String KEYWORD_SEPARATOR = "groupKeywordSeparator";
    public static final String AUTO_ASSIGN_GROUP = "autoAssignGroup";
    public static final String LIST_OF_FILE_COLUMNS = "listOfFileColumns";
    public static final String EXTRA_FILE_COLUMNS = "extraFileColumns";
    public static final String ARXIV_COLUMN = "arxivColumn";
    public static final String FILE_COLUMN = "fileColumn";
    public static final String PREFER_URL_DOI = "preferUrlDoi";
    public static final String URL_COLUMN = "urlColumn";
    // Colors
    public static final String TABLE_COLOR_CODES_ON = "tableColorCodesOn";
    public static final String TABLE_RESOLVED_COLOR_CODES_ON = "tableResolvedColorCodesOn";
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
    public static final String MARKED_ENTRY_BACKGROUND = "markedEntryBackground";
    public static final String TABLE_RESOLVED_FIELD_BACKGROUND = "tableResolvedFieldBackground";
    public static final String TABLE_BACKGROUND = "tableBackground";
    public static final String ICON_ENABLED_COLOR = "iconEnabledColor";
    public static final String ICON_DISABLED_COLOR = "iconDisabledColor";
    public static final String TABLE_SHOW_GRID = "tableShowGrid";
    public static final String TABLE_ROW_PADDING = "tableRowPadding";
    public static final String MENU_FONT_SIZE = "menuFontSize";
    public static final String OVERRIDE_DEFAULT_FONTS = "overrideDefaultFonts";
    public static final String FONT_SIZE = "fontSize";
    public static final String FONT_STYLE = "fontStyle";
    public static final String ICON_SIZE_LARGE = "iconSizeLarge";
    public static final String ICON_SIZE_SMALL = "iconSizeSmall";
    public static final String RECENT_DATABASES = "recentDatabases";
    public static final String RENAME_ON_MOVE_FILE_TO_FILE_DIR = "renameOnMoveFileToFileDir";
    public static final String MEMORY_STICK_MODE = "memoryStickMode";
    public static final String DEFAULT_OWNER = "defaultOwner";
    public static final String DEFAULT_ENCODING = "defaultEncoding";
    public static final String TOOLBAR_VISIBLE = "toolbarVisible";
    // Timestamp preferences
    public static final String USE_TIME_STAMP = "useTimeStamp";
    public static final String UPDATE_TIMESTAMP = "updateTimestamp";
    public static final String TIME_STAMP_FIELD = "timeStampField";
    public static final String TIME_STAMP_FORMAT = "timeStampFormat";
    public static final String OVERWRITE_TIME_STAMP = "overwriteTimeStamp";

    public static final String WARN_ABOUT_DUPLICATES_IN_INSPECTION = "warnAboutDuplicatesInInspection";
    public static final String UNMARK_ALL_ENTRIES_BEFORE_IMPORTING = "unmarkAllEntriesBeforeImporting";
    public static final String MARK_IMPORTED_ENTRIES = "markImportedEntries";
    public static final String GENERATE_KEYS_AFTER_INSPECTION = "generateKeysAfterInspection";
    public static final String NON_WRAPPABLE_FIELDS = "nonWrappableFields";
    public static final String RESOLVE_STRINGS_ALL_FIELDS = "resolveStringsAllFields";
    public static final String DO_NOT_RESOLVE_STRINGS_FOR = "doNotResolveStringsFor";
    public static final String MERGE_ENTRIES_DIFF_MODE = "mergeEntriesDiffMode";
    public static final String CUSTOM_EXPORT_FORMAT = "customExportFormat";
    public static final String CUSTOM_IMPORT_FORMAT = "customImportFormat";
    public static final String KEY_PATTERN_REGEX = "KeyPatternRegex";
    public static final String KEY_PATTERN_REPLACEMENT = "KeyPatternReplacement";
    public static final String CONSOLE_COMMAND = "consoleCommand";
    public static final String USE_DEFAULT_CONSOLE_APPLICATION = "useDefaultConsoleApplication";
    public static final String ADOBE_ACROBAT_COMMAND = "adobeAcrobatCommand";
    public static final String SUMATRA_PDF_COMMAND = "sumatraCommand";
    public static final String USE_PDF_READER = "usePDFReader";
    // Currently, it is not possible to specify defaults for specific entry types
    // When this should be made possible, the code to inspect is org.jabref.gui.preftabs.BibtexKeyPatternPrefTab.storeSettings() -> LabelPattern keypatterns = getCiteKeyPattern(); etc
    public static final String DEFAULT_BIBTEX_KEY_PATTERN = "defaultBibtexKeyPattern";
    public static final String GRAY_OUT_NON_HITS = "grayOutNonHits";
    public static final String CONFIRM_DELETE = "confirmDelete";
    public static final String WARN_BEFORE_OVERWRITING_KEY = "warnBeforeOverwritingKey";
    public static final String AVOID_OVERWRITING_KEY = "avoidOverwritingKey";
    public static final String OVERWRITE_OWNER = "overwriteOwner";
    public static final String USE_OWNER = "useOwner";
    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";
    public static final String SHOW_FILE_LINKS_UPGRADE_WARNING = "showFileLinksUpgradeWarning";
    public static final String SIDE_PANE_WIDTH = "sidePaneWidth";
    public static final String LAST_USED_EXPORT = "lastUsedExport";
    public static final String FLOAT_MARKED_ENTRIES = "floatMarkedEntries";
    public static final String CITE_COMMAND = "citeCommand";
    public static final String GENERATE_KEYS_BEFORE_SAVING = "generateKeysBeforeSaving";
    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";
    public static final String KEY_GEN_ALWAYS_ADD_LETTER = "keyGenAlwaysAddLetter";
    public static final String KEY_GEN_FIRST_LETTER_A = "keyGenFirstLetterA";
    public static final String ENFORCE_LEGAL_BIBTEX_KEY = "enforceLegalBibtexKey";
    public static final String LOCAL_AUTO_SAVE = "localAutoSave";
    public static final String RUN_AUTOMATIC_FILE_SEARCH = "runAutomaticFileSearch";
    public static final String NUMERIC_FIELDS = "numericFields";
    public static final String AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String AUTOLINK_USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    public static final String BIB_LOC_AS_PRIMARY_DIR = "bibLocAsPrimaryDir";
    public static final String SELECTED_FETCHER_INDEX = "selectedFetcherIndex";
    public static final String WEB_SEARCH_VISIBLE = "webSearchVisible";
    public static final String GROUP_SIDEPANE_VISIBLE = "groupSidepaneVisible";
    public static final String ALLOW_FILE_AUTO_OPEN_BROWSE = "allowFileAutoOpenBrowse";
    public static final String CUSTOM_TAB_NAME = "customTabName_";
    public static final String CUSTOM_TAB_FIELDS = "customTabFields_";
    public static final String USE_UNIT_FORMATTER_ON_SEARCH = "useUnitFormatterOnSearch";
    public static final String USE_CASE_KEEPER_ON_SEARCH = "useCaseKeeperOnSearch";
    public static final String ASK_AUTO_NAMING_PDFS_AGAIN = "AskAutoNamingPDFsAgain";
    public static final String CLEANUP_DOI = "CleanUpDOI";
    public static final String CLEANUP_ISSN = "CleanUpISSN";
    public static final String CLEANUP_MOVE_PDF = "CleanUpMovePDF";
    public static final String CLEANUP_MAKE_PATHS_RELATIVE = "CleanUpMakePathsRelative";
    public static final String CLEANUP_RENAME_PDF = "CleanUpRenamePDF";
    public static final String CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS = "CleanUpRenamePDFonlyRelativePaths";
    public static final String CLEANUP_UPGRADE_EXTERNAL_LINKS = "CleanUpUpgradeExternalLinks";
    public static final String CLEANUP_CONVERT_TO_BIBLATEX = "CleanUpConvertToBiblatex";
    public static final String CLEANUP_CONVERT_TO_BIBTEX = "CleanUpConvertToBibtex";
    public static final String CLEANUP_FIX_FILE_LINKS = "CleanUpFixFileLinks";
    public static final String CLEANUP_FORMATTERS = "CleanUpFormatters";
    public static final String IMPORT_DEFAULT_PDF_IMPORT_STYLE = "importDefaultPDFimportStyle";
    public static final String IMPORT_ALWAYSUSE = "importAlwaysUsePDFImportStyle";
    public static final String IMPORT_FILENAMEPATTERN = "importFileNamePattern";
    public static final String IMPORT_FILEDIRPATTERN = "importFileDirPattern";
    public static final String NAME_FORMATTER_VALUE = "nameFormatterFormats";
    public static final String NAME_FORMATER_KEY = "nameFormatterNames";
    public static final String PUSH_TO_APPLICATION = "pushToApplication";
    public static final String SHOW_RECOMMENDATIONS = "showRecommendations";
    public static final String VALIDATE_IN_ENTRY_EDITOR = "validateInEntryEditor";
    // Dropped file handler
    public static final String DROPPEDFILEHANDLER_RENAME = "DroppedFileHandler_RenameFile";
    public static final String DROPPEDFILEHANDLER_MOVE = "DroppedFileHandler_MoveFile";
    public static final String DROPPEDFILEHANDLER_COPY = "DroppedFileHandler_CopyFile";
    public static final String DROPPEDFILEHANDLER_LEAVE = "DroppedFileHandler_LeaveFileInDir";
    // Remote
    public static final String USE_REMOTE_SERVER = "useRemoteServer";
    public static final String REMOTE_SERVER_PORT = "remoteServerPort";

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
    // Special field preferences
    public static final String SHOWCOLUMN_RELEVANCE = "showRelevanceColumn";
    public static final String SHOWCOLUMN_READ = "showReadColumn";
    public static final String SHOWCOLUMN_RANKING = "showRankingColumn";
    public static final String SHOWCOLUMN_QUALITY = "showQualityColumn";
    public static final String SHOWCOLUMN_PRIORITY = "showPriorityColumn";
    public static final String SHOWCOLUMN_PRINTED = "showPrintedColumn";
    public static final String SPECIALFIELDSENABLED = "specialFieldsEnabled";
    // The choice between AUTOSYNCSPECIALFIELDSTOKEYWORDS and SERIALIZESPECIALFIELDS is mutually exclusive
    public static final String SERIALIZESPECIALFIELDS = "serializeSpecialFields";
    // The choice between AUTOSYNCSPECIALFIELDSTOKEYWORDS and SERIALIZESPECIALFIELDS is mutually exclusive
    // At least in the settings, not in the implementation. But having both confused the users, therefore, having activated both options at the same time has been disabled
    public static final String AUTOSYNCSPECIALFIELDSTOKEYWORDS = "autoSyncSpecialFieldsToKeywords";
    // Prefs node for BibtexKeyPatterns
    public static final String BIBTEX_KEY_PATTERNS_NODE = "bibtexkeypatterns";
    // Prefs node for customized entry types
    public static final String CUSTOMIZED_BIBTEX_TYPES = "customizedBibtexTypes";
    public static final String CUSTOMIZED_BIBLATEX_TYPES = "customizedBiblatexTypes";
    // Version
    public static final String VERSION_IGNORED_UPDATE = "versionIgnoreUpdate";
    //KeyBindings - keys - public because needed for pref migration
    public static final String BINDINGS = "bindings";

    //AutcompleteFields - public because needed for pref migration
    public static final String AUTOCOMPLETER_COMPLETE_FIELDS = "autoCompleteFields";

    // Id Entry Generator Preferences
    public static final String ID_ENTRY_GENERATOR = "idEntryGenerator";

    // Auto completion
    private static final String AUTO_COMPLETE = "autoComplete";
    private static final String AUTOCOMPLETER_FIRSTNAME_MODE = "autoCompFirstNameMode";
    private static final String AUTOCOMPLETER_LAST_FIRST = "autoCompLF";
    private static final String AUTOCOMPLETER_FIRST_LAST = "autoCompFF";

    private static final String BIND_NAMES = "bindNames";
    // User
    private static final String USER_ID = "userId";
    private static final String EXTERNAL_JOURNAL_LISTS = "externalJournalLists";
    private static final String PERSONAL_JOURNAL_LIST = "personalJournalList";
    private static final String USE_IEEE_ABRV = "useIEEEAbrv";

    // Telemetry collection
    private static final String COLLECT_TELEMETRY = "collectTelemetry";
    private static final String ALREADY_ASKED_TO_COLLECT_TELEMETRY = "askedCollectTelemetry";
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefPreferences.class);
    private static final Class PREFS_BASE_CLASS = JabRefMain.class;
    private static final String DB_CONNECT_USERNAME = "dbConnectUsername";
    private static final String DB_CONNECT_DATABASE = "dbConnectDatabase";
    private static final String DB_CONNECT_HOSTNAME = "dbConnectHostname";
    private static final String DB_CONNECT_SERVER_TYPE = "dbConnectServerType";
    private static final String PROTECTED_TERMS_ENABLED_EXTERNAL = "protectedTermsEnabledExternal";
    private static final String PROTECTED_TERMS_DISABLED_EXTERNAL = "protectedTermsDisabledExternal";
    private static final String PROTECTED_TERMS_ENABLED_INTERNAL = "protectedTermsEnabledInternal";
    private static final String PROTECTED_TERMS_DISABLED_INTERNAL = "protectedTermsDisabledInternal";
    // Preview
    private static final String CYCLE_PREVIEW_POS = "cyclePreviewPos";
    private static final String CYCLE_PREVIEW = "cyclePreview";
    private static final String PREVIEW_PANEL_HEIGHT = "previewPanelHeight";
    private static final String PREVIEW_STYLE = "previewStyle";
    private static final String PREVIEW_ENABLED = "previewEnabled";

    // Helper string
    private static final String USER_HOME = System.getProperty("user.home");
    // solves the issue java.lang.RuntimeException: Internal graphics not initialized yet
    private final static Integer UNSET_MENU_FONT_SIZE = -123;
    // The only instance of this class:
    private static JabRefPreferences singleton;
    /**
     * HashMap that contains all preferences which are set by default
     */
    public final Map<String, Object> defaults = new HashMap<>();
    public final String MARKING_WITH_NUMBER_PATTERN;
    // Object containing custom export formats:
    public final CustomExportList customExports;
    /**
     * Set with all custom {@link org.jabref.logic.importer.Importer}s
     */
    public final CustomImportList customImports;
    // The following field is used as a global variable during the export of a database.
    // By setting this field to the path of the database's default file directory, formatters
    // that should resolve external file paths can access this field. This is an ugly hack
    // to solve the problem of formatters not having access to any context except for the
    // string to be formatted and possible formatter arguments.
    public List<String> fileDirForDatabase;
    private final Preferences prefs;
    private GlobalBibtexKeyPattern keyPattern;
    // Object containing info about customized entry editor tabs.
    private EntryEditorTabList tabList;

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
        prefs = Preferences.userNodeForPackage(PREFS_BASE_CLASS);

        // Since some of the preference settings themselves use localized strings, we cannot set the language after
        // the initialization of the preferences in main
        // Otherwise that language framework will be instantiated and more importantly, statically initialized preferences
        // like the SearchDisplayMode will never be translated.
        Localization.setLanguage(prefs.get(LANGUAGE, "en"));

        SearchPreferences.putDefaults(defaults);

        defaults.put(TEXMAKER_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("texmaker", "Texmaker"));
        defaults.put(WIN_EDT_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("WinEdt", "WinEdt Team\\WinEdt"));
        defaults.put(TEXSTUDIO_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("texstudio", "TeXstudio"));

        defaults.put(BIBLATEX_DEFAULT_MODE, Boolean.FALSE);

        // Set DOI to be the default ID entry generator
        defaults.put(ID_ENTRY_GENERATOR, DoiFetcher.NAME);


        if (OS.OS_X) {
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(WIN_LOOK_AND_FEEL, UIManager.getSystemLookAndFeelClassName());
            defaults.put(EMACS_PATH, "emacsclient");
        } else if (OS.WINDOWS) {
            defaults.put(WIN_LOOK_AND_FEEL, "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            defaults.put(EMACS_PATH, "emacsclient.exe");
        } else {
            // Linux
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(WIN_LOOK_AND_FEEL, "javax.swing.plaf.nimbus.NimbusLookAndFeel");
            defaults.put(EMACS_PATH, "emacsclient");
        }

        defaults.put(FX_FONT_RENDERING_TWEAK, OS.LINUX); //we turn this on per default on Linux
        defaults.put(EMACS_ADDITIONAL_PARAMETERS, "-n -e");

        defaults.put(PUSH_TO_APPLICATION, "TeXstudio");

        defaults.put(RECENT_DATABASES, "");
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
        defaults.put(ENTRY_EDITOR_HEIGHT, 400);
        defaults.put(TABLE_COLOR_CODES_ON, Boolean.FALSE);
        defaults.put(TABLE_RESOLVED_COLOR_CODES_ON, Boolean.FALSE);
        defaults.put(NAMES_AS_IS, Boolean.FALSE); // "Show names unchanged"
        defaults.put(NAMES_FIRST_LAST, Boolean.FALSE); // "Show 'Firstname Lastname'"
        defaults.put(NAMES_NATBIB, Boolean.TRUE); // "Natbib style"
        defaults.put(ABBR_AUTHOR_NAMES, Boolean.TRUE); // "Abbreviate names"
        defaults.put(NAMES_LAST_ONLY, Boolean.TRUE); // "Show last names only"
        // system locale as default
        defaults.put(LANGUAGE, Locale.getDefault().getLanguage());

        // Sorting preferences
        defaults.put(TABLE_PRIMARY_SORT_FIELD, FieldName.AUTHOR);
        defaults.put(TABLE_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(TABLE_SECONDARY_SORT_FIELD, FieldName.YEAR);
        defaults.put(TABLE_SECONDARY_SORT_DESCENDING, Boolean.TRUE);
        defaults.put(TABLE_TERTIARY_SORT_FIELD, FieldName.TITLE);
        defaults.put(TABLE_TERTIARY_SORT_DESCENDING, Boolean.FALSE);

        defaults.put(REFORMAT_FILE_ON_SAVE_AND_EXPORT, Boolean.FALSE);

        // export order
        defaults.put(EXPORT_IN_ORIGINAL_ORDER, Boolean.FALSE);
        defaults.put(EXPORT_IN_SPECIFIED_ORDER, Boolean.FALSE);

        // export order: if EXPORT_IN_SPECIFIED_ORDER, then use following criteria
        defaults.put(EXPORT_PRIMARY_SORT_FIELD, BibEntry.KEY_FIELD);
        defaults.put(EXPORT_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(EXPORT_SECONDARY_SORT_FIELD, FieldName.AUTHOR);
        defaults.put(EXPORT_SECONDARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(EXPORT_TERTIARY_SORT_FIELD, FieldName.TITLE);
        defaults.put(EXPORT_TERTIARY_SORT_DESCENDING, Boolean.TRUE);

        defaults.put(NEWLINE, System.lineSeparator());

        defaults.put(SIDE_PANE_COMPONENT_NAMES, "");
        defaults.put(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, "");

        defaults.put(COLUMN_NAMES, "entrytype;author/editor;title;year;journal/booktitle;bibtexkey");
        defaults.put(COLUMN_WIDTHS, "75;300;470;60;130;100");
        defaults.put(XMP_PRIVACY_FILTERS, "pdf;timestamp;keywords;owner;note;review");
        defaults.put(USE_XMP_PRIVACY_FILTER, Boolean.FALSE);
        defaults.put(NUMBER_COL_WIDTH, 32);
        defaults.put(WORKING_DIRECTORY, USER_HOME);
        defaults.put(EXPORT_WORKING_DIRECTORY, USER_HOME);
        // Remembers working directory of last import
        defaults.put(IMPORT_WORKING_DIRECTORY, USER_HOME);
        defaults.put(PREFS_EXPORT_PATH, USER_HOME);
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
        defaults.put(TERMS_POS_X, 0);
        defaults.put(TERMS_POS_Y, 0);
        defaults.put(TERMS_SIZE_X, 500);
        defaults.put(TERMS_SIZE_Y, 500);
        defaults.put(DEFAULT_SHOW_SOURCE, Boolean.FALSE);

        defaults.put(DEFAULT_AUTO_SORT, Boolean.FALSE);

        defaults.put(MERGE_ENTRIES_DIFF_MODE, 2);

        defaults.put(SHOW_RECOMMENDATIONS, Boolean.TRUE);
        defaults.put(VALIDATE_IN_ENTRY_EDITOR, Boolean.TRUE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS, Boolean.FALSE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CA, Boolean.TRUE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CF, Boolean.TRUE);
        defaults.put(AUTO_COMPLETE, Boolean.FALSE);
        defaults.put(AUTOCOMPLETER_FIRSTNAME_MODE, AutoCompleteFirstNameMode.BOTH.name());
        defaults.put(AUTOCOMPLETER_FIRST_LAST, Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put(AUTOCOMPLETER_LAST_FIRST, Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(AUTOCOMPLETER_COMPLETE_FIELDS, "author;editor;title;journal;publisher;keywords;crossref;related;entryset");
        defaults.put(GROUP_INTERSECT_SELECTIONS, Boolean.FALSE);
        defaults.put(GROUPS_DEFAULT_FIELD, FieldName.KEYWORDS);
        defaults.put(AUTO_ASSIGN_GROUP, Boolean.TRUE);
        defaults.put(KEYWORD_SEPARATOR, ", ");
        defaults.put(TOOLBAR_VISIBLE, Boolean.TRUE);
        defaults.put(DEFAULT_ENCODING, StandardCharsets.UTF_8.name());
        defaults.put(DEFAULT_OWNER, System.getProperty("user.name"));
        defaults.put(MEMORY_STICK_MODE, Boolean.FALSE);
        defaults.put(RENAME_ON_MOVE_FILE_TO_FILE_DIR, Boolean.TRUE);

        defaults.put(FONT_STYLE, Font.PLAIN);
        defaults.put(FONT_SIZE, 12);
        defaults.put(OVERRIDE_DEFAULT_FONTS, Boolean.FALSE);
        defaults.put(MENU_FONT_SIZE, UNSET_MENU_FONT_SIZE);
        defaults.put(ICON_SIZE_LARGE, 24);
        defaults.put(ICON_SIZE_SMALL, 16);
        defaults.put(TABLE_ROW_PADDING, 9);
        defaults.put(TABLE_SHOW_GRID, Boolean.FALSE);
        // Main table color settings:
        defaults.put(TABLE_BACKGROUND, "255:255:255");
        defaults.put(TABLE_REQ_FIELD_BACKGROUND, "230:235:255");
        defaults.put(TABLE_OPT_FIELD_BACKGROUND, "230:255:230");
        defaults.put(TABLE_RESOLVED_FIELD_BACKGROUND, "240:240:240");
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

        // default icon colors
        defaults.put(ICON_ENABLED_COLOR, "79:95:143");
        defaults.put(ICON_DISABLED_COLOR, "200:200:200");

        defaults.put(INCOMPLETE_ENTRY_BACKGROUND, "250:175:175");

        defaults.put(URL_COLUMN, Boolean.TRUE);
        defaults.put(PREFER_URL_DOI, Boolean.FALSE);
        defaults.put(FILE_COLUMN, Boolean.TRUE);
        defaults.put(ARXIV_COLUMN, Boolean.FALSE);

        defaults.put(EXTRA_FILE_COLUMNS, Boolean.FALSE);
        defaults.put(LIST_OF_FILE_COLUMNS, "");

        defaults.put(PROTECTED_TERMS_ENABLED_INTERNAL, convertListToString(ProtectedTermsLoader.getInternalLists()));
        defaults.put(PROTECTED_TERMS_DISABLED_INTERNAL, "");
        defaults.put(PROTECTED_TERMS_ENABLED_EXTERNAL, "");
        defaults.put(PROTECTED_TERMS_DISABLED_EXTERNAL, "");

        // OpenOffice/LibreOffice
        if (OS.WINDOWS) {
            defaults.put(OO_PATH, OpenOfficePreferences.DEFAULT_WINDOWS_PATH);
            defaults.put(OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_WIN_EXEC_PATH);
            defaults.put(OO_JARS_PATH, OpenOfficePreferences.DEFAULT_WINDOWS_PATH);
        } else if (OS.OS_X) {
            defaults.put(OO_PATH, OpenOfficePreferences.DEFAULT_OSX_PATH);
            defaults.put(OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_OSX_EXEC_PATH);
            defaults.put(OO_JARS_PATH, OpenOfficePreferences.DEFAULT_OSX_PATH);
        } else { // Linux
            defaults.put(OO_PATH, OpenOfficePreferences.DEFAULT_LINUX_PATH);
            defaults.put(OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_LINUX_EXEC_PATH);
            defaults.put(OO_JARS_PATH, OpenOfficePreferences.DEFAULT_LINUX_PATH);
        }

        defaults.put(OO_SYNC_WHEN_CITING, Boolean.FALSE);
        defaults.put(OO_SHOW_PANEL, Boolean.FALSE);
        defaults.put(OO_USE_ALL_OPEN_BASES, Boolean.TRUE);
        defaults.put(OO_BIBLIOGRAPHY_STYLE_FILE, StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        defaults.put(OO_EXTERNAL_STYLE_FILES, "");
        defaults.put(STYLES_POS_X, 0);
        defaults.put(STYLES_POS_Y, 0);
        defaults.put(STYLES_SIZE_X, 600);
        defaults.put(STYLES_SIZE_Y, 400);

        defaults.put(SPECIALFIELDSENABLED, Boolean.TRUE);
        defaults.put(SHOWCOLUMN_PRIORITY, Boolean.FALSE);
        defaults.put(SHOWCOLUMN_QUALITY, Boolean.FALSE);
        defaults.put(SHOWCOLUMN_RANKING, Boolean.TRUE);
        defaults.put(SHOWCOLUMN_RELEVANCE, Boolean.FALSE);
        defaults.put(SHOWCOLUMN_PRINTED, Boolean.FALSE);
        defaults.put(SHOWCOLUMN_READ, Boolean.FALSE);
        defaults.put(AUTOSYNCSPECIALFIELDSTOKEYWORDS, Boolean.TRUE);
        defaults.put(SERIALIZESPECIALFIELDS, Boolean.FALSE);

        defaults.put(USE_OWNER, Boolean.FALSE);
        defaults.put(OVERWRITE_OWNER, Boolean.FALSE);
        defaults.put(AVOID_OVERWRITING_KEY, Boolean.FALSE);
        defaults.put(WARN_BEFORE_OVERWRITING_KEY, Boolean.TRUE);
        defaults.put(CONFIRM_DELETE, Boolean.TRUE);
        defaults.put(GRAY_OUT_NON_HITS, Boolean.TRUE);
        defaults.put(DEFAULT_BIBTEX_KEY_PATTERN, "[auth][year]");
        defaults.put(DO_NOT_RESOLVE_STRINGS_FOR, FieldName.URL);
        defaults.put(RESOLVE_STRINGS_ALL_FIELDS, Boolean.FALSE);
        defaults.put(NON_WRAPPABLE_FIELDS, "pdf;ps;url;doi;file;isbn;issn");
        defaults.put(GENERATE_KEYS_AFTER_INSPECTION, Boolean.TRUE);
        defaults.put(MARK_IMPORTED_ENTRIES, Boolean.TRUE);
        defaults.put(UNMARK_ALL_ENTRIES_BEFORE_IMPORTING, Boolean.TRUE);
        defaults.put(WARN_ABOUT_DUPLICATES_IN_INSPECTION, Boolean.TRUE);
        defaults.put(USE_TIME_STAMP, Boolean.FALSE);
        defaults.put(OVERWRITE_TIME_STAMP, Boolean.FALSE);

        // default time stamp follows ISO-8601. Reason: https://xkcd.com/1179/
        defaults.put(TIME_STAMP_FORMAT, "yyyy-MM-dd");

        defaults.put(TIME_STAMP_FIELD, FieldName.TIMESTAMP);
        defaults.put(UPDATE_TIMESTAMP, Boolean.FALSE);

        defaults.put(GENERATE_KEYS_BEFORE_SAVING, Boolean.FALSE);

        defaults.put(USE_REMOTE_SERVER, Boolean.TRUE);
        defaults.put(REMOTE_SERVER_PORT, 6050);

        defaults.put(PERSONAL_JOURNAL_LIST, "");
        defaults.put(EXTERNAL_JOURNAL_LISTS, "");
        defaults.put(CITE_COMMAND, "\\cite"); // obsoleted by the app-specific ones (not any more?)
        defaults.put(FLOAT_MARKED_ENTRIES, Boolean.TRUE);

        defaults.put(LAST_USED_EXPORT, "");
        defaults.put(SIDE_PANE_WIDTH, -1);

        defaults.put(IMPORT_INSPECTION_DIALOG_WIDTH, 650);
        defaults.put(IMPORT_INSPECTION_DIALOG_HEIGHT, 650);
        defaults.put(SHOW_FILE_LINKS_UPGRADE_WARNING, Boolean.TRUE);
        defaults.put(AUTOLINK_EXACT_KEY_ONLY, Boolean.FALSE);
        defaults.put(NUMERIC_FIELDS, "mittnum;author");
        defaults.put(RUN_AUTOMATIC_FILE_SEARCH, Boolean.FALSE);
        defaults.put(LOCAL_AUTO_SAVE, Boolean.FALSE);
        defaults.put(ENFORCE_LEGAL_BIBTEX_KEY, Boolean.TRUE);
        // Curly brackets ({}) are the default delimiters, not quotes (") as these cause trouble when they appear within the field value:
        // Currently, JabRef does not escape them
        defaults.put(KEY_GEN_FIRST_LETTER_A, Boolean.TRUE);
        defaults.put(KEY_GEN_ALWAYS_ADD_LETTER, Boolean.FALSE);
        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
        defaults.put(OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.FALSE);
        defaults.put(ALLOW_FILE_AUTO_OPEN_BROWSE, Boolean.TRUE);
        defaults.put(WEB_SEARCH_VISIBLE, Boolean.FALSE);
        defaults.put(GROUP_SIDEPANE_VISIBLE, Boolean.FALSE);
        defaults.put(SELECTED_FETCHER_INDEX, 0);
        defaults.put(BIB_LOC_AS_PRIMARY_DIR, Boolean.FALSE);
        defaults.put(DB_CONNECT_SERVER_TYPE, "MySQL");
        defaults.put(DB_CONNECT_HOSTNAME, "localhost");
        defaults.put(DB_CONNECT_DATABASE, "jabref");
        defaults.put(DB_CONNECT_USERNAME, "root");
        defaults.put(COLLECT_TELEMETRY, Boolean.FALSE);
        defaults.put(ALREADY_ASKED_TO_COLLECT_TELEMETRY, Boolean.FALSE);

        defaults.put(ASK_AUTO_NAMING_PDFS_AGAIN, Boolean.TRUE);
        insertDefaultCleanupPreset(defaults);

        // defaults for DroppedFileHandler UI
        defaults.put(DROPPEDFILEHANDLER_LEAVE, Boolean.FALSE);
        defaults.put(DROPPEDFILEHANDLER_COPY, Boolean.TRUE);
        defaults.put(DROPPEDFILEHANDLER_MOVE, Boolean.FALSE);
        defaults.put(DROPPEDFILEHANDLER_RENAME, Boolean.FALSE);

        defaults.put(IMPORT_ALWAYSUSE, Boolean.FALSE);
        defaults.put(IMPORT_DEFAULT_PDF_IMPORT_STYLE, ImportSettingsTab.DEFAULT_STYLE);

        // use BibTeX key appended with filename as default pattern
        defaults.put(IMPORT_FILENAMEPATTERN, ImportSettingsTab.DEFAULT_FILENAMEPATTERNS[1]);
        //Default empty String to be backwards compatible
        defaults.put(IMPORT_FILEDIRPATTERN, "");

        customExports = new CustomExportList(new ExportComparator());
        customImports = new CustomImportList(this);

        MARKING_WITH_NUMBER_PATTERN = "\\[" + get(DEFAULT_OWNER).replaceAll("\\\\", "\\\\\\\\") + ":(\\d+)\\]";

        String defaultExpression = "**/.*[bibtexkey].*\\\\.[extension]";
        defaults.put(AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(AUTOLINK_USE_REG_EXP_SEARCH_KEY, Boolean.FALSE);
        defaults.put(USE_IEEE_ABRV, Boolean.FALSE);
        defaults.put(USE_CASE_KEEPER_ON_SEARCH, Boolean.TRUE);
        defaults.put(USE_UNIT_FORMATTER_ON_SEARCH, Boolean.TRUE);

        defaults.put(USE_DEFAULT_CONSOLE_APPLICATION, Boolean.TRUE);
        if (OS.WINDOWS) {
            defaults.put(CONSOLE_COMMAND, "C:\\Program Files\\ConEmu\\ConEmu64.exe /single /dir \"%DIR\"");
            defaults.put(ADOBE_ACROBAT_COMMAND, "C:\\Program Files (x86)\\Adobe\\Acrobat Reader DC\\Reader");
            defaults.put(SUMATRA_PDF_COMMAND, "C:\\Program Files\\SumatraPDF");
            defaults.put(USE_PDF_READER, ADOBE_ACROBAT_COMMAND);
        } else {
            defaults.put(CONSOLE_COMMAND, "");
            defaults.put(ADOBE_ACROBAT_COMMAND, "");
            defaults.put(SUMATRA_PDF_COMMAND, "");
            defaults.put(USE_PDF_READER, "");
        }

        //versioncheck defaults
        defaults.put(VERSION_IGNORED_UPDATE, "");

        // preview
        defaults.put(CYCLE_PREVIEW, "Preview;" + CitationStyle.DEFAULT);
        defaults.put(CYCLE_PREVIEW_POS, 0);
        defaults.put(PREVIEW_PANEL_HEIGHT, 200);
        defaults.put(PREVIEW_ENABLED, Boolean.TRUE);
        defaults.put(PREVIEW_STYLE,
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

        setLanguageDependentDefaultValues();
    }

    public static JabRefPreferences getInstance() {
        if (JabRefPreferences.singleton == null) {
            JabRefPreferences.singleton = new JabRefPreferences();
        }
        return JabRefPreferences.singleton;
    }

    private static String convertListToString(List<String> value) {
        return value.stream().map(val -> StringUtil.quote(val, ";", '\\')).collect(Collectors.joining(";"));
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

    private static Preferences getPrefsNodeForCustomizedEntryTypes(BibDatabaseMode mode) {
        if (mode == BibDatabaseMode.BIBLATEX) {
            return Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(CUSTOMIZED_BIBLATEX_TYPES);
        }
        if (mode == BibDatabaseMode.BIBTEX) {
            return Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(CUSTOMIZED_BIBTEX_TYPES);
        }

        throw new IllegalArgumentException("Unknown BibDatabaseMode: " + mode);
    }

    private static Optional<String> getNextUnit(Reader data) throws IOException {
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
            return Optional.of(res.toString());
        } else if (c == -1) {
            // end of stream
            return Optional.empty();
        } else {
            return Optional.of("");
        }
    }

    private static void insertDefaultCleanupPreset(Map<String, Object> storage) {
        EnumSet<CleanupPreset.CleanupStep> deactivatedJobs = EnumSet.of(
                CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS,
                CleanupPreset.CleanupStep.MOVE_PDF,
                CleanupPreset.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS,
                CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX,
                CleanupPreset.CleanupStep.CONVERT_TO_BIBTEX);

        CleanupPreset preset = new CleanupPreset(EnumSet.complementOf(deactivatedJobs), Cleanups.DEFAULT_SAVE_ACTIONS);

        storage.put(CLEANUP_DOI, preset.isCleanUpDOI());
        storage.put(CLEANUP_ISSN, preset.isCleanUpISSN());
        storage.put(CLEANUP_MOVE_PDF, preset.isMovePDF());
        storage.put(CLEANUP_MAKE_PATHS_RELATIVE, preset.isMakePathsRelative());
        storage.put(CLEANUP_RENAME_PDF, preset.isRenamePDF());
        storage.put(CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS, preset.isRenamePdfOnlyRelativePaths());
        storage.put(CLEANUP_UPGRADE_EXTERNAL_LINKS, preset.isCleanUpUpgradeExternalLinks());
        storage.put(CLEANUP_CONVERT_TO_BIBLATEX, preset.isConvertToBiblatex());
        storage.put(CLEANUP_CONVERT_TO_BIBTEX, preset.isConvertToBibtex());
        storage.put(CLEANUP_FIX_FILE_LINKS, preset.isFixFileLinks());
        storage.put(CLEANUP_FORMATTERS, convertListToString(preset.getFormatterCleanups().getAsStringList(OS.NEWLINE)));
    }

    public int getFontSizeFX() {
        // Decrease font size by 3 since JavaFX has default font size of 9, while Swing uses 12
        return getInt(MENU_FONT_SIZE) - 3;
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
        while (true) {
            // saved as CUSTOMTABNAME_def{number} and ; separated
            String fields = (String) defaults.get(CUSTOM_TAB_FIELDS + "_def" + defNumber);

            if ((fields == null) || fields.isEmpty()) {
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
        String fieldNames = InternalBibtexFields.DEFAULT_GENERAL_FIELDS.stream().collect(Collectors.joining(";"));
        defaults.put(CUSTOM_TAB_FIELDS + "_def0", fieldNames);

        // Entry editor tab 1:
        defaults.put(CUSTOM_TAB_FIELDS + "_def1", FieldName.ABSTRACT);
        defaults.put(CUSTOM_TAB_NAME + "_def1", Localization.lang("Abstract"));

        // Entry editor tab 2: Comments Field - used for research comments, etc.
        defaults.put(CUSTOM_TAB_FIELDS + "_def2", FieldName.COMMENT);
        defaults.put(CUSTOM_TAB_NAME + "_def2", Localization.lang("Comments"));

        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
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

    public Optional<String> getAsOptional(String key) {
        return Optional.ofNullable(prefs.get(key, (String) defaults.get(key)));
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
        if (key.equals(JabRefPreferences.MENU_FONT_SIZE)) {
            Integer menuFontSize = (Integer) defaults.get(key);
            if (menuFontSize.equals(UNSET_MENU_FONT_SIZE)) {
                menuFontSize = (int) javafx.scene.text.Font.getDefault().getSize();
                defaults.put(key, menuFontSize);
            }
            return menuFontSize;
        } else {
            return (Integer) defaults.get(key);
        }
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
        Optional<String> rs;
        try {
            while ((rs = getNextUnit(rd)).isPresent()) {
                res.add(rs.get());
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
     * Returns the default BibDatabase mode, which can be either BIBTEX or BIBLATEX.
     *
     * @return the default BibDatabaseMode
     */
    public BibDatabaseMode getDefaultBibDatabaseMode() {
        if (getBoolean(BIBLATEX_DEFAULT_MODE)) {
            return BibDatabaseMode.BIBLATEX;
        } else {
            return BibDatabaseMode.BIBTEX;
        }
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
     * Clear all preferences.
     *
     * @throws BackingStoreException
     */
    public void clear() throws BackingStoreException {
        clearAllCustomEntryTypes();
        clearKeyPatterns();
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
            LOGGER.warn("Cannot communicate with backing store", ex);
        }
    }

    /**
     * Fetches key patterns from preferences.
     * The implementation doesn't cache the results
     *
     * @return LabelPattern containing all keys. Returned LabelPattern has no parent
     */
    public GlobalBibtexKeyPattern getKeyPattern() {
        keyPattern = GlobalBibtexKeyPattern.fromPattern(get(DEFAULT_BIBTEX_KEY_PATTERN));
        Preferences pre = Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(BIBTEX_KEY_PATTERNS_NODE);
        try {
            String[] keys = pre.keys();
            if (keys.length > 0) {
                for (String key : keys) {
                    keyPattern.addBibtexKeyPattern(key, pre.get(key, null));
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
    public void putKeyPattern(GlobalBibtexKeyPattern pattern) {
        keyPattern = pattern;

        // Store overridden definitions to Preferences.
        Preferences pre = Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(BIBTEX_KEY_PATTERNS_NODE);
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
                // see org.jabref.logic.labelPattern.BibtexKeyGenerator.split(String)
                pre.put(key, pattern.getValue(key).get(0));
            }
        }
    }

    private void clearKeyPatterns() throws BackingStoreException {
        Preferences pre = Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(BIBTEX_KEY_PATTERNS_NODE);
        pre.clear();
    }

    public void storeCustomEntryTypes(List<CustomEntryType> customEntryTypes, BibDatabaseMode bibDatabaseMode) {
        Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode);

        try {
            // clear old custom types
            clearCustomEntryTypes(bibDatabaseMode);

            // store current custom types
            customEntryTypes.forEach(type -> prefsNode.put(type.getName(), type.getAsString()));

            prefsNode.flush();
        } catch (BackingStoreException e) {
            LOGGER.info("Updating stored custom entry types failed.", e);
        }
    }

    public List<CustomEntryType> loadCustomEntryTypes(BibDatabaseMode bibDatabaseMode) {
        List<CustomEntryType> storedEntryTypes = new ArrayList<>();
        Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode);
        try {
            Arrays.stream(prefsNode.keys())
                    .map(key -> prefsNode.get(key, null))
                    .filter(Objects::nonNull)
                    .forEach(typeString -> CustomEntryType.parse(typeString).ifPresent(storedEntryTypes::add));
        } catch (BackingStoreException e) {
            LOGGER.info("Parsing customized entry types failed.", e);
        }
        return storedEntryTypes;
    }

    private void clearAllCustomEntryTypes() throws BackingStoreException {
        for (BibDatabaseMode mode : BibDatabaseMode.values()) {
            clearCustomEntryTypes(mode);
        }
    }

    private void clearCustomEntryTypes(BibDatabaseMode mode) throws BackingStoreException {
        Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(mode);
        prefsNode.clear();
    }

    public Map<String, Object> getPreferences() {
        Map<String, Object> result = new HashMap<>();
        try {
            for (String key : this.prefs.keys()) {
                Object value = getObject(key);
                result.put(key, value);
            }
        } catch (BackingStoreException e) {
            LOGGER.info("could not retrieve preference keys", e);
        }
        return result;
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
            throw new JabRefException("Could not export preferences", Localization.lang("Could not export preferences"),
                    ex);
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

    public String getWrappedUsername() {
        return '[' + get(DEFAULT_OWNER) + ']';
    }

    public Charset getDefaultEncoding() {
        return Charset.forName(get(DEFAULT_ENCODING));
    }

    public void setDefaultEncoding(Charset encoding) {
        put(DEFAULT_ENCODING, encoding.name());
    }

    public FileHistory getFileHistory() {
        return new FileHistory(getStringList(RECENT_DATABASES));
    }

    public void storeFileHistory(FileHistory history) {
        if (!history.isEmpty()) {
            putStringList(RECENT_DATABASES, history.getHistory());
        }
    }

    public FileDirectoryPreferences getFileDirectoryPreferences() {
        List<String> fields = Arrays.asList(FieldName.FILE, FieldName.PDF, FieldName.PS);
        Map<String, String> fieldDirectories = new HashMap<>();
        fields.stream().forEach(
                fieldName -> fieldDirectories.put(fieldName, get(fieldName + FileDirectoryPreferences.DIR_SUFFIX)));
        return new FileDirectoryPreferences(getUser(), fieldDirectories,
                getBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR));
    }

    public UpdateFieldPreferences getUpdateFieldPreferences() {
        return new UpdateFieldPreferences(getBoolean(USE_OWNER), getBoolean(OVERWRITE_OWNER), get(DEFAULT_OWNER),
                getBoolean(USE_TIME_STAMP), getBoolean(OVERWRITE_TIME_STAMP), get(TIME_STAMP_FIELD),
                get(TIME_STAMP_FORMAT));
    }

    public LatexFieldFormatterPreferences getLatexFieldFormatterPreferences() {
        return new LatexFieldFormatterPreferences(getBoolean(RESOLVE_STRINGS_ALL_FIELDS),
                getStringList(DO_NOT_RESOLVE_STRINGS_FOR), getFieldContentParserPreferences());
    }

    public FieldContentParserPreferences getFieldContentParserPreferences() {
        return new FieldContentParserPreferences(getStringList(NON_WRAPPABLE_FIELDS));
    }

    public boolean isKeywordSyncEnabled() {
        return getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)
                && getBoolean(JabRefPreferences.AUTOSYNCSPECIALFIELDSTOKEYWORDS);
    }

    public ImportFormatPreferences getImportFormatPreferences() {
        return new ImportFormatPreferences(customImports, getDefaultEncoding(), getKeywordDelimiter(),
                getBibtexKeyPatternPreferences(), getFieldContentParserPreferences(),
                isKeywordSyncEnabled());
    }

    public SavePreferences loadForExportFromPreferences() {
        Boolean saveInOriginalOrder = this.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER);
        SaveOrderConfig saveOrder = null;
        if (!saveInOriginalOrder) {
            if (this.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
                saveOrder = this.loadExportSaveOrder();
            } else {
                saveOrder = this.loadTableSaveOrder();
            }
        }
        Charset encoding = this.getDefaultEncoding();
        Boolean makeBackup = this.getBoolean(JabRefPreferences.BACKUP);
        SavePreferences.DatabaseSaveType saveType = SavePreferences.DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = false;
        Boolean reformatFile = this.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = this.getLatexFieldFormatterPreferences();
        GlobalBibtexKeyPattern globalCiteKeyPattern =  this.getKeyPattern();
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile, latexFieldFormatterPreferences, globalCiteKeyPattern);
    }

    public SavePreferences loadForSaveFromPreferences() {
        Boolean saveInOriginalOrder = false;
        SaveOrderConfig saveOrder = null;
        Charset encoding = this.getDefaultEncoding();
        Boolean makeBackup = this.getBoolean(JabRefPreferences.BACKUP);
        SavePreferences.DatabaseSaveType saveType = SavePreferences.DatabaseSaveType.ALL;
        Boolean takeMetadataSaveOrderInAccount = true;
        Boolean reformatFile = this.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT);
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = this.getLatexFieldFormatterPreferences();
        GlobalBibtexKeyPattern globalCiteKeyPattern =  this.getKeyPattern();
        return new SavePreferences(saveInOriginalOrder, saveOrder, encoding, makeBackup, saveType,
                takeMetadataSaveOrderInAccount, reformatFile, latexFieldFormatterPreferences, globalCiteKeyPattern);
    }

    public ExporterFactory getExporterFactory(JournalAbbreviationLoader abbreviationLoader) {
        Map<String, TemplateExporter> customFormats = this.customExports.getCustomExportFormats(this, abbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = this.getLayoutFormatterPreferences(abbreviationLoader);
        SavePreferences savePreferences = this.loadForExportFromPreferences();
        XmpPreferences xmpPreferences = this.getXMPPreferences();
        return ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);
    }

    public BibtexKeyPatternPreferences getBibtexKeyPatternPreferences() {
        return new BibtexKeyPatternPreferences(get(KEY_PATTERN_REGEX),
                get(KEY_PATTERN_REPLACEMENT), getBoolean(KEY_GEN_ALWAYS_ADD_LETTER), getBoolean(KEY_GEN_FIRST_LETTER_A),
                getBoolean(ENFORCE_LEGAL_BIBTEX_KEY), getKeyPattern(), getKeywordDelimiter());
    }

    public TimestampPreferences getTimestampPreferences() {
        return new TimestampPreferences(getBoolean(USE_TIME_STAMP), getBoolean(UPDATE_TIMESTAMP), get(TIME_STAMP_FIELD), get(TIME_STAMP_FORMAT), getBoolean(OVERWRITE_TIME_STAMP));
    }

    public LayoutFormatterPreferences getLayoutFormatterPreferences(
            JournalAbbreviationLoader journalAbbreviationLoader) {
        Objects.requireNonNull(journalAbbreviationLoader);
        return new LayoutFormatterPreferences(getNameFormatterPreferences(), getJournalAbbreviationPreferences(),
                getFileLinkPreferences(), journalAbbreviationLoader);
    }

    public XmpPreferences getXMPPreferences() {
        return new XmpPreferences(getBoolean(USE_XMP_PRIVACY_FILTER), getStringList(XMP_PRIVACY_FILTERS),
                getKeywordDelimiter());
    }

    public OpenOfficePreferences getOpenOfficePreferences() {
        return new OpenOfficePreferences(
                this.get(JabRefPreferences.OO_JARS_PATH),
                this.get(JabRefPreferences.OO_EXECUTABLE_PATH),
                this.get(JabRefPreferences.OO_PATH),
                this.getBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES),
                this.getBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING),
                this.getBoolean(JabRefPreferences.OO_SHOW_PANEL),
                this.getStringList(JabRefPreferences.OO_EXTERNAL_STYLE_FILES),
                this.get(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE));
    }

    public void setOpenOfficePreferences(OpenOfficePreferences openOfficePreferences) {
        this.put(JabRefPreferences.OO_JARS_PATH, openOfficePreferences.getJarsPath());
        this.put(JabRefPreferences.OO_EXECUTABLE_PATH, openOfficePreferences.getExecutablePath());
        this.put(JabRefPreferences.OO_PATH, openOfficePreferences.getInstallationPath());
        this.putBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES, openOfficePreferences.getUseAllDatabases());
        this.putBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING, openOfficePreferences.getSyncWhenCiting());
        this.putBoolean(JabRefPreferences.OO_SHOW_PANEL, openOfficePreferences.getShowPanel());
        this.putStringList(JabRefPreferences.OO_EXTERNAL_STYLE_FILES, openOfficePreferences.getExternalStyles());
        this.put(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE, openOfficePreferences.getCurrentStyle());
    }

    private NameFormatterPreferences getNameFormatterPreferences() {
        return new NameFormatterPreferences(getStringList(NAME_FORMATER_KEY), getStringList(NAME_FORMATTER_VALUE));
    }

    public FileLinkPreferences getFileLinkPreferences() {
        return new FileLinkPreferences(
                Collections.singletonList(get(FieldName.FILE + FileDirectoryPreferences.DIR_SUFFIX)),
                fileDirForDatabase);
    }

    public JabRefPreferences storeVersionPreferences(VersionPreferences versionPreferences) {
        put(VERSION_IGNORED_UPDATE, versionPreferences.getIgnoredVersion().toString());
        return this;
    }

    public VersionPreferences getVersionPreferences() {
        Version ignoredVersion = Version.parse(get(VERSION_IGNORED_UPDATE));
        return new VersionPreferences(ignoredVersion);
    }

    public JabRefPreferences storePreviewPreferences(PreviewPreferences previewPreferences) {
        putInt(CYCLE_PREVIEW_POS, previewPreferences.getPreviewCyclePosition());
        putStringList(CYCLE_PREVIEW, previewPreferences.getPreviewCycle());
        putInt(PREVIEW_PANEL_HEIGHT, previewPreferences.getPreviewPanelHeight());
        put(PREVIEW_STYLE, previewPreferences.getPreviewStyle());
        putBoolean(PREVIEW_ENABLED, previewPreferences.isPreviewPanelEnabled());
        return this;
    }

    public PreviewPreferences getPreviewPreferences() {
        int cyclePos = getInt(CYCLE_PREVIEW_POS);
        List<String> cycle = getStringList(CYCLE_PREVIEW);
        int panelHeight = getInt(PREVIEW_PANEL_HEIGHT);
        String style = get(PREVIEW_STYLE);
        String styleDefault = (String) defaults.get(PREVIEW_STYLE);
        boolean enabled = getBoolean(PREVIEW_ENABLED);
        return new PreviewPreferences(cycle, cyclePos, panelHeight, enabled, style, styleDefault);
    }

    public void storeProxyPreferences(ProxyPreferences proxyPreferences) {
        putBoolean(PROXY_USE, proxyPreferences.isUseProxy());
        put(PROXY_HOSTNAME, proxyPreferences.getHostname());
        put(PROXY_PORT, proxyPreferences.getPort());
        putBoolean(PROXY_USE_AUTHENTICATION, proxyPreferences.isUseAuthentication());
        put(PROXY_USERNAME, proxyPreferences.getUsername());
        put(PROXY_PASSWORD, proxyPreferences.getPassword());
    }

    public ProxyPreferences getProxyPreferences() {
        Boolean useProxy = getBoolean(PROXY_USE);
        String hostname = get(PROXY_HOSTNAME);
        String port = get(PROXY_PORT);
        Boolean useAuthentication = getBoolean(PROXY_USE_AUTHENTICATION);
        String username = get(PROXY_USERNAME);
        String password = get(PROXY_PASSWORD);
        return new ProxyPreferences(useProxy, hostname, port, useAuthentication, username, password);
    }

    public ProtectedTermsPreferences getProtectedTermsPreferences() {
        return new ProtectedTermsPreferences(getStringList(PROTECTED_TERMS_ENABLED_INTERNAL),
                getStringList(PROTECTED_TERMS_ENABLED_EXTERNAL), getStringList(PROTECTED_TERMS_DISABLED_INTERNAL),
                getStringList(PROTECTED_TERMS_DISABLED_EXTERNAL));
    }

    public void setProtectedTermsPreferences(ProtectedTermsLoader loader) {
        List<String> enabledExternalList = new ArrayList<>();
        List<String> disabledExternalList = new ArrayList<>();
        List<String> enabledInternalList = new ArrayList<>();
        List<String> disabledInternalList = new ArrayList<>();

        for (ProtectedTermsList list : loader.getProtectedTermsLists()) {
            if (list.isInternalList()) {
                if (list.isEnabled()) {
                    enabledInternalList.add(list.getLocation());
                } else {
                    disabledInternalList.add(list.getLocation());
                }
            } else {
                if (list.isEnabled()) {
                    enabledExternalList.add(list.getLocation());
                } else {
                    disabledExternalList.add(list.getLocation());
                }
            }
        }

        putStringList(PROTECTED_TERMS_ENABLED_EXTERNAL, enabledExternalList);
        putStringList(PROTECTED_TERMS_DISABLED_EXTERNAL, disabledExternalList);
        putStringList(PROTECTED_TERMS_ENABLED_INTERNAL, enabledInternalList);
        putStringList(PROTECTED_TERMS_DISABLED_INTERNAL, disabledInternalList);

    }

    @Override
    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        return new JournalAbbreviationPreferences(getStringList(EXTERNAL_JOURNAL_LISTS), get(PERSONAL_JOURNAL_LIST),
                getBoolean(USE_IEEE_ABRV), getDefaultEncoding());
    }

    public CleanupPreferences getCleanupPreferences(JournalAbbreviationLoader journalAbbreviationLoader) {
        return new CleanupPreferences(get(IMPORT_FILENAMEPATTERN), get(IMPORT_FILEDIRPATTERN),
                getLayoutFormatterPreferences(journalAbbreviationLoader), getFileDirectoryPreferences());
    }

    public CleanupPreset getCleanupPreset() {
        Set<CleanupPreset.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreset.CleanupStep.class);

        if (this.getBoolean(JabRefPreferences.CLEANUP_DOI)) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_DOI);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_ISSN)) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_ISSN);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_MOVE_PDF)) {
            activeJobs.add(CleanupPreset.CleanupStep.MOVE_PDF);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_MAKE_PATHS_RELATIVE)) {
            activeJobs.add(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_RENAME_PDF)) {
            activeJobs.add(CleanupPreset.CleanupStep.RENAME_PDF);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS)) {
            activeJobs.add(CleanupPreset.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_UPGRADE_EXTERNAL_LINKS)) {
            activeJobs.add(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_CONVERT_TO_BIBLATEX)) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_CONVERT_TO_BIBTEX)) {
            activeJobs.add(CleanupPreset.CleanupStep.CONVERT_TO_BIBTEX);
        }
        if (this.getBoolean(JabRefPreferences.CLEANUP_FIX_FILE_LINKS)) {
            activeJobs.add(CleanupPreset.CleanupStep.FIX_FILE_LINKS);
        }

        FieldFormatterCleanups formatterCleanups = Cleanups.parse(
                this.getStringList(JabRefPreferences.CLEANUP_FORMATTERS));

        return new CleanupPreset(activeJobs, formatterCleanups);
    }

    public void setCleanupPreset(CleanupPreset cleanupPreset) {
        this.putBoolean(JabRefPreferences.CLEANUP_DOI, cleanupPreset.isActive(CleanupPreset.CleanupStep.CLEAN_UP_DOI));
        this.putBoolean(JabRefPreferences.CLEANUP_ISSN, cleanupPreset.isActive(CleanupPreset.CleanupStep.CLEAN_UP_ISSN));
        this.putBoolean(JabRefPreferences.CLEANUP_MOVE_PDF, cleanupPreset.isActive(CleanupPreset.CleanupStep.MOVE_PDF));
        this.putBoolean(JabRefPreferences.CLEANUP_MAKE_PATHS_RELATIVE, cleanupPreset.isActive(CleanupPreset.CleanupStep.MAKE_PATHS_RELATIVE));
        this.putBoolean(JabRefPreferences.CLEANUP_RENAME_PDF, cleanupPreset.isActive(CleanupPreset.CleanupStep.RENAME_PDF));
        this.putBoolean(JabRefPreferences.CLEANUP_RENAME_PDF_ONLY_RELATIVE_PATHS,
                cleanupPreset.isActive(CleanupPreset.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS));
        this.putBoolean(JabRefPreferences.CLEANUP_UPGRADE_EXTERNAL_LINKS,
                cleanupPreset.isActive(CleanupPreset.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS));
        this.putBoolean(JabRefPreferences.CLEANUP_CONVERT_TO_BIBLATEX, cleanupPreset.isActive(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX));
        this.putBoolean(JabRefPreferences.CLEANUP_CONVERT_TO_BIBTEX, cleanupPreset.isActive(CleanupPreset.CleanupStep.CONVERT_TO_BIBTEX));
        this.putBoolean(JabRefPreferences.CLEANUP_FIX_FILE_LINKS, cleanupPreset.isActive(CleanupPreset.CleanupStep.FIX_FILE_LINKS));
        this.putStringList(JabRefPreferences.CLEANUP_FORMATTERS, cleanupPreset.getFormatterCleanups().getAsStringList(OS.NEWLINE));
    }

    public RemotePreferences getRemotePreferences() {
        return new RemotePreferences(getInt(REMOTE_SERVER_PORT), getBoolean(USE_REMOTE_SERVER));
    }

    public void setRemotePreferences(RemotePreferences remotePreferences) {
        putInt(REMOTE_SERVER_PORT, remotePreferences.getPort());
        putBoolean(USE_REMOTE_SERVER, remotePreferences.useRemoteServer());
    }

    public void storeExportSaveOrder(SaveOrderConfig config) {
        putBoolean(EXPORT_PRIMARY_SORT_DESCENDING, config.sortCriteria[0].descending);
        putBoolean(EXPORT_SECONDARY_SORT_DESCENDING, config.sortCriteria[1].descending);
        putBoolean(EXPORT_TERTIARY_SORT_DESCENDING, config.sortCriteria[2].descending);

        put(EXPORT_PRIMARY_SORT_FIELD, config.sortCriteria[0].field);
        put(EXPORT_SECONDARY_SORT_FIELD, config.sortCriteria[1].field);
        put(EXPORT_TERTIARY_SORT_FIELD, config.sortCriteria[2].field);
    }

    public SaveOrderConfig loadTableSaveOrder() {
        SaveOrderConfig config = new SaveOrderConfig();
        config.sortCriteria[0].field = get(TABLE_PRIMARY_SORT_FIELD);
        config.sortCriteria[0].descending = getBoolean(TABLE_PRIMARY_SORT_DESCENDING);
        config.sortCriteria[1].field = get(TABLE_SECONDARY_SORT_FIELD);
        config.sortCriteria[1].descending = getBoolean(TABLE_SECONDARY_SORT_DESCENDING);
        config.sortCriteria[2].field = get(TABLE_TERTIARY_SORT_FIELD);
        config.sortCriteria[2].descending = getBoolean(TABLE_TERTIARY_SORT_DESCENDING);

        return config;
    }

    public SaveOrderConfig loadExportSaveOrder() {
        SaveOrderConfig config = new SaveOrderConfig();
        config.sortCriteria[0].field = get(EXPORT_PRIMARY_SORT_FIELD);
        config.sortCriteria[0].descending = getBoolean(EXPORT_PRIMARY_SORT_DESCENDING);
        config.sortCriteria[1].field = get(EXPORT_SECONDARY_SORT_FIELD);
        config.sortCriteria[1].descending = getBoolean(EXPORT_SECONDARY_SORT_DESCENDING);
        config.sortCriteria[2].field = get(EXPORT_TERTIARY_SORT_FIELD);
        config.sortCriteria[2].descending = getBoolean(EXPORT_TERTIARY_SORT_DESCENDING);

        return config;
    }

    public Character getKeywordDelimiter() {
        return get(KEYWORD_SEPARATOR).charAt(0);
    }

    public String getOrCreateUserId() {
        Optional<String> userId = getAsOptional(USER_ID);
        if (userId.isPresent()) {
            return userId.get();
        } else {
            String newUserId = UUID.randomUUID().toString();
            put(USER_ID, newUserId);
            return newUserId;
        }
    }

    public Boolean shouldCollectTelemetry() {
        return getBoolean(COLLECT_TELEMETRY);
    }

    public void setShouldCollectTelemetry(boolean value) {
        putBoolean(COLLECT_TELEMETRY, value);
    }

    public Boolean shouldAskToCollectTelemetry() {
        return getBoolean(ALREADY_ASKED_TO_COLLECT_TELEMETRY);
    }

    public void askedToCollectTelemetry() {
        putBoolean(ALREADY_ASKED_TO_COLLECT_TELEMETRY, true);
    }

    @Override
    public void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository) {
        putStringList(JabRefPreferences.BIND_NAMES, keyBindingRepository.getBindNames());
        putStringList(JabRefPreferences.BINDINGS, keyBindingRepository.getBindings());
    }

    @Override
    public KeyBindingRepository getKeyBindingRepository() {
        return new KeyBindingRepository(getStringList(BIND_NAMES), getStringList(BINDINGS));
    }

    @Override
    public void storeJournalAbbreviationPreferences(JournalAbbreviationPreferences abbreviationsPreferences) {
        putStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, abbreviationsPreferences.getExternalJournalLists());
        putBoolean(JabRefPreferences.USE_IEEE_ABRV, abbreviationsPreferences.useIEEEAbbreviations());
    }

    public AutoLinkPreferences getAutoLinkPreferences() {
        return new AutoLinkPreferences(
                getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY),
                get(JabRefPreferences.AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY),
                getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY),
                getKeywordDelimiter());
    }

    public AutoCompletePreferences getAutoCompletePreferences() {
        return new AutoCompletePreferences(
                getBoolean(AUTO_COMPLETE),
                AutoCompleteFirstNameMode.parse(get(AUTOCOMPLETER_FIRSTNAME_MODE)),
                getBoolean(AUTOCOMPLETER_LAST_FIRST),
                getBoolean(AUTOCOMPLETER_FIRST_LAST),
                getStringList(AUTOCOMPLETER_COMPLETE_FIELDS),
                getJournalAbbreviationPreferences());
    }

    public void storeAutoCompletePreferences(AutoCompletePreferences autoCompletePreferences) {
        putBoolean(AUTO_COMPLETE, autoCompletePreferences.shouldAutoComplete());
        put(AUTOCOMPLETER_FIRSTNAME_MODE, autoCompletePreferences.getFirstNameMode().name());
        putBoolean(AUTOCOMPLETER_LAST_FIRST, autoCompletePreferences.getOnlyCompleteLastFirst());
        putBoolean(AUTOCOMPLETER_FIRST_LAST, autoCompletePreferences.getOnlyCompleteFirstLast());
        putStringList(AUTOCOMPLETER_COMPLETE_FIELDS, autoCompletePreferences.getCompleteFields());
    }
}
