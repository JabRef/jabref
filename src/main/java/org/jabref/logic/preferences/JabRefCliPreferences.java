package org.jabref.logic.preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;

import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.SemanticScholarFetcher;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.InternalPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiDefaultPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.ACMPortalFetcher;
import org.jabref.logic.importer.fetcher.AstrophysicsDataSystem;
import org.jabref.logic.importer.fetcher.BiodiversityLibrary;
import org.jabref.logic.importer.fetcher.DBLPFetcher;
import org.jabref.logic.importer.fetcher.IEEE;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.importer.fetcher.ScienceDirect;
import org.jabref.logic.importer.fetcher.SpringerFetcher;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.logic.os.OS;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.Version;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.ai.AiProvider;
import org.jabref.model.ai.EmbeddingModel;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.strings.StringUtil;

import com.airhacks.afterburner.injection.Injector;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code JabRefPreferences} class provides the preferences and their defaults using the JDK {@code java.util.prefs}
 * class.
 * <p>
 * Internally it defines symbols used to pick a value from the {@code java.util.prefs} interface and keeps a hashmap
 * with all the default values.
 * <p>
 * There are still some similar preferences classes ({@link OpenOfficePreferences} and {@link SharedDatabasePreferences}) which also use
 * the {@code java.util.prefs} API.
 * <p>
 * contents of the defaults HashMap that are defined in this class.
 * There are more default parameters in this map which belong to separate preference classes.
 */
@Singleton
@Service
public class JabRefCliPreferences implements CliPreferences {

    public static final String LANGUAGE = "language";

    public static final String BIBLATEX_DEFAULT_MODE = "biblatexMode";

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

    public static final String XMP_PRIVACY_FILTERS = "xmpPrivacyFilters";
    public static final String USE_XMP_PRIVACY_FILTER = "useXmpPrivacyFilter";
    public static final String DEFAULT_SHOW_SOURCE = "defaultShowSource";

    public static final String AUTO_OPEN_FORM = "autoOpenForm";
    public static final String IMPORT_WORKING_DIRECTORY = "importWorkingDirectory";
    public static final String LAST_USED_EXPORT = "lastUsedExport";
    public static final String EXPORT_WORKING_DIRECTORY = "exportWorkingDirectory";
    public static final String WORKING_DIRECTORY = "workingDirectory";
    public static final String BACKUP_DIRECTORY = "backupDirectory";
    public static final String CREATE_BACKUP = "createBackup";

    public static final String KEYWORD_SEPARATOR = "groupKeywordSeparator";

    public static final String MEMORY_STICK_MODE = "memoryStickMode";
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

    public static final String NON_WRAPPABLE_FIELDS = "nonWrappableFields";
    public static final String RESOLVE_STRINGS_FOR_FIELDS = "resolveStringsForFields";
    public static final String DO_NOT_RESOLVE_STRINGS = "doNotResolveStrings";

    // merge related
    public static final String MERGE_ENTRIES_DIFF_MODE = "mergeEntriesDiffMode";
    public static final String MERGE_ENTRIES_SHOULD_SHOW_DIFF = "mergeEntriesShouldShowDiff";
    public static final String MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF = "mergeEntriesShouldShowUnifiedDiff";
    public static final String MERGE_ENTRIES_HIGHLIGHT_WORDS = "mergeEntriesHighlightWords";

    public static final String MERGE_SHOW_ONLY_CHANGED_FIELDS = "mergeShowOnlyChangedFields";

    public static final String SHOW_USER_COMMENTS_FIELDS = "showUserCommentsFields";

    public static final String MERGE_APPLY_TO_ALL_ENTRIES = "mergeApplyToAllEntries";

    public static final String DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES = "duplicateResolverDecisionResult";

    public static final String CUSTOM_EXPORT_FORMAT = "customExportFormat";
    public static final String CUSTOM_IMPORT_FORMAT = "customImportFormat";
    public static final String KEY_PATTERN_REGEX = "KeyPatternRegex";
    public static final String KEY_PATTERN_REPLACEMENT = "KeyPatternReplacement";
    public static final String MAIN_FILE_DIRECTORY = "fileDirectory";

    public static final String SEARCH_DISPLAY_MODE = "searchDisplayMode";
    public static final String SEARCH_CASE_SENSITIVE = "caseSensitiveSearch";
    public static final String SEARCH_REG_EXP = "regExpSearch";
    public static final String SEARCH_FULLTEXT = "fulltextSearch";
    public static final String SEARCH_KEEP_SEARCH_STRING = "keepSearchString";
    public static final String SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP = "keepOnTop";
    public static final String SEARCH_WINDOW_HEIGHT = "searchWindowHeight";
    public static final String SEARCH_WINDOW_WIDTH = "searchWindowWidth";
    public static final String SEARCH_WINDOW_DIVIDER_POS = "searchWindowDividerPos";
    public static final String SEARCH_CATALOGS = "searchCatalogs";
    public static final String DEFAULT_PLAIN_CITATION_PARSER = "defaultPlainCitationParser";
    public static final String IMPORTERS_ENABLED = "importersEnabled";
    public static final String GENERATE_KEY_ON_IMPORT = "generateKeyOnImport";
    public static final String GROBID_ENABLED = "grobidEnabled";
    public static final String GROBID_OPT_OUT = "grobidOptOut";
    public static final String GROBID_URL = "grobidURL";

    public static final String DEFAULT_CITATION_KEY_PATTERN = "defaultBibtexKeyPattern";
    public static final String UNWANTED_CITATION_KEY_CHARACTERS = "defaultUnwantedBibtexKeyCharacters";
    public static final String CONFIRM_LINKED_FILE_DELETE = "confirmLinkedFileDelete";
    public static final String TRASH_INSTEAD_OF_DELETE = "trashInsteadOfDelete";
    public static final String WARN_BEFORE_OVERWRITING_KEY = "warnBeforeOverwritingKey";
    public static final String AVOID_OVERWRITING_KEY = "avoidOverwritingKey";
    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";
    public static final String AUTOLINK_FILES_ENABLED = "autoLinkFilesEnabled";

    public static final String GENERATE_KEYS_BEFORE_SAVING = "generateKeysBeforeSaving";
    public static final String KEY_GEN_ALWAYS_ADD_LETTER = "keyGenAlwaysAddLetter";
    public static final String KEY_GEN_FIRST_LETTER_A = "keyGenFirstLetterA";
    public static final String ALLOW_INTEGER_EDITION_BIBTEX = "allowIntegerEditionBibtex";
    public static final String LOCAL_AUTO_SAVE = "localAutoSave";
    public static final String AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String AUTOLINK_USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    // bibLocAsPrimaryDir is a misleading antique variable name, we keep it for reason of compatibility

    public static final String STORE_RELATIVE_TO_BIB = "bibLocAsPrimaryDir";
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
    public static final String SHOW_RECOMMENDATIONS = "showRecommendations";
    public static final String SHOW_AI_SUMMARY = "showAiSummary";
    public static final String SHOW_AI_CHAT = "showAiChat";
    public static final String ACCEPT_RECOMMENDATIONS = "acceptRecommendations";
    public static final String SHOW_LATEX_CITATIONS = "showLatexCitations";
    public static final String SEND_LANGUAGE_DATA = "sendLanguageData";
    public static final String SEND_OS_DATA = "sendOSData";
    public static final String SEND_TIMEZONE_DATA = "sendTimezoneData";
    public static final String VALIDATE_IN_ENTRY_EDITOR = "validateInEntryEditor";
    public static final String SHOW_SCITE_TAB = "showSciteTab";

    /**
     * The OpenOffice/LibreOffice connection preferences are: OO_PATH main directory for OO/LO installation, used to detect location on Win/macOS when using manual connect OO_EXECUTABLE_PATH path to soffice-file OO_JARS_PATH directory that contains juh.jar, jurt.jar, ridl.jar, unoil.jar OO_SYNC_WHEN_CITING true if the reference list is updated when adding a new citation OO_SHOW_PANEL true if the OO panel is shown on startup OO_USE_ALL_OPEN_DATABASES true if all databases should be used when citing OO_BIBLIOGRAPHY_STYLE_FILE path to the used style file OO_EXTERNAL_STYLE_FILES list with paths to external style files STYLES_*_* size and position of "Select style" dialog
     */
    public static final String OO_EXECUTABLE_PATH = "ooExecutablePath";
    public static final String OO_SYNC_WHEN_CITING = "syncOOWhenCiting";
    public static final String OO_USE_ALL_OPEN_BASES = "useAllOpenBases";
    public static final String OO_BIBLIOGRAPHY_STYLE_FILE = "ooBibliographyStyleFile";
    public static final String OO_EXTERNAL_STYLE_FILES = "ooExternalStyleFiles";
    public static final String OO_CURRENT_STYLE = "ooCurrentStyle";
    public static final String OO_ALWAYS_ADD_CITED_ON_PAGES = "ooAlwaysAddCitedOnPages";

    // Prefs node for CitationKeyPatterns
    public static final String CITATION_KEY_PATTERNS_NODE = "bibtexkeypatterns";
    // Prefs node for customized entry types
    public static final String CUSTOMIZED_BIBTEX_TYPES = "customizedBibtexTypes";
    public static final String CUSTOMIZED_BIBLATEX_TYPES = "customizedBiblatexTypes";
    // Version
    public static final String VERSION_IGNORED_UPDATE = "versionIgnoreUpdate";
    public static final String VERSION_CHECK_ENABLED = "versionCheck";

    // String delimiter
    public static final Character STRINGLIST_DELIMITER = ';';

    // TODO: USed by both importer preferences and workspace preferences
    protected static final String WARN_ABOUT_DUPLICATES_IN_INSPECTION = "warnAboutDuplicatesInInspection";

    // Helper string
    protected static final String USER_HOME = System.getProperty("user.home");

    // UI
    private static final String FONT_FAMILY = "fontFamily";

    // region last files opened
    private static final String LAST_EDITED = "lastEdited";
    private static final String LAST_FOCUSED = "lastFocused";
    private static final String RECENT_DATABASES = "recentDatabases";
    // endregion

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
    private static final String FETCHER_CUSTOM_KEY_PERSIST = "fetcherCustomKeyPersist";

    // SSL
    private static final String TRUSTSTORE_PATH = "truststorePath";

    // User
    private static final String USER_ID = "userId";

    // Journal
    private static final String EXTERNAL_JOURNAL_LISTS = "externalJournalLists";
    private static final String USE_AMS_FJOURNAL = "useAMSFJournal";

    // Protected terms
    private static final String PROTECTED_TERMS_ENABLED_EXTERNAL = "protectedTermsEnabledExternal";
    private static final String PROTECTED_TERMS_DISABLED_EXTERNAL = "protectedTermsDisabledExternal";
    private static final String PROTECTED_TERMS_ENABLED_INTERNAL = "protectedTermsEnabledInternal";
    private static final String PROTECTED_TERMS_DISABLED_INTERNAL = "protectedTermsDisabledInternal";

    // Dialog states
    private static final String PREFS_EXPORT_PATH = "prefsExportPath";
    private static final String DOWNLOAD_LINKED_FILES = "downloadLinkedFiles";
    private static final String FULLTEXT_INDEX_LINKED_FILES = "fulltextIndexLinkedFiles";
    private static final String KEEP_DOWNLOAD_URL = "keepDownloadUrl";

    // Indexes for Strings within stored custom export entries
    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    // Remote
    private static final String USE_REMOTE_SERVER = "useRemoteServer";
    private static final String REMOTE_SERVER_PORT = "remoteServerPort";

    private static final String AI_ENABLED = "aiEnabled";
    private static final String AI_AUTO_GENERATE_EMBEDDINGS = "aiAutoGenerateEmbeddings";
    private static final String AI_AUTO_GENERATE_SUMMARIES = "aiAutoGenerateSummaries";
    private static final String AI_PROVIDER = "aiProvider";
    private static final String AI_OPEN_AI_CHAT_MODEL = "aiOpenAiChatModel";
    private static final String AI_MISTRAL_AI_CHAT_MODEL = "aiMistralAiChatModel";
    private static final String AI_GEMINI_CHAT_MODEL = "aiGeminiChatModel";
    private static final String AI_HUGGING_FACE_CHAT_MODEL = "aiHuggingFaceChatModel";
    private static final String AI_CUSTOMIZE_SETTINGS = "aiCustomizeSettings";
    private static final String AI_EMBEDDING_MODEL = "aiEmbeddingModel";
    private static final String AI_OPEN_AI_API_BASE_URL = "aiOpenAiApiBaseUrl";
    private static final String AI_MISTRAL_AI_API_BASE_URL = "aiMistralAiApiBaseUrl";
    private static final String AI_GEMINI_API_BASE_URL = "aiGeminiApiBaseUrl";
    private static final String AI_HUGGING_FACE_API_BASE_URL = "aiHuggingFaceApiBaseUrl";
    private static final String AI_SYSTEM_MESSAGE = "aiSystemMessage";
    private static final String AI_TEMPERATURE = "aiTemperature";
    private static final String AI_CONTEXT_WINDOW_SIZE = "aiMessageWindowSize";
    private static final String AI_DOCUMENT_SPLITTER_CHUNK_SIZE = "aiDocumentSplitterChunkSize";
    private static final String AI_DOCUMENT_SPLITTER_OVERLAP_SIZE = "aiDocumentSplitterOverlapSize";
    private static final String AI_RAG_MAX_RESULTS_COUNT = "aiRagMaxResultsCount";
    private static final String AI_RAG_MIN_SCORE = "aiRagMinScore";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefCliPreferences.class);
    private static final Preferences PREFS_NODE = Preferences.userRoot().node("/org/jabref");

    // The only instance of this class:
    private static JabRefCliPreferences singleton;
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
    private DOIPreferences doiPreferences;
    private OwnerPreferences ownerPreferences;
    private TimestampPreferences timestampPreferences;
    private OpenOfficePreferences openOfficePreferences;
    private ImporterPreferences importerPreferences;
    private GrobidPreferences grobidPreferences;
    private ProtectedTermsPreferences protectedTermsPreferences;
    private MrDlibPreferences mrDlibPreferences;
    private FilePreferences filePreferences;
    private RemotePreferences remotePreferences;
    private ProxyPreferences proxyPreferences;
    private SSLPreferences sslPreferences;
    private SearchPreferences searchPreferences;
    private AutoLinkPreferences autoLinkPreferences;
    private ExportPreferences exportPreferences;
    private NameFormatterPreferences nameFormatterPreferences;
    private BibEntryPreferences bibEntryPreferences;
    private InternalPreferences internalPreferences;
    private XmpPreferences xmpPreferences;
    private CleanupPreferences cleanupPreferences;
    private CitationKeyPatternPreferences citationKeyPatternPreferences;
    private JournalAbbreviationPreferences journalAbbreviationPreferences;
    private FieldPreferences fieldPreferences;
    private AiPreferences aiPreferences;
    private LastFilesOpenedPreferences lastFilesOpenedPreferences;

    /**
     * @implNote The constructor is made protected to enforce this as a singleton class:
     */
    protected JabRefCliPreferences() {
        try {
            if (new File("jabref.xml").exists()) {
                importPreferences(Path.of("jabref.xml"));
            }
        } catch (JabRefException e) {
            LOGGER.warn("Could not import preferences from jabref.xml", e);
        }

        // load user preferences
        prefs = PREFS_NODE;

        // Since some of the preference settings themselves use localized strings, we cannot set the language after
        // the initialization of the preferences in main
        // Otherwise that language framework will be instantiated and more importantly, statically initialized preferences
        // will never be translated.
        Localization.setLanguage(getLanguage());

        defaults.put(SEARCH_DISPLAY_MODE, Boolean.TRUE);
        defaults.put(SEARCH_CASE_SENSITIVE, Boolean.FALSE);
        defaults.put(SEARCH_REG_EXP, Boolean.FALSE);
        defaults.put(SEARCH_FULLTEXT, Boolean.FALSE);
        defaults.put(SEARCH_KEEP_SEARCH_STRING, Boolean.FALSE);
        defaults.put(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, Boolean.TRUE);
        defaults.put(SEARCH_WINDOW_HEIGHT, 176.0);
        defaults.put(SEARCH_WINDOW_WIDTH, 600.0);
        defaults.put(SEARCH_WINDOW_DIVIDER_POS, 0.5);
        defaults.put(SEARCH_CATALOGS, convertListToString(List.of(
                ACMPortalFetcher.FETCHER_NAME,
                SpringerFetcher.FETCHER_NAME,
                DBLPFetcher.FETCHER_NAME,
                IEEE.FETCHER_NAME)));
        defaults.put(DEFAULT_PLAIN_CITATION_PARSER, PlainCitationParserChoice.RULE_BASED.name());
        defaults.put(IMPORTERS_ENABLED, Boolean.TRUE);
        defaults.put(GENERATE_KEY_ON_IMPORT, Boolean.TRUE);

        // region: Grobid
        defaults.put(GROBID_ENABLED, Boolean.FALSE);
        defaults.put(GROBID_OPT_OUT, Boolean.FALSE);
        defaults.put(GROBID_URL, "http://grobid.jabref.org:8070");
        // endregion

        defaults.put(BIBLATEX_DEFAULT_MODE, Boolean.FALSE);

        defaults.put(USE_CUSTOM_DOI_URI, Boolean.FALSE);
        defaults.put(BASE_DOI_URI, "https://doi.org");

        if (OS.OS_X) {
            defaults.put(FONT_FAMILY, "SansSerif");
        } else {
            // Linux
            defaults.put(FONT_FAMILY, "SansSerif");
        }

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
        defaults.put(TRUSTSTORE_PATH, Directories
                                        .getSslDirectory()
                                        .resolve("truststore.jks").toString());

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
        defaults.put(EXPORT_TERTIARY_SORT_DESCENDING, Boolean.FALSE);

        defaults.put(NEWLINE, System.lineSeparator());

        defaults.put(XMP_PRIVACY_FILTERS, "pdf;timestamp;keywords;owner;note;review");
        defaults.put(USE_XMP_PRIVACY_FILTER, Boolean.FALSE);
        defaults.put(WORKING_DIRECTORY, USER_HOME);
        defaults.put(EXPORT_WORKING_DIRECTORY, USER_HOME);

        defaults.put(CREATE_BACKUP, Boolean.TRUE);

        // Remembers working directory of last import
        defaults.put(IMPORT_WORKING_DIRECTORY, USER_HOME);
        defaults.put(PREFS_EXPORT_PATH, USER_HOME);
        defaults.put(AUTO_OPEN_FORM, Boolean.TRUE);
        defaults.put(DEFAULT_SHOW_SOURCE, Boolean.FALSE);

        defaults.put(SHOW_USER_COMMENTS_FIELDS, Boolean.TRUE);

        defaults.put(SHOW_RECOMMENDATIONS, Boolean.TRUE);
        defaults.put(SHOW_AI_CHAT, Boolean.TRUE);
        defaults.put(SHOW_AI_SUMMARY, Boolean.TRUE);
        defaults.put(ACCEPT_RECOMMENDATIONS, Boolean.FALSE);
        defaults.put(SHOW_LATEX_CITATIONS, Boolean.TRUE);
        defaults.put(SHOW_SCITE_TAB, Boolean.TRUE);
        defaults.put(SEND_LANGUAGE_DATA, Boolean.FALSE);
        defaults.put(SEND_OS_DATA, Boolean.FALSE);
        defaults.put(SEND_TIMEZONE_DATA, Boolean.FALSE);
        defaults.put(VALIDATE_IN_ENTRY_EDITOR, Boolean.TRUE);
        defaults.put(KEYWORD_SEPARATOR, ", ");
        defaults.put(DEFAULT_ENCODING, StandardCharsets.UTF_8.name());
        defaults.put(DEFAULT_OWNER, System.getProperty("user.name"));
        defaults.put(MEMORY_STICK_MODE, Boolean.FALSE);

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
        defaults.put(OO_ALWAYS_ADD_CITED_ON_PAGES, Boolean.FALSE);
        defaults.put(OO_USE_ALL_OPEN_BASES, Boolean.TRUE);
        defaults.put(OO_BIBLIOGRAPHY_STYLE_FILE, StyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH);
        defaults.put(OO_EXTERNAL_STYLE_FILES, "");
        defaults.put(OO_CURRENT_STYLE, CitationStyle.getDefault().getPath()); // Default CSL Style is IEEE

        defaults.put(FETCHER_CUSTOM_KEY_NAMES, "Springer;IEEEXplore;SAO/NASA ADS;ScienceDirect;Biodiversity Heritage");
        defaults.put(FETCHER_CUSTOM_KEY_USES, "FALSE;FALSE;FALSE;FALSE;FALSE");
        defaults.put(FETCHER_CUSTOM_KEY_PERSIST, Boolean.FALSE);

        defaults.put(USE_OWNER, Boolean.FALSE);
        defaults.put(OVERWRITE_OWNER, Boolean.FALSE);
        defaults.put(AVOID_OVERWRITING_KEY, Boolean.FALSE);
        defaults.put(WARN_BEFORE_OVERWRITING_KEY, Boolean.TRUE);
        defaults.put(CONFIRM_LINKED_FILE_DELETE, Boolean.TRUE);
        defaults.put(KEEP_DOWNLOAD_URL, Boolean.TRUE);
        defaults.put(DEFAULT_CITATION_KEY_PATTERN, "[auth][year]");
        defaults.put(UNWANTED_CITATION_KEY_CHARACTERS, "-`ʹ:!;?^");
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
        defaults.put(LAST_USED_EXPORT, "");

        defaults.put(STORE_RELATIVE_TO_BIB, Boolean.TRUE);

        defaults.put(AUTOLINK_EXACT_KEY_ONLY, Boolean.FALSE);
        defaults.put(AUTOLINK_FILES_ENABLED, Boolean.TRUE);
        defaults.put(LOCAL_AUTO_SAVE, Boolean.FALSE);
        defaults.put(ALLOW_INTEGER_EDITION_BIBTEX, Boolean.FALSE);
        // Curly brackets ({}) are the default delimiters, not quotes (") as these cause trouble when they appear within the field value:
        // Currently, JabRef does not escape them
        defaults.put(KEY_GEN_FIRST_LETTER_A, Boolean.TRUE);
        defaults.put(KEY_GEN_ALWAYS_ADD_LETTER, Boolean.FALSE);

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

        // version check defaults
        defaults.put(VERSION_IGNORED_UPDATE, "");
        defaults.put(VERSION_CHECK_ENABLED, Boolean.TRUE);

        setLanguageDependentDefaultValues();

        // region last files opened
        defaults.put(RECENT_DATABASES, "");
        defaults.put(LAST_FOCUSED, "");
        defaults.put(LAST_EDITED, "");
        // endregion

        // region:AI
        defaults.put(AI_ENABLED, AiDefaultPreferences.ENABLE_CHAT);
        defaults.put(AI_AUTO_GENERATE_EMBEDDINGS, AiDefaultPreferences.AUTO_GENERATE_EMBEDDINGS);
        defaults.put(AI_AUTO_GENERATE_SUMMARIES, AiDefaultPreferences.AUTO_GENERATE_SUMMARIES);
        defaults.put(AI_PROVIDER, AiDefaultPreferences.PROVIDER.name());
        defaults.put(AI_OPEN_AI_CHAT_MODEL, AiDefaultPreferences.CHAT_MODELS.get(AiProvider.OPEN_AI).getName());
        defaults.put(AI_MISTRAL_AI_CHAT_MODEL, AiDefaultPreferences.CHAT_MODELS.get(AiProvider.MISTRAL_AI).getName());
        defaults.put(AI_GEMINI_CHAT_MODEL, AiDefaultPreferences.CHAT_MODELS.get(AiProvider.GEMINI).getName());
        defaults.put(AI_HUGGING_FACE_CHAT_MODEL, AiDefaultPreferences.CHAT_MODELS.get(AiProvider.HUGGING_FACE).getName());
        defaults.put(AI_CUSTOMIZE_SETTINGS, AiDefaultPreferences.CUSTOMIZE_SETTINGS);
        defaults.put(AI_EMBEDDING_MODEL, AiDefaultPreferences.EMBEDDING_MODEL.name());
        defaults.put(AI_OPEN_AI_API_BASE_URL, AiProvider.OPEN_AI.getApiUrl());
        defaults.put(AI_MISTRAL_AI_API_BASE_URL, AiProvider.MISTRAL_AI.getApiUrl());
        defaults.put(AI_GEMINI_API_BASE_URL, AiProvider.GEMINI.getApiUrl());
        defaults.put(AI_HUGGING_FACE_API_BASE_URL, AiProvider.HUGGING_FACE.getApiUrl());
        defaults.put(AI_SYSTEM_MESSAGE, AiDefaultPreferences.SYSTEM_MESSAGE);
        defaults.put(AI_TEMPERATURE, AiDefaultPreferences.TEMPERATURE);
        defaults.put(AI_CONTEXT_WINDOW_SIZE, AiDefaultPreferences.getContextWindowSize(AiDefaultPreferences.PROVIDER, AiDefaultPreferences.CHAT_MODELS.get(AiDefaultPreferences.PROVIDER).getName()));
        defaults.put(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, AiDefaultPreferences.DOCUMENT_SPLITTER_CHUNK_SIZE);
        defaults.put(AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, AiDefaultPreferences.DOCUMENT_SPLITTER_OVERLAP);
        defaults.put(AI_RAG_MAX_RESULTS_COUNT, AiDefaultPreferences.RAG_MAX_RESULTS_COUNT);
        defaults.put(AI_RAG_MIN_SCORE, AiDefaultPreferences.RAG_MIN_SCORE);
        // endregion
    }

    public void setLanguageDependentDefaultValues() {
        // Entry editor tab 0:
        defaults.put(CUSTOM_TAB_NAME + "_def0", Localization.lang("General"));
        String fieldNames = FieldFactory.getDefaultGeneralFields().stream().map(Field::getName).collect(Collectors.joining(STRINGLIST_DELIMITER.toString()));
        defaults.put(CUSTOM_TAB_FIELDS + "_def0", fieldNames);

        // Entry editor tab 1:
        defaults.put(CUSTOM_TAB_FIELDS + "_def1", StandardField.ABSTRACT.getName());
        defaults.put(CUSTOM_TAB_NAME + "_def1", Localization.lang("Abstract"));
    }

    /**
     * @deprecated Never ever add a call to this method. There should be only one caller.
     *             All other usages should get the preferences passed (or injected).
     *             The JabRef team leaves the <code>@deprecated</code> annotation to have IntelliJ listing this method with a strike-through.
     */
    @Deprecated
    public static JabRefCliPreferences getInstance() {
        if (JabRefCliPreferences.singleton == null) {
            JabRefCliPreferences.singleton = new JabRefCliPreferences();
        }
        return JabRefCliPreferences.singleton;
    }

    //*************************************************************************************************************
    // Common serializer logic
    //*************************************************************************************************************

    @VisibleForTesting
    static String convertListToString(List<String> value) {
        return value.stream().map(val -> StringUtil.quote(val, STRINGLIST_DELIMITER.toString(), '\\')).collect(Collectors.joining(STRINGLIST_DELIMITER.toString()));
    }

    @VisibleForTesting
    static List<String> convertStringToList(String toConvert) {
        if (StringUtil.isBlank(toConvert)) {
            return Collections.emptyList();
        }

        return Splitter.on(STRINGLIST_DELIMITER).splitToList(toConvert);
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

    public String getEmptyIsDefault(String key) {
        String defaultValue = (String) defaults.get(key);
        String result = prefs.get(key, defaultValue);
        if ("".equals(result)) {
            return defaultValue;
        }
        return result;
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

    protected void remove(String key) {
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
                LOGGER.warn("Could not export preferences for memory stick mode: {}", e.getMessage(), e);
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
    protected List<String> getSeries(String key) {
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
    protected void purgeSeries(String prefix, int number) {
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
        } catch (BackingStoreException
                 | IOException ex) {
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
        } catch (InvalidPreferencesFormatException
                 | IOException ex) {
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
                getDOIPreferences(),
                getFilePreferences().mainFileDirectoryProperty());
    }

    @Override
    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        if (journalAbbreviationPreferences != null) {
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
        storeBibEntryTypes(entryTypesManager.getAllCustomizedTypes(BibDatabaseMode.BIBTEX), BibDatabaseMode.BIBTEX);
        storeBibEntryTypes(entryTypesManager.getAllCustomizedTypes(BibDatabaseMode.BIBLATEX), BibDatabaseMode.BIBLATEX);
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
        if (openOfficePreferences != null) {
            return openOfficePreferences;
        }

        String currentStylePath = get(OO_CURRENT_STYLE);

        OOStyle currentStyle = CitationStyle.getDefault(); // Defaults to IEEE CSL Style

        // Reassign currentStyle if it is not a CSL style
        if (CitationStyle.isCitationStyleFile(currentStylePath)) {
            currentStyle = CitationStyle.createCitationStyleFromFile(currentStylePath) // Assigns CSL Style
                         .orElse(CitationStyle.getDefault());
        } else {
            // For now, must be a JStyle. In future, make separate cases for JStyles (.jstyle) and BibTeX (.bst) styles
            try {
                currentStyle = new JStyle(currentStylePath, getLayoutFormatterPreferences(),
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
            } catch (IOException ex) {
                LOGGER.warn("Could not create JStyle", ex);
            }
        }

        openOfficePreferences = new OpenOfficePreferences(
                get(OO_EXECUTABLE_PATH),
                getBoolean(OO_USE_ALL_OPEN_BASES),
                getBoolean(OO_SYNC_WHEN_CITING),
                getStringList(OO_EXTERNAL_STYLE_FILES),
                get(OO_BIBLIOGRAPHY_STYLE_FILE),
                currentStyle,
                getBoolean(OO_ALWAYS_ADD_CITED_ON_PAGES));

        EasyBind.listen(openOfficePreferences.executablePathProperty(), (obs, oldValue, newValue) -> put(OO_EXECUTABLE_PATH, newValue));
        EasyBind.listen(openOfficePreferences.useAllDatabasesProperty(), (obs, oldValue, newValue) -> putBoolean(OO_USE_ALL_OPEN_BASES, newValue));
        EasyBind.listen(openOfficePreferences.alwaysAddCitedOnPagesProperty(), (obs, oldValue, newValue) -> putBoolean(OO_ALWAYS_ADD_CITED_ON_PAGES, newValue));
        EasyBind.listen(openOfficePreferences.syncWhenCitingProperty(), (obs, oldValue, newValue) -> putBoolean(OO_SYNC_WHEN_CITING, newValue));

        openOfficePreferences.getExternalStyles().addListener((InvalidationListener) change ->
                putStringList(OO_EXTERNAL_STYLE_FILES, openOfficePreferences.getExternalStyles()));
        EasyBind.listen(openOfficePreferences.currentJStyleProperty(), (obs, oldValue, newValue) -> put(OO_BIBLIOGRAPHY_STYLE_FILE, newValue));
        EasyBind.listen(openOfficePreferences.currentStyleProperty(), (obs, oldValue, newValue) -> put(OO_CURRENT_STYLE, newValue.getPath()));

        return openOfficePreferences;
    }

    @Override
    public LibraryPreferences getLibraryPreferences() {
        if (libraryPreferences != null) {
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
    public DOIPreferences getDOIPreferences() {
        if (doiPreferences != null) {
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
        if (ownerPreferences != null) {
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

            // this propagates down to filePreferences
            getInternalPreferences().getUserAndHostProperty().setValue(newValue);
        });
        EasyBind.listen(ownerPreferences.overwriteOwnerProperty(), (obs, oldValue, newValue) -> putBoolean(OVERWRITE_OWNER, newValue));

        return ownerPreferences;
    }

    @Override
    public TimestampPreferences getTimestampPreferences() {
        if (timestampPreferences != null) {
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

    //*************************************************************************************************************
    // Network preferences
    //*************************************************************************************************************

    @Override
    public RemotePreferences getRemotePreferences() {
        if (remotePreferences != null) {
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
        if (proxyPreferences != null) {
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
                LOGGER.warn("JabRef could not open the key store", ex);
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
        if (sslPreferences != null) {
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

    private GlobalCitationKeyPatterns getGlobalCitationKeyPattern() {
        GlobalCitationKeyPatterns citationKeyPattern = GlobalCitationKeyPatterns.fromPattern(get(DEFAULT_CITATION_KEY_PATTERN));
        Preferences preferences = PREFS_NODE.node(CITATION_KEY_PATTERNS_NODE);
        try {
            String[] keys = preferences.keys();
            for (String key : keys) {
                citationKeyPattern.addCitationKeyPattern(
                        EntryTypeFactory.parse(key),
                        preferences.get(key, null));
            }
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences.getKeyPattern", ex);
        }

        return citationKeyPattern;
    }

    // public for use in PreferenceMigrations
    public void storeGlobalCitationKeyPattern(GlobalCitationKeyPatterns pattern) {
        if ((pattern.getDefaultValue() == null)
                || pattern.getDefaultValue().equals(CitationKeyPattern.NULL_CITATION_KEY_PATTERN)) {
            put(DEFAULT_CITATION_KEY_PATTERN, "");
        } else {
            put(DEFAULT_CITATION_KEY_PATTERN, pattern.getDefaultValue().stringRepresentation());
        }

        // Store overridden definitions to Preferences.
        Preferences preferences = PREFS_NODE.node(CITATION_KEY_PATTERNS_NODE);
        try {
            preferences.clear(); // We remove all old entries.
        } catch (BackingStoreException ex) {
            LOGGER.info("BackingStoreException in JabRefPreferences::putKeyPattern", ex);
        }

        for (EntryType entryType : pattern.getAllKeys()) {
            if (!pattern.isDefaultValue(entryType)) {
                // first entry in the map is the full pattern
                preferences.put(entryType.getName(), pattern.getValue(entryType).stringRepresentation());
            }
        }
    }

    private void clearCitationKeyPatterns() throws BackingStoreException {
        Preferences preferences = PREFS_NODE.node(CITATION_KEY_PATTERNS_NODE);
        preferences.clear();
        getCitationKeyPatternPreferences().setKeyPatterns(getGlobalCitationKeyPattern());
    }

    @Override
    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        if (citationKeyPatternPreferences != null) {
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
        EasyBind.listen(citationKeyPatternPreferences.keyPatternsProperty(),
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
    // BibEntryPreferences
    //*************************************************************************************************************

    @Override
    public BibEntryPreferences getBibEntryPreferences() {
        if (bibEntryPreferences != null) {
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

    protected Path getDefaultPath() {
        return Path.of("/");
    }

    @Override
    public InternalPreferences getInternalPreferences() {
        if (internalPreferences != null) {
            return internalPreferences;
        }

        internalPreferences = new InternalPreferences(
                Version.parse(get(VERSION_IGNORED_UPDATE)),
                getBoolean(VERSION_CHECK_ENABLED),
                getPath(PREFS_EXPORT_PATH, getDefaultPath()),
                getUserAndHost(),
                getBoolean(MEMORY_STICK_MODE));

        EasyBind.listen(internalPreferences.ignoredVersionProperty(),
                (obs, oldValue, newValue) -> put(VERSION_IGNORED_UPDATE, newValue.toString()));
        EasyBind.listen(internalPreferences.versionCheckEnabledProperty(),
                (obs, oldValue, newValue) -> putBoolean(VERSION_CHECK_ENABLED, newValue));
        EasyBind.listen(internalPreferences.lastPreferencesExportPathProperty(),
                (obs, oldValue, newValue) -> put(PREFS_EXPORT_PATH, newValue.toString()));
        // user is a static value, should only be changed for debugging
        EasyBind.listen(internalPreferences.memoryStickModeProperty(), (obs, oldValue, newValue) -> {
            putBoolean(MEMORY_STICK_MODE, newValue);
            if (!newValue) {
                try {
                    Files.deleteIfExists(Path.of("jabref.xml"));
                } catch (IOException e) {
                    LOGGER.warn("Error accessing filesystem", e);
                }
            }
        });

        return internalPreferences;
    }

    private String getUserAndHost() {
        if (StringUtil.isNotBlank(userAndHost)) {
            return userAndHost;
        }
        userAndHost = get(DEFAULT_OWNER) + '-' + OS.getHostName();
        return userAndHost;
    }

    protected Language getLanguage() {
        return Stream.of(Language.values())
                     .filter(language -> language.getId().equalsIgnoreCase(get(LANGUAGE)))
                     .findFirst()
                     .orElse(Language.ENGLISH);
    }

    @Override
    public FieldPreferences getFieldPreferences() {
        if (fieldPreferences != null) {
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

    protected boolean moveToTrashSupported() {
        return false;
    }

    @Override
    public FilePreferences getFilePreferences() {
        if (filePreferences != null) {
            return filePreferences;
        }

        filePreferences = new FilePreferences(
                getInternalPreferences().getUserAndHost(),
                getPath(MAIN_FILE_DIRECTORY, getDefaultPath()).toString(),
                getBoolean(STORE_RELATIVE_TO_BIB),
                get(IMPORT_FILENAMEPATTERN),
                get(IMPORT_FILEDIRPATTERN),
                getBoolean(DOWNLOAD_LINKED_FILES),
                getBoolean(FULLTEXT_INDEX_LINKED_FILES),
                Path.of(get(WORKING_DIRECTORY)),
                getBoolean(CREATE_BACKUP),
                // We choose the data directory, because a ".bak" file should survive cache cleanups
                getPath(BACKUP_DIRECTORY, Directories.getBackupDirectory()),
                getBoolean(CONFIRM_LINKED_FILE_DELETE),
                // We make use of the fallback, because we need AWT being initialized, which is not the case at the constructor JabRefPreferences()
                getBoolean(TRASH_INSTEAD_OF_DELETE, moveToTrashSupported()),
                getBoolean(KEEP_DOWNLOAD_URL));

        EasyBind.listen(getInternalPreferences().getUserAndHostProperty(), (obs, oldValue, newValue) -> filePreferences.getUserAndHostProperty().setValue(newValue));
        EasyBind.listen(filePreferences.mainFileDirectoryProperty(), (obs, oldValue, newValue) -> put(MAIN_FILE_DIRECTORY, newValue));
        EasyBind.listen(filePreferences.storeFilesRelativeToBibFileProperty(), (obs, oldValue, newValue) -> putBoolean(STORE_RELATIVE_TO_BIB, newValue));
        EasyBind.listen(filePreferences.fileNamePatternProperty(), (obs, oldValue, newValue) -> put(IMPORT_FILENAMEPATTERN, newValue));
        EasyBind.listen(filePreferences.fileDirectoryPatternProperty(), (obs, oldValue, newValue) -> put(IMPORT_FILEDIRPATTERN, newValue));
        EasyBind.listen(filePreferences.downloadLinkedFilesProperty(), (obs, oldValue, newValue) -> putBoolean(DOWNLOAD_LINKED_FILES, newValue));
        EasyBind.listen(filePreferences.fulltextIndexLinkedFilesProperty(), (obs, oldValue, newValue) -> putBoolean(FULLTEXT_INDEX_LINKED_FILES, newValue));
        EasyBind.listen(filePreferences.workingDirectoryProperty(), (obs, oldValue, newValue) -> put(WORKING_DIRECTORY, newValue.toString()));
        EasyBind.listen(filePreferences.createBackupProperty(), (obs, oldValue, newValue) -> putBoolean(CREATE_BACKUP, newValue));
        EasyBind.listen(filePreferences.backupDirectoryProperty(), (obs, oldValue, newValue) -> put(BACKUP_DIRECTORY, newValue.toString()));
        EasyBind.listen(filePreferences.confirmDeleteLinkedFileProperty(), (obs, oldValue, newValue) -> putBoolean(CONFIRM_LINKED_FILE_DELETE, newValue));
        EasyBind.listen(filePreferences.moveToTrashProperty(), (obs, oldValue, newValue) -> putBoolean(TRASH_INSTEAD_OF_DELETE, newValue));
        EasyBind.listen(filePreferences.shouldKeepDownloadUrlProperty(), (obs, oldValue, newValue) -> putBoolean(KEEP_DOWNLOAD_URL, newValue));

        return filePreferences;
    }

    @Override
    public AutoLinkPreferences getAutoLinkPreferences() {
        if (autoLinkPreferences != null) {
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
        if (exportPreferences != null) {
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

    protected SaveOrder getExportSaveOrder() {
        List<SaveOrder.SortCriterion> sortCriteria = new ArrayList<>();

        if (!"".equals(get(EXPORT_PRIMARY_SORT_FIELD))) {
            sortCriteria.add(new SaveOrder.SortCriterion(FieldFactory.parseField(get(EXPORT_PRIMARY_SORT_FIELD)), getBoolean(EXPORT_PRIMARY_SORT_DESCENDING)));
        }
        if (!"".equals(get(EXPORT_SECONDARY_SORT_FIELD))) {
            sortCriteria.add(new SaveOrder.SortCriterion(FieldFactory.parseField(get(EXPORT_SECONDARY_SORT_FIELD)), getBoolean(EXPORT_SECONDARY_SORT_DESCENDING)));
        }
        if (!"".equals(get(EXPORT_TERTIARY_SORT_FIELD))) {
            sortCriteria.add(new SaveOrder.SortCriterion(FieldFactory.parseField(get(EXPORT_TERTIARY_SORT_FIELD)), getBoolean(EXPORT_TERTIARY_SORT_DESCENDING)));
        }

        return new SaveOrder(
                SaveOrder.OrderType.fromBooleans(getBoolean(EXPORT_IN_SPECIFIED_ORDER), getBoolean(EXPORT_IN_ORIGINAL_ORDER)),
                sortCriteria
        );
    }

    private void storeExportSaveOrder(SaveOrder saveOrder) {
        putBoolean(EXPORT_IN_ORIGINAL_ORDER, saveOrder.getOrderType() == SaveOrder.OrderType.ORIGINAL);
        putBoolean(EXPORT_IN_SPECIFIED_ORDER, saveOrder.getOrderType() == SaveOrder.OrderType.SPECIFIED);

        long saveOrderCount = saveOrder.getSortCriteria().size();
        if (saveOrderCount >= 1) {
            put(EXPORT_PRIMARY_SORT_FIELD, saveOrder.getSortCriteria().getFirst().field.getName());
            putBoolean(EXPORT_PRIMARY_SORT_DESCENDING, saveOrder.getSortCriteria().getFirst().descending);
        } else {
            put(EXPORT_PRIMARY_SORT_FIELD, "");
            putBoolean(EXPORT_PRIMARY_SORT_DESCENDING, false);
        }
        if (saveOrderCount >= 2) {
            put(EXPORT_SECONDARY_SORT_FIELD, saveOrder.getSortCriteria().get(1).field.getName());
            putBoolean(EXPORT_SECONDARY_SORT_DESCENDING, saveOrder.getSortCriteria().get(1).descending);
        } else {
            put(EXPORT_SECONDARY_SORT_FIELD, "");
            putBoolean(EXPORT_SECONDARY_SORT_DESCENDING, false);
        }
        if (saveOrderCount >= 3) {
            put(EXPORT_TERTIARY_SORT_FIELD, saveOrder.getSortCriteria().get(2).field.getName());
            putBoolean(EXPORT_TERTIARY_SORT_DESCENDING, saveOrder.getSortCriteria().get(2).descending);
        } else {
            put(EXPORT_TERTIARY_SORT_FIELD, "");
            putBoolean(EXPORT_TERTIARY_SORT_DESCENDING, false);
        }
    }

    @Override
    public SelfContainedSaveConfiguration getSelfContainedExportConfiguration() {
        SaveOrder exportSaveOrder = getExportSaveOrder();
        SelfContainedSaveOrder saveOrder = switch (exportSaveOrder.getOrderType()) {
            case TABLE -> {
                LOGGER.warn("Table sort order requested, but JabRef is in CLI mode. Falling back to defeault save order");
                yield SaveOrder.getDefaultSaveOrder();
            }
            case SPECIFIED ->
                    SelfContainedSaveOrder.of(exportSaveOrder);
            case ORIGINAL ->
                    SaveOrder.getDefaultSaveOrder();
        };

        return new SelfContainedSaveConfiguration(
                saveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, getLibraryPreferences()
                .shouldAlwaysReformatOnSave());
    }

    private List<TemplateExporter> getCustomExportFormats() {
        LayoutFormatterPreferences layoutPreferences = getLayoutFormatterPreferences();
        SelfContainedSaveConfiguration saveConfiguration = getSelfContainedExportConfiguration();
        List<TemplateExporter> formats = new ArrayList<>();

        for (String toImport : getSeries(CUSTOM_EXPORT_FORMAT)) {
            List<String> formatData = convertStringToList(toImport);
            TemplateExporter format = new TemplateExporter(
                    formatData.getFirst(),
                    formatData.get(EXPORTER_FILENAME_INDEX),
                    formatData.get(EXPORTER_EXTENSION_INDEX),
                    layoutPreferences,
                    saveConfiguration.getSelfContainedSaveOrder());
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
                exporterData.addFirst(exporters.get(i).getName());
                exporterData.add(EXPORTER_FILENAME_INDEX, exporters.get(i).getLayoutFileName());
                // Only stores the first extension associated with FileType
                exporterData.add(EXPORTER_EXTENSION_INDEX, exporters.get(i).getFileType().getExtensions().getFirst());
                putStringList(CUSTOM_EXPORT_FORMAT + i, exporterData);
            }
            purgeSeries(CUSTOM_EXPORT_FORMAT, exporters.size());
        }
    }

    // region Cleanup preferences

    @Override
    public CleanupPreferences getCleanupPreferences() {
        if (cleanupPreferences != null) {
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

    // endregion

    // region last files opened

    @Override
    public LastFilesOpenedPreferences getLastFilesOpenedPreferences() {
        if (lastFilesOpenedPreferences != null) {
            return lastFilesOpenedPreferences;
        }

        lastFilesOpenedPreferences = new LastFilesOpenedPreferences(
                getStringList(LAST_EDITED).stream()
                                          .map(Path::of)
                                          .toList(),
                Path.of(get(LAST_FOCUSED)),
                getFileHistory());

        lastFilesOpenedPreferences.getLastFilesOpened().addListener((ListChangeListener<Path>) change -> {
            if (change.getList().isEmpty()) {
                remove(LAST_EDITED);
            } else {
                putStringList(LAST_EDITED, lastFilesOpenedPreferences.getLastFilesOpened().stream()
                                                                     .map(Path::toAbsolutePath)
                                                                     .map(Path::toString)
                                                                     .toList());
            }
        });
        EasyBind.listen(lastFilesOpenedPreferences.lastFocusedFileProperty(), (obs, oldValue, newValue) -> {
            if (newValue != null) {
                put(LAST_FOCUSED, newValue.toAbsolutePath().toString());
            } else {
                remove(LAST_FOCUSED);
            }
        });
        lastFilesOpenedPreferences.getFileHistory().addListener((InvalidationListener) change -> storeFileHistory(lastFilesOpenedPreferences.getFileHistory()));

        return lastFilesOpenedPreferences;
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

    // endregion

    // region other preferences

    @Override
    public AiPreferences getAiPreferences() {
        if (aiPreferences != null) {
            return aiPreferences;
        }

        boolean aiEnabled = getBoolean(AI_ENABLED);

        aiPreferences = new AiPreferences(
                aiEnabled,
                getBoolean(AI_AUTO_GENERATE_EMBEDDINGS),
                getBoolean(AI_AUTO_GENERATE_SUMMARIES),
                AiProvider.valueOf(get(AI_PROVIDER)),
                get(AI_OPEN_AI_CHAT_MODEL),
                get(AI_MISTRAL_AI_CHAT_MODEL),
                get(AI_GEMINI_CHAT_MODEL),
                get(AI_HUGGING_FACE_CHAT_MODEL),
                getBoolean(AI_CUSTOMIZE_SETTINGS),
                get(AI_OPEN_AI_API_BASE_URL),
                get(AI_MISTRAL_AI_API_BASE_URL),
                get(AI_GEMINI_API_BASE_URL),
                get(AI_HUGGING_FACE_API_BASE_URL),
                EmbeddingModel.valueOf(get(AI_EMBEDDING_MODEL)),
                get(AI_SYSTEM_MESSAGE),
                getDouble(AI_TEMPERATURE),
                getInt(AI_CONTEXT_WINDOW_SIZE),
                getInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE),
                getInt(AI_DOCUMENT_SPLITTER_OVERLAP_SIZE),
                getInt(AI_RAG_MAX_RESULTS_COUNT),
                getDouble(AI_RAG_MIN_SCORE));

        EasyBind.listen(aiPreferences.enableAiProperty(), (obs, oldValue, newValue) -> putBoolean(AI_ENABLED, newValue));
        EasyBind.listen(aiPreferences.autoGenerateEmbeddingsProperty(), (obs, oldValue, newValue) -> putBoolean(AI_AUTO_GENERATE_EMBEDDINGS, newValue));
        EasyBind.listen(aiPreferences.autoGenerateSummariesProperty(), (obs, oldValue, newValue) -> putBoolean(AI_AUTO_GENERATE_SUMMARIES, newValue));

        EasyBind.listen(aiPreferences.aiProviderProperty(), (obs, oldValue, newValue) -> put(AI_PROVIDER, newValue.name()));

        EasyBind.listen(aiPreferences.openAiChatModelProperty(), (obs, oldValue, newValue) -> put(AI_OPEN_AI_CHAT_MODEL, newValue));
        EasyBind.listen(aiPreferences.mistralAiChatModelProperty(), (obs, oldValue, newValue) -> put(AI_MISTRAL_AI_CHAT_MODEL, newValue));
        EasyBind.listen(aiPreferences.geminiChatModelProperty(), (obs, oldValue, newValue) -> put(AI_GEMINI_CHAT_MODEL, newValue));
        EasyBind.listen(aiPreferences.huggingFaceChatModelProperty(), (obs, oldValue, newValue) -> put(AI_HUGGING_FACE_CHAT_MODEL, newValue));

        EasyBind.listen(aiPreferences.customizeExpertSettingsProperty(), (obs, oldValue, newValue) -> putBoolean(AI_CUSTOMIZE_SETTINGS, newValue));

        EasyBind.listen(aiPreferences.openAiApiBaseUrlProperty(), (obs, oldValue, newValue) -> put(AI_OPEN_AI_API_BASE_URL, newValue));
        EasyBind.listen(aiPreferences.mistralAiApiBaseUrlProperty(), (obs, oldValue, newValue) -> put(AI_MISTRAL_AI_API_BASE_URL, newValue));
        EasyBind.listen(aiPreferences.geminiApiBaseUrlProperty(), (obs, oldValue, newValue) -> put(AI_GEMINI_API_BASE_URL, newValue));
        EasyBind.listen(aiPreferences.huggingFaceApiBaseUrlProperty(), (obs, oldValue, newValue) -> put(AI_HUGGING_FACE_API_BASE_URL, newValue));

        EasyBind.listen(aiPreferences.embeddingModelProperty(), (obs, oldValue, newValue) -> put(AI_EMBEDDING_MODEL, newValue.name()));
        EasyBind.listen(aiPreferences.instructionProperty(), (obs, oldValue, newValue) -> put(AI_SYSTEM_MESSAGE, newValue));
        EasyBind.listen(aiPreferences.temperatureProperty(), (obs, oldValue, newValue) -> putDouble(AI_TEMPERATURE, newValue.doubleValue()));
        EasyBind.listen(aiPreferences.contextWindowSizeProperty(), (obs, oldValue, newValue) -> putInt(AI_CONTEXT_WINDOW_SIZE, newValue));
        EasyBind.listen(aiPreferences.documentSplitterChunkSizeProperty(), (obs, oldValue, newValue) -> putInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, newValue));
        EasyBind.listen(aiPreferences.documentSplitterOverlapSizeProperty(), (obs, oldValue, newValue) -> putInt(AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, newValue));
        EasyBind.listen(aiPreferences.ragMaxResultsCountProperty(), (obs, oldValue, newValue) -> putInt(AI_RAG_MAX_RESULTS_COUNT, newValue));
        EasyBind.listen(aiPreferences.ragMinScoreProperty(), (obs, oldValue, newValue) -> putDouble(AI_RAG_MIN_SCORE, newValue.doubleValue()));

        return aiPreferences;
    }

    @Override
    public SearchPreferences getSearchPreferences() {
        if (searchPreferences != null) {
            return searchPreferences;
        }

        searchPreferences = new SearchPreferences(
                getBoolean(SEARCH_DISPLAY_MODE) ? SearchDisplayMode.FILTER : SearchDisplayMode.FLOAT,
                getBoolean(SEARCH_REG_EXP),
                getBoolean(SEARCH_CASE_SENSITIVE),
                getBoolean(SEARCH_FULLTEXT),
                getBoolean(SEARCH_KEEP_SEARCH_STRING),
                getBoolean(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP),
                getDouble(SEARCH_WINDOW_HEIGHT),
                getDouble(SEARCH_WINDOW_WIDTH),
                getDouble(SEARCH_WINDOW_DIVIDER_POS));

        searchPreferences.getObservableSearchFlags().addListener((SetChangeListener<SearchFlags>) c -> {
            putBoolean(SEARCH_FULLTEXT, searchPreferences.getObservableSearchFlags().contains(SearchFlags.FULLTEXT));
        });
        EasyBind.listen(searchPreferences.searchDisplayModeProperty(), (obs, oldValue, newValue) -> putBoolean(SEARCH_DISPLAY_MODE, newValue == SearchDisplayMode.FILTER));
        EasyBind.listen(searchPreferences.keepSearchStingProperty(), (obs, oldValue, newValue) -> putBoolean(SEARCH_KEEP_SEARCH_STRING, newValue));
        EasyBind.listen(searchPreferences.keepWindowOnTopProperty(), (obs, oldValue, newValue) -> putBoolean(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, searchPreferences.shouldKeepWindowOnTop()));
        EasyBind.listen(searchPreferences.getSearchWindowHeightProperty(), (obs, oldValue, newValue) -> putDouble(SEARCH_WINDOW_HEIGHT, searchPreferences.getSearchWindowHeight()));
        EasyBind.listen(searchPreferences.getSearchWindowWidthProperty(), (obs, oldValue, newValue) -> putDouble(SEARCH_WINDOW_WIDTH, searchPreferences.getSearchWindowWidth()));
        EasyBind.listen(searchPreferences.getSearchWindowDividerPositionProperty(), (obs, oldValue, newValue) -> putDouble(SEARCH_WINDOW_DIVIDER_POS, searchPreferences.getSearchWindowDividerPosition()));

        return searchPreferences;
    }

    @Override
    public XmpPreferences getXmpPreferences() {
        if (xmpPreferences != null) {
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
        if (nameFormatterPreferences != null) {
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
    public MrDlibPreferences getMrDlibPreferences() {
        if (mrDlibPreferences != null) {
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
        if (protectedTermsPreferences != null) {
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
        if (importerPreferences != null) {
            return importerPreferences;
        }

        importerPreferences = new ImporterPreferences(
                getBoolean(IMPORTERS_ENABLED),
                getBoolean(GENERATE_KEY_ON_IMPORT),
                Path.of(get(IMPORT_WORKING_DIRECTORY)),
                getBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION),
                getCustomImportFormats(),
                getFetcherKeys(),
                getDefaultFetcherKeys(),
                getBoolean(FETCHER_CUSTOM_KEY_PERSIST),
                getStringList(SEARCH_CATALOGS),
                PlainCitationParserChoice.valueOf(get(DEFAULT_PLAIN_CITATION_PARSER))
        );

        EasyBind.listen(importerPreferences.importerEnabledProperty(), (obs, oldValue, newValue) -> putBoolean(IMPORTERS_ENABLED, newValue));
        EasyBind.listen(importerPreferences.generateNewKeyOnImportProperty(), (obs, oldValue, newValue) -> putBoolean(GENERATE_KEY_ON_IMPORT, newValue));
        EasyBind.listen(importerPreferences.importWorkingDirectoryProperty(), (obs, oldValue, newValue) -> put(IMPORT_WORKING_DIRECTORY, newValue.toString()));
        EasyBind.listen(importerPreferences.warnAboutDuplicatesOnImportProperty(), (obs, oldValue, newValue) -> putBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION, newValue));
        EasyBind.listen(importerPreferences.persistCustomKeysProperty(), (obs, oldValue, newValue) -> putBoolean(FETCHER_CUSTOM_KEY_PERSIST, newValue));
        importerPreferences.getApiKeys().addListener((InvalidationListener) c -> storeFetcherKeys(importerPreferences.getApiKeys()));
        importerPreferences.getCustomImporters().addListener((InvalidationListener) c -> storeCustomImportFormats(importerPreferences.getCustomImporters()));
        importerPreferences.getCatalogs().addListener((InvalidationListener) c -> putStringList(SEARCH_CATALOGS, importerPreferences.getCatalogs()));
        EasyBind.listen(importerPreferences.defaultPlainCitationParserProperty(), (obs, oldValue, newValue) -> put(DEFAULT_PLAIN_CITATION_PARSER, newValue.name()));

        return importerPreferences;
    }

    private Set<CustomImporter> getCustomImportFormats() {
        Set<CustomImporter> importers = new TreeSet<>();

        for (String toImport : getSeries(CUSTOM_IMPORT_FORMAT)) {
            List<String> importerString = convertStringToList(toImport);
            try {
                if (importerString.size() == 2) {
                    // New format: basePath, className
                    importers.add(new CustomImporter(importerString.getFirst(), importerString.get(1)));
                } else {
                    // Old format: name, cliId, className, basePath
                    importers.add(new CustomImporter(importerString.get(3), importerString.get(2)));
                }
            } catch (Exception e) {
                LOGGER.warn("Could not load {} from preferences. Will ignore.", importerString.getFirst(), e);
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
                    LOGGER.debug("No api key stored for {} fetcher", fetcher);
                    keys.add("");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("JabRef could not open the key store");
        }

        return keys;
    }

    private Map<String, String> getDefaultFetcherKeys() {
        BuildInfo buildInfo = Injector.instantiateModelOrService(BuildInfo.class);
        if (buildInfo == null) {
            LOGGER.warn("Could not instantiate BuildInfo.");
            return Collections.emptyMap();
        }

        Map<String, String> keys = new HashMap<>();
        keys.put(SemanticScholarFetcher.FETCHER_NAME, buildInfo.semanticScholarApiKey);
        keys.put(AstrophysicsDataSystem.FETCHER_NAME, buildInfo.astrophysicsDataSystemAPIKey);
        keys.put(BiodiversityLibrary.FETCHER_NAME, buildInfo.biodiversityHeritageApiKey);
        keys.put(ScienceDirect.FETCHER_NAME, buildInfo.scienceDirectApiKey);
        keys.put(SpringerFetcher.FETCHER_NAME, buildInfo.springerNatureAPIKey);
        // SpringerLink uses the same key and fetcher name as SpringerFetcher

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

        if (getBoolean(FETCHER_CUSTOM_KEY_PERSIST)) {
            storeFetcherKeysToKeyring(names, keys);
        } else {
            clearCustomFetcherKeys();
        }
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
            LOGGER.error("Unable to open key store", ex);
        }
    }

    private void clearCustomFetcherKeys() {
        List<String> names = getStringList(FETCHER_CUSTOM_KEY_NAMES);
        try (final Keyring keyring = Keyring.create()) {
            try {
                for (String name : names) {
                    keyring.deletePassword("org.jabref.customapikeys", name);
                }
            } catch (PasswordAccessException ex) {
                // nothing to do, no password to remove
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to open key store");
        }
    }

    @Override
    public GrobidPreferences getGrobidPreferences() {
        if (grobidPreferences != null) {
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

    // endregion
}
