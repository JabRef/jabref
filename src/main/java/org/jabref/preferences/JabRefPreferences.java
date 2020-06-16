package org.jabref.preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import java.util.stream.Stream;

import javafx.scene.control.TableColumn.SortType;
import javafx.scene.paint.Color;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.JabRefMain;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableNameFormatPreferences;
import org.jabref.gui.maintable.MainTableNameFormatPreferences.AbbreviationStyle;
import org.jabref.gui.maintable.MainTableNameFormatPreferences.DisplayStyle;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.mergeentries.MergeEntries;
import org.jabref.gui.preferences.ImportTabViewModel;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.util.ThemeLoader;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupPreset;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.StyleLoader;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.Version;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.bibtexkeypattern.GlobalCitationKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.metadata.FilePreferences;
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
    public static final String FX_THEME = "fxTheme";
    public static final String LANGUAGE = "language";
    public static final String NAMES_LAST_ONLY = "namesLastOnly";
    public static final String ABBR_AUTHOR_NAMES = "abbrAuthorNames";
    public static final String NAMES_NATBIB = "namesNatbib";
    public static final String NAMES_FIRST_LAST = "namesFf";
    public static final String BIBLATEX_DEFAULT_MODE = "biblatexMode";
    public static final String NAMES_AS_IS = "namesAsIs";
    public static final String ENTRY_EDITOR_HEIGHT = "entryEditorHeightFX";
    public static final String AUTO_RESIZE_MODE = "autoResizeMode";
    public static final String WINDOW_MAXIMISED = "windowMaximised";

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

    // Variable names have changed to ensure backward compatibility with pre 5.0 releases of JabRef
    // Pre 5.1: columnNames, columnWidths, columnSortTypes, columnSortOrder
    public static final String COLUMN_NAMES = "mainTableColumnNames";
    public static final String COLUMN_WIDTHS = "mainTableColumnWidths";
    public static final String COLUMN_SORT_TYPES = "mainTableColumnSortTypes";
    public static final String COLUMN_SORT_ORDER = "mainTableColumnSortOrder";

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
    public static final String LAST_EDITED = "lastEdited";
    public static final String OPEN_LAST_EDITED = "openLastEdited";
    public static final String LAST_FOCUSED = "lastFocused";
    public static final String AUTO_OPEN_FORM = "autoOpenForm";
    public static final String IMPORT_WORKING_DIRECTORY = "importWorkingDirectory";
    public static final String EXPORT_WORKING_DIRECTORY = "exportWorkingDirectory";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String EDITOR_EMACS_KEYBINDINGS = "editorEMACSkeyBindings";
    public static final String EDITOR_EMACS_KEYBINDINGS_REBIND_CA = "editorEMACSkeyBindingsRebindCA";
    public static final String EDITOR_EMACS_KEYBINDINGS_REBIND_CF = "editorEMACSkeyBindingsRebindCF";
    public static final String GROUPS_DEFAULT_FIELD = "groupsDefaultField";

    public static final String KEYWORD_SEPARATOR = "groupKeywordSeparator";
    public static final String AUTO_ASSIGN_GROUP = "autoAssignGroup";
    public static final String DISPLAY_GROUP_COUNT = "displayGroupCount";
    public static final String EXTRA_FILE_COLUMNS = "extraFileColumns";
    public static final String OVERRIDE_DEFAULT_FONT_SIZE = "overrideDefaultFontSize";
    public static final String MAIN_FONT_SIZE = "mainFontSize";

    public static final String RECENT_DATABASES = "recentDatabases";
    public static final String RENAME_ON_MOVE_FILE_TO_FILE_DIR = "renameOnMoveFileToFileDir";
    public static final String MEMORY_STICK_MODE = "memoryStickMode";
    public static final String SHOW_ADVANCED_HINTS = "showAdvancedHints";
    public static final String DEFAULT_ENCODING = "defaultEncoding";

    public static final String USE_OWNER = "useOwner";
    public static final String DEFAULT_OWNER = "defaultOwner";
    public static final String OVERWRITE_OWNER = "overwriteOwner";

    public static final String USE_TIME_STAMP = "useTimeStamp";
    public static final String UPDATE_TIMESTAMP = "updateTimestamp";
    public static final String TIME_STAMP_FIELD = "timeStampField";
    public static final String TIME_STAMP_FORMAT = "timeStampFormat";
    public static final String OVERWRITE_TIME_STAMP = "overwriteTimeStamp";

    public static final String WARN_ABOUT_DUPLICATES_IN_INSPECTION = "warnAboutDuplicatesInInspection";
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
    public static final String USE_DEFAULT_FILE_BROWSER_APPLICATION = "userDefaultFileBrowserApplication";
    public static final String FILE_BROWSER_COMMAND = "fileBrowserCommand";
    public static final String MAIN_FILE_DIRECTORY = "fileDirectory";

    // Currently, it is not possible to specify defaults for specific entry types
    // When this should be made possible, the code to inspect is org.jabref.gui.preferences.CitationKeyPatternPrefTab.storeSettings() -> LabelPattern keypatterns = getCiteKeyPattern(); etc
    public static final String DEFAULT_CITATION_KEY_PATTERN = "defaultBibtexKeyPattern";
    public static final String UNWANTED_CITATION_KEY_CHARACTERS = "defaultUnwantedBibtexKeyCharacters";
    public static final String GRAY_OUT_NON_HITS = "grayOutNonHits";
    public static final String CONFIRM_DELETE = "confirmDelete";
    public static final String WARN_BEFORE_OVERWRITING_KEY = "warnBeforeOverwritingKey";
    public static final String AVOID_OVERWRITING_KEY = "avoidOverwritingKey";
    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";
    public static final String SHOW_FILE_LINKS_UPGRADE_WARNING = "showFileLinksUpgradeWarning";
    public static final String SIDE_PANE_WIDTH = "sidePaneWidthFX";
    public static final String LAST_USED_EXPORT = "lastUsedExport";
    public static final String CITE_COMMAND = "citeCommand";
    public static final String GENERATE_KEYS_BEFORE_SAVING = "generateKeysBeforeSaving";
    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";
    public static final String KEY_GEN_ALWAYS_ADD_LETTER = "keyGenAlwaysAddLetter";
    public static final String KEY_GEN_FIRST_LETTER_A = "keyGenFirstLetterA";
    public static final String ALLOW_INTEGER_EDITION_BIBTEX = "allowIntegerEditionBibtex";
    public static final String LOCAL_AUTO_SAVE = "localAutoSave";
    public static final String RUN_AUTOMATIC_FILE_SEARCH = "runAutomaticFileSearch";
    public static final String AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String AUTOLINK_USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    public static final String BIB_LOC_AS_PRIMARY_DIR = "bibLocAsPrimaryDir";
    public static final String SELECTED_FETCHER_INDEX = "selectedFetcherIndex";
    public static final String WEB_SEARCH_VISIBLE = "webSearchVisible";
    public static final String GROUP_SIDEPANE_VISIBLE = "groupSidepaneVisible";
    public static final String ALLOW_FILE_AUTO_OPEN_BROWSE = "allowFileAutoOpenBrowse";
    public static final String CUSTOM_TAB_NAME = "customTabName_";
    public static final String CUSTOM_TAB_FIELDS = "customTabFields_";
    public static final String ASK_AUTO_NAMING_PDFS_AGAIN = "AskAutoNamingPDFsAgain";
    public static final String CLEANUP = "CleanUp";
    public static final String CLEANUP_FORMATTERS = "CleanUpFormatters";
    public static final String IMPORT_FILENAMEPATTERN = "importFileNamePattern";
    public static final String IMPORT_FILEDIRPATTERN = "importFileDirPattern";
    public static final String DOWNLOAD_LINKED_FILES = "downloadLinkedFiles";
    public static final String NAME_FORMATTER_VALUE = "nameFormatterFormats";
    public static final String NAME_FORMATER_KEY = "nameFormatterNames";
    public static final String PUSH_TO_APPLICATION = "pushToApplication";
    public static final String SHOW_RECOMMENDATIONS = "showRecommendations";
    public static final String ACCEPT_RECOMMENDATIONS = "acceptRecommendations";
    public static final String SHOW_LATEX_CITATIONS = "showLatexCitations";
    public static final String SEND_LANGUAGE_DATA = "sendLanguageData";
    public static final String SEND_OS_DATA = "sendOSData";
    public static final String SEND_TIMEZONE_DATA = "sendTimezoneData";
    public static final String VALIDATE_IN_ENTRY_EDITOR = "validateInEntryEditor";

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

    // Special field preferences
    public static final String SPECIALFIELDSENABLED = "specialFieldsEnabled";
    // The choice between AUTOSYNCSPECIALFIELDSTOKEYWORDS and SERIALIZESPECIALFIELDS is mutually exclusive
    // At least in the settings, not in the implementation. But having both confused the users, therefore, having activated both options at the same time has been disabled
    public static final String SERIALIZESPECIALFIELDS = "serializeSpecialFields";
    public static final String AUTOSYNCSPECIALFIELDSTOKEYWORDS = "autoSyncSpecialFieldsToKeywords";
    // Prefs node for CitationKeyPatterns
    public static final String CITATION_KEY_PATTERNS_NODE = "bibtexkeypatterns";
    // Prefs node for customized entry types
    public static final String CUSTOMIZED_BIBTEX_TYPES = "customizedBibtexTypes";
    public static final String CUSTOMIZED_BIBLATEX_TYPES = "customizedBiblatexTypes";
    // Version
    public static final String VERSION_IGNORED_UPDATE = "versionIgnoreUpdate";
    // KeyBindings - keys - public because needed for pref migration
    public static final String BINDINGS = "bindings";

    // AutcompleteFields - public because needed for pref migration
    public static final String AUTOCOMPLETER_COMPLETE_FIELDS = "autoCompleteFields";

    // Id Entry Generator Preferences
    public static final String ID_ENTRY_GENERATOR = "idEntryGenerator";

    // String delimiter
    public static final Character STRINGLIST_DELIMITER = ';';

    // UI
    private static final String FONT_FAMILY = "fontFamily";

    // Preview
    private static final String PREVIEW_STYLE = "previewStyle";
    private static final String CYCLE_PREVIEW_POS = "cyclePreviewPos";
    private static final String CYCLE_PREVIEW = "cyclePreview";
    private static final String PREVIEW_PANEL_HEIGHT = "previewPanelHeightFX";
    private static final String PREVIEW_AS_TAB = "previewAsTab";

    // Proxy
    private static final String PROXY_PORT = "proxyPort";
    private static final String PROXY_HOSTNAME = "proxyHostname";
    private static final String PROXY_USE = "useProxy";
    private static final String PROXY_USERNAME = "proxyUsername";
    private static final String PROXY_PASSWORD = "proxyPassword";
    private static final String PROXY_USE_AUTHENTICATION = "useProxyAuthentication";

    // Auto completion
    private static final String AUTO_COMPLETE = "autoComplete";
    private static final String AUTOCOMPLETER_FIRSTNAME_MODE = "autoCompFirstNameMode";
    private static final String AUTOCOMPLETER_LAST_FIRST = "autoCompLF";
    private static final String AUTOCOMPLETER_FIRST_LAST = "autoCompFF";

    private static final String BIND_NAMES = "bindNames";
    // User
    private static final String USER_ID = "userId";
    private static final String EXTERNAL_JOURNAL_LISTS = "externalJournalLists";

    // Telemetry collection
    private static final String COLLECT_TELEMETRY = "collectTelemetry";
    private static final String ALREADY_ASKED_TO_COLLECT_TELEMETRY = "askedCollectTelemetry";
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefPreferences.class);
    private static final Class<JabRefMain> PREFS_BASE_CLASS = JabRefMain.class;
    private static final String DB_CONNECT_USERNAME = "dbConnectUsername";
    private static final String DB_CONNECT_DATABASE = "dbConnectDatabase";
    private static final String DB_CONNECT_HOSTNAME = "dbConnectHostname";
    private static final String DB_CONNECT_SERVER_TYPE = "dbConnectServerType";
    private static final String PROTECTED_TERMS_ENABLED_EXTERNAL = "protectedTermsEnabledExternal";
    private static final String PROTECTED_TERMS_DISABLED_EXTERNAL = "protectedTermsDisabledExternal";
    private static final String PROTECTED_TERMS_ENABLED_INTERNAL = "protectedTermsEnabledInternal";
    private static final String PROTECTED_TERMS_DISABLED_INTERNAL = "protectedTermsDisabledInternal";

    // GroupViewMode
    private static final String GROUP_INTERSECT_UNION_VIEW_MODE = "groupIntersectUnionViewModes";

    // Dialog states
    private static final String PREFS_EXPORT_PATH = "prefsExportPath";

    // Helper string
    private static final String USER_HOME = System.getProperty("user.home");

    // Indexes for Strings within stored custom export entries
    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    // Remote
    private static final String USE_REMOTE_SERVER = "useRemoteServer";
    private static final String REMOTE_SERVER_PORT = "remoteServerPort";

    // The only instance of this class:
    private static JabRefPreferences singleton;
    /**
     * HashMap that contains all preferences which are set by default
     */
    public final Map<String, Object> defaults = new HashMap<>();
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

    /**
     * Cache variable for <code>getGlobalCitationKeyPattern</code>
     */
    private GlobalCitationKeyPattern globalCitationKeyPattern;

    /**
     * Cache variable for getEntryEditorTabList
     */
    private Map<String, Set<Field>> entryEditorTabList;

    /**
     * Cache variable for getMainTableColumns
     */
    private List<MainTableColumnModel> mainTableColumns;

    /**
     * Cache variable for getColumnSortOrder
     */
    private List<MainTableColumnModel> mainTableColumnSortOrder;

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
        Localization.setLanguage(getLanguage());

        SearchPreferences.putDefaults(defaults);

        defaults.put(TEXMAKER_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("texmaker", "Texmaker"));
        defaults.put(WIN_EDT_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("WinEdt", "WinEdt Team\\WinEdt"));
        defaults.put(TEXSTUDIO_PATH, JabRefDesktop.getNativeDesktop().detectProgramPath("texstudio", "TeXstudio"));

        defaults.put(BIBLATEX_DEFAULT_MODE, Boolean.FALSE);

        // Set DOI to be the default ID entry generator
        defaults.put(ID_ENTRY_GENERATOR, DoiFetcher.NAME);

        if (OS.OS_X) {
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(EMACS_PATH, "emacsclient");
        } else if (OS.WINDOWS) {
            defaults.put(EMACS_PATH, "emacsclient.exe");
        } else {
            // Linux
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(EMACS_PATH, "emacsclient");
        }

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

        defaults.put(LYXPIPE, USER_HOME + File.separator + ".lyx/lyxpipe");
        defaults.put(VIM, "vim");
        defaults.put(VIM_SERVER, "vim");
        defaults.put(POS_X, 0);
        defaults.put(POS_Y, 0);
        defaults.put(SIZE_X, 1024);
        defaults.put(SIZE_Y, 768);
        defaults.put(WINDOW_MAXIMISED, Boolean.TRUE);
        defaults.put(AUTO_RESIZE_MODE, Boolean.TRUE);
        defaults.put(ENTRY_EDITOR_HEIGHT, 0.65);
        defaults.put(NAMES_AS_IS, Boolean.FALSE); // "Show names unchanged"
        defaults.put(NAMES_FIRST_LAST, Boolean.FALSE); // "Show 'Firstname Lastname'"
        defaults.put(NAMES_NATBIB, Boolean.TRUE); // "Natbib style"
        defaults.put(ABBR_AUTHOR_NAMES, Boolean.TRUE); // "Abbreviate names"
        defaults.put(NAMES_LAST_ONLY, Boolean.TRUE); // "Show last names only"
        // system locale as default
        defaults.put(LANGUAGE, Locale.getDefault().getLanguage());

        defaults.put(REFORMAT_FILE_ON_SAVE_AND_EXPORT, Boolean.FALSE);

        // export order
        defaults.put(EXPORT_IN_ORIGINAL_ORDER, Boolean.FALSE);
        defaults.put(EXPORT_IN_SPECIFIED_ORDER, Boolean.FALSE);

        // export order: if EXPORT_IN_SPECIFIED_ORDER, then use following criteria
        defaults.put(EXPORT_PRIMARY_SORT_FIELD, InternalField.KEY_FIELD.getName());
        defaults.put(EXPORT_PRIMARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(EXPORT_SECONDARY_SORT_FIELD, StandardField.AUTHOR.getName());
        defaults.put(EXPORT_SECONDARY_SORT_DESCENDING, Boolean.FALSE);
        defaults.put(EXPORT_TERTIARY_SORT_FIELD, StandardField.TITLE.getName());
        defaults.put(EXPORT_TERTIARY_SORT_DESCENDING, Boolean.TRUE);

        defaults.put(NEWLINE, System.lineSeparator());

        defaults.put(SIDE_PANE_COMPONENT_NAMES, "");
        defaults.put(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, "");

        defaults.put(COLUMN_NAMES, "groups;files;linked_id;field:entrytype;field:author/editor;field:title;field:year;field:journal/booktitle;field:bibtexkey");
        defaults.put(COLUMN_WIDTHS, "28;28;28;75;300;470;60;130;100");

        defaults.put(XMP_PRIVACY_FILTERS, "pdf;timestamp;keywords;owner;note;review");
        defaults.put(USE_XMP_PRIVACY_FILTER, Boolean.FALSE);
        defaults.put(WORKING_DIRECTORY, USER_HOME);
        defaults.put(EXPORT_WORKING_DIRECTORY, USER_HOME);
        // Remembers working directory of last import
        defaults.put(IMPORT_WORKING_DIRECTORY, USER_HOME);
        defaults.put(PREFS_EXPORT_PATH, USER_HOME);
        defaults.put(AUTO_OPEN_FORM, Boolean.TRUE);
        defaults.put(OPEN_LAST_EDITED, Boolean.TRUE);
        defaults.put(LAST_EDITED, "");
        defaults.put(LAST_FOCUSED, "");
        defaults.put(DEFAULT_SHOW_SOURCE, Boolean.FALSE);

        defaults.put(DEFAULT_AUTO_SORT, Boolean.FALSE);

        defaults.put(MERGE_ENTRIES_DIFF_MODE, MergeEntries.DiffMode.WORD.name());

        defaults.put(SHOW_RECOMMENDATIONS, Boolean.TRUE);
        defaults.put(ACCEPT_RECOMMENDATIONS, Boolean.FALSE);
        defaults.put(SHOW_LATEX_CITATIONS, Boolean.TRUE);
        defaults.put(SEND_LANGUAGE_DATA, Boolean.FALSE);
        defaults.put(SEND_OS_DATA, Boolean.FALSE);
        defaults.put(SEND_TIMEZONE_DATA, Boolean.FALSE);
        defaults.put(VALIDATE_IN_ENTRY_EDITOR, Boolean.TRUE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS, Boolean.FALSE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CA, Boolean.TRUE);
        defaults.put(EDITOR_EMACS_KEYBINDINGS_REBIND_CF, Boolean.TRUE);
        defaults.put(AUTO_COMPLETE, Boolean.FALSE);
        defaults.put(AUTOCOMPLETER_FIRSTNAME_MODE, AutoCompleteFirstNameMode.BOTH.name());
        defaults.put(AUTOCOMPLETER_FIRST_LAST, Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put(AUTOCOMPLETER_LAST_FIRST, Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(AUTOCOMPLETER_COMPLETE_FIELDS, "author;editor;title;journal;publisher;keywords;crossref;related;entryset");
        defaults.put(GROUPS_DEFAULT_FIELD, StandardField.KEYWORDS.getName());
        defaults.put(AUTO_ASSIGN_GROUP, Boolean.TRUE);
        defaults.put(DISPLAY_GROUP_COUNT, Boolean.TRUE);
        defaults.put(GROUP_INTERSECT_UNION_VIEW_MODE, GroupViewMode.INTERSECTION.name());
        defaults.put(KEYWORD_SEPARATOR, ", ");
        defaults.put(DEFAULT_ENCODING, StandardCharsets.UTF_8.name());
        defaults.put(DEFAULT_OWNER, System.getProperty("user.name"));
        defaults.put(MEMORY_STICK_MODE, Boolean.FALSE);
        defaults.put(SHOW_ADVANCED_HINTS, Boolean.TRUE);
        defaults.put(RENAME_ON_MOVE_FILE_TO_FILE_DIR, Boolean.TRUE);

        defaults.put(EXTRA_FILE_COLUMNS, Boolean.FALSE);

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

        defaults.put(SPECIALFIELDSENABLED, Boolean.TRUE);
        defaults.put(AUTOSYNCSPECIALFIELDSTOKEYWORDS, Boolean.TRUE);
        defaults.put(SERIALIZESPECIALFIELDS, Boolean.FALSE);

        defaults.put(USE_OWNER, Boolean.FALSE);
        defaults.put(OVERWRITE_OWNER, Boolean.FALSE);
        defaults.put(AVOID_OVERWRITING_KEY, Boolean.FALSE);
        defaults.put(WARN_BEFORE_OVERWRITING_KEY, Boolean.TRUE);
        defaults.put(CONFIRM_DELETE, Boolean.TRUE);
        defaults.put(GRAY_OUT_NON_HITS, Boolean.TRUE);
        defaults.put(DEFAULT_CITATION_KEY_PATTERN, "[auth][year]");
        defaults.put(UNWANTED_CITATION_KEY_CHARACTERS, "-`ʹ:!;?^+");
        defaults.put(DO_NOT_RESOLVE_STRINGS_FOR, StandardField.URL.getName());
        defaults.put(RESOLVE_STRINGS_ALL_FIELDS, Boolean.FALSE);
        defaults.put(NON_WRAPPABLE_FIELDS, "pdf;ps;url;doi;file;isbn;issn");
        defaults.put(WARN_ABOUT_DUPLICATES_IN_INSPECTION, Boolean.TRUE);
        defaults.put(USE_TIME_STAMP, Boolean.FALSE);
        defaults.put(OVERWRITE_TIME_STAMP, Boolean.FALSE);

        // default time stamp follows ISO-8601. Reason: https://xkcd.com/1179/
        defaults.put(TIME_STAMP_FORMAT, "yyyy-MM-dd");

        defaults.put(TIME_STAMP_FIELD, StandardField.TIMESTAMP.getName());
        defaults.put(UPDATE_TIMESTAMP, Boolean.FALSE);

        defaults.put(GENERATE_KEYS_BEFORE_SAVING, Boolean.FALSE);

        defaults.put(USE_REMOTE_SERVER, Boolean.TRUE);
        defaults.put(REMOTE_SERVER_PORT, 6050);

        defaults.put(EXTERNAL_JOURNAL_LISTS, "");
        defaults.put(CITE_COMMAND, "\\cite"); // obsoleted by the app-specific ones (not any more?)

        defaults.put(LAST_USED_EXPORT, "");
        defaults.put(SIDE_PANE_WIDTH, 0.15);

        defaults.put(MAIN_FONT_SIZE, 9);
        defaults.put(OVERRIDE_DEFAULT_FONT_SIZE, false);

        defaults.put(SHOW_FILE_LINKS_UPGRADE_WARNING, Boolean.TRUE);
        defaults.put(AUTOLINK_EXACT_KEY_ONLY, Boolean.FALSE);
        defaults.put(RUN_AUTOMATIC_FILE_SEARCH, Boolean.FALSE);
        defaults.put(LOCAL_AUTO_SAVE, Boolean.FALSE);
        defaults.put(ALLOW_INTEGER_EDITION_BIBTEX, Boolean.FALSE);
        // Curly brackets ({}) are the default delimiters, not quotes (") as these cause trouble when they appear within the field value:
        // Currently, JabRef does not escape them
        defaults.put(KEY_GEN_FIRST_LETTER_A, Boolean.TRUE);
        defaults.put(KEY_GEN_ALWAYS_ADD_LETTER, Boolean.FALSE);
        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
        defaults.put(OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.FALSE);
        defaults.put(ALLOW_FILE_AUTO_OPEN_BROWSE, Boolean.TRUE);
        defaults.put(WEB_SEARCH_VISIBLE, Boolean.TRUE);
        defaults.put(GROUP_SIDEPANE_VISIBLE, Boolean.TRUE);
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

        // use citation key appended with filename as default pattern
        defaults.put(IMPORT_FILENAMEPATTERN, ImportTabViewModel.DEFAULT_FILENAME_PATTERNS[1]);
        // Default empty String to be backwards compatible
        defaults.put(IMPORT_FILEDIRPATTERN, "");
        // Don't download files by default
        defaults.put(DOWNLOAD_LINKED_FILES, false);

        customImports = new CustomImportList(this);

        String defaultExpression = "**/.*[bibtexkey].*\\\\.[extension]";
        defaults.put(AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, defaultExpression);
        defaults.put(AUTOLINK_USE_REG_EXP_SEARCH_KEY, Boolean.FALSE);

        defaults.put(USE_DEFAULT_CONSOLE_APPLICATION, Boolean.TRUE);
        defaults.put(USE_DEFAULT_FILE_BROWSER_APPLICATION, Boolean.TRUE);
        if (OS.WINDOWS) {
            defaults.put(CONSOLE_COMMAND, "C:\\Program Files\\ConEmu\\ConEmu64.exe /single /dir \"%DIR\"");
            defaults.put(FILE_BROWSER_COMMAND, "explorer.exe /select, \"%DIR\"");
        } else {
            defaults.put(CONSOLE_COMMAND, "");
            defaults.put(FILE_BROWSER_COMMAND, "");
        }

        // versioncheck defaults
        defaults.put(VERSION_IGNORED_UPDATE, "");

        // preview
        defaults.put(CYCLE_PREVIEW, "Preview;" + CitationStyle.DEFAULT);
        defaults.put(CYCLE_PREVIEW_POS, 0);
        defaults.put(PREVIEW_PANEL_HEIGHT, 0.65);
        defaults.put(PREVIEW_AS_TAB, Boolean.FALSE);
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
                        + "\\begin{comment}<BR><BR><b>Comment: </b> \\format[Markdown,HTMLChars]{\\comment} \\end{comment}"
                        + "</dd>__NEWLINE__<p></p></font>");

        // set default theme
        defaults.put(JabRefPreferences.FX_THEME, ThemeLoader.MAIN_CSS);

        setLanguageDependentDefaultValues();
    }

    public static JabRefPreferences getInstance() {
        if (JabRefPreferences.singleton == null) {
            JabRefPreferences.singleton = new JabRefPreferences();
        }
        return JabRefPreferences.singleton;
    }

    private static String convertListToString(List<String> value) {
        return value.stream().map(val -> StringUtil.quote(val, STRINGLIST_DELIMITER.toString(), '\\')).collect(Collectors.joining(STRINGLIST_DELIMITER.toString()));
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

        // true if a STRINGLIST_DELIMITER is found
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
                if (c == STRINGLIST_DELIMITER) {
                    if (escape) {
                        res.append(STRINGLIST_DELIMITER);
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

        for (CleanupPreset.CleanupStep action : EnumSet.allOf(CleanupPreset.CleanupStep.class)) {
            storage.put(JabRefPreferences.CLEANUP + action.name(), !deactivatedJobs.contains(action));
        }
        storage.put(CLEANUP_FORMATTERS, convertListToString(Cleanups.DEFAULT_SAVE_ACTIONS.getAsStringList(OS.NEWLINE)));
    }

    public Map<SidePaneType, Integer> getSidePanePreferredPositions() {
        Map<SidePaneType, Integer> preferredPositions = new HashMap<>();

        List<String> componentNames = getStringList(SIDE_PANE_COMPONENT_NAMES);
        List<String> componentPositions = getStringList(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS);

        for (int i = 0; i < componentNames.size(); ++i) {
            String name = componentNames.get(i);
            try {
                SidePaneType type = Enum.valueOf(SidePaneType.class, name);
                preferredPositions.put(type, Integer.parseInt(componentPositions.get(i)));
            } catch (NumberFormatException e) {
                LOGGER.debug("Invalid number format for side pane component '" + name + "'", e);
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Following component is not a side pane: '" + name + "'", e);
            }
        }

        return preferredPositions;
    }

    @Override
    public String getUser() {
        try {
            return get(DEFAULT_OWNER) + '-' + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            LOGGER.debug("Hostname not found.", ex);
            return get(DEFAULT_OWNER);
        }
    }

    @Override
    public String getTheme() {
        return get(FX_THEME);
    }

    public void setLanguageDependentDefaultValues() {
        // Entry editor tab 0:
        defaults.put(CUSTOM_TAB_NAME + "_def0", Localization.lang("General"));
        String fieldNames = FieldFactory.getDefaultGeneralFields().stream().map(Field::getName).collect(Collectors.joining(STRINGLIST_DELIMITER.toString()));
        defaults.put(CUSTOM_TAB_FIELDS + "_def0", fieldNames);

        // Entry editor tab 1:
        defaults.put(CUSTOM_TAB_FIELDS + "_def1", StandardField.ABSTRACT.getName());
        defaults.put(CUSTOM_TAB_NAME + "_def1", Localization.lang("Abstract"));

        // Entry editor tab 2: Comments Field - used for research comments, etc.
        defaults.put(CUSTOM_TAB_FIELDS + "_def2", StandardField.COMMENT.getName());
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

    public double getDouble(String key) {
        return prefs.getDouble(key, getDoubleDefault(key));
    }

    public int getIntDefault(String key) {
        return (Integer) defaults.get(key);
    }

    private double getDoubleDefault(String key) {
        return ((Number) defaults.get(key)).doubleValue();
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

    public void putInt(String key, Number value) {
        prefs.putInt(key, value.intValue());
    }

    public void putDouble(String key, double value) {
        prefs.putDouble(key, value);
    }

    public void remove(String key) {
        prefs.remove(key);
    }

    /**
     * Puts a list of strings into the Preferences, by linking its elements with a STRINGLIST_DELIMITER into a single
     * string. Escape characters make the process transparent even if strings contains a STRINGLIST_DELIMITER.
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
            return Collections.emptyList();
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
     * Set the default value for a key. This is useful for plugins that need to add default values for the prefs keys they use.
     *
     * @param key   The preferences key.
     * @param value The default value.
     */
    public void putDefaultValue(String key, Object value) {
        defaults.put(key, value);
    }

    /**
     * Stores a color in preferences.
     *
     * @param key   The key for this setting.
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
        clearAllBibEntryTypes();
        clearCitationKeyPatterns();
        prefs.clear();
        new SharedDatabasePreferences().clear();
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

    public void storeBibEntryTypes(List<BibEntryType> BibEntryTypes, BibDatabaseMode bibDatabaseMode) {
        Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode);

        try {
            // clear old custom types
            clearBibEntryTypes(bibDatabaseMode);

            // store current custom types
            BibEntryTypes.forEach(type -> prefsNode.put(type.getType().getName(), BibEntryTypesManager.serialize(type)));

            prefsNode.flush();
        } catch (BackingStoreException e) {
            LOGGER.info("Updating stored custom entry types failed.", e);
        }
    }

    @Override
    public List<BibEntryType> loadBibEntryTypes(BibDatabaseMode bibDatabaseMode) {
        List<BibEntryType> storedEntryTypes = new ArrayList<>();
        Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode);
        try {
            Arrays.stream(prefsNode.keys())
                  .map(key -> prefsNode.get(key, null))
                  .filter(Objects::nonNull)
                  .forEach(typeString -> BibEntryTypesManager.parse(typeString).ifPresent(storedEntryTypes::add));
        } catch (BackingStoreException e) {
            LOGGER.info("Parsing customized entry types failed.", e);
        }
        return storedEntryTypes;
    }

    private void clearAllBibEntryTypes() throws BackingStoreException {
        for (BibDatabaseMode mode : BibDatabaseMode.values()) {
            clearBibEntryTypes(mode);
        }
    }

    private void clearBibEntryTypes(BibDatabaseMode mode) throws BackingStoreException {
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
                try {
                    return this.getInt(key);
                } catch (ClassCastException e3) {
                    return this.getDouble(key);
                }
            }
        }
    }

    /**
     * Removes all entries keyed by prefix+number, where number is equal to or higher than the given number.
     *
     * @param number or higher.
     */
    private void purgeSeries(String prefix, int number) {
        int n = number;
        while (get(prefix + n) != null) {
            remove(prefix + n);
            n++;
        }
    }

    /**
     * Exports Preferences to an XML file.
     *
     * @param filename String File to export to
     */
    public void exportPreferences(String filename) throws JabRefException {
        exportPreferences(Path.of(filename));
    }

    public void exportPreferences(Path file) throws JabRefException {
        try (OutputStream os = Files.newOutputStream(file)) {
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
     * @throws JabRefException thrown if importing the preferences failed due to an InvalidPreferencesFormatException or an IOException
     */
    public void importPreferences(String filename) throws JabRefException {
        importPreferences(Path.of(filename));
    }

    public void importPreferences(Path file) throws JabRefException {
        try (InputStream is = Files.newInputStream(file)) {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException | IOException ex) {
            throw new JabRefException("Could not import preferences", Localization.lang("Could not import preferences"),
                    ex);
        }
    }

    /**
     * ONLY FOR TESTING!
     * <p>
     * Do not use in production code. Otherwise the singleton pattern is broken and preferences might get lost.
     *
     * @param owPrefs The custom preferences to overwrite the currently present
     */
    public void overwritePreferences(JabRefPreferences owPrefs) {
        singleton = owPrefs;
    }

    public String getWrappedUsername() {
        return '[' + get(DEFAULT_OWNER) + ']';
    }

    public FileHistory getFileHistory() {
        return new FileHistory(getStringList(RECENT_DATABASES).stream().map(Path::of).collect(Collectors.toList()));
    }

    public void storeFileHistory(FileHistory history) {
        if (!history.isEmpty()) {
            putStringList(RECENT_DATABASES, history.getHistory().stream().map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
        }
    }

    @Override
    public FilePreferences getFilePreferences() {
        return new FilePreferences(
                getUser(),
                get(MAIN_FILE_DIRECTORY),
                getBoolean(BIB_LOC_AS_PRIMARY_DIR),
                get(IMPORT_FILENAMEPATTERN),
                get(IMPORT_FILEDIRPATTERN),
                getBoolean(DOWNLOAD_LINKED_FILES));
    }

    @Override
    public FieldWriterPreferences getFieldWriterPreferences() {
        return new FieldWriterPreferences(
                getBoolean(RESOLVE_STRINGS_ALL_FIELDS),
                getStringList(DO_NOT_RESOLVE_STRINGS_FOR).stream().map(FieldFactory::parseField).collect(Collectors.toList()),
                getFieldContentParserPreferences());
    }

    @Override
    public FieldContentFormatterPreferences getFieldContentParserPreferences() {
        return new FieldContentFormatterPreferences(getStringList(NON_WRAPPABLE_FIELDS).stream().map(FieldFactory::parseField).collect(Collectors.toList()));
    }

    @Override
    public boolean isKeywordSyncEnabled() {
        return getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)
                && getBoolean(JabRefPreferences.AUTOSYNCSPECIALFIELDSTOKEYWORDS);
    }

    @Override
    public ImportFormatPreferences getImportFormatPreferences() {
        return new ImportFormatPreferences(
                customImports,
                getDefaultEncoding(),
                getKeywordDelimiter(),
                getCitationKeyPatternPreferences(),
                getFieldContentParserPreferences(),
                getXMPPreferences(),
                isKeywordSyncEnabled());
    }

    @Override
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
        return new SavePreferences(
                saveInOriginalOrder,
                saveOrder,
                this.getDefaultEncoding(),
                SavePreferences.DatabaseSaveType.ALL,
                false,
                this.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT),
                this.getFieldWriterPreferences(),
                getCitationKeyPatternPreferences());
    }

    public SavePreferences loadForSaveFromPreferences() {
        return new SavePreferences(
                false,
                null,
                this.getDefaultEncoding(),
                SavePreferences.DatabaseSaveType.ALL,
                true,
                this.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT),
                this.getFieldWriterPreferences(),
                getCitationKeyPatternPreferences());
    }

    public ExporterFactory getExporterFactory(JournalAbbreviationRepository abbreviationRepository) {
        List<TemplateExporter> customFormats = getCustomExportFormats(abbreviationRepository);
        LayoutFormatterPreferences layoutPreferences = this.getLayoutFormatterPreferences(abbreviationRepository);
        SavePreferences savePreferences = this.loadForExportFromPreferences();
        XmpPreferences xmpPreferences = this.getXMPPreferences();
        return ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);
    }

    @Override
    public LayoutFormatterPreferences getLayoutFormatterPreferences(JournalAbbreviationRepository repository) {
        return new LayoutFormatterPreferences(
                getNameFormatterPreferences(),
                getFileLinkPreferences(),
                repository);
    }

    @Override
    public XmpPreferences getXMPPreferences() {
        return new XmpPreferences(
                getBoolean(USE_XMP_PRIVACY_FILTER),
                getStringList(XMP_PRIVACY_FILTERS).stream().map(FieldFactory::parseField).collect(Collectors.toSet()),
                getKeywordDelimiter());
    }

    @Override
    public OpenOfficePreferences getOpenOfficePreferences() {
        return new OpenOfficePreferences(
                get(JabRefPreferences.OO_JARS_PATH),
                get(JabRefPreferences.OO_EXECUTABLE_PATH),
                get(JabRefPreferences.OO_PATH),
                getBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES),
                getBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING),
                getBoolean(JabRefPreferences.OO_SHOW_PANEL),
                getStringList(JabRefPreferences.OO_EXTERNAL_STYLE_FILES),
                get(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE));
    }

    @Override
    public void setOpenOfficePreferences(OpenOfficePreferences openOfficePreferences) {
        put(JabRefPreferences.OO_JARS_PATH, openOfficePreferences.getJarsPath());
        put(JabRefPreferences.OO_EXECUTABLE_PATH, openOfficePreferences.getExecutablePath());
        put(JabRefPreferences.OO_PATH, openOfficePreferences.getInstallationPath());
        putBoolean(JabRefPreferences.OO_USE_ALL_OPEN_BASES, openOfficePreferences.getUseAllDatabases());
        putBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING, openOfficePreferences.getSyncWhenCiting());
        putBoolean(JabRefPreferences.OO_SHOW_PANEL, openOfficePreferences.getShowPanel());
        putStringList(JabRefPreferences.OO_EXTERNAL_STYLE_FILES, openOfficePreferences.getExternalStyles());
        put(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE, openOfficePreferences.getCurrentStyle());
    }

    private NameFormatterPreferences getNameFormatterPreferences() {
        return new NameFormatterPreferences(getStringList(NAME_FORMATER_KEY), getStringList(NAME_FORMATTER_VALUE));
    }

    private FileLinkPreferences getFileLinkPreferences() {
        return new FileLinkPreferences(
                get(MAIN_FILE_DIRECTORY),
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
        putStringList(CYCLE_PREVIEW, previewPreferences.getPreviewCycle().stream().map(layout -> {
            if (layout instanceof CitationStylePreviewLayout) {
                return ((CitationStylePreviewLayout) layout).getFilePath();
            } else {
                return layout.getName();
            }
        }).collect(Collectors.toList()));
        putDouble(PREVIEW_PANEL_HEIGHT, previewPreferences.getPreviewPanelDividerPosition().doubleValue());
        put(PREVIEW_STYLE, previewPreferences.getPreviewStyle());
        putBoolean(PREVIEW_AS_TAB, previewPreferences.showPreviewAsExtraTab());
        return this;
    }

    @Override
    public PreviewPreferences getPreviewPreferences() {
        List<String> cycle = getStringList(CYCLE_PREVIEW);
        double panelHeight = getDouble(PREVIEW_PANEL_HEIGHT);
        String style = get(PREVIEW_STYLE);
        String styleDefault = (String) defaults.get(PREVIEW_STYLE);
        boolean showAsTab = getBoolean(PREVIEW_AS_TAB);

        // For backwards compatibility always add at least the default preview to the cycle
        if (cycle.isEmpty()) {
            cycle.add("Preview");
        }

        List<PreviewLayout> layouts = cycle.stream()
                                           .map(layout -> {
                                               if (CitationStyle.isCitationStyleFile(layout)) {
                                                   return CitationStyle.createCitationStyleFromFile(layout)
                                                                       .map(file -> (PreviewLayout) new CitationStylePreviewLayout(file))
                                                                       .orElse(null);
                                               } else {
                                                   return new TextBasedPreviewLayout(style, getLayoutFormatterPreferences(Globals.journalAbbreviationRepository));
                                               }
                                           })
                                           .filter(Objects::nonNull)
                                           .collect(Collectors.toList());

        int cyclePos;
        int storedCyclePos = getInt(CYCLE_PREVIEW_POS);
        if (storedCyclePos < layouts.size()) {
            cyclePos = storedCyclePos;
        } else {
            cyclePos = 0; // fallback if stored position is no longer valid
        }

        return new PreviewPreferences(layouts, cyclePos, panelHeight, style, styleDefault, showAsTab);
    }

    public ProtectedTermsPreferences getProtectedTermsPreferences() {
        return new ProtectedTermsPreferences(
                getStringList(PROTECTED_TERMS_ENABLED_INTERNAL),
                getStringList(PROTECTED_TERMS_ENABLED_EXTERNAL),
                getStringList(PROTECTED_TERMS_DISABLED_INTERNAL),
                getStringList(PROTECTED_TERMS_DISABLED_EXTERNAL));
    }

    @Override
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
        return new JournalAbbreviationPreferences(getStringList(EXTERNAL_JOURNAL_LISTS), getDefaultEncoding());
    }

    @Override
    public CleanupPreferences getCleanupPreferences(JournalAbbreviationRepository abbreviationRepository) {
        return new CleanupPreferences(
                getLayoutFormatterPreferences(abbreviationRepository),
                getFilePreferences());
    }

    @Override
    public CleanupPreset getCleanupPreset() {
        Set<CleanupPreset.CleanupStep> activeJobs = EnumSet.noneOf(CleanupPreset.CleanupStep.class);

        for (CleanupPreset.CleanupStep action : EnumSet.allOf(CleanupPreset.CleanupStep.class)) {
            if (getBoolean(JabRefPreferences.CLEANUP + action.name())) {
                activeJobs.add(action);
            }
        }

        FieldFormatterCleanups formatterCleanups = Cleanups.parse(getStringList(JabRefPreferences.CLEANUP_FORMATTERS));

        return new CleanupPreset(activeJobs, formatterCleanups);
    }

    @Override
    public void setCleanupPreset(CleanupPreset cleanupPreset) {
        for (CleanupPreset.CleanupStep action : EnumSet.allOf(CleanupPreset.CleanupStep.class)) {
            putBoolean(JabRefPreferences.CLEANUP + action.name(), cleanupPreset.isActive(action));
        }

        putStringList(JabRefPreferences.CLEANUP_FORMATTERS, cleanupPreset.getFormatterCleanups().getAsStringList(OS.NEWLINE));
    }

    @Override
    public void storeExportSaveOrder(SaveOrderConfig config) {
        putBoolean(EXPORT_PRIMARY_SORT_DESCENDING, config.getSortCriteria().get(0).descending);
        putBoolean(EXPORT_SECONDARY_SORT_DESCENDING, config.getSortCriteria().get(1).descending);
        putBoolean(EXPORT_TERTIARY_SORT_DESCENDING, config.getSortCriteria().get(2).descending);
        putBoolean(EXPORT_IN_ORIGINAL_ORDER, config.saveInOriginalOrder());
        putBoolean(EXPORT_IN_SPECIFIED_ORDER, config.saveInSpecifiedOrder());

        put(EXPORT_PRIMARY_SORT_FIELD, config.getSortCriteria().get(0).field.getName());
        put(EXPORT_SECONDARY_SORT_FIELD, config.getSortCriteria().get(1).field.getName());
        put(EXPORT_TERTIARY_SORT_FIELD, config.getSortCriteria().get(2).field.getName());
    }

    private SaveOrderConfig loadTableSaveOrder() {
        SaveOrderConfig config = new SaveOrderConfig();

        updateMainTableColumns();
        List<MainTableColumnModel> sortOrder = createMainTableColumnSortOrder();

        sortOrder.forEach(column -> config.getSortCriteria().add(new SaveOrderConfig.SortCriterion(
                FieldFactory.parseField(column.getQualifier()),
                column.getSortType().toString())));

        return config;
    }

    @Override
    public SaveOrderConfig loadExportSaveOrder() {
        return new SaveOrderConfig(getBoolean(EXPORT_IN_ORIGINAL_ORDER), getBoolean(EXPORT_IN_SPECIFIED_ORDER),
                new SaveOrderConfig.SortCriterion(FieldFactory.parseField(get(EXPORT_PRIMARY_SORT_FIELD)), getBoolean(EXPORT_PRIMARY_SORT_DESCENDING)),
                new SaveOrderConfig.SortCriterion(FieldFactory.parseField(get(EXPORT_SECONDARY_SORT_FIELD)), getBoolean(EXPORT_SECONDARY_SORT_DESCENDING)),
                new SaveOrderConfig.SortCriterion(FieldFactory.parseField(get(EXPORT_TERTIARY_SORT_FIELD)), getBoolean(EXPORT_TERTIARY_SORT_DESCENDING)));
    }

    @Override
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
    }

    @Override
    public AutoLinkPreferences getAutoLinkPreferences() {
        return new AutoLinkPreferences(
                getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY),
                get(JabRefPreferences.AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY),
                getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY),
                getKeywordDelimiter());
    }

    public void storeSidePanePreferredPositions(Map<SidePaneType, Integer> preferredPositions) {
        // Split the map into a pair of parallel String lists suitable for storage
        List<String> names = preferredPositions.keySet().stream()
                                               .map(Enum::toString)
                                               .collect(Collectors.toList());

        List<String> positions = preferredPositions.values().stream()
                                                   .map(integer -> Integer.toString(integer))
                                                   .collect(Collectors.toList());

        putStringList(SIDE_PANE_COMPONENT_NAMES, names);
        putStringList(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, positions);
    }

    @Override
    public Path getWorkingDir() {
        return Path.of(get(WORKING_DIRECTORY));
    }

    @Override
    public void setWorkingDir(Path dir) {
        put(WORKING_DIRECTORY, dir.toString());
    }

    public void setPreviewStyle(String previewStyle) {
        put(PREVIEW_STYLE, previewStyle);
    }

    public String getPreviewStyle() {
        return get(PREVIEW_STYLE);
    }

    public Optional<Integer> getFontSize() {
        if (getBoolean(OVERRIDE_DEFAULT_FONT_SIZE)) {
            return Optional.of(getInt(MAIN_FONT_SIZE));
        } else {
            return Optional.empty();
        }
    }

    public String setLastPreferencesExportPath() {
        return get(PREFS_EXPORT_PATH);
    }

    public void setLastPreferencesExportPath(Path exportFile) {
        put(PREFS_EXPORT_PATH, exportFile.toString());
    }

    public void setIdBasedFetcherForEntryGenerator(String fetcherName) {
        put(ID_ENTRY_GENERATOR, fetcherName);
    }

    public String getIdBasedFetcherForEntryGenerator() {
        return get(ID_ENTRY_GENERATOR);
    }

    @Override
    public List<TemplateExporter> getCustomExportFormats(JournalAbbreviationRepository abbreviationRepository) {
        int i = 0;
        List<TemplateExporter> formats = new ArrayList<>();
        String exporterName;
        String filename;
        String extension;
        LayoutFormatterPreferences layoutPreferences = getLayoutFormatterPreferences(abbreviationRepository);
        SavePreferences savePreferences = loadForExportFromPreferences();
        List<String> formatData;
        while (!((formatData = getStringList(CUSTOM_EXPORT_FORMAT + i)).isEmpty())) {
            exporterName = formatData.get(EXPORTER_NAME_INDEX);
            filename = formatData.get(EXPORTER_FILENAME_INDEX);
            extension = formatData.get(EXPORTER_EXTENSION_INDEX);
            TemplateExporter format = new TemplateExporter(exporterName, filename, extension,
                    layoutPreferences, savePreferences);
            format.setCustomExport(true);
            formats.add(format);
            i++;
        }
        return formats;
    }

    @Override
    public void storeCustomExportFormats(List<TemplateExporter> exporters) {
        if (exporters.isEmpty()) {
            purgeCustomExportFormats(0);
        } else {
            for (int i = 0; i < exporters.size(); i++) {
                List<String> exporterData = new ArrayList<>();
                exporterData.add(EXPORTER_NAME_INDEX, exporters.get(i).getName());
                exporterData.add(EXPORTER_FILENAME_INDEX, exporters.get(i).getLayoutFileName());
                // Only stores the first extension associated with FileType
                exporterData.add(EXPORTER_EXTENSION_INDEX, exporters.get(i).getFileType().getExtensions().get(0));
                putStringList(CUSTOM_EXPORT_FORMAT + i, exporterData);
            }
            purgeCustomExportFormats(exporters.size());
        }
    }

    private void purgeCustomExportFormats(int from) {
        int i = from;
        while (!getStringList(CUSTOM_EXPORT_FORMAT + i).isEmpty()) {
            remove(CUSTOM_EXPORT_FORMAT + i);
            i++;
        }
    }

    @Override
    public String getExportWorkingDirectory() {
        return get(EXPORT_WORKING_DIRECTORY);
    }

    @Override
    public void setExportWorkingDirectory(String layoutFileDirString) {
        put(EXPORT_WORKING_DIRECTORY, layoutFileDirString);
    }

    @Override
    public boolean shouldWarnAboutDuplicatesForImport() {
        return getBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION);
    }

    @Override
    public void setShouldWarnAboutDuplicatesForImport(boolean value) {
        putBoolean(JabRefPreferences.WARN_ABOUT_DUPLICATES_IN_INSPECTION, value);
    }

    @Override
    public void saveCustomEntryTypes() {
        saveCustomEntryTypes(BibDatabaseMode.BIBTEX);
        saveCustomEntryTypes(BibDatabaseMode.BIBLATEX);
    }

    private void saveCustomEntryTypes(BibDatabaseMode bibDatabaseMode) {
        List<BibEntryType> customBiblatexBibTexTypes = new ArrayList<>(Globals.entryTypesManager.getAllTypes(bibDatabaseMode));

        storeBibEntryTypes(customBiblatexBibTexTypes, bibDatabaseMode);
    }

    public NewLineSeparator getNewLineSeparator() {
        return NewLineSeparator.parse(get(JabRefPreferences.NEWLINE));
    }

    public void setNewLineSeparator(NewLineSeparator newLineSeparator) {
        String escapeChars = newLineSeparator.toString();
        put(JabRefPreferences.NEWLINE, escapeChars);

        // we also have to change Globals variable as globals is not a getter, but a constant
        OS.NEWLINE = escapeChars;
    }

    //*************************************************************************************************************
    // GeneralPreferences
    //*************************************************************************************************************

    @Override
    public Language getLanguage() {
        String languageId = get(LANGUAGE);
        return Stream.of(Language.values())
                     .filter(language -> language.getId().equalsIgnoreCase(languageId))
                     .findFirst()
                     .orElse(Language.ENGLISH);
    }

    @Override
    public void setLanguage(Language language) {
        Language oldLanguage = getLanguage();
        put(LANGUAGE, language.getId());
        if (language != oldLanguage) {
            // Update any defaults that might be language dependent:
            setLanguageDependentDefaultValues();
        }
    }

    @Override
    public Charset getDefaultEncoding() {
        return Charset.forName(get(DEFAULT_ENCODING));
    }

    @Override
    public boolean shouldCollectTelemetry() {
        return getBoolean(COLLECT_TELEMETRY);
    }

    @Override
    public void setShouldCollectTelemetry(boolean value) {
        putBoolean(COLLECT_TELEMETRY, value);
    }

    @Override
    public boolean shouldAskToCollectTelemetry() {
        return getBoolean(ALREADY_ASKED_TO_COLLECT_TELEMETRY);
    }

    @Override
    public void askedToCollectTelemetry() {
        putBoolean(ALREADY_ASKED_TO_COLLECT_TELEMETRY, true);
    }

    @Override
    public String getUnwantedCharacters() {
        return get(UNWANTED_CITATION_KEY_CHARACTERS);
    }

    @Override
    public boolean getAllowIntegerEdition() {
        return getBoolean(ALLOW_INTEGER_EDITION_BIBTEX);
    }

    @Override
    public GeneralPreferences getGeneralPreferences() {
        return new GeneralPreferences(
                getDefaultEncoding(),
                getBoolean(BIBLATEX_DEFAULT_MODE) ? BibDatabaseMode.BIBLATEX : BibDatabaseMode.BIBTEX,
                getBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION),
                getBoolean(CONFIRM_DELETE),
                getBoolean(ALLOW_INTEGER_EDITION_BIBTEX),
                getBoolean(MEMORY_STICK_MODE),
                shouldCollectTelemetry(),
                getBoolean(SHOW_ADVANCED_HINTS));
    }

    @Override
    public void storeGeneralPreferences(GeneralPreferences preferences) {
        put(DEFAULT_ENCODING, preferences.getDefaultEncoding().name());
        putBoolean(BIBLATEX_DEFAULT_MODE, (preferences.getDefaultBibDatabaseMode() == BibDatabaseMode.BIBLATEX));
        putBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION, preferences.isWarnAboutDuplicatesInInspection());
        putBoolean(CONFIRM_DELETE, preferences.isConfirmDelete());
        putBoolean(ALLOW_INTEGER_EDITION_BIBTEX, preferences.isAllowIntegerEditionBibtex());
        putBoolean(MEMORY_STICK_MODE, preferences.isMemoryStickMode());
        setShouldCollectTelemetry(preferences.isCollectTelemetry());
        putBoolean(SHOW_ADVANCED_HINTS, preferences.isShowAdvancedHints());
    }

    @Override
    public OwnerPreferences getOwnerPreferences() {
        return new OwnerPreferences(
                getBoolean(USE_OWNER),
                get(DEFAULT_OWNER),
                getBoolean(OVERWRITE_OWNER));
    }

    @Override
    public void storeOwnerPreferences(OwnerPreferences preferences) {
        putBoolean(USE_OWNER, preferences.isUseOwner());
        put(DEFAULT_OWNER, preferences.getDefaultOwner());
        putBoolean(OVERWRITE_OWNER, preferences.isOverwriteOwner());
    }

    @Override
    public TimestampPreferences getTimestampPreferences() {
        return new TimestampPreferences(
                getBoolean(USE_TIME_STAMP),
                getBoolean(UPDATE_TIMESTAMP),
                FieldFactory.parseField(get(TIME_STAMP_FIELD)),
                get(TIME_STAMP_FORMAT),
                getBoolean(OVERWRITE_TIME_STAMP));
    }

    @Override
    public void storeTimestampPreferences(TimestampPreferences preferences) {
        putBoolean(USE_TIME_STAMP, preferences.isUseTimestamps());
        putBoolean(UPDATE_TIMESTAMP, preferences.isUpdateTimestamp());
        put(TIME_STAMP_FIELD, preferences.getTimestampField().getName());
        put(TIME_STAMP_FORMAT, preferences.getTimestampFormat());
        putBoolean(OVERWRITE_TIME_STAMP, preferences.isOverwriteTimestamp());
    }

    //*************************************************************************************************************
    // ToDo: GroupPreferences
    //*************************************************************************************************************

    @Override
    public GroupViewMode getGroupViewMode() {
        return GroupViewMode.valueOf(get(GROUP_INTERSECT_UNION_VIEW_MODE));
    }

    @Override
    public void setGroupViewMode(GroupViewMode mode) {
        put(GROUP_INTERSECT_UNION_VIEW_MODE, mode.name());
    }

    @Override
    public boolean getDisplayGroupCount() {
        return getBoolean(JabRefPreferences.DISPLAY_GROUP_COUNT);
    }

    //*************************************************************************************************************
    // EntryEditorPreferences
    //*************************************************************************************************************

    /**
     * Creates a list of defined tabs in the entry editor from cache
     *
     * @return a list of defined tabs
     */
    @Override
    public Map<String, Set<Field>> getEntryEditorTabList() {
        if (entryEditorTabList == null) {
            updateEntryEditorTabList();
        }
        return entryEditorTabList;
    }

    /**
     * Reloads the list of the currently defined tabs  in the entry editor from scratch to cache
     */
    @Override
    public void updateEntryEditorTabList() {
        Map<String, Set<Field>> tabs = new LinkedHashMap<>();
        int i = 0;
        String name;
        if (hasKey(CUSTOM_TAB_NAME + 0)) {
            // The user has modified from the default values:
            while (hasKey(CUSTOM_TAB_NAME + i)) {
                name = get(CUSTOM_TAB_NAME + i);
                Set<Field> entry = FieldFactory.parseFieldList(get(CUSTOM_TAB_FIELDS + i));
                tabs.put(name, entry);
                i++;
            }
        } else {
            // Nothing set, so we use the default values:
            while (get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i) != null) {
                name = get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i);
                Set<Field> entry = FieldFactory.parseFieldList(get(CUSTOM_TAB_FIELDS + "_def" + i));
                tabs.put(name, entry);
                i++;
            }
        }
        entryEditorTabList = tabs;
    }

    /**
     * Stores the defined tabs and corresponding fields in the preferences.
     *
     * @param customTabs a map of tab names and the corresponding set of fields to be displayed in
     */
    @Override
    public void storeEntryEditorTabList(Map<String, Set<Field>> customTabs) {
        String[] names = customTabs.keySet().toArray(String[]::new);
        String[] fields = customTabs.values().stream()
                                    .map(set -> set.stream()
                                                   .map(Field::getName)
                                                   .collect(Collectors.joining(STRINGLIST_DELIMITER.toString())))
                                    .toArray(String[]::new);

        for (int i = 0; i < customTabs.size(); i++) {
            put(CUSTOM_TAB_NAME + i, names[i]);
            put(CUSTOM_TAB_FIELDS + i, fields[i]);
        }

        purgeSeries(CUSTOM_TAB_NAME, customTabs.size());
        purgeSeries(CUSTOM_TAB_FIELDS, customTabs.size());

        updateEntryEditorTabList();
    }

    /**
     * Get a Map of default tab names to default tab fields.
     *
     * @return A map of keys with tab names and a set of corresponding fields
     */
    @Override
    public Map<String, Set<Field>> getDefaultTabNamesAndFields() {
        Map<String, Set<Field>> customTabsMap = new LinkedHashMap<>();

        int defNumber = 0;
        while (true) {
            // Saved as 'CUSTOMTABNAME_def{number}' and seperated by ';'
            String name = (String) defaults.get(CUSTOM_TAB_NAME + "_def" + defNumber);
            String fields = (String) defaults.get(CUSTOM_TAB_FIELDS + "_def" + defNumber);

            if (StringUtil.isNullOrEmpty(name) || StringUtil.isNullOrEmpty(fields)) {
                break;
            }

            customTabsMap.put(name, FieldFactory.parseFieldList((String) defaults.get(CUSTOM_TAB_FIELDS + "_def" + defNumber)));
            defNumber++;
        }
        return customTabsMap;
    }

    /**
     * Get a Map of default tab names to default tab fields.
     *
     * @return A map of keys with tab names and a set of corresponding fields
     */
    @Override
    public List<Field> getAllDefaultTabFieldNames() {
        List<Field> customFields = new ArrayList<>();

        int defNumber = 0;
        while (true) {
            // saved as CUSTOMTABNAME_def{number} and ; separated
            String fields = (String) defaults.get(CUSTOM_TAB_FIELDS + "_def" + defNumber);

            if (StringUtil.isNullOrEmpty(fields)) {
                break;
            }

            customFields.addAll(Arrays.stream(fields.split(STRINGLIST_DELIMITER.toString())).map(FieldFactory::parseField).collect(Collectors.toList()));
            defNumber++;
        }
        return customFields;
    }

    @Override
    public EntryEditorPreferences getEntryEditorPreferences() {
        return new EntryEditorPreferences(getEntryEditorTabList(),
                getBoolean(AUTO_OPEN_FORM),
                getBoolean(SHOW_RECOMMENDATIONS),
                getBoolean(ACCEPT_RECOMMENDATIONS),
                getBoolean(SHOW_LATEX_CITATIONS),
                getBoolean(DEFAULT_SHOW_SOURCE),
                getBoolean(VALIDATE_IN_ENTRY_EDITOR));
    }

    @Override
    public void storeEntryEditorPreferences(EntryEditorPreferences preferences) {
        storeEntryEditorTabList(preferences.getEntryEditorTabList());
        putBoolean(AUTO_OPEN_FORM, preferences.shouldOpenOnNewEntry());
        putBoolean(SHOW_RECOMMENDATIONS, preferences.shouldShowRecommendationsTab());
        putBoolean(ACCEPT_RECOMMENDATIONS, preferences.isMrdlibAccepted());
        putBoolean(SHOW_LATEX_CITATIONS, preferences.shouldShowLatexCitationsTab());
        putBoolean(DEFAULT_SHOW_SOURCE, preferences.showSourceTabByDefault());
        putBoolean(VALIDATE_IN_ENTRY_EDITOR, preferences.isEnableValidation());
    }

    //*************************************************************************************************************
    // Network preferences
    //*************************************************************************************************************

    @Override
    public RemotePreferences getRemotePreferences() {
        return new RemotePreferences(getInt(REMOTE_SERVER_PORT), getBoolean(USE_REMOTE_SERVER));
    }

    @Override
    public void storeRemotePreferences(RemotePreferences remotePreferences) {
        putInt(REMOTE_SERVER_PORT, remotePreferences.getPort());
        putBoolean(USE_REMOTE_SERVER, remotePreferences.useRemoteServer());
    }

    @Override
    public ProxyPreferences getProxyPreferences() {
        Boolean useProxy = getBoolean(PROXY_USE);
        String hostname = get(PROXY_HOSTNAME);
        String port = get(PROXY_PORT);
        Boolean useAuthentication = getBoolean(PROXY_USE_AUTHENTICATION);
        String username = get(PROXY_USERNAME);
        String password = get(PROXY_PASSWORD);
        return new ProxyPreferences(useProxy, hostname, port, useAuthentication, username, password);
    }

    @Override
    public void storeProxyPreferences(ProxyPreferences proxyPreferences) {
        putBoolean(PROXY_USE, proxyPreferences.isUseProxy());
        put(PROXY_HOSTNAME, proxyPreferences.getHostname());
        put(PROXY_PORT, proxyPreferences.getPort());
        putBoolean(PROXY_USE_AUTHENTICATION, proxyPreferences.isUseAuthentication());
        put(PROXY_USERNAME, proxyPreferences.getUsername());
        put(PROXY_PASSWORD, proxyPreferences.getPassword());
    }

    //*************************************************************************************************************
    // CitationKeyPatternPreferences
    //*************************************************************************************************************

    /**
     * Creates the GlobalCitationKeyPattern from cache
     *
     * @return GlobalCitationKeyPattern containing all keys without a parent AbstractCitationKeyPattern
     */
    @Override
    public GlobalCitationKeyPattern getGlobalCitationKeyPattern() {
        if (this.globalCitationKeyPattern == null) {
            updateGlobalCitationKeyPattern();
        }
        return this.globalCitationKeyPattern;
    }

    /**
     * Reloads the GlobalCitationKeyPattern from scratch to cache
     */
    @Override
    public void updateGlobalCitationKeyPattern() {
        this.globalCitationKeyPattern = GlobalCitationKeyPattern.fromPattern(get(DEFAULT_CITATION_KEY_PATTERN));
        Preferences preferences = Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(CITATION_KEY_PATTERNS_NODE);
        try {
            String[] keys = preferences.keys();
            if (keys.length > 0) {
                for (String key : keys) {
                    this.globalCitationKeyPattern.addCitationKeyPattern(
                            EntryTypeFactory.parse(key),
                            preferences.get(key, null));
                }
            }
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences.getKeyPattern", ex);
        }
    }

    /**
     * Stores the given key pattern in the preferences
     *
     * @param pattern the pattern to store
     */
    public void storeGlobalCitationKeyPattern(GlobalCitationKeyPattern pattern) {
        this.globalCitationKeyPattern = pattern;

        if ((this.globalCitationKeyPattern.getDefaultValue() == null)
                || this.globalCitationKeyPattern.getDefaultValue().isEmpty()) {
            put(DEFAULT_CITATION_KEY_PATTERN, "");
        } else {
            put(DEFAULT_CITATION_KEY_PATTERN, globalCitationKeyPattern.getDefaultValue().get(0));
        }

        // Store overridden definitions to Preferences.
        Preferences preferences = Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(CITATION_KEY_PATTERNS_NODE);
        try {
            preferences.clear(); // We remove all old entries.
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences::putKeyPattern", ex);
        }

        for (EntryType entryType : pattern.getAllKeys()) {
            if (!pattern.isDefaultValue(entryType)) {
                // first entry in the map is the full pattern
                preferences.put(entryType.getName(), pattern.getValue(entryType).get(0));
            }
        }

        updateGlobalCitationKeyPattern();
    }

    private void clearCitationKeyPatterns() throws BackingStoreException {
        Preferences preferences = Preferences.userNodeForPackage(PREFS_BASE_CLASS).node(CITATION_KEY_PATTERNS_NODE);
        preferences.clear();
        updateGlobalCitationKeyPattern();
    }

    @Override
    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        CitationKeyPatternPreferences.KeySuffix keySuffix =
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B;

        if (getBoolean(KEY_GEN_ALWAYS_ADD_LETTER)) {
            keySuffix = CitationKeyPatternPreferences.KeySuffix.ALWAYS;
        } else if (getBoolean(KEY_GEN_FIRST_LETTER_A)) {
            keySuffix = CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A;
        }

        return new CitationKeyPatternPreferences(
                getBoolean(AVOID_OVERWRITING_KEY),
                getBoolean(WARN_BEFORE_OVERWRITING_KEY),
                getBoolean(GENERATE_KEYS_BEFORE_SAVING),
                keySuffix,
                get(KEY_PATTERN_REGEX),
                get(KEY_PATTERN_REPLACEMENT),
                get(UNWANTED_CITATION_KEY_CHARACTERS),
                getGlobalCitationKeyPattern(),
                getKeywordDelimiter());
    }

    @Override
    public void storeCitationKeyPatternPreferences(CitationKeyPatternPreferences preferences) {
        putBoolean(AVOID_OVERWRITING_KEY, preferences.shouldAvoidOverwriteCiteKey());
        putBoolean(WARN_BEFORE_OVERWRITING_KEY, preferences.shouldWarnBeforeOverwriteCiteKey());
        putBoolean(GENERATE_KEYS_BEFORE_SAVING, preferences.shouldGenerateCiteKeysBeforeSaving());

        switch (preferences.getKeySuffix()) {
            case ALWAYS:
                putBoolean(KEY_GEN_ALWAYS_ADD_LETTER, true);
                putBoolean(KEY_GEN_FIRST_LETTER_A, false);
                break;
            case SECOND_WITH_A:
                putBoolean(KEY_GEN_ALWAYS_ADD_LETTER, false);
                putBoolean(KEY_GEN_FIRST_LETTER_A, true);
                break;
            default:
            case SECOND_WITH_B:
                putBoolean(KEY_GEN_ALWAYS_ADD_LETTER, false);
                putBoolean(KEY_GEN_FIRST_LETTER_A, false);
                break;
        }

        put(KEY_PATTERN_REGEX, preferences.getKeyPatternRegex());
        put(KEY_PATTERN_REPLACEMENT, preferences.getKeyPatternReplacement());
        put(UNWANTED_CITATION_KEY_CHARACTERS, preferences.getUnwantedCharacters());

        storeGlobalCitationKeyPattern(preferences.getKeyPattern());
    }

    //*************************************************************************************************************
    // ExternalApplicationsPreferences
    //*************************************************************************************************************

    @Override
    public ExternalApplicationsPreferences getExternalApplicationsPreferences() {
        return new ExternalApplicationsPreferences(
                get(EMAIL_SUBJECT),
                getBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES),
                get(PUSH_TO_APPLICATION),
                get(CITE_COMMAND),
                !getBoolean(USE_DEFAULT_CONSOLE_APPLICATION), // mind the !
                get(CONSOLE_COMMAND),
                !getBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION), // mind the !
                get(FILE_BROWSER_COMMAND));
    }

    @Override
    public void storeExternalApplicationsPreferences(ExternalApplicationsPreferences preferences) {
        put(EMAIL_SUBJECT, preferences.getEmailSubject());
        putBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES, preferences.shouldAutoOpenEmailAttachmentsFolder());
        put(PUSH_TO_APPLICATION, preferences.getPushToApplicationName());
        put(CITE_COMMAND, preferences.getCiteCommand());
        putBoolean(USE_DEFAULT_CONSOLE_APPLICATION, !preferences.useCustomTerminal()); // mind the !
        put(CONSOLE_COMMAND, preferences.getCustomTerminalCommand());
        putBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION, !preferences.useCustomFileBrowser()); // mind the !
        put(FILE_BROWSER_COMMAND, preferences.getCustomFileBrowserCommand());
    }

    //*************************************************************************************************************
    // MainTablePreferences
    //*************************************************************************************************************

    /**
     * Creates the GlobalCitationKeyPattern from cache
     *
     * @return GlobalCitationKeyPattern containing all keys without a parent AbstractKeyPattern
     */
    private List<MainTableColumnModel> createMainTableColumns() {
        if (this.mainTableColumns == null) {
            updateMainTableColumns();
        }
        return this.mainTableColumns;
    }

    /**
     * Reloads the GlobalCitationKeyPattern from scratch
     */
    @Override
    public void updateMainTableColumns() {
        List<String> columnNames = getStringList(COLUMN_NAMES);

        List<Double> columnWidths = getStringList(COLUMN_WIDTHS)
                .stream()
                .map(string -> {
                    try {
                        return Double.parseDouble(string);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Exception while parsing column widths. Choosing default.", e);
                        return ColumnPreferences.DEFAULT_COLUMN_WIDTH;
                    }
                })
                .collect(Collectors.toList());

        List<SortType> columnSortTypes = getStringList(COLUMN_SORT_TYPES)
                .stream()
                .map(SortType::valueOf)
                .collect(Collectors.toList());

        List<MainTableColumnModel> columns = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            MainTableColumnModel columnModel = MainTableColumnModel.parse(columnNames.get(i));

            if (i < columnWidths.size()) {
                columnModel.widthProperty().setValue(columnWidths.get(i));
            }

            if (i < columnSortTypes.size()) {
                columnModel.sortTypeProperty().setValue(columnSortTypes.get(i));
            }

            columns.add(columnModel);
        }

        mainTableColumns = columns;
    }

    /**
     * Creates the ColumnSortOrder from cache
     *
     * @return List containing only the the columns in its proper sort order
     */
    private List<MainTableColumnModel> createMainTableColumnSortOrder() {
        if (this.mainTableColumnSortOrder == null) {
            updateColumnSortOrder();
        }
        return this.mainTableColumnSortOrder;
    }

    /**
     * Reloads the MainTableColumnSortOrder from scratch to cache
     */
    private void updateColumnSortOrder() {
        List<MainTableColumnModel> columnsOrdered = new ArrayList<>();
        getStringList(COLUMN_SORT_ORDER).forEach(columnName ->
                mainTableColumns.stream().filter(column ->
                        column.getName().equals(columnName))
                                             .findFirst()
                                             .ifPresent(columnsOrdered::add));

        mainTableColumnSortOrder = columnsOrdered;
    }

    @Override
    public ColumnPreferences getColumnPreferences() {
        return new ColumnPreferences(
                createMainTableColumns(),
                createMainTableColumnSortOrder());
    }

    /**
     * Stores the ColumnPreferences in the preferences
     *
     * @param columnPreferences the preferences to store
     */
    @Override
    public void storeColumnPreferences(ColumnPreferences columnPreferences) {
        putStringList(COLUMN_NAMES, columnPreferences.getColumns().stream()
                                                     .map(MainTableColumnModel::getName)
                                                     .collect(Collectors.toList()));

        List<String> columnWidthsInOrder = new ArrayList<>();
        columnPreferences.getColumns().forEach(column -> columnWidthsInOrder.add(column.widthProperty().getValue().toString()));
        putStringList(COLUMN_WIDTHS, columnWidthsInOrder);

        List<String> columnSortTypesInOrder = new ArrayList<>();
        columnPreferences.getColumns().forEach(column -> columnSortTypesInOrder.add(column.sortTypeProperty().getValue().toString()));
        putStringList(COLUMN_SORT_TYPES, columnSortTypesInOrder);

        putStringList(COLUMN_SORT_ORDER, columnPreferences
                .getColumnSortOrder().stream()
                .map(MainTableColumnModel::getName)
                .collect(Collectors.toList()));

        // Update cache
        mainTableColumns = columnPreferences.getColumns();
    }

    @Override
    public MainTablePreferences getMainTablePreferences() {
        return new MainTablePreferences(getColumnPreferences(),
                getBoolean(AUTO_RESIZE_MODE),
                getBoolean(EXTRA_FILE_COLUMNS));
    }

    @Override
    public void storeMainTablePreferences(MainTablePreferences mainTablePreferences) {
        storeColumnPreferences(mainTablePreferences.getColumnPreferences());
        putBoolean(AUTO_RESIZE_MODE, mainTablePreferences.getResizeColumnsToFit());
        putBoolean(EXTRA_FILE_COLUMNS, mainTablePreferences.getExtraFileColumnsEnabled());
    }

    @Override
    public MainTableNameFormatPreferences getMainTableNameFormatPreferences() {
        DisplayStyle displayStyle =
                DisplayStyle.LASTNAME_FIRSTNAME;

        if (getBoolean(JabRefPreferences.NAMES_NATBIB)) {
            displayStyle = DisplayStyle.NATBIB;
        } else if (getBoolean(JabRefPreferences.NAMES_AS_IS)) {
            displayStyle = DisplayStyle.AS_IS;
        } else if (getBoolean(JabRefPreferences.NAMES_FIRST_LAST)) {
            displayStyle = DisplayStyle.FIRSTNAME_LASTNAME;
        }

        AbbreviationStyle abbreviationStyle =
                AbbreviationStyle.NONE;

        if (getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES)) {
            abbreviationStyle = AbbreviationStyle.FULL;
        } else if (getBoolean(JabRefPreferences.NAMES_LAST_ONLY)) {
            abbreviationStyle = AbbreviationStyle.LASTNAME_ONLY;
        }

        return new MainTableNameFormatPreferences(displayStyle, abbreviationStyle);
    }

    @Override
    public void storeMainTableNameFormatPreferences(MainTableNameFormatPreferences preferences) {
        putBoolean(JabRefPreferences.NAMES_NATBIB, preferences.getDisplayStyle() == DisplayStyle.NATBIB);
        putBoolean(JabRefPreferences.NAMES_AS_IS, preferences.getDisplayStyle() == DisplayStyle.AS_IS);
        putBoolean(JabRefPreferences.NAMES_FIRST_LAST, preferences.getDisplayStyle() == DisplayStyle.FIRSTNAME_LASTNAME);

        putBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES, preferences.getAbbreviationStyle() == AbbreviationStyle.FULL);
        putBoolean(JabRefPreferences.NAMES_LAST_ONLY, preferences.getAbbreviationStyle() == AbbreviationStyle.LASTNAME_ONLY);
    }

    //*************************************************************************************************************
    // ToDo: Misc preferences
    //*************************************************************************************************************

    @Override
    public AutoCompletePreferences getAutoCompletePreferences() {
        AutoCompletePreferences.NameFormat nameFormat = AutoCompletePreferences.NameFormat.BOTH;
        if (getBoolean(AUTOCOMPLETER_LAST_FIRST)) {
            nameFormat = AutoCompletePreferences.NameFormat.LAST_FIRST;
        } else if (getBoolean(AUTOCOMPLETER_FIRST_LAST)) {
            nameFormat = AutoCompletePreferences.NameFormat.FIRST_LAST;
        }

        return new AutoCompletePreferences(
                getBoolean(AUTO_COMPLETE),
                AutoCompleteFirstNameMode.parse(get(AUTOCOMPLETER_FIRSTNAME_MODE)),
                nameFormat,
                getStringList(AUTOCOMPLETER_COMPLETE_FIELDS).stream().map(FieldFactory::parseField).collect(Collectors.toSet()),
                getJournalAbbreviationPreferences());
    }

    @Override
    public void storeAutoCompletePreferences(AutoCompletePreferences preferences) {
        putBoolean(AUTO_COMPLETE, preferences.shouldAutoComplete());
        put(AUTOCOMPLETER_FIRSTNAME_MODE, preferences.getFirstNameMode().name());
        putStringList(AUTOCOMPLETER_COMPLETE_FIELDS, preferences.getCompleteFields().stream().map(Field::getName).collect(Collectors.toList()));

        if (preferences.getNameFormat() == AutoCompletePreferences.NameFormat.BOTH) {
            putBoolean(AUTOCOMPLETER_LAST_FIRST, false);
            putBoolean(AUTOCOMPLETER_FIRST_LAST, false);
        } else if (preferences.getNameFormat() == AutoCompletePreferences.NameFormat.LAST_FIRST) {
            putBoolean(AUTOCOMPLETER_LAST_FIRST, true);
            putBoolean(AUTOCOMPLETER_FIRST_LAST, false);
        } else {
            putBoolean(AUTOCOMPLETER_LAST_FIRST, false);
            putBoolean(AUTOCOMPLETER_FIRST_LAST, true);
        }
    }

    @Override
    public SpecialFieldsPreferences getSpecialFieldsPreferences() {
        return new SpecialFieldsPreferences(
                getBoolean(SPECIALFIELDSENABLED),
                getBoolean(AUTOSYNCSPECIALFIELDSTOKEYWORDS),
                getBoolean(SERIALIZESPECIALFIELDS));
    }

    @Override
    public void storeSpecialFieldsPreferences(SpecialFieldsPreferences specialFieldsPreferences) {
        putBoolean(SPECIALFIELDSENABLED, specialFieldsPreferences.getSpecialFieldsEnabled());
        putBoolean(AUTOSYNCSPECIALFIELDSTOKEYWORDS, specialFieldsPreferences.getAutoSyncSpecialFieldsToKeyWords());
        putBoolean(SERIALIZESPECIALFIELDS, specialFieldsPreferences.getSerializeSpecialFields());
    }
}
