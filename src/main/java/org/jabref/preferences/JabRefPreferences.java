package org.jabref.preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.scene.control.TableColumn.SortType;

import org.jabref.gui.Globals;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.maintable.NameDisplayPreferences.AbbreviationStyle;
import org.jabref.gui.maintable.NameDisplayPreferences.DisplayStyle;
import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.gui.push.PushToApplications;
import org.jabref.gui.search.SearchDisplayMode;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.theme.Theme;
import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.FetcherApiKey;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.Version;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.strings.StringUtil;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code JabRefPreferences} class provides the preferences and their defaults using the JDK {@code java.util.prefs}
 * class.
 * <p>
 * Internally it defines symbols used to pick a value from the {@code java.util.prefs} interface and keeps a hashmap
 * with all the default values.
 * <p>
 * There are still some similar preferences classes ({@link org.jabref.logic.openoffice.OpenOfficePreferences} and {@link org.jabref.logic.shared.prefs.SharedDatabasePreferences}) which also use
 * the {@code java.util.prefs} API.
 */
public class JabRefPreferences implements PreferencesService {

    // Push to application preferences
    public static final String PUSH_EMACS_PATH = "emacsPath";
    public static final String PUSH_EMACS_ADDITIONAL_PARAMETERS = "emacsParameters";
    public static final String PUSH_LYXPIPE = "lyxpipe";
    public static final String PUSH_TEXSTUDIO_PATH = "TeXstudioPath";
    public static final String PUSH_WINEDT_PATH = "winEdtPath";
    public static final String PUSH_TEXMAKER_PATH = "texmakerPath";
    public static final String PUSH_VIM_SERVER = "vimServer";
    public static final String PUSH_VIM = "vim";

    /* contents of the defaults HashMap that are defined in this class.
     * There are more default parameters in this map which belong to separate preference classes.
     */
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

    public static final String SEARCH_DIALOG_COLUMN_WIDTHS = "searchTableColumnWidths";
    public static final String SEARCH_DIALOG_COLUMN_SORT_TYPES = "searchDialogColumnSortTypes";
    public static final String SEARCH_DIALOG_COLUMN_SORT_ORDER = "searchDalogColumnSortOrder";

    public static final String SIDE_PANE_COMPONENT_PREFERRED_POSITIONS = "sidePaneComponentPreferredPositions";
    public static final String SIDE_PANE_COMPONENT_NAMES = "sidePaneComponentNames";
    public static final String XMP_PRIVACY_FILTERS = "xmpPrivacyFilters";
    public static final String USE_XMP_PRIVACY_FILTER = "useXmpPrivacyFilter";
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
    public static final String LAST_USED_EXPORT = "lastUsedExport";
    public static final String EXPORT_WORKING_DIRECTORY = "exportWorkingDirectory";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String BACKUP_DIRECTORY = "backupDirectory";
    public static final String CREATE_BACKUP = "createBackup";

    public static final String KEYWORD_SEPARATOR = "groupKeywordSeparator";
    public static final String AUTO_ASSIGN_GROUP = "autoAssignGroup";
    public static final String DISPLAY_GROUP_COUNT = "displayGroupCount";
    public static final String EXTRA_FILE_COLUMNS = "extraFileColumns";
    public static final String OVERRIDE_DEFAULT_FONT_SIZE = "overrideDefaultFontSize";
    public static final String MAIN_FONT_SIZE = "mainFontSize";

    public static final String RECENT_DATABASES = "recentDatabases";
    public static final String MEMORY_STICK_MODE = "memoryStickMode";
    public static final String SHOW_ADVANCED_HINTS = "showAdvancedHints";
    public static final String DEFAULT_ENCODING = "defaultEncoding";

    public static final String BASE_DOI_URI = "baseDOIURI";
    public static final String USE_CUSTOM_DOI_URI = "useCustomDOIURI";

    public static final String USE_OWNER = "useOwner";
    public static final String DEFAULT_OWNER = "defaultOwner";
    public static final String OVERWRITE_OWNER = "overwriteOwner";

    // Required for migration from pre-v5.3 only
    public static final String UPDATE_TIMESTAMP = "updateTimestamp";
    public static final String TIME_STAMP_FIELD = "timeStampField";
    public static final String TIME_STAMP_FORMAT = "timeStampFormat";

    public static final String ADD_CREATION_DATE = "addCreationDate";
    public static final String ADD_MODIFICATION_DATE = "addModificationDate";

    public static final String WARN_ABOUT_DUPLICATES_IN_INSPECTION = "warnAboutDuplicatesInInspection";
    public static final String NON_WRAPPABLE_FIELDS = "nonWrappableFields";
    public static final String RESOLVE_STRINGS_FOR_FIELDS = "resolveStringsForFields";
    public static final String DO_NOT_RESOLVE_STRINGS = "doNotResolveStrings";

    // merge related
    public static final String MERGE_ENTRIES_DIFF_MODE = "mergeEntriesDiffMode";
    public static final String MERGE_ENTRIES_SHOULD_SHOW_DIFF = "mergeEntriesShouldShowDiff";
    public static final String MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF = "mergeEntriesShouldShowUnifiedDiff";
    public static final String MERGE_ENTRIES_HIGHLIGHT_WORDS = "mergeEntriesHighlightWords";


    public static final String MERGE_SHOW_ONLY_CHANGED_FIELDS = "mergeShowOnlyChangedFields";

    public static final String CUSTOM_EXPORT_FORMAT = "customExportFormat";
    public static final String CUSTOM_IMPORT_FORMAT = "customImportFormat";
    public static final String KEY_PATTERN_REGEX = "KeyPatternRegex";
    public static final String KEY_PATTERN_REPLACEMENT = "KeyPatternReplacement";
    public static final String CONSOLE_COMMAND = "consoleCommand";
    public static final String USE_DEFAULT_CONSOLE_APPLICATION = "useDefaultConsoleApplication";
    public static final String USE_DEFAULT_FILE_BROWSER_APPLICATION = "userDefaultFileBrowserApplication";
    public static final String FILE_BROWSER_COMMAND = "fileBrowserCommand";
    public static final String MAIN_FILE_DIRECTORY = "fileDirectory";

    public static final String SEARCH_DISPLAY_MODE = "searchDisplayMode";
    public static final String SEARCH_CASE_SENSITIVE = "caseSensitiveSearch";
    public static final String SEARCH_REG_EXP = "regExpSearch";
    public static final String SEARCH_FULLTEXT = "fulltextSearch";
    public static final String SEARCH_KEEP_SEARCH_STRING = "keepSearchString";
    public static final String SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP = "keepOnTop";
    public static final String SEARCH_WINDOW_HEIGHT = "searchWindowHeight";
    public static final String SEARCH_WINDOW_WIDTH = "searchWindowWidth";
    public static final String GENERATE_KEY_ON_IMPORT = "generateKeyOnImport";
    public static final String GROBID_ENABLED = "grobidEnabled";
    public static final String GROBID_OPT_OUT = "grobidOptOut";
    public static final String GROBID_URL = "grobidURL";

    public static final String DEFAULT_CITATION_KEY_PATTERN = "defaultBibtexKeyPattern";
    public static final String UNWANTED_CITATION_KEY_CHARACTERS = "defaultUnwantedBibtexKeyCharacters";
    public static final String CONFIRM_DELETE = "confirmDelete";
    public static final String WARN_BEFORE_OVERWRITING_KEY = "warnBeforeOverwritingKey";
    public static final String AVOID_OVERWRITING_KEY = "avoidOverwritingKey";
    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";
    public static final String AUTOLINK_FILES_ENABLED = "autoLinkFilesEnabled";
    public static final String SIDE_PANE_WIDTH = "sidePaneWidthFX";
    public static final String CITE_COMMAND = "citeCommand";
    public static final String GENERATE_KEYS_BEFORE_SAVING = "generateKeysBeforeSaving";
    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String KINDLE_EMAIL = "kindleEmail";
    public static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";
    public static final String KEY_GEN_ALWAYS_ADD_LETTER = "keyGenAlwaysAddLetter";
    public static final String KEY_GEN_FIRST_LETTER_A = "keyGenFirstLetterA";
    public static final String ALLOW_INTEGER_EDITION_BIBTEX = "allowIntegerEditionBibtex";
    public static final String LOCAL_AUTO_SAVE = "localAutoSave";
    public static final String AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String AUTOLINK_USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    // bibLocAsPrimaryDir is a misleading antique variable name, we keep it for reason of compatibility
    public static final String STORE_RELATIVE_TO_BIB = "bibLocAsPrimaryDir";
    public static final String SELECTED_FETCHER_INDEX = "selectedFetcherIndex";
    public static final String WEB_SEARCH_VISIBLE = "webSearchVisible";
    public static final String GROUP_SIDEPANE_VISIBLE = "groupSidepaneVisible";
    public static final String CUSTOM_TAB_NAME = "customTabName_";
    public static final String CUSTOM_TAB_FIELDS = "customTabFields_";
    public static final String ASK_AUTO_NAMING_PDFS_AGAIN = "AskAutoNamingPDFsAgain";
    public static final String CLEANUP_JOBS = "CleanUpJobs";
    public static final String CLEANUP_FIELD_FORMATTERS_ENABLED = "CleanUpFormattersEnabled";
    public static final String CLEANUP_FIELD_FORMATTERS = "CleanUpFormatters";
    public static final String IMPORT_FILENAMEPATTERN = "importFileNamePattern";
    public static final String IMPORT_FILEDIRPATTERN = "importFileDirPattern";
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
     * The OpenOffice/LibreOffice connection preferences are: OO_PATH main directory for OO/LO installation, used to detect location on Win/macOS when using manual connect OO_EXECUTABLE_PATH path to soffice-file OO_JARS_PATH directory that contains juh.jar, jurt.jar, ridl.jar, unoil.jar OO_SYNC_WHEN_CITING true if the reference list is updated when adding a new citation OO_SHOW_PANEL true if the OO panel is shown on startup OO_USE_ALL_OPEN_DATABASES true if all databases should be used when citing OO_BIBLIOGRAPHY_STYLE_FILE path to the used style file OO_EXTERNAL_STYLE_FILES list with paths to external style files STYLES_*_* size and position of "Select style" dialog
     */
    public static final String OO_EXECUTABLE_PATH = "ooExecutablePath";
    public static final String OO_SHOW_PANEL = "showOOPanel";
    public static final String OO_SYNC_WHEN_CITING = "syncOOWhenCiting";
    public static final String OO_USE_ALL_OPEN_BASES = "useAllOpenBases";
    public static final String OO_BIBLIOGRAPHY_STYLE_FILE = "ooBibliographyStyleFile";
    public static final String OO_EXTERNAL_STYLE_FILES = "ooExternalStyleFiles";

    // Special field preferences
    public static final String SPECIALFIELDSENABLED = "specialFieldsEnabled";

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

    // Preview - public for pref migrations
    public static final String PREVIEW_STYLE = "previewStyle";
    public static final String CYCLE_PREVIEW_POS = "cyclePreviewPos";
    public static final String CYCLE_PREVIEW = "cyclePreview";
    public static final String PREVIEW_AS_TAB = "previewAsTab";

    // UI
    private static final String FONT_FAMILY = "fontFamily";

    // Proxy
    private static final String PROXY_PORT = "proxyPort";
    private static final String PROXY_HOSTNAME = "proxyHostname";
    private static final String PROXY_USE = "useProxy";
    private static final String PROXY_USE_AUTHENTICATION = "useProxyAuthentication";
    private static final String PROXY_USERNAME = "proxyUsername";
    private static final String PROXY_PASSWORD = "proxyPassword";
    private static final String PROXY_PERSIST_PASSWORD = "persistPassword";

    // Web search
    private static final String FETCHER_CUSTOM_KEY_NAMES = "fetcherCustomKeyNames";
    private static final String FETCHER_CUSTOM_KEY_USES = "fetcherCustomKeyUses";

    // SSL
    private static final String TRUSTSTORE_PATH = "truststorePath";

    // Auto completion
    private static final String AUTO_COMPLETE = "autoComplete";
    private static final String AUTOCOMPLETER_FIRSTNAME_MODE = "autoCompFirstNameMode";
    private static final String AUTOCOMPLETER_LAST_FIRST = "autoCompLF";
    private static final String AUTOCOMPLETER_FIRST_LAST = "autoCompFF";

    private static final String BIND_NAMES = "bindNames";
    // User
    private static final String USER_ID = "userId";

    // Journal
    private static final String EXTERNAL_JOURNAL_LISTS = "externalJournalLists";
    private static final String USE_AMS_FJOURNAL = "useAMSFJournal";

    // Telemetry collection
    private static final String COLLECT_TELEMETRY = "collectTelemetry";
    private static final String ALREADY_ASKED_TO_COLLECT_TELEMETRY = "askedCollectTelemetry";
    private static final String PROTECTED_TERMS_ENABLED_EXTERNAL = "protectedTermsEnabledExternal";
    private static final String PROTECTED_TERMS_DISABLED_EXTERNAL = "protectedTermsDisabledExternal";
    private static final String PROTECTED_TERMS_ENABLED_INTERNAL = "protectedTermsEnabledInternal";
    private static final String PROTECTED_TERMS_DISABLED_INTERNAL = "protectedTermsDisabledInternal";

    // GroupViewMode
    private static final String GROUP_INTERSECT_UNION_VIEW_MODE = "groupIntersectUnionViewModes";
    private static final String DEFAULT_HIERARCHICAL_CONTEXT = "defaultHierarchicalContext";

    // Dialog states
    private static final String PREFS_EXPORT_PATH = "prefsExportPath";
    private static final String DOWNLOAD_LINKED_FILES = "downloadLinkedFiles";
    private static final String FULLTEXT_INDEX_LINKED_FILES = "fulltextIndexLinkedFiles";

    // Helper string
    private static final String USER_HOME = System.getProperty("user.home");

    // Indexes for Strings within stored custom export entries
    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    // Remote
    private static final String USE_REMOTE_SERVER = "useRemoteServer";
    private static final String REMOTE_SERVER_PORT = "remoteServerPort";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefPreferences.class);
    private static final Preferences PREFS_NODE = Preferences.userRoot().node("/org/jabref");

    // The only instance of this class:
    private static JabRefPreferences singleton;
    /**
     * HashMap that contains all preferences which are set by default
     */
    public final Map<String, Object> defaults = new HashMap<>();

    private final Preferences prefs;

    /**
     * Cache variables
     */
    private String userAndHost;

    private LibraryPreferences libraryPreferences;
    private TelemetryPreferences telemetryPreferences;
    private DOIPreferences doiPreferences;
    private OwnerPreferences ownerPreferences;
    private TimestampPreferences timestampPreferences;
    private PreviewPreferences previewPreferences;
    private OpenOfficePreferences openOfficePreferences;
    private SidePanePreferences sidePanePreferences;
    private WorkspacePreferences workspacePreferences;
    private ImporterPreferences importerPreferences;
    private GrobidPreferences grobidPreferences;
    private ProtectedTermsPreferences protectedTermsPreferences;
    private MrDlibPreferences mrDlibPreferences;
    private EntryEditorPreferences entryEditorPreferences;
    private FilePreferences filePreferences;
    private GuiPreferences guiPreferences;
    private RemotePreferences remotePreferences;
    private ProxyPreferences proxyPreferences;
    private SSLPreferences sslPreferences;
    private SearchPreferences searchPreferences;
    private AutoLinkPreferences autoLinkPreferences;
    private ExportPreferences exportPreferences;
    private NameFormatterPreferences nameFormatterPreferences;
    private BibEntryPreferences bibEntryPreferences;
    private InternalPreferences internalPreferences;
    private SpecialFieldsPreferences specialFieldsPreferences;
    private GroupsPreferences groupsPreferences;
    private XmpPreferences xmpPreferences;
    private AutoCompletePreferences autoCompletePreferences;
    private CleanupPreferences cleanupPreferences;
    private PushToApplicationPreferences pushToApplicationPreferences;
    private ExternalApplicationsPreferences externalApplicationsPreferences;
    private CitationKeyPatternPreferences citationKeyPatternPreferences;
    private NameDisplayPreferences nameDisplayPreferences;
    private MainTablePreferences mainTablePreferences;
    private ColumnPreferences mainTableColumnPreferences;
    private ColumnPreferences searchDialogColumnPreferences;
    private JournalAbbreviationPreferences journalAbbreviationPreferences;
    private FieldPreferences fieldPreferences;

    // The constructor is made private to enforce this as a singleton class:
    private JabRefPreferences() {
        try {
            if (new File("jabref.xml").exists()) {
                importPreferences(Path.of("jabref.xml"));
            }
        } catch (JabRefException e) {
            LOGGER.warn("Could not import preferences from jabref.xml: " + e.getMessage(), e);
        }

        // load user preferences
        prefs = PREFS_NODE;

        // Since some of the preference settings themselves use localized strings, we cannot set the language after
        // the initialization of the preferences in main
        // Otherwise that language framework will be instantiated and more importantly, statically initialized preferences
        // like the SearchDisplayMode will never be translated.
        Localization.setLanguage(getLanguage());

        defaults.put(SEARCH_DISPLAY_MODE, SearchDisplayMode.FILTER.toString());
        defaults.put(SEARCH_CASE_SENSITIVE, Boolean.FALSE);
        defaults.put(SEARCH_REG_EXP, Boolean.FALSE);
        defaults.put(SEARCH_FULLTEXT, Boolean.FALSE);
        defaults.put(SEARCH_KEEP_SEARCH_STRING, Boolean.FALSE);
        defaults.put(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, Boolean.TRUE);
        defaults.put(SEARCH_WINDOW_HEIGHT, 176.0);
        defaults.put(SEARCH_WINDOW_WIDTH, 600.0);

        defaults.put(GENERATE_KEY_ON_IMPORT, Boolean.TRUE);
        defaults.put(GROBID_ENABLED, Boolean.FALSE);
        defaults.put(GROBID_OPT_OUT, Boolean.FALSE);
        defaults.put(GROBID_URL, "http://grobid.jabref.org:8070");

        defaults.put(PUSH_TEXMAKER_PATH, OS.getNativeDesktop().detectProgramPath("texmaker", "Texmaker"));
        defaults.put(PUSH_WINEDT_PATH, OS.getNativeDesktop().detectProgramPath("WinEdt", "WinEdt Team\\WinEdt"));
        defaults.put(PUSH_TEXSTUDIO_PATH, OS.getNativeDesktop().detectProgramPath("texstudio", "TeXstudio"));
        defaults.put(PUSH_LYXPIPE, USER_HOME + File.separator + ".lyx/lyxpipe");
        defaults.put(PUSH_VIM, "vim");
        defaults.put(PUSH_VIM_SERVER, "vim");
        defaults.put(PUSH_EMACS_ADDITIONAL_PARAMETERS, "-n -e");

        defaults.put(BIBLATEX_DEFAULT_MODE, Boolean.FALSE);

        // Set DOI to be the default ID entry generator
        defaults.put(ID_ENTRY_GENERATOR, DoiFetcher.NAME);

        defaults.put(USE_CUSTOM_DOI_URI, Boolean.FALSE);
        defaults.put(BASE_DOI_URI, "https://doi.org");

        if (OS.OS_X) {
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(PUSH_EMACS_PATH, "emacsclient");
        } else if (OS.WINDOWS) {
            defaults.put(PUSH_EMACS_PATH, "emacsclient.exe");
        } else {
            // Linux
            defaults.put(FONT_FAMILY, "SansSerif");
            defaults.put(PUSH_EMACS_PATH, "emacsclient");
        }

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
        defaults.put(PROXY_PERSIST_PASSWORD, Boolean.FALSE);

        // SSL
        defaults.put(TRUSTSTORE_PATH, OS.getNativeDesktop()
                                        .getSslDirectory()
                                        .resolve("truststore.jks").toString());

        defaults.put(POS_X, 0);
        defaults.put(POS_Y, 0);
        defaults.put(SIZE_X, 1024);
        defaults.put(SIZE_Y, 768);
        defaults.put(WINDOW_MAXIMISED, Boolean.TRUE);
        defaults.put(AUTO_RESIZE_MODE, Boolean.FALSE); // By default disable "Fit table horizontally on the screen"
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
        defaults.put(EXPORT_IN_ORIGINAL_ORDER, Boolean.TRUE);
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

        defaults.put(COLUMN_NAMES, "groups;files;linked_id;field:entrytype;field:author/editor;field:title;field:year;field:journal/booktitle;special:ranking;special:readstatus;special:priority");
        defaults.put(COLUMN_WIDTHS, "28;28;28;75;300;470;60;130;50;50;50");

        defaults.put(XMP_PRIVACY_FILTERS, "pdf;timestamp;keywords;owner;note;review");
        defaults.put(USE_XMP_PRIVACY_FILTER, Boolean.FALSE);
        defaults.put(WORKING_DIRECTORY, USER_HOME);
        defaults.put(EXPORT_WORKING_DIRECTORY, USER_HOME);

        defaults.put(CREATE_BACKUP, Boolean.TRUE);

        // Remembers working directory of last import
        defaults.put(IMPORT_WORKING_DIRECTORY, USER_HOME);
        defaults.put(PREFS_EXPORT_PATH, USER_HOME);
        defaults.put(AUTO_OPEN_FORM, Boolean.TRUE);
        defaults.put(OPEN_LAST_EDITED, Boolean.TRUE);
        defaults.put(LAST_EDITED, "");
        defaults.put(LAST_FOCUSED, "");
        defaults.put(DEFAULT_SHOW_SOURCE, Boolean.FALSE);

        defaults.put(MERGE_ENTRIES_DIFF_MODE, DiffMode.WORD.name());
        defaults.put(MERGE_ENTRIES_SHOULD_SHOW_DIFF, Boolean.TRUE);
        defaults.put(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, Boolean.TRUE);
        defaults.put(MERGE_ENTRIES_HIGHLIGHT_WORDS, Boolean.TRUE);
        defaults.put(MERGE_SHOW_ONLY_CHANGED_FIELDS, Boolean.FALSE);

        defaults.put(SHOW_RECOMMENDATIONS, Boolean.TRUE);
        defaults.put(ACCEPT_RECOMMENDATIONS, Boolean.FALSE);
        defaults.put(SHOW_LATEX_CITATIONS, Boolean.TRUE);
        defaults.put(SEND_LANGUAGE_DATA, Boolean.FALSE);
        defaults.put(SEND_OS_DATA, Boolean.FALSE);
        defaults.put(SEND_TIMEZONE_DATA, Boolean.FALSE);
        defaults.put(VALIDATE_IN_ENTRY_EDITOR, Boolean.TRUE);
        defaults.put(AUTO_COMPLETE, Boolean.FALSE);
        defaults.put(AUTOCOMPLETER_FIRSTNAME_MODE, AutoCompleteFirstNameMode.BOTH.name());
        defaults.put(AUTOCOMPLETER_FIRST_LAST, Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put(AUTOCOMPLETER_LAST_FIRST, Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(AUTOCOMPLETER_COMPLETE_FIELDS, "author;editor;title;journal;publisher;keywords;crossref;related;entryset");
        defaults.put(AUTO_ASSIGN_GROUP, Boolean.TRUE);
        defaults.put(DISPLAY_GROUP_COUNT, Boolean.TRUE);
        defaults.put(GROUP_INTERSECT_UNION_VIEW_MODE, GroupViewMode.INTERSECTION.name());
        defaults.put(DEFAULT_HIERARCHICAL_CONTEXT, GroupHierarchyType.INDEPENDENT.name());
        defaults.put(KEYWORD_SEPARATOR, ", ");
        defaults.put(DEFAULT_ENCODING, StandardCharsets.UTF_8.name());
        defaults.put(DEFAULT_OWNER, System.getProperty("user.name"));
        defaults.put(MEMORY_STICK_MODE, Boolean.FALSE);
        defaults.put(SHOW_ADVANCED_HINTS, Boolean.TRUE);

        defaults.put(EXTRA_FILE_COLUMNS, Boolean.FALSE);

        defaults.put(PROTECTED_TERMS_ENABLED_INTERNAL, convertListToString(ProtectedTermsLoader.getInternalLists()));
        defaults.put(PROTECTED_TERMS_DISABLED_INTERNAL, "");
        defaults.put(PROTECTED_TERMS_ENABLED_EXTERNAL, "");
        defaults.put(PROTECTED_TERMS_DISABLED_EXTERNAL, "");

        // OpenOffice/LibreOffice
        if (OS.WINDOWS) {
            defaults.put(OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_WIN_EXEC_PATH);
        } else if (OS.OS_X) {
            defaults.put(OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_OSX_EXEC_PATH);
        } else { // Linux
            defaults.put(OO_EXECUTABLE_PATH, OpenOfficePreferences.DEFAULT_LINUX_EXEC_PATH);
        }

        defaults.put(OO_SYNC_WHEN_CITING, Boolean.TRUE);
        defaults.put(OO_SHOW_PANEL, Boolean.FALSE);
        defaults.put(OO_USE_ALL_OPEN_BASES, Boolean.TRUE);
        defaults.put(OO_BIBLIOGRAPHY_STYLE_FILE, StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        defaults.put(OO_EXTERNAL_STYLE_FILES, "");

        defaults.put(SPECIALFIELDSENABLED, Boolean.TRUE);

        defaults.put(FETCHER_CUSTOM_KEY_NAMES, "Springer;IEEEXplore;SAO/NASA ADS;ScienceDirect;Biodiversity Heritage");
        defaults.put(FETCHER_CUSTOM_KEY_USES, "FALSE;FALSE;FALSE;FALSE;FALSE");

        defaults.put(USE_OWNER, Boolean.FALSE);
        defaults.put(OVERWRITE_OWNER, Boolean.FALSE);
        defaults.put(AVOID_OVERWRITING_KEY, Boolean.FALSE);
        defaults.put(WARN_BEFORE_OVERWRITING_KEY, Boolean.TRUE);
        defaults.put(CONFIRM_DELETE, Boolean.TRUE);
        defaults.put(DEFAULT_CITATION_KEY_PATTERN, "[auth][year]");
        defaults.put(UNWANTED_CITATION_KEY_CHARACTERS, "-`ʹ:!;?^+");
        defaults.put(RESOLVE_STRINGS_FOR_FIELDS, "author;booktitle;editor;editora;editorb;editorc;institution;issuetitle;journal;journalsubtitle;journaltitle;mainsubtitle;month;publisher;shortauthor;shorteditor;subtitle;titleaddon");
        defaults.put(DO_NOT_RESOLVE_STRINGS, Boolean.FALSE);
        defaults.put(NON_WRAPPABLE_FIELDS, "pdf;ps;url;doi;file;isbn;issn");
        defaults.put(WARN_ABOUT_DUPLICATES_IN_INSPECTION, Boolean.TRUE);
        defaults.put(ADD_CREATION_DATE, Boolean.FALSE);
        defaults.put(ADD_MODIFICATION_DATE, Boolean.FALSE);

        defaults.put(UPDATE_TIMESTAMP, Boolean.FALSE);
        defaults.put(TIME_STAMP_FIELD, StandardField.TIMESTAMP.getName());
        // default time stamp follows ISO-8601. Reason: https://xkcd.com/1179/
        defaults.put(TIME_STAMP_FORMAT, "yyyy-MM-dd");

        defaults.put(GENERATE_KEYS_BEFORE_SAVING, Boolean.FALSE);

        defaults.put(USE_REMOTE_SERVER, Boolean.TRUE);
        defaults.put(REMOTE_SERVER_PORT, 6050);

        defaults.put(EXTERNAL_JOURNAL_LISTS, "");
        defaults.put(USE_AMS_FJOURNAL, true);
        defaults.put(CITE_COMMAND, "\\cite"); // obsoleted by the app-specific ones (not any more?)

        defaults.put(LAST_USED_EXPORT, "");
        defaults.put(SIDE_PANE_WIDTH, 0.15);

        defaults.put(MAIN_FONT_SIZE, 9);
        defaults.put(OVERRIDE_DEFAULT_FONT_SIZE, false);

        defaults.put(AUTOLINK_EXACT_KEY_ONLY, Boolean.FALSE);
        defaults.put(AUTOLINK_FILES_ENABLED, Boolean.TRUE);
        defaults.put(LOCAL_AUTO_SAVE, Boolean.FALSE);
        defaults.put(ALLOW_INTEGER_EDITION_BIBTEX, Boolean.FALSE);
        // Curly brackets ({}) are the default delimiters, not quotes (") as these cause trouble when they appear within the field value:
        // Currently, JabRef does not escape them
        defaults.put(KEY_GEN_FIRST_LETTER_A, Boolean.TRUE);
        defaults.put(KEY_GEN_ALWAYS_ADD_LETTER, Boolean.FALSE);
        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
        defaults.put(KINDLE_EMAIL, "");
        defaults.put(OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.FALSE);
        defaults.put(WEB_SEARCH_VISIBLE, Boolean.TRUE);
        defaults.put(GROUP_SIDEPANE_VISIBLE, Boolean.TRUE);
        defaults.put(SELECTED_FETCHER_INDEX, 0);
        defaults.put(STORE_RELATIVE_TO_BIB, Boolean.TRUE);
        defaults.put(COLLECT_TELEMETRY, Boolean.FALSE);
        defaults.put(ALREADY_ASKED_TO_COLLECT_TELEMETRY, Boolean.FALSE);

        defaults.put(ASK_AUTO_NAMING_PDFS_AGAIN, Boolean.TRUE);
        defaults.put(CLEANUP_JOBS, convertListToString(getDefaultCleanupJobs().stream().map(Enum::name).toList()));
        defaults.put(CLEANUP_FIELD_FORMATTERS_ENABLED, Boolean.FALSE);
        defaults.put(CLEANUP_FIELD_FORMATTERS, FieldFormatterCleanups.getMetaDataString(FieldFormatterCleanups.DEFAULT_SAVE_ACTIONS, OS.NEWLINE));

        // use citation key appended with filename as default pattern
        defaults.put(IMPORT_FILENAMEPATTERN, FilePreferences.DEFAULT_FILENAME_PATTERNS[1]);
        // Default empty String to be backwards compatible
        defaults.put(IMPORT_FILEDIRPATTERN, "");
        // Download files by default
        defaults.put(DOWNLOAD_LINKED_FILES, true);
        // Create Fulltext-Index by default
        defaults.put(FULLTEXT_INDEX_LINKED_FILES, true);

        String defaultExpression = "**/.*[citationkey].*\\\\.[extension]";
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

        // version check defaults
        defaults.put(VERSION_IGNORED_UPDATE, "");

        // preview
        defaults.put(CYCLE_PREVIEW, "Preview;" + CitationStyle.DEFAULT);
        defaults.put(CYCLE_PREVIEW_POS, 0);
        defaults.put(PREVIEW_AS_TAB, Boolean.FALSE);
        defaults.put(PREVIEW_STYLE,
                "<font face=\"sans-serif\">" +
                        "<b>\\bibtextype</b><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>\\end{citationkey}__NEWLINE__" +
                        "\\begin{author}<BR><BR>\\format[Authors(LastFirst, FullName,Sep= / ,LastSep= / ),HTMLChars]{\\author}\\end{author}__NEWLINE__" +
                        "\\begin{editor & !author}<BR><BR>\\format[Authors(LastFirst,FullName,Sep= / ,LastSep= / ),HTMLChars]{\\editor} (\\format[IfPlural(Eds.,Ed.)]{\\editor})\\end{editor & !author}__NEWLINE__" +
                        "\\begin{title}<BR><b>\\format[HTMLChars]{\\title}</b> \\end{title}__NEWLINE__" +
                        "<BR>\\begin{date}\\date\\end{date}\\begin{edition}, \\edition. edition\\end{edition}__NEWLINE__" +
                        "\\begin{editor & author}<BR><BR>\\format[Authors(LastFirst,FullName,Sep= / ,LastSep= / ),HTMLChars]{\\editor} (\\format[IfPlural(Eds.,Ed.)]{\\editor})\\end{editor & author}__NEWLINE__" +
                        "\\begin{booktitle}<BR><i>\\format[HTMLChars]{\\booktitle}</i>\\end{booktitle}__NEWLINE__" +
                        "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}" +
                        "\\begin{editor & !author}<BR>\\end{editor & !author}\\begin{!editor}<BR>\\end{!editor}\\begin{journal}<BR><i>\\format[HTMLChars]{\\journal}</i> \\end{journal} \\begin{volume}, Vol. \\volume\\end{volume}\\begin{series}<BR>\\format[HTMLChars]{\\series}\\end{series}\\begin{number}, No. \\format[HTMLChars]{\\number}\\end{number}__NEWLINE__" +
                        "\\begin{school} \\format[HTMLChars]{\\school}, \\end{school}__NEWLINE__" +
                        "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__" +
                        "\\begin{publisher}<BR>\\format[HTMLChars]{\\publisher}\\end{publisher}\\begin{location}: \\format[HTMLChars]{\\location} \\end{location}__NEWLINE__" +
                        "\\begin{pages}<BR> p. \\format[FormatPagesForHTML]{\\pages}\\end{pages}__NEWLINE__" +
                        "\\begin{abstract}<BR><BR><b>Abstract: </b>\\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__" +
                        "\\begin{owncitation}<BR><BR><b>Own citation: </b>\\format[HTMLChars]{\\owncitation} \\end{owncitation}__NEWLINE__" +
                        "\\begin{comment}<BR><BR><b>Comment: </b>\\format[HTMLChars]{\\comment}\\end{comment}__NEWLINE__" +
                        "</font>__NEWLINE__");

        // set default theme
        defaults.put(FX_THEME, Theme.BASE_CSS);
        setLanguageDependentDefaultValues();
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
     * @return Instance of JaRefPreferences
     * @deprecated Use {@link PreferencesService} instead
     */
    @Deprecated
    public static JabRefPreferences getInstance() {
        if (JabRefPreferences.singleton == null) {
            JabRefPreferences.singleton = new JabRefPreferences();
        }
        return JabRefPreferences.singleton;
    }

    //*************************************************************************************************************
    // Common serializer logic
    //*************************************************************************************************************

    private static String convertListToString(List<String> value) {
        return value.stream().map(val -> StringUtil.quote(val, STRINGLIST_DELIMITER.toString(), '\\')).collect(Collectors.joining(STRINGLIST_DELIMITER.toString()));
    }

    private static List<String> convertStringToList(String toConvert) {
        if (StringUtil.isBlank(toConvert)) {
            return Collections.emptyList();
        }

        StringReader reader = new StringReader(toConvert);
        List<String> result = new ArrayList<>();
        Optional<String> rs;
        try {
            while ((rs = getNextUnit(reader)).isPresent()) {
                result.add(rs.get());
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to convert String to List", e);
        }
        return result;
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

    //*************************************************************************************************************
    // Backingstore access logic
    //*************************************************************************************************************

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

    private void remove(String key) {
        prefs.remove(key);
    }

    /**
     * Puts a list of strings into the Preferences, by linking its elements with a STRINGLIST_DELIMITER into a single string. Escape characters make the process transparent even if strings contains a STRINGLIST_DELIMITER.
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
        return convertStringToList(get(key));
    }

    /**
     * Returns a Path
     */
    private Path getPath(String key, Path defaultValue) {
        String rawPath = get(key);
        return StringUtil.isNotBlank(rawPath) ? Path.of(rawPath) : defaultValue;
    }

    /**
     * Clear all preferences.
     *
     * @throws BackingStoreException if JabRef is unable to write to the registry/the preferences storage
     */
    @Override
    public void clear() throws BackingStoreException {
        clearAllBibEntryTypes();
        clearCitationKeyPatterns();
        clearTruststoreFromCustomCertificates();
        clearCustomFetcherKeys();
        prefs.clear();
        new SharedDatabasePreferences().clear();
    }

    private void clearTruststoreFromCustomCertificates() {
        TrustStoreManager trustStoreManager = new TrustStoreManager(Path.of(defaults.get(TRUSTSTORE_PATH).toString()));
        trustStoreManager.clearCustomCertificates();
    }

    /**
     * Removes the given key from the preferences.
     *
     * @throws IllegalArgumentException if the key does not exist
     */
    @Override
    public void deleteKey(String key) throws IllegalArgumentException {
        String keyTrimmed = key.trim();
        if (hasKey(keyTrimmed)) {
            remove(keyTrimmed);
        } else {
            throw new IllegalArgumentException("Unknown preference key");
        }
    }

    /**
     * Calling this method will write all preferences into the preference store.
     */
    @Override
    public void flush() {
        if (getBoolean(MEMORY_STICK_MODE)) {
            try {
                exportPreferences(Path.of("jabref.xml"));
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

    @Override
    public Map<String, Object> getPreferences() {
        Map<String, Object> result = new HashMap<>();

        try {
            addPrefsRecursively(this.prefs, result);
        } catch (BackingStoreException e) {
            LOGGER.info("could not retrieve preference keys", e);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDefaults() {
        return defaults;
    }

    private void addPrefsRecursively(Preferences prefs, Map<String, Object> result) throws BackingStoreException {
        for (String key : prefs.keys()) {
            result.put(key, getObject(prefs, key));
        }
        for (String child : prefs.childrenNames()) {
            addPrefsRecursively(prefs.node(child), result);
        }
    }

    private Object getObject(Preferences prefs, String key) {
        try {
            return prefs.get(key, (String) defaults.get(key));
        } catch (ClassCastException e) {
            try {
                return prefs.getBoolean(key, getBooleanDefault(key));
            } catch (ClassCastException e2) {
                try {
                    return prefs.getInt(key, getIntDefault(key));
                } catch (ClassCastException e3) {
                    return prefs.getDouble(key, getDoubleDefault(key));
                }
            }
        }
    }

    /**
     * Returns a list of Strings stored by key+N with N being an incrementing number
     */
    private List<String> getSeries(String key) {
        int i = 0;
        List<String> series = new ArrayList<>();
        String item;
        while (!StringUtil.isBlank(item = get(key + i))) {
            series.add(item);
            i++;
        }
        return series;
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
     * @param path Path to export to
     */
    @Override
    public void exportPreferences(Path path) throws JabRefException {
        LOGGER.debug("Exporting preferences {}", path.toAbsolutePath());
        try (OutputStream os = Files.newOutputStream(path)) {
            prefs.exportSubtree(os);
        } catch (BackingStoreException | IOException ex) {
            throw new JabRefException(
                    "Could not export preferences",
                    Localization.lang("Could not export preferences"),
                    ex);
        }
    }

    /**
     * Imports Preferences from an XML file.
     *
     * @param file Path of file to import from
     * @throws JabRefException thrown if importing the preferences failed due to an InvalidPreferencesFormatException or an IOException
     */
    @Override
    public void importPreferences(Path file) throws JabRefException {
        try (InputStream is = Files.newInputStream(file)) {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException | IOException ex) {
            throw new JabRefException(
                    "Could not import preferences",
                    Localization.lang("Could not import preferences"),
                    ex);
        }
    }

    //*************************************************************************************************************
    // ToDo: Cleanup
    //*************************************************************************************************************

    @Override
    public LayoutFormatterPreferences getLayoutFormatterPreferences() {
        return new LayoutFormatterPreferences(
                getNameFormatterPreferences(),
                getFilePreferences().mainFileDirectoryProperty());
    }

    @Override
    public KeyBindingRepository getKeyBindingRepository() {
        return new KeyBindingRepository(getStringList(BIND_NAMES), getStringList(BINDINGS));
    }

    @Override
    public void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository) {
        putStringList(BIND_NAMES, keyBindingRepository.getBindNames());
        putStringList(BINDINGS, keyBindingRepository.getBindings());
    }

    @Override
    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        if (Objects.nonNull(journalAbbreviationPreferences)) {
            return journalAbbreviationPreferences;
        }

        journalAbbreviationPreferences = new JournalAbbreviationPreferences(
                getStringList(EXTERNAL_JOURNAL_LISTS),
                getBoolean(USE_AMS_FJOURNAL));

        journalAbbreviationPreferences.getExternalJournalLists().addListener((InvalidationListener) change ->
                putStringList(EXTERNAL_JOURNAL_LISTS, journalAbbreviationPreferences.getExternalJournalLists()));
        EasyBind.listen(journalAbbreviationPreferences.useFJournalFieldProperty(),
                (obs, oldValue, newValue) -> putBoolean(USE_AMS_FJOURNAL, newValue));

        return journalAbbreviationPreferences;
    }

    //*************************************************************************************************************
    // CustomEntryTypes
    //*************************************************************************************************************

    @Override
    public BibEntryTypesManager getCustomEntryTypesRepository() {
        BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();
        EnumSet.allOf(BibDatabaseMode.class).forEach(mode ->
                bibEntryTypesManager.addCustomOrModifiedTypes(getBibEntryTypes(mode), mode));
        return bibEntryTypesManager;
    }

    private List<BibEntryType> getBibEntryTypes(BibDatabaseMode bibDatabaseMode) {
        List<BibEntryType> storedEntryTypes = new ArrayList<>();
        Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode);
        try {
            Arrays.stream(prefsNode.keys())
                  .map(key -> prefsNode.get(key, null))
                  .filter(Objects::nonNull)
                  .forEach(typeString -> MetaDataParser.parseCustomEntryType(typeString).ifPresent(storedEntryTypes::add));
        } catch (BackingStoreException e) {
            LOGGER.info("Parsing customized entry types failed.", e);
        }
        return storedEntryTypes;
    }

    private void clearAllBibEntryTypes() {
        for (BibDatabaseMode mode : BibDatabaseMode.values()) {
            clearBibEntryTypes(mode);
        }
    }

    private void clearBibEntryTypes(BibDatabaseMode mode) {
        try {
            Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(mode);
            prefsNode.clear();
            prefsNode.flush();
        } catch (BackingStoreException e) {
            LOGGER.error("Resetting customized entry types failed.", e);
        }
    }

    @Override
    public void storeCustomEntryTypesRepository(BibEntryTypesManager entryTypesManager) {
        clearAllBibEntryTypes();
        storeBibEntryTypes(entryTypesManager.getAllTypes(BibDatabaseMode.BIBTEX), BibDatabaseMode.BIBTEX);
        storeBibEntryTypes(entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX), BibDatabaseMode.BIBLATEX);
    }

    private void storeBibEntryTypes(Collection<BibEntryType> bibEntryTypes, BibDatabaseMode bibDatabaseMode) {
        Preferences prefsNode = getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode);

        try {
            // clear old custom types
            clearBibEntryTypes(bibDatabaseMode);

            // store current custom types
            bibEntryTypes.forEach(type -> prefsNode.put(type.getType().getName(), MetaDataSerializer.serializeCustomEntryTypes(type)));

            prefsNode.flush();
        } catch (BackingStoreException e) {
            LOGGER.info("Updating stored custom entry types failed.", e);
        }
    }

    private static Preferences getPrefsNodeForCustomizedEntryTypes(BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBTEX
                ? PREFS_NODE.node(CUSTOMIZED_BIBTEX_TYPES)
                : PREFS_NODE.node(CUSTOMIZED_BIBLATEX_TYPES);
    }

    //*************************************************************************************************************
    // Misc
    //*************************************************************************************************************

    @Override
    public OpenOfficePreferences getOpenOfficePreferences() {
        if (Objects.nonNull(openOfficePreferences)) {
            return openOfficePreferences;
        }

        openOfficePreferences = new OpenOfficePreferences(
                get(OO_EXECUTABLE_PATH),
                getBoolean(OO_USE_ALL_OPEN_BASES),
                getBoolean(OO_SYNC_WHEN_CITING),
                getStringList(OO_EXTERNAL_STYLE_FILES),
                get(OO_BIBLIOGRAPHY_STYLE_FILE));

        EasyBind.listen(openOfficePreferences.executablePathProperty(), (obs, oldValue, newValue) -> put(OO_EXECUTABLE_PATH, newValue));
        EasyBind.listen(openOfficePreferences.useAllDatabasesProperty(), (obs, oldValue, newValue) -> putBoolean(OO_USE_ALL_OPEN_BASES, newValue));
        EasyBind.listen(openOfficePreferences.syncWhenCitingProperty(), (obs, oldValue, newValue) -> putBoolean(OO_SYNC_WHEN_CITING, newValue));
        openOfficePreferences.getExternalStyles().addListener((InvalidationListener) change ->
                putStringList(OO_EXTERNAL_STYLE_FILES, openOfficePreferences.getExternalStyles()));
        EasyBind.listen(openOfficePreferences.currentStyleProperty(), (obs, oldValue, newValue) -> put(OO_BIBLIOGRAPHY_STYLE_FILE, newValue));

        return openOfficePreferences;
    }

    @Override
    public LibraryPreferences getLibraryPreferences() {
        if (Objects.nonNull(libraryPreferences)) {
            return libraryPreferences;
        }

        libraryPreferences = new LibraryPreferences(
                getBoolean(BIBLATEX_DEFAULT_MODE) ? BibDatabaseMode.BIBLATEX : BibDatabaseMode.BIBTEX,
                getBoolean(REFORMAT_FILE_ON_SAVE_AND_EXPORT),
                getBoolean(LOCAL_AUTO_SAVE));

        EasyBind.listen(libraryPreferences.defaultBibDatabaseModeProperty(), (obs, oldValue, newValue) -> putBoolean(BIBLATEX_DEFAULT_MODE, (newValue == BibDatabaseMode.BIBLATEX)));
        EasyBind.listen(libraryPreferences.alwaysReformatOnSaveProperty(), (obs, oldValue, newValue) -> putBoolean(REFORMAT_FILE_ON_SAVE_AND_EXPORT, newValue));
        EasyBind.listen(libraryPreferences.autoSaveProperty(), (obs, oldValue, newValue) -> putBoolean(LOCAL_AUTO_SAVE, newValue));

        return libraryPreferences;
    }

    @Override
    public TelemetryPreferences getTelemetryPreferences() {
        if (Objects.nonNull(telemetryPreferences)) {
            return telemetryPreferences;
        }

        telemetryPreferences = new TelemetryPreferences(
                getBoolean(COLLECT_TELEMETRY),
                !getBoolean(ALREADY_ASKED_TO_COLLECT_TELEMETRY), // mind the !
                getTelemetryUserId()
        );

        EasyBind.listen(telemetryPreferences.collectTelemetryProperty(), (obs, oldValue, newValue) -> putBoolean(COLLECT_TELEMETRY, newValue));
        EasyBind.listen(telemetryPreferences.askToCollectTelemetryProperty(), (obs, oldValue, newValue) -> putBoolean(ALREADY_ASKED_TO_COLLECT_TELEMETRY, !newValue));

        return telemetryPreferences;
    }

    private String getTelemetryUserId() {
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
    public DOIPreferences getDOIPreferences() {
        if (Objects.nonNull(doiPreferences)) {
            return doiPreferences;
        }

        doiPreferences = new DOIPreferences(
                getBoolean(USE_CUSTOM_DOI_URI),
                get(BASE_DOI_URI));

        EasyBind.listen(doiPreferences.useCustomProperty(), (obs, oldValue, newValue) -> putBoolean(USE_CUSTOM_DOI_URI, newValue));
        EasyBind.listen(doiPreferences.defaultBaseURIProperty(), (obs, oldValue, newValue) -> put(BASE_DOI_URI, newValue));

        return doiPreferences;
    }

    @Override
    public OwnerPreferences getOwnerPreferences() {
        if (Objects.nonNull(ownerPreferences)) {
            return ownerPreferences;
        }

        ownerPreferences = new OwnerPreferences(
                getBoolean(USE_OWNER),
                get(DEFAULT_OWNER),
                getBoolean(OVERWRITE_OWNER));

        EasyBind.listen(ownerPreferences.useOwnerProperty(), (obs, oldValue, newValue) -> putBoolean(USE_OWNER, newValue));
        EasyBind.listen(ownerPreferences.defaultOwnerProperty(), (obs, oldValue, newValue) -> {
            put(DEFAULT_OWNER, newValue);
            // trigger re-determination of userAndHost and the dependent preferences
            userAndHost = null;
            filePreferences = null;
            internalPreferences = null;
        });
        EasyBind.listen(ownerPreferences.overwriteOwnerProperty(), (obs, oldValue, newValue) -> putBoolean(OVERWRITE_OWNER, newValue));

        return ownerPreferences;
    }

    @Override
    public TimestampPreferences getTimestampPreferences() {
        if (Objects.nonNull(timestampPreferences)) {
            return timestampPreferences;
        }

        timestampPreferences = new TimestampPreferences(
                getBoolean(ADD_CREATION_DATE),
                getBoolean(ADD_MODIFICATION_DATE),
                getBoolean(UPDATE_TIMESTAMP),
                FieldFactory.parseField(get(TIME_STAMP_FIELD)),
                get(TIME_STAMP_FORMAT));

        EasyBind.listen(timestampPreferences.addCreationDateProperty(), (obs, oldValue, newValue) -> putBoolean(ADD_CREATION_DATE, newValue));
        EasyBind.listen(timestampPreferences.addModificationDateProperty(), (obs, oldValue, newValue) -> putBoolean(ADD_MODIFICATION_DATE, newValue));

        return timestampPreferences;
    }

    @Override
    public GroupsPreferences getGroupsPreferences() {
        if (Objects.nonNull(groupsPreferences)) {
            return groupsPreferences;
        }

        groupsPreferences = new GroupsPreferences(
                GroupViewMode.valueOf(get(GROUP_INTERSECT_UNION_VIEW_MODE)),
                getBoolean(AUTO_ASSIGN_GROUP),
                getBoolean(DISPLAY_GROUP_COUNT),
                GroupHierarchyType.valueOf(get(DEFAULT_HIERARCHICAL_CONTEXT))
        );

        EasyBind.listen(groupsPreferences.groupViewModeProperty(), (obs, oldValue, newValue) -> put(GROUP_INTERSECT_UNION_VIEW_MODE, newValue.name()));
        EasyBind.listen(groupsPreferences.autoAssignGroupProperty(), (obs, oldValue, newValue) -> putBoolean(AUTO_ASSIGN_GROUP, newValue));
        EasyBind.listen(groupsPreferences.displayGroupCountProperty(), (obs, oldValue, newValue) -> putBoolean(DISPLAY_GROUP_COUNT, newValue));
        EasyBind.listen(groupsPreferences.defaultHierarchicalContextProperty(), (obs, oldValue, newValue) -> put(DEFAULT_HIERARCHICAL_CONTEXT, newValue.name()));

        return groupsPreferences;
    }

    //*************************************************************************************************************
    // EntryEditorPreferences
    //*************************************************************************************************************

    @Override
    public EntryEditorPreferences getEntryEditorPreferences() {
        if (Objects.nonNull(entryEditorPreferences)) {
            return entryEditorPreferences;
        }

        entryEditorPreferences = new EntryEditorPreferences(
                getEntryEditorTabs(),
                getDefaultEntryEditorTabs(),
                getBoolean(AUTO_OPEN_FORM),
                getBoolean(SHOW_RECOMMENDATIONS),
                getBoolean(ACCEPT_RECOMMENDATIONS),
                getBoolean(SHOW_LATEX_CITATIONS),
                getBoolean(DEFAULT_SHOW_SOURCE),
                getBoolean(VALIDATE_IN_ENTRY_EDITOR),
                getBoolean(ALLOW_INTEGER_EDITION_BIBTEX),
                getDouble(ENTRY_EDITOR_HEIGHT),
                getBoolean(AUTOLINK_FILES_ENABLED));

        EasyBind.listen(entryEditorPreferences.entryEditorTabs(), (obs, oldValue, newValue) -> storeEntryEditorTabs(newValue));
        // defaultEntryEditorTabs are read-only
        EasyBind.listen(entryEditorPreferences.shouldOpenOnNewEntryProperty(), (obs, oldValue, newValue) -> putBoolean(AUTO_OPEN_FORM, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowRecommendationsTabProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_RECOMMENDATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.isMrdlibAcceptedProperty(), (obs, oldValue, newValue) -> putBoolean(ACCEPT_RECOMMENDATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowLatexCitationsTabProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_LATEX_CITATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.showSourceTabByDefaultProperty(), (obs, oldValue, newValue) -> putBoolean(DEFAULT_SHOW_SOURCE, newValue));
        EasyBind.listen(entryEditorPreferences.enableValidationProperty(), (obs, oldValue, newValue) -> putBoolean(VALIDATE_IN_ENTRY_EDITOR, newValue));
        EasyBind.listen(entryEditorPreferences.allowIntegerEditionBibtexProperty(), (obs, oldValue, newValue) -> putBoolean(ALLOW_INTEGER_EDITION_BIBTEX, newValue));
        EasyBind.listen(entryEditorPreferences.dividerPositionProperty(), (obs, oldValue, newValue) -> putDouble(ENTRY_EDITOR_HEIGHT, newValue.doubleValue()));
        EasyBind.listen(entryEditorPreferences.autoLinkEnabledProperty(), (obs, oldValue, newValue) -> putBoolean(AUTOLINK_FILES_ENABLED, newValue));

        return entryEditorPreferences;
    }

    /**
     * Get a Map of defined tab names to default tab fields.
     *
     * @return A map of the currently defined tabs in the entry editor from scratch to cache
     */
    private Map<String, Set<Field>> getEntryEditorTabs() {
        Map<String, Set<Field>> tabs = new LinkedHashMap<>();
        List<String> tabNames = getSeries(CUSTOM_TAB_NAME);
        List<String> tabFields = getSeries(CUSTOM_TAB_FIELDS);

        if (tabNames.isEmpty() || (tabNames.size() != tabFields.size())) {
            // Nothing set, so we use the default values
            tabNames = getSeries(CUSTOM_TAB_NAME + "_def");
            tabFields = getSeries(CUSTOM_TAB_FIELDS + "_def");
        }

        for (int i = 0; i < tabNames.size(); i++) {
            tabs.put(tabNames.get(i), FieldFactory.parseFieldList(tabFields.get(i)));
        }
        return tabs;
    }

    /**
     * Stores the defined tabs and corresponding fields in the preferences.
     *
     * @param customTabs a map of tab names and the corresponding set of fields to be displayed in
     */
    private void storeEntryEditorTabs(Map<String, Set<Field>> customTabs) {
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

        getEntryEditorTabs();
    }

    private Map<String, Set<Field>> getDefaultEntryEditorTabs() {
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

    //*************************************************************************************************************
    // Network preferences
    //*************************************************************************************************************

    @Override
    public RemotePreferences getRemotePreferences() {
        if (Objects.nonNull(remotePreferences)) {
            return remotePreferences;
        }

        remotePreferences = new RemotePreferences(
                getInt(REMOTE_SERVER_PORT),
                getBoolean(USE_REMOTE_SERVER));

        EasyBind.listen(remotePreferences.portProperty(), (obs, oldValue, newValue) -> putInt(REMOTE_SERVER_PORT, newValue));
        EasyBind.listen(remotePreferences.useRemoteServerProperty(), (obs, oldValue, newValue) -> putBoolean(USE_REMOTE_SERVER, newValue));

        return remotePreferences;
    }

    @Override
    public ProxyPreferences getProxyPreferences() {
        if (Objects.nonNull(proxyPreferences)) {
            return proxyPreferences;
        }

        proxyPreferences = new ProxyPreferences(
                getBoolean(PROXY_USE),
                get(PROXY_HOSTNAME),
                get(PROXY_PORT),
                getBoolean(PROXY_USE_AUTHENTICATION),
                get(PROXY_USERNAME),
                getProxyPassword(),
                getBoolean(PROXY_PERSIST_PASSWORD));

        EasyBind.listen(proxyPreferences.useProxyProperty(), (obs, oldValue, newValue) -> putBoolean(PROXY_USE, newValue));
        EasyBind.listen(proxyPreferences.hostnameProperty(), (obs, oldValue, newValue) -> put(PROXY_HOSTNAME, newValue));
        EasyBind.listen(proxyPreferences.portProperty(), (obs, oldValue, newValue) -> put(PROXY_PORT, newValue));
        EasyBind.listen(proxyPreferences.useAuthenticationProperty(), (obs, oldValue, newValue) -> putBoolean(PROXY_USE_AUTHENTICATION, newValue));
        EasyBind.listen(proxyPreferences.usernameProperty(), (obs, oldValue, newValue) -> put(PROXY_USERNAME, newValue));
        EasyBind.listen(proxyPreferences.passwordProperty(), (obs, oldValue, newValue) -> setProxyPassword(newValue));
        EasyBind.listen(proxyPreferences.persistPasswordProperty(), (obs, oldValue, newValue) -> {
            putBoolean(PROXY_PERSIST_PASSWORD, newValue);
            if (!newValue) {
                try (final Keyring keyring = Keyring.create()) {
                    keyring.deletePassword("org.jabref", "proxy");
                } catch (Exception ex) {
                    LOGGER.warn("Unable to remove proxy credentials");
                }
            }
        });

        return proxyPreferences;
    }

    private String getProxyPassword() {
        if (getBoolean(PROXY_PERSIST_PASSWORD)) {
            try (final Keyring keyring = Keyring.create()) {
                return new Password(
                        keyring.getPassword("org.jabref", "proxy"),
                        getInternalPreferences().getUserAndHost())
                        .decrypt();
            } catch (PasswordAccessException ex) {
                LOGGER.warn("JabRef uses proxy password from key store but no password is stored");
            } catch (Exception ex) {
                LOGGER.warn("JabRef could not open the key store");
            }
        }
        return (String) defaults.get(PROXY_PASSWORD);
    }

    private void setProxyPassword(String password) {
        if (getProxyPreferences().shouldPersistPassword()) {
            try (final Keyring keyring = Keyring.create()) {
                if (StringUtil.isBlank(password)) {
                    keyring.deletePassword("org.jabref", "proxy");
                } else {
                    keyring.setPassword("org.jabref", "proxy", new Password(
                            password.trim(),
                            getInternalPreferences().getUserAndHost())
                            .encrypt());
                }
            } catch (Exception ex) {
                LOGGER.warn("Unable to open key store", ex);
            }
        }
    }

    @Override
    public SSLPreferences getSSLPreferences() {
        if (Objects.nonNull(sslPreferences)) {
            return sslPreferences;
        }

        sslPreferences = new SSLPreferences(
                get(TRUSTSTORE_PATH)
        );

        return sslPreferences;
    }

    //*************************************************************************************************************
    // CitationKeyPatternPreferences
    //*************************************************************************************************************

    private GlobalCitationKeyPattern getGlobalCitationKeyPattern() {
        GlobalCitationKeyPattern citationKeyPattern = GlobalCitationKeyPattern.fromPattern(get(DEFAULT_CITATION_KEY_PATTERN));
        Preferences preferences = PREFS_NODE.node(CITATION_KEY_PATTERNS_NODE);
        try {
            String[] keys = preferences.keys();
            for (String key : keys) {
                citationKeyPattern.addCitationKeyPattern(
                        EntryTypeFactory.parse(key),
                        preferences.get(key, null));
            }
        } catch (
                BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences.getKeyPattern", ex);
        }

        return citationKeyPattern;
    }

    // public for use in PreferenceMigrations
    public void storeGlobalCitationKeyPattern(GlobalCitationKeyPattern pattern) {
        if ((pattern.getDefaultValue() == null)
                || pattern.getDefaultValue().isEmpty()) {
            put(DEFAULT_CITATION_KEY_PATTERN, "");
        } else {
            put(DEFAULT_CITATION_KEY_PATTERN, pattern.getDefaultValue().get(0));
        }

        // Store overridden definitions to Preferences.
        Preferences preferences = PREFS_NODE.node(CITATION_KEY_PATTERNS_NODE);
        try {
            preferences.clear(); // We remove all old entries.
        } catch (
                BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences::putKeyPattern", ex);
        }

        for (EntryType entryType : pattern.getAllKeys()) {
            if (!pattern.isDefaultValue(entryType)) {
                // first entry in the map is the full pattern
                preferences.put(entryType.getName(), pattern.getValue(entryType).get(0));
            }
        }
    }

    private void clearCitationKeyPatterns() throws BackingStoreException {
        Preferences preferences = PREFS_NODE.node(CITATION_KEY_PATTERNS_NODE);
        preferences.clear();
        getCitationKeyPatternPreferences().setKeyPattern(getGlobalCitationKeyPattern());
    }

    @Override
    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        if (Objects.nonNull(citationKeyPatternPreferences)) {
            return citationKeyPatternPreferences;
        }

        citationKeyPatternPreferences = new CitationKeyPatternPreferences(
                getBoolean(AVOID_OVERWRITING_KEY),
                getBoolean(WARN_BEFORE_OVERWRITING_KEY),
                getBoolean(GENERATE_KEYS_BEFORE_SAVING),
                getKeySuffix(),
                get(KEY_PATTERN_REGEX),
                get(KEY_PATTERN_REPLACEMENT),
                get(UNWANTED_CITATION_KEY_CHARACTERS),
                getGlobalCitationKeyPattern(),
                (String) defaults.get(DEFAULT_CITATION_KEY_PATTERN),
                getBibEntryPreferences().keywordSeparatorProperty());

        EasyBind.listen(citationKeyPatternPreferences.shouldAvoidOverwriteCiteKeyProperty(),
                (obs, oldValue, newValue) -> putBoolean(AVOID_OVERWRITING_KEY, newValue));
        EasyBind.listen(citationKeyPatternPreferences.shouldWarnBeforeOverwriteCiteKeyProperty(),
                (obs, oldValue, newValue) -> putBoolean(WARN_BEFORE_OVERWRITING_KEY, newValue));
        EasyBind.listen(citationKeyPatternPreferences.shouldGenerateCiteKeysBeforeSavingProperty(),
                (obs, oldValue, newValue) -> putBoolean(GENERATE_KEYS_BEFORE_SAVING, newValue));
        EasyBind.listen(citationKeyPatternPreferences.keySuffixProperty(), (obs, oldValue, newValue) -> {
                    putBoolean(KEY_GEN_ALWAYS_ADD_LETTER, newValue == CitationKeyPatternPreferences.KeySuffix.ALWAYS);
                    putBoolean(KEY_GEN_FIRST_LETTER_A, newValue == CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A);
                });
        EasyBind.listen(citationKeyPatternPreferences.keyPatternRegexProperty(),
                (obs, oldValue, newValue) -> put(KEY_PATTERN_REGEX, newValue));
        EasyBind.listen(citationKeyPatternPreferences.keyPatternReplacementProperty(),
                (obs, oldValue, newValue) -> put(KEY_PATTERN_REPLACEMENT, newValue));
        EasyBind.listen(citationKeyPatternPreferences.unwantedCharactersProperty(),
                (obs, oldValue, newValue) -> put(UNWANTED_CITATION_KEY_CHARACTERS, newValue));
        EasyBind.listen(citationKeyPatternPreferences.keyPatternProperty(),
                (obs, oldValue, newValue) -> storeGlobalCitationKeyPattern(newValue));

        return citationKeyPatternPreferences;
    }

    private CitationKeyPatternPreferences.KeySuffix getKeySuffix() {
        CitationKeyPatternPreferences.KeySuffix keySuffix =
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B;
        if (getBoolean(KEY_GEN_ALWAYS_ADD_LETTER)) {
            keySuffix = CitationKeyPatternPreferences.KeySuffix.ALWAYS;
        } else if (getBoolean(KEY_GEN_FIRST_LETTER_A)) {
            keySuffix = CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A;
        }
        return keySuffix;
    }

    //*************************************************************************************************************
    // ExternalApplicationsPreferences
    //*************************************************************************************************************

    @Override
    public PushToApplicationPreferences getPushToApplicationPreferences() {
        if (Objects.nonNull(pushToApplicationPreferences)) {
            return pushToApplicationPreferences;
        }

        Map<String, String> applicationCommands = new HashMap<>();
        applicationCommands.put(PushToApplications.EMACS, get(PUSH_EMACS_PATH));
        applicationCommands.put(PushToApplications.LYX, get(PUSH_LYXPIPE));
        applicationCommands.put(PushToApplications.TEXMAKER, get(PUSH_TEXMAKER_PATH));
        applicationCommands.put(PushToApplications.TEXSTUDIO, get(PUSH_TEXSTUDIO_PATH));
        applicationCommands.put(PushToApplications.VIM, get(PUSH_VIM));
        applicationCommands.put(PushToApplications.WIN_EDT, get(PUSH_WINEDT_PATH));

        pushToApplicationPreferences = new PushToApplicationPreferences(
                get(PUSH_TO_APPLICATION),
                applicationCommands,
                get(PUSH_EMACS_ADDITIONAL_PARAMETERS),
                get(PUSH_VIM_SERVER)
        );

        EasyBind.listen(pushToApplicationPreferences.activeApplicationNameProperty(), (obs, oldValue, newValue) -> put(PUSH_TO_APPLICATION, newValue));
        pushToApplicationPreferences.getCommandPaths().addListener((obs, oldValue, newValue) -> storePushToApplicationPath(newValue));
        EasyBind.listen(pushToApplicationPreferences.emacsArgumentsProperty(), (obs, oldValue, newValue) -> put(PUSH_EMACS_ADDITIONAL_PARAMETERS, newValue));
        EasyBind.listen(pushToApplicationPreferences.vimServerProperty(), (obs, oldValue, newValue) -> put(PUSH_VIM_SERVER, newValue));

        return pushToApplicationPreferences;
    }

    private void storePushToApplicationPath(Map<String, String> commandPair) {
        commandPair.forEach((key, value) -> {
            switch (key) {
                case PushToApplications.EMACS ->
                        put(PUSH_EMACS_PATH, value);
                case PushToApplications.LYX ->
                        put(PUSH_LYXPIPE, value);
                case PushToApplications.TEXMAKER ->
                        put(PUSH_TEXMAKER_PATH, value);
                case PushToApplications.TEXSTUDIO ->
                        put(PUSH_TEXSTUDIO_PATH, value);
                case PushToApplications.VIM ->
                        put(PUSH_VIM, value);
                case PushToApplications.WIN_EDT ->
                        put(PUSH_WINEDT_PATH, value);
            }
        });
    }

    @Override
    public ExternalApplicationsPreferences getExternalApplicationsPreferences() {
        if (Objects.nonNull(externalApplicationsPreferences)) {
            return externalApplicationsPreferences;
        }

        externalApplicationsPreferences = new ExternalApplicationsPreferences(
                get(EMAIL_SUBJECT),
                getBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES),
                get(CITE_COMMAND),
                !getBoolean(USE_DEFAULT_CONSOLE_APPLICATION), // mind the !
                get(CONSOLE_COMMAND),
                !getBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION), // mind the !
                get(FILE_BROWSER_COMMAND),
                get(KINDLE_EMAIL));

        EasyBind.listen(externalApplicationsPreferences.eMailSubjectProperty(),
                (obs, oldValue, newValue) -> put(EMAIL_SUBJECT, newValue));
        EasyBind.listen(externalApplicationsPreferences.autoOpenEmailAttachmentsFolderProperty(),
                (obs, oldValue, newValue) -> putBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES, newValue));
        EasyBind.listen(externalApplicationsPreferences.citeCommandProperty(),
                (obs, oldValue, newValue) -> put(CITE_COMMAND, newValue));
        EasyBind.listen(externalApplicationsPreferences.useCustomTerminalProperty(),
                (obs, oldValue, newValue) -> putBoolean(USE_DEFAULT_CONSOLE_APPLICATION, !newValue)); // mind the !
        EasyBind.listen(externalApplicationsPreferences.customTerminalCommandProperty(),
                (obs, oldValue, newValue) -> put(CONSOLE_COMMAND, newValue));
        EasyBind.listen(externalApplicationsPreferences.useCustomFileBrowserProperty(),
                (obs, oldValue, newValue) -> putBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION, !newValue)); // mind the !
        EasyBind.listen(externalApplicationsPreferences.customFileBrowserCommandProperty(),
                (obs, oldValue, newValue) -> put(FILE_BROWSER_COMMAND, newValue));
        EasyBind.listen(externalApplicationsPreferences.kindleEmailProperty(),
                (obs, oldValue, newValue) -> put(KINDLE_EMAIL, newValue));

        return externalApplicationsPreferences;
    }

    //*************************************************************************************************************
    // Main table and search dialog preferences
    //*************************************************************************************************************

    @Override
    public MainTablePreferences getMainTablePreferences() {
        if (Objects.nonNull(mainTablePreferences)) {
            return mainTablePreferences;
        }

        mainTablePreferences = new MainTablePreferences(
                getMainTableColumnPreferences(),
                getBoolean(AUTO_RESIZE_MODE),
                getBoolean(EXTRA_FILE_COLUMNS));

        EasyBind.listen(mainTablePreferences.resizeColumnsToFitProperty(),
                (obs, oldValue, newValue) -> putBoolean(AUTO_RESIZE_MODE, newValue));
        EasyBind.listen(mainTablePreferences.extraFileColumnsEnabledProperty(),
                (obs, oldValue, newValue) -> putBoolean(EXTRA_FILE_COLUMNS, newValue));

        return mainTablePreferences;
    }

    @Override
    public ColumnPreferences getMainTableColumnPreferences() {
        if (Objects.nonNull(mainTableColumnPreferences)) {
            return mainTableColumnPreferences;
        }

        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, COLUMN_WIDTHS, COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(COLUMN_SORT_ORDER, columns);
        mainTableColumnPreferences = new ColumnPreferences(columns, columnSortOrder);

        mainTableColumnPreferences.getColumns().addListener((InvalidationListener) change -> {
            putStringList(COLUMN_NAMES, getColumnNamesAsStringList(mainTableColumnPreferences));
            putStringList(COLUMN_WIDTHS, getColumnWidthsAsStringList(mainTableColumnPreferences));
            putStringList(COLUMN_SORT_TYPES, getColumnSortTypesAsStringList(mainTableColumnPreferences));
        });
        mainTableColumnPreferences.getColumnSortOrder().addListener((InvalidationListener) change ->
                putStringList(COLUMN_SORT_ORDER, getColumnSortOrderAsStringList(mainTableColumnPreferences)));

        return mainTableColumnPreferences;
    }

    @Override
    public ColumnPreferences getSearchDialogColumnPreferences() {
        if (Objects.nonNull(searchDialogColumnPreferences)) {
            return searchDialogColumnPreferences;
        }

        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, SEARCH_DIALOG_COLUMN_WIDTHS, SEARCH_DIALOG_COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(SEARCH_DIALOG_COLUMN_SORT_ORDER, columns);
        searchDialogColumnPreferences = new ColumnPreferences(columns, columnSortOrder);

        searchDialogColumnPreferences.getColumns().addListener((InvalidationListener) change -> {
            // MainTable and SearchResultTable use the same set of columnNames
            // putStringList(SEARCH_DIALOG_COLUMN_NAMES, getColumnNamesAsStringList(columnPreferences));
            putStringList(SEARCH_DIALOG_COLUMN_WIDTHS, getColumnWidthsAsStringList(searchDialogColumnPreferences));
            putStringList(SEARCH_DIALOG_COLUMN_SORT_TYPES, getColumnSortTypesAsStringList(searchDialogColumnPreferences));
        });
        searchDialogColumnPreferences.getColumnSortOrder().addListener((InvalidationListener) change ->
                putStringList(SEARCH_DIALOG_COLUMN_SORT_ORDER, getColumnSortOrderAsStringList(searchDialogColumnPreferences)));

        return searchDialogColumnPreferences;
    }

    // --- Generic column handling ---
    @SuppressWarnings("SameParameterValue")
    private List<MainTableColumnModel> getColumns(String columnNamesList, String columnWidthList, String sortTypeList, double defaultWidth) {
        List<String> columnNames = getStringList(columnNamesList);
        List<Double> columnWidths = getStringList(columnWidthList)
                .stream()
                .map(string -> {
                    try {
                        return Double.parseDouble(string);
                    } catch (
                            NumberFormatException e) {
                        LOGGER.error("Exception while parsing column widths. Choosing default.", e);
                        return defaultWidth;
                    }
                }).toList();

        List<SortType> columnSortTypes = getStringList(sortTypeList)
                .stream()
                .map(SortType::valueOf).toList();

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
        return columns;
    }

    private List<MainTableColumnModel> getColumnSortOrder(String sortOrderList, List<MainTableColumnModel> tableColumns) {
        List<MainTableColumnModel> columnsOrdered = new ArrayList<>();
        getStringList(sortOrderList).forEach(columnName -> tableColumns.stream().filter(column -> column.getName().equals(columnName))
                                                                       .findFirst()
                                                                       .ifPresent(columnsOrdered::add));

        return columnsOrdered;
    }

    private static List<String> getColumnNamesAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumns().stream()
                                .map(MainTableColumnModel::getName)
                                .toList();
    }

    private static List<String> getColumnWidthsAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumns().stream()
                                .map(column -> column.widthProperty().getValue().toString())
                                .toList();
    }

    private static List<String> getColumnSortTypesAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumns().stream()
                                .map(column -> column.sortTypeProperty().getValue().toString())
                                .toList();
    }

    private static List<String> getColumnSortOrderAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumnSortOrder().stream()
                                .map(MainTableColumnModel::getName)
                                .collect(Collectors.toList());
    }

    //*************************************************************************************************************
    // NameDisplayPreferences
    //*************************************************************************************************************

    @Override
    public NameDisplayPreferences getNameDisplayPreferences() {
        if (Objects.nonNull(nameDisplayPreferences)) {
            return nameDisplayPreferences;
        }

        nameDisplayPreferences = new NameDisplayPreferences(
                getNameDisplayStyle(),
                getNameAbbreviationStyle());

        EasyBind.listen(nameDisplayPreferences.displayStyleProperty(), (obs, oldValue, newValue) -> {
            putBoolean(NAMES_NATBIB, newValue == DisplayStyle.NATBIB);
            putBoolean(NAMES_AS_IS, newValue == DisplayStyle.AS_IS);
            putBoolean(NAMES_FIRST_LAST, newValue == DisplayStyle.FIRSTNAME_LASTNAME);
        });
        EasyBind.listen(nameDisplayPreferences.abbreviationStyleProperty(), (obs, oldValue, newValue) -> {
            putBoolean(ABBR_AUTHOR_NAMES, newValue == AbbreviationStyle.FULL);
            putBoolean(NAMES_LAST_ONLY, newValue == AbbreviationStyle.LASTNAME_ONLY);
        });

        return nameDisplayPreferences;
    }

    private AbbreviationStyle getNameAbbreviationStyle() {
        AbbreviationStyle abbreviationStyle = AbbreviationStyle.NONE; // default
        if (getBoolean(ABBR_AUTHOR_NAMES)) {
            abbreviationStyle = AbbreviationStyle.FULL;
        } else if (getBoolean(NAMES_LAST_ONLY)) {
            abbreviationStyle = AbbreviationStyle.LASTNAME_ONLY;
        }
        return abbreviationStyle;
    }

    private DisplayStyle getNameDisplayStyle() {
        DisplayStyle displayStyle = DisplayStyle.LASTNAME_FIRSTNAME; // default
        if (getBoolean(NAMES_NATBIB)) {
            displayStyle = DisplayStyle.NATBIB;
        } else if (getBoolean(NAMES_AS_IS)) {
            displayStyle = DisplayStyle.AS_IS;
        } else if (getBoolean(NAMES_FIRST_LAST)) {
            displayStyle = DisplayStyle.FIRSTNAME_LASTNAME;
        }
        return displayStyle;
    }

    //*************************************************************************************************************
    // BibEntryPreferences
    //*************************************************************************************************************

    @Override
    public BibEntryPreferences getBibEntryPreferences() {
        if (Objects.nonNull(bibEntryPreferences)) {
            return bibEntryPreferences;
        }

        bibEntryPreferences = new BibEntryPreferences(
                get(KEYWORD_SEPARATOR).charAt(0)
        );

        EasyBind.listen(bibEntryPreferences.keywordSeparatorProperty(), ((observable, oldValue, newValue) -> put(KEYWORD_SEPARATOR, String.valueOf(newValue))));

        return bibEntryPreferences;
    }

    //*************************************************************************************************************
    // InternalPreferences
    //*************************************************************************************************************

    @Override
    public InternalPreferences getInternalPreferences() {
        if (Objects.nonNull(internalPreferences)) {
            return internalPreferences;
        }

        internalPreferences = new InternalPreferences(
                Version.parse(get(VERSION_IGNORED_UPDATE)),
                getPath(PREFS_EXPORT_PATH, OS.getNativeDesktop().getDefaultFileChooserDirectory()),
                getUserAndHost(),
                getBoolean(MEMORY_STICK_MODE));

        EasyBind.listen(internalPreferences.ignoredVersionProperty(),
                (obs, oldValue, newValue) -> put(VERSION_IGNORED_UPDATE, newValue.toString()));
        EasyBind.listen(internalPreferences.lastPreferencesExportPathProperty(),
                (obs, oldValue, newValue) -> put(PREFS_EXPORT_PATH, newValue.toString()));
        // user is a static value, should only be changed for debugging
        EasyBind.listen(internalPreferences.memoryStickModeProperty(), (obs, oldValue, newValue) -> {
            putBoolean(MEMORY_STICK_MODE, newValue);
            if (!newValue) {
                try {
                    Files.deleteIfExists(Path.of("jabref.xml"));
                } catch (IOException e) {
                    LOGGER.warn("Error accessing filesystem");
                }
            }
        });

        return internalPreferences;
    }

    private String getUserAndHost() {
        if (StringUtil.isNotBlank(userAndHost)) {
            return userAndHost;
        }
        userAndHost = get(DEFAULT_OWNER) + '-' + OS.getNativeDesktop().getHostName();
        return userAndHost;
    }

    //*************************************************************************************************************
    // WorkspacePreferences
    //*************************************************************************************************************

    @Override
    public WorkspacePreferences getWorkspacePreferences() {
        if (workspacePreferences != null) {
            return workspacePreferences;
        }

        workspacePreferences = new WorkspacePreferences(
                getLanguage(),
                getBoolean(OVERRIDE_DEFAULT_FONT_SIZE),
                getInt(MAIN_FONT_SIZE),
                (Integer) defaults.get(MAIN_FONT_SIZE),
                new Theme(get(FX_THEME)),
                getBoolean(OPEN_LAST_EDITED),
                getBoolean(SHOW_ADVANCED_HINTS),
                getBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION),
                getBoolean(CONFIRM_DELETE));

        EasyBind.listen(workspacePreferences.languageProperty(), (obs, oldValue, newValue) -> {
            put(LANGUAGE, newValue.getId());
            if (oldValue != newValue) {
                setLanguageDependentDefaultValues();
                Localization.setLanguage(newValue);
            }
        });
        EasyBind.listen(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), (obs, oldValue, newValue) -> putBoolean(OVERRIDE_DEFAULT_FONT_SIZE, newValue));
        EasyBind.listen(workspacePreferences.mainFontSizeProperty(), (obs, oldValue, newValue) -> putInt(MAIN_FONT_SIZE, newValue));
        EasyBind.listen(workspacePreferences.themeProperty(), (obs, oldValue, newValue) -> put(FX_THEME, newValue.getName()));
        EasyBind.listen(workspacePreferences.openLastEditedProperty(), (obs, oldValue, newValue) -> putBoolean(OPEN_LAST_EDITED, newValue));
        EasyBind.listen(workspacePreferences.showAdvancedHintsProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_ADVANCED_HINTS, newValue));
        EasyBind.listen(workspacePreferences.warnAboutDuplicatesInInspectionProperty(), (obs, oldValue, newValue) -> putBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION, newValue));
        EasyBind.listen(workspacePreferences.confirmDeleteProperty(), (obs, oldValue, newValue) -> putBoolean(CONFIRM_DELETE, newValue));

        return workspacePreferences;
    }

    private Language getLanguage() {
        return Stream.of(Language.values())
                     .filter(language -> language.getId().equalsIgnoreCase(get(LANGUAGE)))
                     .findFirst()
                     .orElse(Language.ENGLISH);
    }

    @Override
    public FieldPreferences getFieldPreferences() {
        if (Objects.nonNull(fieldPreferences)) {
            return fieldPreferences;
        }

        fieldPreferences = new FieldPreferences(
                !getBoolean(DO_NOT_RESOLVE_STRINGS), // mind the !
                getStringList(RESOLVE_STRINGS_FOR_FIELDS).stream()
                                                         .map(FieldFactory::parseField)
                                                         .collect(Collectors.toList()),
                getStringList(NON_WRAPPABLE_FIELDS).stream()
                                                   .map(FieldFactory::parseField)
                                                   .collect(Collectors.toList()));

        EasyBind.listen(fieldPreferences.resolveStringsProperty(), (obs, oldValue, newValue) -> putBoolean(DO_NOT_RESOLVE_STRINGS, !newValue));
        fieldPreferences.getResolvableFields().addListener((InvalidationListener) change ->
                put(RESOLVE_STRINGS_FOR_FIELDS, FieldFactory.serializeFieldsList(fieldPreferences.getResolvableFields())));
        fieldPreferences.getNonWrappableFields().addListener((InvalidationListener) change ->
                put(NON_WRAPPABLE_FIELDS, FieldFactory.serializeFieldsList(fieldPreferences.getNonWrappableFields())));

        return fieldPreferences;
    }

    //*************************************************************************************************************
    // Linked files preferences
    //*************************************************************************************************************

    @Override
    public FilePreferences getFilePreferences() {
        if (Objects.nonNull(filePreferences)) {
            return filePreferences;
        }

        filePreferences = new FilePreferences(
                getInternalPreferences().getUserAndHost(),
                getPath(MAIN_FILE_DIRECTORY, OS.getNativeDesktop().getDefaultFileChooserDirectory()).toString(),
                getBoolean(STORE_RELATIVE_TO_BIB),
                get(IMPORT_FILENAMEPATTERN),
                get(IMPORT_FILEDIRPATTERN),
                getBoolean(DOWNLOAD_LINKED_FILES),
                getBoolean(FULLTEXT_INDEX_LINKED_FILES),
                Path.of(get(WORKING_DIRECTORY)),
                ExternalFileTypes.fromString(get(EXTERNAL_FILE_TYPES)),
                getBoolean(CREATE_BACKUP),
                // We choose the data directory, because a ".bak" file should survive cache cleanups
                getPath(BACKUP_DIRECTORY, OS.getNativeDesktop().getBackupDirectory()));

        EasyBind.listen(filePreferences.mainFileDirectoryProperty(), (obs, oldValue, newValue) -> put(MAIN_FILE_DIRECTORY, newValue));
        EasyBind.listen(filePreferences.storeFilesRelativeToBibFileProperty(), (obs, oldValue, newValue) -> putBoolean(STORE_RELATIVE_TO_BIB, newValue));
        EasyBind.listen(filePreferences.fileNamePatternProperty(), (obs, oldValue, newValue) -> put(IMPORT_FILENAMEPATTERN, newValue));
        EasyBind.listen(filePreferences.fileDirectoryPatternProperty(), (obs, oldValue, newValue) -> put(IMPORT_FILEDIRPATTERN, newValue));
        EasyBind.listen(filePreferences.downloadLinkedFilesProperty(), (obs, oldValue, newValue) -> putBoolean(DOWNLOAD_LINKED_FILES, newValue));
        EasyBind.listen(filePreferences.fulltextIndexLinkedFilesProperty(), (obs, oldValue, newValue) -> putBoolean(FULLTEXT_INDEX_LINKED_FILES, newValue));
        EasyBind.listen(filePreferences.workingDirectoryProperty(), (obs, oldValue, newValue) -> put(WORKING_DIRECTORY, newValue.toString()));
        filePreferences.getExternalFileTypes().addListener((SetChangeListener<ExternalFileType>) c ->
                put(EXTERNAL_FILE_TYPES, ExternalFileTypes.toStringList(filePreferences.getExternalFileTypes())));
        EasyBind.listen(filePreferences.createBackupProperty(), (obs, oldValue, newValue) -> putBoolean(CREATE_BACKUP, newValue));
        EasyBind.listen(filePreferences.backupDirectoryProperty(), (obs, oldValue, newValue) -> put(BACKUP_DIRECTORY, newValue.toString()));

        return filePreferences;
    }

    @Override
    public AutoLinkPreferences getAutoLinkPreferences() {
        if (Objects.nonNull(autoLinkPreferences)) {
            return autoLinkPreferences;
        }

        autoLinkPreferences = new AutoLinkPreferences(
                getAutoLinkKeyDependency(),
                get(AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY),
                getBoolean(ASK_AUTO_NAMING_PDFS_AGAIN),
                bibEntryPreferences.keywordSeparatorProperty());

        EasyBind.listen(autoLinkPreferences.citationKeyDependencyProperty(), (obs, oldValue, newValue) -> {
                    // Starts bibtex only omitted, as it is not being saved
                    putBoolean(AUTOLINK_EXACT_KEY_ONLY, newValue == AutoLinkPreferences.CitationKeyDependency.EXACT);
                    putBoolean(AUTOLINK_USE_REG_EXP_SEARCH_KEY, newValue == AutoLinkPreferences.CitationKeyDependency.REGEX);
                });
        EasyBind.listen(autoLinkPreferences.askAutoNamingPdfsProperty(),
                (obs, oldValue, newValue) -> putBoolean(ASK_AUTO_NAMING_PDFS_AGAIN, newValue));
        EasyBind.listen(autoLinkPreferences.regularExpressionProperty(),
                (obs, oldValue, newValue) -> put(AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, newValue));

        return autoLinkPreferences;
    }

    private AutoLinkPreferences.CitationKeyDependency getAutoLinkKeyDependency() {
        AutoLinkPreferences.CitationKeyDependency citationKeyDependency =
                AutoLinkPreferences.CitationKeyDependency.START; // default
        if (getBoolean(AUTOLINK_EXACT_KEY_ONLY)) {
            citationKeyDependency = AutoLinkPreferences.CitationKeyDependency.EXACT;
        } else if (getBoolean(AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
            citationKeyDependency = AutoLinkPreferences.CitationKeyDependency.REGEX;
        }
        return citationKeyDependency;
    }

    //*************************************************************************************************************
    // Import/Export preferences
    //*************************************************************************************************************

    @Override
    public ExportPreferences getExportPreferences() {
        if (Objects.nonNull(exportPreferences)) {
            return exportPreferences;
        }

        exportPreferences = new ExportPreferences(
                get(LAST_USED_EXPORT),
                Path.of(get(EXPORT_WORKING_DIRECTORY)),
                getExportSaveOrder(),
                getCustomExportFormats());

        EasyBind.listen(exportPreferences.lastExportExtensionProperty(), (obs, oldValue, newValue) -> put(LAST_USED_EXPORT, newValue));
        EasyBind.listen(exportPreferences.exportWorkingDirectoryProperty(), (obs, oldValue, newValue) -> put(EXPORT_WORKING_DIRECTORY, newValue.toString()));
        EasyBind.listen(exportPreferences.exportSaveOrderProperty(), (obs, oldValue, newValue) -> storeExportSaveOrder(newValue));
        exportPreferences.getCustomExporters().addListener((InvalidationListener) c -> storeCustomExportFormats(exportPreferences.getCustomExporters()));

        return exportPreferences;
    }

    private SaveOrder getExportSaveOrder() {
        List<SaveOrder.SortCriterion> sortCriteria = List.of(
                new SaveOrder.SortCriterion(FieldFactory.parseField(get(EXPORT_PRIMARY_SORT_FIELD)), getBoolean(EXPORT_PRIMARY_SORT_DESCENDING)),
                new SaveOrder.SortCriterion(FieldFactory.parseField(get(EXPORT_SECONDARY_SORT_FIELD)), getBoolean(EXPORT_SECONDARY_SORT_DESCENDING)),
                new SaveOrder.SortCriterion(FieldFactory.parseField(get(EXPORT_TERTIARY_SORT_FIELD)), getBoolean(EXPORT_TERTIARY_SORT_DESCENDING))
        );

        return new SaveOrder(
                SaveOrder.OrderType.fromBooleans(getBoolean(EXPORT_IN_SPECIFIED_ORDER), getBoolean(EXPORT_IN_ORIGINAL_ORDER)),
                sortCriteria
        );
    }

    private void storeExportSaveOrder(SaveOrder saveOrder) {
        putBoolean(EXPORT_IN_ORIGINAL_ORDER, saveOrder.getOrderType() == SaveOrder.OrderType.ORIGINAL);
        putBoolean(EXPORT_IN_SPECIFIED_ORDER, saveOrder.getOrderType() == SaveOrder.OrderType.SPECIFIED);

        put(EXPORT_PRIMARY_SORT_FIELD, saveOrder.getSortCriteria().get(0).field.getName());
        put(EXPORT_SECONDARY_SORT_FIELD, saveOrder.getSortCriteria().get(1).field.getName());
        put(EXPORT_TERTIARY_SORT_FIELD, saveOrder.getSortCriteria().get(2).field.getName());
        putBoolean(EXPORT_PRIMARY_SORT_DESCENDING, saveOrder.getSortCriteria().get(0).descending);
        putBoolean(EXPORT_SECONDARY_SORT_DESCENDING, saveOrder.getSortCriteria().get(1).descending);
        putBoolean(EXPORT_TERTIARY_SORT_DESCENDING, saveOrder.getSortCriteria().get(2).descending);
    }

    private SaveOrder getTableSaveOrder() {
        List<MainTableColumnModel> sortOrder = mainTableColumnPreferences.getColumnSortOrder();
        List<SaveOrder.SortCriterion> criteria = new ArrayList<>();

        for (var column : sortOrder) {
            boolean descending = column.getSortType() == SortType.DESCENDING;
            criteria.add(new SaveOrder.SortCriterion(
                    FieldFactory.parseField(column.getQualifier()),
                    descending));
        }

        return new SaveOrder(SaveOrder.OrderType.TABLE, criteria);
    }

    @Override
    public SaveConfiguration getExportConfiguration() {
        SaveOrder saveOrder = switch (getExportSaveOrder().getOrderType()) {
            case TABLE -> this.getTableSaveOrder();
            case SPECIFIED -> this.getExportSaveOrder();
            case ORIGINAL -> SaveOrder.getDefaultSaveOrder();
        };

        return new SaveConfiguration()
                .withSaveOrder(saveOrder)
                .withMetadataSaveOrder(false)
                .withReformatOnSave(getLibraryPreferences().shouldAlwaysReformatOnSave());
    }

    private List<TemplateExporter> getCustomExportFormats() {
        LayoutFormatterPreferences layoutPreferences = getLayoutFormatterPreferences();
        SaveConfiguration saveConfiguration = getExportConfiguration();
        List<TemplateExporter> formats = new ArrayList<>();

        for (String toImport : getSeries(CUSTOM_EXPORT_FORMAT)) {
            List<String> formatData = convertStringToList(toImport);
            TemplateExporter format = new TemplateExporter(
                    formatData.get(EXPORTER_NAME_INDEX),
                    formatData.get(EXPORTER_FILENAME_INDEX),
                    formatData.get(EXPORTER_EXTENSION_INDEX),
                    layoutPreferences,
                    saveConfiguration);
            format.setCustomExport(true);
            formats.add(format);
        }
        return formats;
    }

    private void storeCustomExportFormats(List<TemplateExporter> exporters) {
        if (exporters.isEmpty()) {
            purgeSeries(CUSTOM_EXPORT_FORMAT, 0);
        } else {
            for (int i = 0; i < exporters.size(); i++) {
                List<String> exporterData = new ArrayList<>();
                exporterData.add(EXPORTER_NAME_INDEX, exporters.get(i).getName());
                exporterData.add(EXPORTER_FILENAME_INDEX, exporters.get(i).getLayoutFileName());
                // Only stores the first extension associated with FileType
                exporterData.add(EXPORTER_EXTENSION_INDEX, exporters.get(i).getFileType().getExtensions().get(0));
                putStringList(CUSTOM_EXPORT_FORMAT + i, exporterData);
            }
            purgeSeries(CUSTOM_EXPORT_FORMAT, exporters.size());
        }
    }

    //*************************************************************************************************************
    // Preview preferences
    //*************************************************************************************************************

    @Override
    public PreviewPreferences getPreviewPreferences() {
        if (Objects.nonNull(previewPreferences)) {
            return previewPreferences;
        }

        String style = get(PREVIEW_STYLE);
        List<PreviewLayout> layouts = getPreviewLayouts(style);

        this.previewPreferences = new PreviewPreferences(
                layouts,
                getPreviewCyclePosition(layouts),
                new TextBasedPreviewLayout(style, getLayoutFormatterPreferences(), Globals.journalAbbreviationRepository),
                (String) defaults.get(PREVIEW_STYLE),
                getBoolean(PREVIEW_AS_TAB));

        previewPreferences.getLayoutCycle().addListener((InvalidationListener) c -> storePreviewLayouts(previewPreferences.getLayoutCycle()));
        EasyBind.listen(previewPreferences.layoutCyclePositionProperty(), (obs, oldValue, newValue) -> putInt(CYCLE_PREVIEW_POS, newValue));
        EasyBind.listen(previewPreferences.customPreviewLayoutProperty(), (obs, oldValue, newValue) -> put(PREVIEW_STYLE, newValue.getText()));
        EasyBind.listen(previewPreferences.showPreviewAsExtraTabProperty(), (obs, oldValue, newValue) -> putBoolean(PREVIEW_AS_TAB, newValue));

        return this.previewPreferences;
    }

    private List<PreviewLayout> getPreviewLayouts(String style) {
        List<String> cycle = getStringList(CYCLE_PREVIEW);

        // For backwards compatibility always add at least the default preview to the cycle
        if (cycle.isEmpty()) {
            cycle.add("Preview");
        }

        return cycle.stream()
                    .map(layout -> {
                        if (CitationStyle.isCitationStyleFile(layout)) {
                            return CitationStyle.createCitationStyleFromFile(layout)
                                                .map(file -> (PreviewLayout) new CitationStylePreviewLayout(file, Globals.entryTypesManager))
                                                .orElse(null);
                        } else {
                            return new TextBasedPreviewLayout(style, getLayoutFormatterPreferences(), Globals.journalAbbreviationRepository);
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }

    private void storePreviewLayouts(ObservableList<PreviewLayout> previewCycle) {
        putStringList(CYCLE_PREVIEW, previewCycle.stream()
                                                 .map(layout -> {
                                                     if (layout instanceof CitationStylePreviewLayout citationStyleLayout) {
                                                         return citationStyleLayout.getFilePath();
                                                     } else {
                                                         return layout.getDisplayName();
                                                     }
                                                 }).collect(Collectors.toList())
        );
    }

    private int getPreviewCyclePosition(List<PreviewLayout> layouts) {
        int storedCyclePos = getInt(CYCLE_PREVIEW_POS);
        if (storedCyclePos < layouts.size()) {
            return storedCyclePos;
        } else {
            return 0; // fallback if stored position is no longer valid
        }
    }

    //*************************************************************************************************************
    // SidePanePreferences
    //*************************************************************************************************************

    @Override
    public SidePanePreferences getSidePanePreferences() {
        if (Objects.nonNull(sidePanePreferences)) {
            return sidePanePreferences;
        }

        sidePanePreferences = new SidePanePreferences(
                getVisibleSidePanes(),
                getSidePanePreferredPositions(),
                getInt(SELECTED_FETCHER_INDEX));

        sidePanePreferences.visiblePanes().addListener((InvalidationListener) listener ->
                storeVisibleSidePanes(sidePanePreferences.visiblePanes()));
        sidePanePreferences.getPreferredPositions().addListener((InvalidationListener) listener ->
                storeSidePanePreferredPositions(sidePanePreferences.getPreferredPositions()));
        EasyBind.listen(sidePanePreferences.webSearchFetcherSelectedProperty(), (obs, oldValue, newValue) -> putInt(SELECTED_FETCHER_INDEX, newValue));

        return sidePanePreferences;
    }

    private Set<SidePaneType> getVisibleSidePanes() {
        HashSet<SidePaneType> visiblePanes = new HashSet<>();
        if (getBoolean(WEB_SEARCH_VISIBLE)) {
            visiblePanes.add(SidePaneType.WEB_SEARCH);
        }
        if (getBoolean(GROUP_SIDEPANE_VISIBLE)) {
            visiblePanes.add(SidePaneType.GROUPS);
        }
        if (getBoolean(OO_SHOW_PANEL)) {
            visiblePanes.add(SidePaneType.OPEN_OFFICE);
        }
        return visiblePanes;
    }

    private void storeVisibleSidePanes(Set<SidePaneType> visiblePanes) {
        putBoolean(WEB_SEARCH_VISIBLE, visiblePanes.contains(SidePaneType.WEB_SEARCH));
        putBoolean(GROUP_SIDEPANE_VISIBLE, visiblePanes.contains(SidePaneType.GROUPS));
        putBoolean(OO_SHOW_PANEL, visiblePanes.contains(SidePaneType.OPEN_OFFICE));
    }

    private Map<SidePaneType, Integer> getSidePanePreferredPositions() {
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

    private void storeSidePanePreferredPositions(Map<SidePaneType, Integer> preferredPositions) {
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

    //*************************************************************************************************************
    // Cleanup preferences
    //*************************************************************************************************************

    @Override
    public CleanupPreferences getCleanupPreferences() {
        if (Objects.nonNull(cleanupPreferences)) {
            return cleanupPreferences;
        }

        cleanupPreferences = new CleanupPreferences(
                EnumSet.copyOf(getStringList(CLEANUP_JOBS).stream()
                                                          .map(CleanupPreferences.CleanupStep::valueOf)
                                                          .collect(Collectors.toSet())),
                new FieldFormatterCleanups(getBoolean(CLEANUP_FIELD_FORMATTERS_ENABLED),
                        FieldFormatterCleanups.parse(StringUtil.unifyLineBreaks(get(CLEANUP_FIELD_FORMATTERS), ""))));

        cleanupPreferences.getObservableActiveJobs().addListener((SetChangeListener<CleanupPreferences.CleanupStep>) c ->
                putStringList(CLEANUP_JOBS, cleanupPreferences.getActiveJobs().stream().map(Enum::name).collect(Collectors.toList())));

        EasyBind.listen(cleanupPreferences.fieldFormatterCleanupsProperty(), (fieldFormatters, oldValue, newValue) -> {
            putBoolean(CLEANUP_FIELD_FORMATTERS_ENABLED, newValue.isEnabled());
            put(CLEANUP_FIELD_FORMATTERS, FieldFormatterCleanups.getMetaDataString(newValue.getConfiguredActions(), OS.NEWLINE));
        });

        return cleanupPreferences;
    }

    @Override
    public CleanupPreferences getDefaultCleanupPreset() {
        return new CleanupPreferences(
                getDefaultCleanupJobs(),
                new FieldFormatterCleanups(
                        (Boolean) defaults.get(CLEANUP_FIELD_FORMATTERS_ENABLED),
                        FieldFormatterCleanups.parse((String) defaults.get(CLEANUP_FIELD_FORMATTERS))));
    }

    private static EnumSet<CleanupPreferences.CleanupStep> getDefaultCleanupJobs() {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs = EnumSet.allOf(CleanupPreferences.CleanupStep.class);
        activeJobs.removeAll(EnumSet.of(
                CleanupPreferences.CleanupStep.CLEAN_UP_UPGRADE_EXTERNAL_LINKS,
                CleanupPreferences.CleanupStep.MOVE_PDF,
                CleanupPreferences.CleanupStep.RENAME_PDF_ONLY_RELATIVE_PATHS,
                CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX,
                CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX));
        return activeJobs;
    }

    //*************************************************************************************************************
    // GUI preferences
    //*************************************************************************************************************

    @Override
    public GuiPreferences getGuiPreferences() {
        if (Objects.nonNull(guiPreferences)) {
            return guiPreferences;
        }

        guiPreferences = new GuiPreferences(
                getDouble(POS_X),
                getDouble(POS_Y),
                getDouble(SIZE_X),
                getDouble(SIZE_Y),
                getBoolean(WINDOW_MAXIMISED),
                getStringList(LAST_EDITED),
                Path.of(get(LAST_FOCUSED)),
                getFileHistory(),
                get(ID_ENTRY_GENERATOR),
                DiffMode.parse(get(MERGE_ENTRIES_DIFF_MODE)),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF),
                getBoolean(MERGE_ENTRIES_HIGHLIGHT_WORDS),
                getDouble(SIDE_PANE_WIDTH),
                getBoolean(MERGE_SHOW_ONLY_CHANGED_FIELDS));

        EasyBind.listen(guiPreferences.positionXProperty(), (obs, oldValue, newValue) -> putDouble(POS_X, newValue.doubleValue()));
        EasyBind.listen(guiPreferences.positionYProperty(), (obs, oldValue, newValue) -> putDouble(POS_Y, newValue.doubleValue()));
        EasyBind.listen(guiPreferences.sizeXProperty(), (obs, oldValue, newValue) -> putDouble(SIZE_X, newValue.doubleValue()));
        EasyBind.listen(guiPreferences.sizeYProperty(), (obs, oldValue, newValue) -> putDouble(SIZE_Y, newValue.doubleValue()));
        EasyBind.listen(guiPreferences.windowMaximisedProperty(), (obs, oldValue, newValue) -> putBoolean(WINDOW_MAXIMISED, newValue));
        guiPreferences.getLastFilesOpened().addListener((ListChangeListener<String>) change -> {
            if (change.getList().isEmpty()) {
                prefs.remove(LAST_EDITED);
            } else {
                putStringList(LAST_EDITED, guiPreferences.getLastFilesOpened());
            }
        });
        EasyBind.listen(guiPreferences.lastFocusedFileProperty(), (obs, oldValue, newValue) -> {
            if (newValue != null) {
                put(LAST_FOCUSED, newValue.toAbsolutePath().toString());
            } else {
                remove(LAST_FOCUSED);
            }
        });
        guiPreferences.getFileHistory().addListener((InvalidationListener) change -> storeFileHistory(guiPreferences.getFileHistory()));
        EasyBind.listen(guiPreferences.lastSelectedIdBasedFetcherProperty(), (obs, oldValue, newValue) -> put(ID_ENTRY_GENERATOR, newValue));
        EasyBind.listen(guiPreferences.mergeDiffModeProperty(), (obs, oldValue, newValue) -> put(MERGE_ENTRIES_DIFF_MODE, newValue.name()));
        EasyBind.listen(guiPreferences.mergeShouldShowDiffProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF, newValue));
        EasyBind.listen(guiPreferences.mergeShouldShowUnifiedDiffProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, newValue));
        EasyBind.listen(guiPreferences.mergeHighlightWordsProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_ENTRIES_HIGHLIGHT_WORDS, newValue));
        EasyBind.listen(guiPreferences.sidePaneWidthProperty(), (obs, oldValue, newValue) -> putDouble(SIDE_PANE_WIDTH, newValue.doubleValue()));
        EasyBind.listen(guiPreferences.mergeShowChangedFieldOnlyProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_SHOW_ONLY_CHANGED_FIELDS, newValue));

        return guiPreferences;
    }

    private FileHistory getFileHistory() {
        return FileHistory.of(getStringList(RECENT_DATABASES).stream()
                                                             .map(Path::of)
                                                             .toList());
    }

    private void storeFileHistory(FileHistory history) {
        putStringList(RECENT_DATABASES, history.stream()
                                               .map(Path::toAbsolutePath)
                                               .map(Path::toString)
                                               .toList());
    }

    //*************************************************************************************************************
    // Misc preferences
    //*************************************************************************************************************

    @Override
    public SearchPreferences getSearchPreferences() {
        if (Objects.nonNull(searchPreferences)) {
            return searchPreferences;
        }

        SearchDisplayMode searchDisplayMode;
        try {
            searchDisplayMode = SearchDisplayMode.valueOf(get(SEARCH_DISPLAY_MODE));
        } catch (IllegalArgumentException ex) {
            // Should only occur when the searchmode is set directly via preferences.put and the enum was not used
            searchDisplayMode = SearchDisplayMode.valueOf((String) defaults.get(SEARCH_DISPLAY_MODE));
        }

        searchPreferences = new SearchPreferences(
                searchDisplayMode,
                getBoolean(SEARCH_CASE_SENSITIVE),
                getBoolean(SEARCH_REG_EXP),
                getBoolean(SEARCH_FULLTEXT),
                getBoolean(SEARCH_KEEP_SEARCH_STRING),
                getBoolean(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP),
                getDouble(SEARCH_WINDOW_HEIGHT),
                getDouble(SEARCH_WINDOW_WIDTH));

        EasyBind.listen(searchPreferences.searchDisplayModeProperty(), (obs, oldValue, newValue) -> put(SEARCH_DISPLAY_MODE, Objects.requireNonNull(searchPreferences.getSearchDisplayMode()).toString()));
        searchPreferences.getObservableSearchFlags().addListener((SetChangeListener<SearchRules.SearchFlags>) c -> {
            putBoolean(SEARCH_CASE_SENSITIVE, searchPreferences.getObservableSearchFlags().contains(SearchRules.SearchFlags.CASE_SENSITIVE));
            putBoolean(SEARCH_REG_EXP, searchPreferences.getObservableSearchFlags().contains(SearchRules.SearchFlags.REGULAR_EXPRESSION));
            putBoolean(SEARCH_FULLTEXT, searchPreferences.getObservableSearchFlags().contains(SearchRules.SearchFlags.FULLTEXT));
            putBoolean(SEARCH_KEEP_SEARCH_STRING, searchPreferences.getObservableSearchFlags().contains(SearchRules.SearchFlags.KEEP_SEARCH_STRING));
        });

        EasyBind.listen(searchPreferences.keepWindowOnTopProperty(), (obs, oldValue, newValue) -> putBoolean(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, searchPreferences.shouldKeepWindowOnTop()));
        EasyBind.listen(searchPreferences.getSearchWindowHeightProperty(), (obs, oldValue, newValue) -> putDouble(SEARCH_WINDOW_HEIGHT, searchPreferences.getSearchWindowHeight()));
        EasyBind.listen(searchPreferences.getSearchWindowWidthProperty(), (obs, oldValue, newValue) -> putDouble(SEARCH_WINDOW_WIDTH, searchPreferences.getSearchWindowWidth()));

        return searchPreferences;
    }

    @Override
    public XmpPreferences getXmpPreferences() {
        if (Objects.nonNull(xmpPreferences)) {
            return xmpPreferences;
        }

        xmpPreferences = new XmpPreferences(
                getBoolean(USE_XMP_PRIVACY_FILTER),
                getStringList(XMP_PRIVACY_FILTERS).stream().map(FieldFactory::parseField).collect(Collectors.toSet()),
                getBibEntryPreferences().keywordSeparatorProperty());

        EasyBind.listen(xmpPreferences.useXmpPrivacyFilterProperty(),
                (obs, oldValue, newValue) -> putBoolean(USE_XMP_PRIVACY_FILTER, newValue));
        xmpPreferences.getXmpPrivacyFilter().addListener((SetChangeListener<Field>) c ->
                putStringList(XMP_PRIVACY_FILTERS, xmpPreferences.getXmpPrivacyFilter().stream()
                                                                 .map(Field::getName)
                                                                 .collect(Collectors.toList())));

        return xmpPreferences;
    }

    @Override
    public NameFormatterPreferences getNameFormatterPreferences() {
        if (Objects.nonNull(nameFormatterPreferences)) {
            return nameFormatterPreferences;
        }

        nameFormatterPreferences = new NameFormatterPreferences(
                getStringList(NAME_FORMATER_KEY),
                getStringList(NAME_FORMATTER_VALUE));

        nameFormatterPreferences.getNameFormatterKey().addListener((InvalidationListener) change ->
                putStringList(NAME_FORMATER_KEY, nameFormatterPreferences.getNameFormatterKey()));
        nameFormatterPreferences.getNameFormatterValue().addListener((InvalidationListener) change ->
                putStringList(NAME_FORMATTER_VALUE, nameFormatterPreferences.getNameFormatterValue()));

        return nameFormatterPreferences;
    }

    @Override
    public AutoCompletePreferences getAutoCompletePreferences() {
        if (Objects.nonNull(autoCompletePreferences)) {
            return autoCompletePreferences;
        }

        AutoCompletePreferences.NameFormat nameFormat = AutoCompletePreferences.NameFormat.BOTH;
        if (getBoolean(AUTOCOMPLETER_LAST_FIRST)) {
            nameFormat = AutoCompletePreferences.NameFormat.LAST_FIRST;
        } else if (getBoolean(AUTOCOMPLETER_FIRST_LAST)) {
            nameFormat = AutoCompletePreferences.NameFormat.FIRST_LAST;
        }

        autoCompletePreferences = new AutoCompletePreferences(
                getBoolean(AUTO_COMPLETE),
                AutoCompleteFirstNameMode.parse(get(AUTOCOMPLETER_FIRSTNAME_MODE)),
                nameFormat,
                getStringList(AUTOCOMPLETER_COMPLETE_FIELDS).stream().map(FieldFactory::parseField).collect(Collectors.toSet())
        );

        EasyBind.listen(autoCompletePreferences.autoCompleteProperty(), (obs, oldValue, newValue) -> putBoolean(AUTO_COMPLETE, newValue));
        EasyBind.listen(autoCompletePreferences.firstNameModeProperty(), (obs, oldValue, newValue) -> put(AUTOCOMPLETER_FIRSTNAME_MODE, newValue.name()));
        autoCompletePreferences.getCompleteFields().addListener((SetChangeListener<Field>) c ->
                putStringList(AUTOCOMPLETER_COMPLETE_FIELDS, autoCompletePreferences.getCompleteFields().stream()
                                                                                    .map(Field::getName)
                                                                                    .collect(Collectors.toList())));
        EasyBind.listen(autoCompletePreferences.nameFormatProperty(), (obs, oldValue, newValue) -> {
            if (autoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.BOTH) {
                putBoolean(AUTOCOMPLETER_LAST_FIRST, false);
                putBoolean(AUTOCOMPLETER_FIRST_LAST, false);
            } else if (autoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.LAST_FIRST) {
                putBoolean(AUTOCOMPLETER_LAST_FIRST, true);
                putBoolean(AUTOCOMPLETER_FIRST_LAST, false);
            } else {
                putBoolean(AUTOCOMPLETER_LAST_FIRST, false);
                putBoolean(AUTOCOMPLETER_FIRST_LAST, true);
            }
        });

        return autoCompletePreferences;
    }

    @Override
    public SpecialFieldsPreferences getSpecialFieldsPreferences() {
        if (Objects.nonNull(specialFieldsPreferences)) {
            return specialFieldsPreferences;
        }

        specialFieldsPreferences = new SpecialFieldsPreferences(getBoolean(SPECIALFIELDSENABLED));

        EasyBind.listen(specialFieldsPreferences.specialFieldsEnabledProperty(), (obs, oldValue, newValue) -> putBoolean(SPECIALFIELDSENABLED, newValue));

        return specialFieldsPreferences;
    }

    @Override
    public MrDlibPreferences getMrDlibPreferences() {
        if (Objects.nonNull(mrDlibPreferences)) {
            return mrDlibPreferences;
        }

        mrDlibPreferences = new MrDlibPreferences(
                getBoolean(ACCEPT_RECOMMENDATIONS),
                getBoolean(SEND_LANGUAGE_DATA),
                getBoolean(SEND_OS_DATA),
                getBoolean(SEND_TIMEZONE_DATA));

        EasyBind.listen(mrDlibPreferences.acceptRecommendationsProperty(), (obs, oldValue, newValue) -> putBoolean(ACCEPT_RECOMMENDATIONS, newValue));
        EasyBind.listen(mrDlibPreferences.sendLanguageProperty(), (obs, oldValue, newValue) -> putBoolean(SEND_LANGUAGE_DATA, newValue));
        EasyBind.listen(mrDlibPreferences.sendOsProperty(), (obs, oldValue, newValue) -> putBoolean(SEND_OS_DATA, newValue));
        EasyBind.listen(mrDlibPreferences.sendTimezoneProperty(), (obs, oldValue, newValue) -> putBoolean(SEND_TIMEZONE_DATA, newValue));

        return mrDlibPreferences;
    }

    @Override
    public ProtectedTermsPreferences getProtectedTermsPreferences() {
        if (Objects.nonNull(protectedTermsPreferences)) {
            return protectedTermsPreferences;
        }

        protectedTermsPreferences = new ProtectedTermsPreferences(
                getStringList(PROTECTED_TERMS_ENABLED_INTERNAL),
                getStringList(PROTECTED_TERMS_ENABLED_EXTERNAL),
                getStringList(PROTECTED_TERMS_DISABLED_INTERNAL),
                getStringList(PROTECTED_TERMS_DISABLED_EXTERNAL)
        );

        protectedTermsPreferences.getEnabledExternalTermLists().addListener((InvalidationListener) change ->
                putStringList(PROTECTED_TERMS_ENABLED_EXTERNAL, protectedTermsPreferences.getEnabledExternalTermLists()));
        protectedTermsPreferences.getDisabledExternalTermLists().addListener((InvalidationListener) change ->
                putStringList(PROTECTED_TERMS_DISABLED_EXTERNAL, protectedTermsPreferences.getDisabledExternalTermLists()));
        protectedTermsPreferences.getEnabledInternalTermLists().addListener((InvalidationListener) change ->
                putStringList(PROTECTED_TERMS_ENABLED_INTERNAL, protectedTermsPreferences.getEnabledInternalTermLists()));
        protectedTermsPreferences.getDisabledInternalTermLists().addListener((InvalidationListener) change ->
                putStringList(PROTECTED_TERMS_DISABLED_INTERNAL, protectedTermsPreferences.getDisabledInternalTermLists()));

        return protectedTermsPreferences;
    }

    //*************************************************************************************************************
    // Importer preferences
    //*************************************************************************************************************

    @Override
    public ImporterPreferences getImporterPreferences() {
        if (Objects.nonNull(importerPreferences)) {
            return importerPreferences;
        }

        importerPreferences = new ImporterPreferences(
                getBoolean(GENERATE_KEY_ON_IMPORT),
                Path.of(get(IMPORT_WORKING_DIRECTORY)),
                getBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION),
                getCustomImportFormats(),
                getFetcherKeys()
        );

        EasyBind.listen(importerPreferences.generateNewKeyOnImportProperty(), (obs, oldValue, newValue) -> putBoolean(GENERATE_KEY_ON_IMPORT, newValue));
        EasyBind.listen(importerPreferences.importWorkingDirectoryProperty(), (obs, oldValue, newValue) -> put(IMPORT_WORKING_DIRECTORY, newValue.toString()));
        EasyBind.listen(importerPreferences.warnAboutDuplicatesOnImportProperty(), (obs, oldValue, newValue) -> putBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION, newValue));
        importerPreferences.getApiKeys().addListener((InvalidationListener) c -> storeFetcherKeys(importerPreferences.getApiKeys()));
        importerPreferences.getCustomImporters().addListener((InvalidationListener) c -> storeCustomImportFormats(importerPreferences.getCustomImporters()));

        return importerPreferences;
    }

    private Set<CustomImporter> getCustomImportFormats() {
        Set<CustomImporter> importers = new TreeSet<>();

        for (String toImport : getSeries(CUSTOM_IMPORT_FORMAT)) {
            List<String> importerString = convertStringToList(toImport);
            try {
                if (importerString.size() == 2) {
                    // New format: basePath, className
                    importers.add(new CustomImporter(importerString.get(0), importerString.get(1)));
                } else {
                    // Old format: name, cliId, className, basePath
                    importers.add(new CustomImporter(importerString.get(3), importerString.get(2)));
                }
            } catch (Exception e) {
                LOGGER.warn("Could not load {} from preferences. Will ignore.", importerString.get(0), e);
            }
        }

        return importers;
    }

    private void storeCustomImportFormats(Set<CustomImporter> importers) {
        purgeSeries(CUSTOM_IMPORT_FORMAT, 0);
        CustomImporter[] importersArray = importers.toArray(new CustomImporter[0]);
        for (int i = 0; i < importersArray.length; i++) {
            putStringList(CUSTOM_IMPORT_FORMAT + i, importersArray[i].getAsStringList());
        }
    }

    private Set<FetcherApiKey> getFetcherKeys() {
        Set<FetcherApiKey> fetcherApiKeys = new HashSet<>();

        List<String> names = getStringList(FETCHER_CUSTOM_KEY_NAMES);
        List<String> uses = getStringList(FETCHER_CUSTOM_KEY_USES);
        List<String> keys = getFetcherKeysFromKeyring(names);

        for (int i = 0; i < names.size(); i++) {
            fetcherApiKeys.add(new FetcherApiKey(
                    names.get(i),
                    // i < uses.size() ? Boolean.parseBoolean(uses.get(i)) : false
                    (i < uses.size()) && Boolean.parseBoolean(uses.get(i)),
                    i < keys.size() ? keys.get(i) : ""));
        }

        return fetcherApiKeys;
    }

    private List<String> getFetcherKeysFromKeyring(List<String> names) {
        List<String> keys = new ArrayList<>();

        try (final Keyring keyring = Keyring.create()) {
            for (String fetcher : names) {
                try {
                    keys.add(new Password(
                            keyring.getPassword("org.jabref.customapikeys", fetcher),
                            getInternalPreferences().getUserAndHost())
                            .decrypt());
                } catch (PasswordAccessException ex) {
                    LOGGER.warn("No api key stored for {} fetcher", fetcher);
                    keys.add("");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("JabRef could not open the key store");
        }

        return keys;
    }

    private void storeFetcherKeys(Set<FetcherApiKey> fetcherApiKeys) {
        List<String> names = new ArrayList<>();
        List<String> uses = new ArrayList<>();
        List<String> keys = new ArrayList<>();

        for (FetcherApiKey apiKey : fetcherApiKeys) {
            names.add(apiKey.getName());
            uses.add(String.valueOf(apiKey.shouldUse()));
            keys.add(apiKey.getKey());
        }

        putStringList(FETCHER_CUSTOM_KEY_NAMES, names);
        putStringList(FETCHER_CUSTOM_KEY_USES, uses);
        storeFetcherKeysToKeyring(names, keys);
    }

    private void storeFetcherKeysToKeyring(List<String> names, List<String> keys) {
        try (final Keyring keyring = Keyring.create()) {
            for (int i = 0; i < names.size(); i++) {
                if (StringUtil.isNullOrEmpty(keys.get(i))) {
                    try {
                        keyring.deletePassword("org.jabref.customapikeys", names.get(i));
                    } catch (PasswordAccessException ex) {
                        // Already removed
                    }
                } else {
                    keyring.setPassword("org.jabref.customapikeys", names.get(i), new Password(
                            keys.get(i),
                            getInternalPreferences().getUserAndHost())
                            .encrypt());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to open key store");
        }
    }

    private void clearCustomFetcherKeys() {
        List<String> names = getStringList(FETCHER_CUSTOM_KEY_NAMES);
        try (final Keyring keyring = Keyring.create()) {
            for (String name : names) {
                keyring.deletePassword("org.jabref.customapikeys", name);
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to open key store");
        }
    }

    @Override
    public GrobidPreferences getGrobidPreferences() {
        if (Objects.nonNull(grobidPreferences)) {
            return grobidPreferences;
        }

        grobidPreferences = new GrobidPreferences(
                getBoolean(GROBID_ENABLED),
                getBoolean(GROBID_OPT_OUT),
                get(GROBID_URL));

        EasyBind.listen(grobidPreferences.grobidEnabledProperty(), (obs, oldValue, newValue) -> putBoolean(GROBID_ENABLED, newValue));
        EasyBind.listen(grobidPreferences.grobidOptOutProperty(), (obs, oldValue, newValue) -> putBoolean(GROBID_OPT_OUT, newValue));
        EasyBind.listen(grobidPreferences.grobidURLProperty(), (obs, oldValue, newValue) -> put(GROBID_URL, newValue));

        return grobidPreferences;
    }

@Override
    public ImportFormatPreferences getImportFormatPreferences() {
        return new ImportFormatPreferences(
                getBibEntryPreferences(),
                getCitationKeyPatternPreferences(),
                getFieldPreferences(),
                getXmpPreferences(),
                getDOIPreferences(),
                getGrobidPreferences());
    }
}
