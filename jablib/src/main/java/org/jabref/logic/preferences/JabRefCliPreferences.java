package org.jabref.logic.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.InternalPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.cleanup.FieldFormatterCleanupMapper;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.ocr.OcrPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.os.OS;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.logic.push.PushApplications;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.Version;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The `JabRefPreferences` class provides the preferences and their defaults using
/// the JDK `java.util.prefs` class.
///
/// Internally it defines symbols used to pick a value from the `java.util.prefs`
/// interface and keeps a hashmap with all the default values.
///
/// There are still some similar preferences classes ({@link OpenOfficePreferences} and
/// {@link SharedDatabasePreferences}) which also use the `java.util.prefs` API.
///
/// contents of the defaults HashMap that are defined in this class.
/// There are more default parameters in this map which belong to separate preference classes.
///
/// This class is injected into formatter using reflection to avoid tight coupling and
/// is easier than injecting via constructor due to amount of refactoring
@Singleton
public class JabRefCliPreferences implements CliPreferences {
    public static final String LANGUAGE = "language";

    // region LibraryPreferences
    public static final String LIBRARY_BIBLATEX_DEFAULT_MODE = "biblatexMode";
    public static final String LIBRARY_REFORMAT_ON_SAVE_AND_EXPORT = "reformatFileOnSaveAndExport";
    public static final String LIBRARY_AUTO_SAVE = "localAutoSave";
    public static final String LIBRARY_ADD_IMPORTED_ENTRIES = "addImportedEntries";
    public static final String LIBRARY_ADD_IMPORTED_ENTRIES_GROUP_NAME = "addImportedEntriesGroupName";
    // endregion

    // region ExportPreferences
    public static final String LAST_USED_EXPORT = "lastUsedExport";
    public static final String EXPORT_WORKING_DIRECTORY = "exportWorkingDirectory";
    public static final String EXPORT_PRIMARY_SORT_FIELD = "exportPriSort";
    public static final String EXPORT_PRIMARY_SORT_DESCENDING = "exportPriDescending";
    public static final String EXPORT_SECONDARY_SORT_FIELD = "exportSecSort";
    public static final String EXPORT_SECONDARY_SORT_DESCENDING = "exportSecDescending";
    public static final String EXPORT_TERTIARY_SORT_FIELD = "exportTerSort";
    public static final String EXPORT_TERTIARY_SORT_DESCENDING = "exportTerDescending";
    public static final String EXPORT_IN_ORIGINAL_ORDER = "exportInOriginalOrder";
    public static final String EXPORT_IN_SPECIFIED_ORDER = "exportInSpecifiedOrder";
    // endregion

    // region XmpPreferences
    public static final String XMP_USE_PRIVACY_FILTER = "useXmpPrivacyFilter";
    public static final String XMP_PRIVACY_FILTERS = "xmpPrivacyFilters";
    // endregion

    public static final String KEYWORD_SEPARATOR = "groupKeywordSeparator";

    public static final String MEMORY_STICK_MODE = "memoryStickMode";

    // region DOIPreferences
    public static final String DOI_BASE_URI = "baseDOIURI";
    public static final String DOI_USE_CUSTOM_URI = "useCustomDOIURI";
    // endregion

    // region OwnerPreferences
    public static final String OWNER_ENABLE = "useOwner";
    public static final String OWNER_DEFAULT = "defaultOwner";
    public static final String OWNER_OVERWRITE = "overwriteOwner";
    // endregion

    // region TimestampPreferences
    public static final String TIMESTAMP_ADD_CREATION_DATE = "addCreationDate";
    public static final String TIMESTAMP_ADD_MODIFICATION_DATE = "addModificationDate";
    // legacy pre-5.3 fields for library cleanups
    public static final String TIMESTAMP_DEPRECATED_UPDATE = "updateTimestamp";
    public static final String TIMESTAMP_DEPRECATED_FIELD = "timeStampField";
    public static final String TIMESTAMP_DEPRECATED_FORMAT = "timeStampFormat";
    // endregion

    public static final String NON_WRAPPABLE_FIELDS = "nonWrappableFields";
    public static final String RESOLVE_STRINGS_FOR_FIELDS = "resolveStringsForFields";
    public static final String DO_NOT_RESOLVE_STRINGS = "doNotResolveStrings";

    // merge related
    public static final String MERGE_ENTRIES_DIFF_MODE = "mergeEntriesDiffMode";
    public static final String MERGE_ENTRIES_SHOULD_SHOW_DIFF = "mergeEntriesShouldShowDiff";
    public static final String MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF = "mergeEntriesShouldShowUnifiedDiff";
    public static final String MERGE_ENTRIES_HIGHLIGHT_WORDS = "mergeEntriesHighlightWords";

    public static final String MERGE_SHOW_ONLY_CHANGED_FIELDS = "mergeShowOnlyChangedFields";

    public static final String MERGE_APPLY_TO_ALL_ENTRIES = "mergeApplyToAllEntries";

    public static final String DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES = "duplicateResolverDecisionResult";

    public static final String CUSTOM_EXPORT_FORMAT = "customExportFormat";

    public static final String SEARCH_DISPLAY_MODE = "searchDisplayMode";
    public static final String SEARCH_CASE_SENSITIVE = "caseSensitiveSearch";
    public static final String SEARCH_REG_EXP = "regExpSearch";
    public static final String SEARCH_FULLTEXT = "fulltextSearch";
    public static final String SEARCH_USE_POSTGRES = "searchUsePostgres";
    public static final String SEARCH_KEEP_SEARCH_STRING = "keepSearchString";
    public static final String SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP = "keepOnTop";
    public static final String SEARCH_WINDOW_HEIGHT = "searchWindowHeight";
    public static final String SEARCH_WINDOW_WIDTH = "searchWindowWidth";
    public static final String SEARCH_WINDOW_DIVIDER_POS = "searchWindowDividerPos";
    public static final String GROBID_ENABLED = "grobidEnabled";
    public static final String GROBID_PREFERENCE = "grobidPreference";
    public static final String GROBID_URL = "grobidURL";

    // region CitationKeyPreferences
    public static final String CITATION_KEY_TRANSLITERATE_FIELDS = "transliterateFields";
    public static final String CITATION_KEY_AVOID_OVERWRITING = "avoidOverwritingKey";
    public static final String CITATION_KEY_WARN_BEFORE_OVERWRITE = "warnBeforeOverwritingKey";
    public static final String CITATION_KEY_GENERATE_BEFORE_SAVING = "generateKeysBeforeSaving";
    public static final String CITATION_KEY_GEN_ALWAYS_ADD_LETTER = "keyGenAlwaysAddLetter";
    public static final String CITATION_KEY_GEN_FIRST_LETTER_A = "keyGenFirstLetterA";
    public static final String CITATION_KEY_PATTERN_REGEX = "KeyPatternRegex";
    public static final String CITATION_KEY_PATTERN_REPLACEMENT = "KeyPatternReplacement";
    public static final String CITATION_KEY_UNWANTED_CHARACTERS = "defaultUnwantedBibtexKeyCharacters";
    public static final String CITATION_KEY_DEFAULT_PATTERN = "defaultBibtexKeyPattern";
    // endregion

    public static final String AUTOLINK_EXACT_KEY_ONLY = "autolinkExactKeyOnly";
    public static final String AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY = "regExpSearchExpression";
    public static final String AUTOLINK_USE_REG_EXP_SEARCH_KEY = "useRegExpSearch";
    // bibLocAsPrimaryDir is a misleading antique variable name, we keep it for reason of compatibility

    public static final String ASK_AUTO_NAMING_PDFS_AGAIN = "AskAutoNamingPDFsAgain";
    public static final String CLEANUP_JOBS = "CleanUpJobs";
    public static final String CLEANUP_FIELD_FORMATTERS_ENABLED = "CleanUpFormattersEnabled";
    public static final String CLEANUP_FIELD_FORMATTERS = "CleanUpFormatters";
    public static final String NAME_FORMATTER_VALUE = "nameFormatterFormats";
    public static final String NAME_FORMATER_KEY = "nameFormatterNames";

    /// The OpenOffice/LibreOffice connection preferences are: OO_PATH main directory for
    /// OO/LO installation, used to detect location on Win/macOS when using manual
    /// connect OO_EXECUTABLE_PATH path to soffice-file OO_JARS_PATH directory that
    /// contains juh.jar, jurt.jar, ridl.jar, unoil.jar OO_SYNC_WHEN_CITING true if the
    /// reference list is updated when adding a new citation OO_SHOW_PANEL true if the OO
    /// panel is shown on startup OO_USE_ALL_OPEN_DATABASES true if all databases should
    /// be used when citing OO_BIBLIOGRAPHY_STYLE_FILE path to the used style file
    /// OO_EXTERNAL_STYLE_FILES list with paths to external style files STYLES_*_* size
    /// and position of "Select style" dialog
    public static final String OO_EXECUTABLE_PATH = "ooExecutablePath";
    public static final String OO_SYNC_WHEN_CITING = "syncOOWhenCiting";
    public static final String OO_USE_ALL_OPEN_BASES = "useAllOpenBases";
    public static final String OO_BIBLIOGRAPHY_STYLE_FILE = "ooBibliographyStyleFile";
    public static final String OO_EXTERNAL_STYLE_FILES = "ooExternalStyleFiles";
    public static final String OO_EXTERNAL_CSL_STYLES = "externalCslStyles";
    public static final String OO_CURRENT_STYLE = "ooCurrentStyle";
    public static final String OO_ALWAYS_ADD_CITED_ON_PAGES = "ooAlwaysAddCitedOnPages";
    public static final String OO_CSL_BIBLIOGRAPHY_TITLE = "cslBibliographyTitle";
    public static final String OO_CSL_BIBLIOGRAPHY_HEADER_FORMAT = "cslBibliographyHeaderFormat";
    public static final String OO_CSL_BIBLIOGRAPHY_BODY_FORMAT = "cslBibliographyBodyFormat";
    public static final String OO_ADD_SPACE_AFTER = "ooAddSpaceAfter";

    // Prefs node for CitationKeyPatterns
    public static final String CITATION_KEY_PATTERNS_NODE = "bibtexkeypatterns";
    // Prefs node for customized entry types
    public static final String CUSTOMIZED_BIBTEX_TYPES = "customizedBibtexTypes";
    public static final String CUSTOMIZED_BIBLATEX_TYPES = "customizedBiblatexTypes";
    public static final String CUSTOMIZED_BIBTEX_TYPES_V2 = "customizedBibtexTypesV2";
    public static final String CUSTOMIZED_BIBLATEX_TYPES_V2 = "customizedBiblatexTypesV2";
    // Version
    public static final String VERSION_IGNORED_UPDATE = "versionIgnoreUpdate";
    public static final String VERSION_CHECK_ENABLED = "versionCheck";

    // String delimiter
    public static final Character STRINGLIST_DELIMITER = ';';

    // region (Linked)FilePreferences
    private static final String FILES_MAIN_DIRECTORY = "fileDirectory";
    private static final String FILES_STORE_RELATIVE_TO_BIB = "bibLocAsPrimaryDir";
    private static final String FILES_AUTO_RENAME_ON_CHANGE = "autoRenameFilesOnChange";
    private static final String FILES_IMPORT_NAMEPATTERN = "importFileNamePattern";
    private static final String FILES_IMPORT_DIRPATTERN = "importFileDirPattern";
    private static final String FILES_DOWNLOAD_LINKED = "downloadLinkedFiles";
    private static final String FILES_FULLTEXT_INDEX = "fulltextIndexLinkedFiles";
    private static final String FILES_WORKING_DIRECTORY = "workingDirectory";

    // FixMe: Missplaced
    private static final String BACKUP_ENABLED = "createBackup";
    private static final String BACKUP_DIRECTORY = "backupDirectory";

    private static final String FILES_CONFIRM_DELETE_LINKED = "confirmLinkedFileDelete";
    private static final String FILES_TRASH_INSTEAD_OF_DELETE = "trashInsteadOfDelete";
    private static final String FILES_ADJUST_FILE_LINKS_ON_TRANSFER = "adjustFileLinksOnTransfer";
    private static final String FILES_COPY_LINKED_FILES_ON_TRANSFER = "copyLinkedFilesOnTransfer";
    private static final String FILES_MOVE_LINKED_FILES_ON_TRANSFER = "moveLinkedFilesOnTransfer";
    private static final String FILES_KEEP_DOWNLOAD_URL = "keepDownloadUrl";
    private static final String FILES_LAST_USED_DIRECTORY = "lastUsedDirectory";
    private static final String FILES_OPEN_FILE_EXPLORER_IN_FILE_DIRECTORY = "openFileExplorerInFileDirectory";
    private static final String FILES_OPEN_FILE_EXPLORER_IN_LAST_USED_DIRECTORY = "openFileExplorerInLastUsedDirectory";
    // endregion

    // region last files opened
    private static final String LAST_EDITED = "lastEdited";
    private static final String LAST_FOCUSED = "lastFocused";
    private static final String RECENT_DATABASES = "recentDatabases";
    // endregion

    // region ProxyPreferences
    private static final String PROXY_PORT = "proxyPort";
    private static final String PROXY_HOSTNAME = "proxyHostname";
    private static final String PROXY_USE = "useProxy";
    private static final String PROXY_USE_AUTHENTICATION = "useProxyAuthentication";
    private static final String PROXY_USERNAME = "proxyUsername";
    // PROXY_PASSWORD = "proxyPassword" handled by KeyRing
    private static final String PROXY_PERSIST_PASSWORD = "persistPassword";
    // endregion

    // SSL
    private static final String SSL_TRUSTSTORE_PATH = "truststorePath";

    // Abbreviation preferences
    private static final String EXTERNAL_JOURNAL_LISTS = "externalJournalLists";
    private static final String USE_AMS_FJOURNAL = "useAMSFJournal";
    private static final String ENABLE_MSC_KEYWORD_DESCRIPTIONS = "enableMscKeywordDescriptions";

    // Protected terms
    private static final String PROTECTED_TERMS_ENABLED_EXTERNAL = "protectedTermsEnabledExternal";
    private static final String PROTECTED_TERMS_DISABLED_EXTERNAL = "protectedTermsDisabledExternal";
    private static final String PROTECTED_TERMS_ENABLED_INTERNAL = "protectedTermsEnabledInternal";
    private static final String PROTECTED_TERMS_DISABLED_INTERNAL = "protectedTermsDisabledInternal";

    // Dialog states
    private static final String PREFS_EXPORT_PATH = "prefsExportPath";

    // Indexes for Strings within stored custom export entries
    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    // region RemotePreferences
    private static final String SERVER_REMOTE_ENABLE = "useRemoteServer";
    private static final String SERVER_REMOTE_PORT = "remoteServerPort";
    private static final String SERVER_HTTP_ENABLE = "enableHttpServer";
    private static final String SERVER_HTTP_PORT = "httpServerPort";
    private static final String SERVER_LANGUAGE_ENABLE = "enableLanguageServer";
    private static final String SERVER_LANGUAGE_PORT = "languageServerPort";
    private static final String SERVER_DIRECT_HTTP_IMPORT = "directHttpImport";
    // endregion

    // region AiPreferences
    private static final String AI_ENABLED = "aiEnabled";
    private static final String AI_AUTO_GENERATE_EMBEDDINGS = "aiAutoGenerateEmbeddings";
    private static final String AI_AUTO_GENERATE_SUMMARIES = "aiAutoGenerateSummaries";
    private static final String AI_GENERATE_FOLLOW_UP_QUESTIONS = "aiGenerateFollowUpQuestions";
    private static final String AI_FOLLOW_UP_QUESTIONS_COUNT = "aiFollowUpQuestionsCount";
    private static final String AI_FOLLOW_UP_QUESTIONS_TEMPLATE = "aiFollowUpQuestionsTemplate";
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
    private static final String AI_SUMMARIZATOR_KIND = "aiSummarizatorKind";
    private static final String AI_TOKEN_ESTIMATOR_KIND = "aiTokenEstimatorKind";
    private static final String AI_TEMPERATURE = "aiTemperature";
    private static final String AI_CONTEXT_WINDOW_SIZE = "aiMessageWindowSize";
    private static final String AI_DOCUMENT_SPLITTER_KIND = "aiDocumentSplitterKind";
    private static final String AI_DOCUMENT_SPLITTER_CHUNK_SIZE = "aiDocumentSplitterChunkSize";
    private static final String AI_DOCUMENT_SPLITTER_OVERLAP_SIZE = "aiDocumentSplitterOverlapSize";
    private static final String AI_ANSWER_ENGINE_KIND = "aiAnswerEngineKind";
    private static final String AI_RAG_MAX_RESULTS_COUNT = "aiRagMaxResultsCount";
    private static final String AI_RAG_MIN_SCORE = "aiRagMinScore";

    private static final String AI_CHATTING_SYSTEM_MESSAGE_TEMPLATE = "aiChattingSystemMessageTemplate";
    private static final String AI_CHATTING_USER_MESSAGE_TEMPLATE = "aiChattingUserMessageTemplate";
    private static final String AI_SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE = "aiSummarizationChunkSystemMessageTemplate";
    private static final String AI_SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE = "aiSummarizationCombineSystemMessageTemplate";
    private static final String AI_CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE = "aiCitationParsingSystemMessageTemplate";
    private static final String AI_SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE = "aiSummarizationFullDocumentSystemMessageTemplate";
    private static final String AI_MARKDOWN_CHAT_EXPORT_TEMPLATE = "aiMarkdownChatExportTemplate";
    // endregion

    // region OCR preferences
    private static final String OCR_ENGINE_PATH = "ocrEnginePath";
    // endregion

    // region Push to application preferences
    private static final String PUSH_TO_APPLICATION = "pushToApplication";
    private static final String PUSH_EMACS_ADDITIONAL_PARAMETERS = "emacsParameters";
    private static final String PUSH_VIM_SERVER = "vimServer";
    private static final String PUSH_CITE_COMMAND = "citeCommand";
    /// Synthetic registry key: command paths are persisted under per-application keys (see PUSH_APPLICATIONS_PATHS), not under this key
    private static final String PUSH_APPLICATIONS_PATHS_KEY = "pushApplicationsCommandPaths";

    private static final Map<PushApplications, String> PUSH_APPLICATIONS_PATHS = Map.of(
            PushApplications.EMACS, "emacsPath",
            PushApplications.LYX, "lyxpipe",
            PushApplications.TEXMAKER, "texmakerPath",
            PushApplications.TEXSTUDIO, "TeXstudioPath",
            PushApplications.TEXWORKS, "TeXworksPath",
            PushApplications.VIM, "vim",
            PushApplications.WIN_EDT, "winEdtPath",
            PushApplications.SUBLIME_TEXT, "sublimeTextPath",
            PushApplications.VSCODE, "VScodePath"
    );
    // endregion

    // region ImporterPreferences
    private static final String IMPORTER_ENABLED = "importersEnabled";
    private static final String IMPORTER_GENERATE_KEY_ON_IMPORT = "generateKeyOnImport";
    private static final String IMPORTER_WORKING_DIRECTORY = "importWorkingDirectory";
    private static final String IMPORTER_WARN_ABOUT_DUPLICATES = "warnAboutDuplicatesInInspection";
    private static final String IMPORTER_CUSTOM_FORMAT = "customImportFormat";
    private static final String FETCHER_CUSTOM_KEY_NAMES = "fetcherCustomKeyNames";
    private static final String FETCHER_CUSTOM_KEY_USES = "fetcherCustomKeyUses";
    private static final String FETCHER_CUSTOM_KEY_PERSIST = "fetcherCustomKeyPersist";
    private static final String IMPORTER_CATALOGS = "searchCatalogs";
    private static final String IMPORTER_DEFAULT_PLAIN_CITATION_PARSER = "defaultPlainCitationParser";
    private static final String IMPORTER_CITATIONS_RELATIONS_STORE_TTL = "citationsRelationsStoreTTL";
    // endregion

    // region Git
    private static final String GITHUB_PAT_KEY = "githubPersonalAccessToken";
    private static final String GITHUB_USERNAME_KEY = "githubUsername";
    private static final String GITHUB_REMOTE_URL_KEY = "githubRemoteUrl";
    private static final String GITHUB_REMEMBER_PAT_KEY = "githubRememberPat";
    // endregion

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefCliPreferences.class);
    private static final Preferences PREFS_NODE = Preferences.userRoot().node("/org/jabref");

    // The only instance of this class:
    private static JabRefCliPreferences singleton;

    /// Cache variables
    private LibraryPreferences libraryPreferences;
    private DOIPreferences doiPreferences;
    private OwnerPreferences ownerPreferences;
    private TimestampPreferences timestampPreferences;
    private OpenOfficePreferences openOfficePreferences;
    private ImporterPreferences importerPreferences;
    private GrobidPreferences grobidPreferences;
    private ProtectedTermsPreferences protectedTermsPreferences;
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
    private AbbreviationPreferences abbreviationPreferences;
    private FieldPreferences fieldPreferences;
    private AiPreferences aiPreferences;
    private OcrPreferences ocrPreferences;
    private LastFilesOpenedPreferences lastFilesOpenedPreferences;
    private PushToApplicationPreferences pushToApplicationPreferences;
    private GitPreferences gitPreferences;

    private final List<PreferenceBinding> allBindings = new ArrayList<>();

    private record PreferenceBinding(Observable property, Object defaultValue, String preferencesKey, Runnable importFromStore, Runnable resetToDefaults) {
    }

    /// @implNote The constructor was made public because dependency injection via constructor
    /// required widespread refactoring, currently we are using reflection in some formatters
    /// to gain access
    public JabRefCliPreferences() {
        try {
            Path preferencesPath = Path.of("jabref.xml");
            if (Files.exists(preferencesPath)) {
                // This overwrites the configured values, which might be undesired by users
                importPreferencesToBackingStore(preferencesPath);
                LOGGER.info("Preferences imported from jabref.xml");
            }
        } catch (JabRefException e) {
            LOGGER.warn("Could not import preferences from jabref.xml", e);
        }

        // Since some of the preference settings themselves use localized strings, we cannot set the language after
        // the initialization of the preferences in main
        // Otherwise that language framework will be instantiated and more importantly, statically initialized preferences
        // will never be translated.
        Localization.setLanguage(getLanguage());
    }

    /// @deprecated Never ever add a call to this method. There should be only one
    /// caller. All other usages should get the preferences passed (or injected). The
    /// JabRef team leaves the `@deprecated` annotation to have IntelliJ listing
    /// this method with a strike-through.
    @Deprecated
    public static JabRefCliPreferences getInstance() {
        if (JabRefCliPreferences.singleton == null) {
            JabRefCliPreferences.singleton = new JabRefCliPreferences();
        }
        return JabRefCliPreferences.singleton;
    }

    // region Common serializer logic

    @VisibleForTesting
    static String convertListToString(List<String> value) {
        return value.stream().map(val -> StringUtil.quote(val, STRINGLIST_DELIMITER.toString(), '\\')).collect(Collectors.joining(STRINGLIST_DELIMITER.toString()));
    }

    @VisibleForTesting
    static List<String> convertStringToList(String toConvert) {
        if (StringUtil.isBlank(toConvert)) {
            // list needs to be mutable in observable preferences
            // keep consistency with Splitter#splitToList
            return new ArrayList<>();
        }

        return Splitter.on(STRINGLIST_DELIMITER).splitToList(toConvert);
    }
    // endregion

    // region Backingstore access logic

    /// Check whether a key is set (differently from null).
    ///
    /// @param key The key to check.
    /// @return true if the key is set, false otherwise.
    public boolean hasKey(String key) {
        return PREFS_NODE.get(key, null) != null;
    }

    public String get(String key, String def) {
        return PREFS_NODE.get(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        return PREFS_NODE.getBoolean(key, def);
    }

    public int getInt(String key, int def) {
        return PREFS_NODE.getInt(key, def);
    }

    public double getDouble(String key, double def) {
        return PREFS_NODE.getDouble(key, def);
    }

    public void put(String key, String value) {
        PREFS_NODE.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        PREFS_NODE.putBoolean(key, value);
    }

    public void putInt(String key, int value) {
        PREFS_NODE.putInt(key, value);
    }

    public void putInt(String key, Number value) {
        PREFS_NODE.putInt(key, value.intValue());
    }

    public void putDouble(String key, double value) {
        PREFS_NODE.putDouble(key, value);
    }

    protected void remove(String key) {
        PREFS_NODE.remove(key);
    }

    /// Puts a list of strings into the Preferences, by linking its elements with a
    /// STRINGLIST_DELIMITER into a single string. Escape characters make the process
    /// transparent even if strings contains a STRINGLIST_DELIMITER.
    public void putStringList(String key, List<String> value) {
        if (value == null) {
            remove(key);
            return;
        }

        put(key, convertListToString(value));
    }

    /// Returns a List of Strings containing the chosen columns.
    public List<String> getStringList(String key) {
        return convertStringToList(get(key, ""));
    }

    /// Returns a Sequenced Set of Fields.
    public SequencedSet<Field> getFieldSequencedSet(String key, ObservableSet<Field> def) {
        return FieldFactory.parseFieldList(get(key, FieldFactory.serializeFieldsList(def)));
    }

    /// Returns a Path
    private Path getPath(String key, Path defaultValue) {
        String rawPath = get(key, "");
        return StringUtil.isNotBlank(rawPath) ? Path.of(rawPath) : defaultValue;
    }

    private void clearTruststoreFromCustomCertificates() {
        TrustStoreManager trustStoreManager = new TrustStoreManager(SSLPreferences.getDefault().getTruststorePath());
        trustStoreManager.clearCustomCertificates();
    }

    /// Removes the given key from the preferences.
    ///
    /// @throws IllegalArgumentException if the key does not exist
    @Override
    public void deleteKey(String key) throws IllegalArgumentException {
        String keyTrimmed = key.trim();
        if (hasKey(keyTrimmed)) {
            remove(keyTrimmed);
        } else {
            throw new IllegalArgumentException("Unknown preference key");
        }
    }

    /// Calling this method will write all preferences into the preference store.
    @Override
    public void flush() {
        if (getInternalPreferences().isMemoryStickMode()) {
            try {
                exportPreferences(Path.of("jabref.xml"));
            } catch (JabRefException e) {
                LOGGER.warn("Could not export preferences for memory stick mode: {}", e.getMessage(), e);
            }
        }
        try {
            PREFS_NODE.flush();
        } catch (BackingStoreException ex) {
            LOGGER.warn("Cannot communicate with backing store", ex);
        }
    }

    /// General binding primitive. All scalar `bind*` helpers delegate here.
    ///
    /// @param persistListener persists value changes to the backing store
    /// @param importFromStore loads the stored value into the property (falling back to the default)
    /// @param resetToDefaults resets the property to its default value
    private <T> void bindCustom(Property<T> property,
                                String key,
                                T defaultValue,
                                ChangeListener<? super T> persistListener,
                                Runnable importFromStore,
                                Runnable resetToDefaults) {
        EasyBind.listen(property, persistListener);
        allBindings.add(new PreferenceBinding(
                property,
                defaultValue,
                key,
                importFromStore,
                resetToDefaults));
    }

    /// Persist-only binding for secrets that live in the system keyring rather than the backing store.
    /// It registers no [PreferenceBinding], so the value is intentionally excluded from [#getPreferences()] and
    /// [#getDefaults()]. Loading and resetting are owned by its accompanying persist flag.
    ///
    /// @param persistListener writes value changes to the keyring
    private <T> void bindToKeyring(Property<T> property, ChangeListener<? super T> persistListener) {
        EasyBind.listen(property, persistListener);
    }

    private void bindBoolean(BooleanProperty property, String key, boolean defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> putBoolean(key, v),
                () -> property.set(getBoolean(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    private void bindInt(IntegerProperty property, String key, int defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> putInt(key, v),
                () -> property.set(getInt(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    private void bindString(StringProperty property, String key, String defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> put(key, v),
                () -> property.set(get(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    /// Binds an object-valued property whose persisted form is a String.
    ///
    /// @param serializer   converts the value to its stored String representation
    /// @param deserializer reconstructs the value from its stored String representation
    private <T> void bindObject(ObjectProperty<T> property,
                                String key,
                                T defaultValue,
                                Function<T, String> serializer,
                                Function<String, T> deserializer) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> put(key, serializer.apply(v)),
                () -> property.set(deserializer.apply(get(key, serializer.apply(defaultValue)))),
                () -> property.set(defaultValue));
    }

    /// Binds an object-valued property whose persisted form is a boolean.
    ///
    /// @param serializer   converts the value to its stored boolean representation
    /// @param deserializer reconstructs the value from its stored boolean representation
    private <T> void bindBooleanObject(ObjectProperty<T> property,
                                       String key,
                                       T defaultValue,
                                       Function<T, Boolean> serializer,
                                       Function<Boolean, T> deserializer) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> putBoolean(key, serializer.apply(v)),
                () -> property.set(deserializer.apply(getBoolean(key, serializer.apply(defaultValue)))),
                () -> property.set(defaultValue));
    }

    /// Binds a map-valued property. Unlike the other `bind*` helpers, persistence is delegated to the given
    /// `persistListener`, since map entries may be stored under multiple backing-store keys.
    ///
    /// @param persistListener persists individual entry changes to the backing store
    /// @param loadFromStore   reads the stored map, falling back to the given defaults for missing entries
    private void bindMap(MapProperty<String, String> map,
                         String key,
                         Map<String, String> defaultMap,
                         MapChangeListener<? super String, ? super String> persistListener,
                         Function<Map<String, String>, Map<String, String>> loadFromStore) {
        Map<String, String> defaultCopy = Map.copyOf(defaultMap);
        map.addListener(persistListener);
        allBindings.add(new PreferenceBinding(
                map,
                defaultCopy,
                key,
                () -> {
                    map.clear();
                    map.putAll(loadFromStore.apply(defaultCopy));
                },
                () -> {
                    map.clear();
                    map.putAll(defaultCopy);
                }));
    }

    private void bindList(ObservableList<String> list, String key, List<String> defaultList) {
        List<String> defaultCopy = List.copyOf(defaultList);
        list.addListener((InvalidationListener) _ -> putStringList(key, list));
        allBindings.add(new PreferenceBinding(
                list,
                defaultList,
                key,
                () -> list.setAll(convertStringToList(get(key, convertListToString(defaultCopy)))),
                () -> list.setAll(defaultCopy)));
    }

    @Override
    public Map<String, Object> getPreferences() {
        Map<String, Object> result = new HashMap<>();
        allBindings.forEach(binding -> result.put(binding.preferencesKey, getObject(binding.property())));
        return result;
    }

    private Object getObject(Observable observable) {
        if (observable instanceof BooleanProperty booleanProperty) {
            return booleanProperty.get();
        } else if (observable instanceof IntegerProperty integerProperty) {
            return integerProperty.get();
        } else if (observable instanceof StringProperty stringProperty) {
            return stringProperty.get();
        } else if (observable instanceof ObservableList<?> observableList) {
            return observableList;
        } else if (observable instanceof MapProperty<?, ?> mapProperty) {
            return mapProperty.get();
        } else if (observable instanceof ObjectProperty<?> objectProperty) {
            return objectProperty.get();
        }

        return null;
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> result = new HashMap<>();
        allBindings.forEach(binding -> result.put(binding.preferencesKey, binding.defaultValue()));
        return result;
    }

    /// Returns a list of Strings stored by key+N with N being an incrementing number
    protected List<String> getSeries(String key) {
        int i = 0;
        List<String> series = new ArrayList<>();
        String item;
        while (!StringUtil.isBlank(item = get(key + i, null))) {
            series.add(item);
            i++;
        }
        return series;
    }

    /// Removes all entries keyed by prefix+number, where number is equal to or higher
    /// than the given number.
    ///
    /// @param number or higher.
    protected void purgeSeries(String prefix, int number) {
        int n = number;
        while (get(prefix + n, null) != null) {
            remove(prefix + n);
            n++;
        }
    }

    /// Exports Preferences to an XML file.
    ///
    /// @param path Path to export to
    @Override
    public void exportPreferences(Path path) throws JabRefException {
        LOGGER.debug("Exporting preferences {}", path.toAbsolutePath());
        try (OutputStream os = Files.newOutputStream(path)) {
            PREFS_NODE.exportSubtree(os);
        } catch (BackingStoreException
                 | IOException ex) {
            throw new JabRefException(
                    "Could not export preferences",
                    Localization.lang("Could not export preferences"),
                    ex);
        }
    }

    /// Clear all preferences.
    ///
    /// @throws BackingStoreException if JabRef is unable to write to the registry/the preference storage
    @Override
    public void clear() throws BackingStoreException {
        clearAllBibEntryTypes();
        clearCitationKeyPatterns();
        clearTruststoreFromCustomCertificates();
        clearCustomFetcherKeys();
        PREFS_NODE.clear();
        new SharedDatabasePreferences().clear();

        getInternalPreferences().setAll(InternalPreferences.getDefault());
        getFieldPreferences().setAll(FieldPreferences.getDefault());
        getBibEntryPreferences().setAll(BibEntryPreferences.getDefault());
        getCitationKeyPatternPreferences().setAll(
                CitationKeyPatternPreferences.getDefault()
                                             .withKeywordSeparator(getBibEntryPreferences().keywordSeparatorProperty())
        );
        getFilePreferences().setAll(FilePreferences.getDefault()
                                                   .withUserHostInfo(getInternalPreferences().getUserAndHostInfoProperty())
                                                   .withMoveToTrash(moveToTrashSupported())
                                                   .withMainFileDirectory(getDefaultPath())
                                                   .withLastUsedDirectory(getDefaultPath())
        );
        getAiPreferences().setAll(AiPreferences.getDefault());
        getOcrPreferences().setAll(OcrPreferences.getDefault());
        getNameFormatterPreferences().setAll(NameFormatterPreferences.getDefault());
        getCleanupPreferences().setAll(CleanupPreferences.getDefault());
        getImporterPreferences().setAll(ImporterPreferences.getDefault());
        getAutoLinkPreferences().setAll(
                AutoLinkPreferences.getDefault()
                                   .withKeywordSeparator(getBibEntryPreferences().keywordSeparatorProperty()));
        getExportPreferences().setAll(ExportPreferences.getDefault());
        getSSLPreferences().setAll(SSLPreferences.getDefault());
        getSearchPreferences().setAll(SearchPreferences.getDefault());
        getLastFilesOpenedPreferences().setAll(LastFilesOpenedPreferences.getDefault());
        getXmpPreferences().setAll(XmpPreferences.getDefault());
        getProtectedTermsPreferences().setAll(ProtectedTermsPreferences.getDefault());
        getGrobidPreferences().setAll(GrobidPreferences.getDefault());
        getOpenOfficePreferences(JournalAbbreviationLoader.loadRepository(getAbbreviationPreferences())).setAll(
                OpenOfficePreferences.getDefault());

        // ensure registration of bindings
        getProxyPreferences();
        getLibraryPreferences();
        getDOIPreferences();
        getOwnerPreferences();
        getTimestampPreferences();
        getRemotePreferences();
        getPushToApplicationPreferences();
        getAbbreviationPreferences();
        getGitPreferences();

        allBindings.forEach(binding -> binding.resetToDefaults().run());
    }

    /// Imports Preferences from an XML file.
    ///
    /// @param path Path of file to import from
    /// @throws JabRefException thrown if importing the preferences failed due to an InvalidPreferencesFormatException or an IOException
    @Override
    public void importPreferences(Path path) throws JabRefException {
        importPreferencesToBackingStore(path);

        // TODO: We need to load all CLI preferences from the backing store
        //       See org.jabref.gui.preferences.JabRefGuiPreferences.importPreferences for the GUI

        // in case of incomplete or corrupt XML fall back to current preferences
        getInternalPreferences().setAll(getInternalPreferencesFromBackingStore(getInternalPreferences()));
        getFieldPreferences().setAll(getFieldPreferencesFromBackingStore(getFieldPreferences()));
        getCitationKeyPatternPreferences().setAll(getCitationKeyPatternPreferencesFromBackingStore(getCitationKeyPatternPreferences()));
        getFilePreferences().setAll(getFilePreferencesFromBackingStore(getFilePreferences()));
        getBibEntryPreferences().setAll(getBibEntryPreferencesFromBackingStore(getBibEntryPreferences()));
        getAiPreferences().setAll(getAiPreferencesFromBackingStore(getAiPreferences()));
        getOcrPreferences().setAll(getOcrPreferencesFromBackingStore(getOcrPreferences()));
        getNameFormatterPreferences().setAll(getNameFormatterPreferencesFromBackingStore(getNameFormatterPreferences()));
        getCleanupPreferences().setAll(getCleanupPreferencesFromBackingStore(getCleanupPreferences()));
        getImporterPreferences().setAll(getImporterPreferencesFromBackingStore(getImporterPreferences()));
        getAutoLinkPreferences().setAll(getAutoLinkPreferencesFromBackingStore(getAutoLinkPreferences()));
        getExportPreferences().setAll(getExportPreferencesFromBackingStore(getExportPreferences()));
        getSSLPreferences().setAll(getSSLPreferencesFromBackingStore(getSSLPreferences()));
        getSearchPreferences().setAll(getSearchPreferencesFromBackingStore(getSearchPreferences()));
        getLastFilesOpenedPreferences().setAll(getLastFilesOpenedPreferencesFromBackingStore(getLastFilesOpenedPreferences()));
        getXmpPreferences().setAll(getXmpPreferencesFromBackingStore(getXmpPreferences()));
        getProtectedTermsPreferences().setAll(getProtectedTermsPreferencesFromBackingStore(getProtectedTermsPreferences()));
        getGrobidPreferences().setAll(getGrobidPreferencesFromBackingStore(getGrobidPreferences()));
        JournalAbbreviationRepository repository = JournalAbbreviationLoader.loadRepository(getAbbreviationPreferences());
        getOpenOfficePreferences(repository).setAll(
                getOpenOfficePreferencesFromBackingStore(getOpenOfficePreferences(repository), repository));

        // ensure registration of bindings
        getProxyPreferences();
        getLibraryPreferences();
        getDOIPreferences();
        getOwnerPreferences();
        getTimestampPreferences();
        getRemotePreferences();
        getPushToApplicationPreferences();
        getAbbreviationPreferences();
        getGitPreferences();

        allBindings.forEach(binding -> binding.importFromStore().run());
    }

    private static void importPreferencesToBackingStore(Path path) throws JabRefException {
        LOGGER.debug("Importing preferences {}", path.toAbsolutePath());
        try (InputStream is = Files.newInputStream(path)) {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException | IOException ex) {
            throw new JabRefException(
                    "Could not import preferences",
                    Localization.lang("Could not import preferences"),
                    ex);
        }
    }

    protected Path getDefaultPath() {
        return Path.of("/");
    }

    protected Language getLanguage() {
        return Language.getLanguageFor(get(LANGUAGE, Locale.getDefault().getLanguage()));
    }

    // region JournalAbbreviationPreferences
    @Override
    public AbbreviationPreferences getAbbreviationPreferences() {
        if (abbreviationPreferences != null) {
            return abbreviationPreferences;
        }

        AbbreviationPreferences defaultValues = AbbreviationPreferences.getDefault();

        abbreviationPreferences = new AbbreviationPreferences(
                convertStringToList(get(EXTERNAL_JOURNAL_LISTS, convertListToString(defaultValues.getExternalJournalLists()))),
                getBoolean(USE_AMS_FJOURNAL, defaultValues.useFJournalFieldProperty().get()),
                getBoolean(ENABLE_MSC_KEYWORD_DESCRIPTIONS, defaultValues.shouldEnableMscKeywordDescriptions())
        );

        bindList(abbreviationPreferences.getExternalJournalLists(), EXTERNAL_JOURNAL_LISTS, defaultValues.getExternalJournalLists());
        bindBoolean(abbreviationPreferences.useFJournalFieldProperty(), USE_AMS_FJOURNAL, defaultValues.useFJournalFieldProperty().get());
        bindBoolean(abbreviationPreferences.shouldEnableMscKeywordDescriptionsProperty(), ENABLE_MSC_KEYWORD_DESCRIPTIONS, defaultValues.shouldEnableMscKeywordDescriptions());

        return abbreviationPreferences;
    }
    // endregion

    // region PushToApplicationPreferences
    public PushToApplicationPreferences getPushToApplicationPreferences() {
        if (pushToApplicationPreferences != null) {
            return pushToApplicationPreferences;
        }

        PushToApplicationPreferences defaultValues = PushToApplicationPreferences.getDefault();

        pushToApplicationPreferences = new PushToApplicationPreferences(
                get(PUSH_TO_APPLICATION, defaultValues.getActiveApplicationName()),
                readPushToApplicationPath(defaultValues.getCommandPaths()),
                get(PUSH_EMACS_ADDITIONAL_PARAMETERS, defaultValues.getEmacsArguments()),
                get(PUSH_VIM_SERVER, defaultValues.getVimServer()),
                CitationCommandString.from(get(PUSH_CITE_COMMAND, defaultValues.getCiteCommand().toString()))
        );

        bindString(pushToApplicationPreferences.activeApplicationNameProperty(), PUSH_TO_APPLICATION, defaultValues.getActiveApplicationName());
        bindString(pushToApplicationPreferences.emacsArgumentsProperty(), PUSH_EMACS_ADDITIONAL_PARAMETERS, defaultValues.getEmacsArguments());
        bindString(pushToApplicationPreferences.vimServerProperty(), PUSH_VIM_SERVER, defaultValues.getVimServer());
        bindObject(pushToApplicationPreferences.citeCommandProperty(), PUSH_CITE_COMMAND, defaultValues.getCiteCommand(),
                CitationCommandString::toString, CitationCommandString::from);

        // Command paths are persisted under per-application keys (see storePushToApplicationPath), not under a single preferences key
        bindMap(pushToApplicationPreferences.getCommandPaths(), PUSH_APPLICATIONS_PATHS_KEY, defaultValues.getCommandPaths(),
                this::storePushToApplicationPath, this::readPushToApplicationPath);

        return pushToApplicationPreferences;
    }

    /// An empty string is used as the default value to ensure that an installation of a tool leads to the new path
    /// (instead of leaving the empty one). Reason: an empty string is returned by org.jabref.gui.desktop.os.Windows.
    /// detectProgramPath if the program is not found. That path is stored in the preferences.
    private Map<String, String> readPushToApplicationPath(Map<String, String> defaults) {
        Map<String, String> commands = new HashMap<>();

        PUSH_APPLICATIONS_PATHS.forEach((app, key) -> {
            String value = get(key, defaults.getOrDefault(key, ""));
            commands.put(app.getDisplayName(), value);
        });

        return commands;
    }

    private void storePushToApplicationPath(MapChangeListener.Change<? extends String, ? extends String> change) {
        // is only for the preferences and therefore is okay to throw NoSuchElementException
        PushApplications app = PushApplications.getApplicationByDisplayName(change.getKey()).get();
        String key = PUSH_APPLICATIONS_PATHS.get(app);

        if (key == null) {
            return;
        }

        if (change.wasRemoved() || (change.wasAdded() && "".equals(change.getValueAdded()))) {
            remove(key);
        } else if (change.wasAdded()) {
            put(key, change.getValueAdded());
        }
    }
    // endregion

    // region CustomEntryTypes
    @Override
    public BibEntryTypesManager getCustomEntryTypesRepository() {
        BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();
        EnumSet.allOf(BibDatabaseMode.class).forEach(mode ->
                bibEntryTypesManager.addCustomOrModifiedTypes(getBibEntryTypes(mode), mode));
        return bibEntryTypesManager;
    }

    private List<BibEntryType> getBibEntryTypes(BibDatabaseMode bibDatabaseMode) {
        Map<String, BibEntryType> storedEntryTypes = new HashMap<>();
        addEntryTypesFromPreferences(getPrefsNodeForCustomizedEntryTypesV2(bibDatabaseMode), storedEntryTypes, true, "v2");
        addEntryTypesFromPreferences(getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode), storedEntryTypes, false, "v1");
        return new ArrayList<>(storedEntryTypes.values());
    }

    private void addEntryTypesFromPreferences(Preferences prefsNode,
                                              Map<String, BibEntryType> storedEntryTypes,
                                              boolean allowOverWrite,
                                              String versionLabel) {
        try {
            Arrays.stream(prefsNode.keys())
                  .map(key -> prefsNode.get(key, null))
                  .filter(Objects::nonNull)
                  .forEach(typeString -> MetaDataParser.parseCustomEntryType(typeString).ifPresent(entryType -> {
                      String entryTypeName = entryType.getType().getName();
                      if (allowOverWrite) {
                          storedEntryTypes.put(entryTypeName, entryType);
                      } else {
                          storedEntryTypes.putIfAbsent(entryTypeName, entryType);
                      }
                  }));
        } catch (BackingStoreException e) {
            LOGGER.info("Parsing customized entry types ({}) failed.", versionLabel, e);
        }
    }

    private void clearAllBibEntryTypes() {
        for (BibDatabaseMode mode : BibDatabaseMode.values()) {
            clearBibEntryTypes(mode);
        }
    }

    private void clearBibEntryTypes(BibDatabaseMode mode) {
        try {
            Preferences v1Node = getPrefsNodeForCustomizedEntryTypes(mode);
            v1Node.clear();
            v1Node.flush();
            Preferences v2Node = getPrefsNodeForCustomizedEntryTypesV2(mode);
            v2Node.clear();
            v2Node.flush();
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
        Preferences prefsNodev1 = getPrefsNodeForCustomizedEntryTypes(bibDatabaseMode);
        Preferences prefsNodev2 = getPrefsNodeForCustomizedEntryTypesV2(bibDatabaseMode);

        try {
            // clear old custom types
            clearBibEntryTypes(bibDatabaseMode);

            // store current custom types
            bibEntryTypes.forEach(type -> prefsNodev1.put(type.getType().getName(), MetaDataSerializer.serializeCustomEntryTypes(type)));
            bibEntryTypes.forEach(type -> prefsNodev2.put(type.getType().getName(), MetaDataSerializer.serializeCustomEntryTypesV2(type)));

            prefsNodev1.flush();
            prefsNodev2.flush();
        } catch (BackingStoreException e) {
            LOGGER.info("Updating stored custom entry types failed.", e);
        }
    }

    private static Preferences getPrefsNodeForCustomizedEntryTypes(BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBTEX
               ? PREFS_NODE.node(CUSTOMIZED_BIBTEX_TYPES)
               : PREFS_NODE.node(CUSTOMIZED_BIBLATEX_TYPES);
    }

    private static Preferences getPrefsNodeForCustomizedEntryTypesV2(BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBTEX
               ? PREFS_NODE.node(CUSTOMIZED_BIBTEX_TYPES_V2)
               : PREFS_NODE.node(CUSTOMIZED_BIBLATEX_TYPES_V2);
    }
    // endregion

    // region LibraryPreferences
    @Override
    public LibraryPreferences getLibraryPreferences() {
        if (libraryPreferences != null) {
            return libraryPreferences;
        }

        LibraryPreferences defaultValues = LibraryPreferences.getDefault();

        libraryPreferences = new LibraryPreferences(
                getBoolean(LIBRARY_BIBLATEX_DEFAULT_MODE, defaultValues.getDefaultBibDatabaseMode() == BibDatabaseMode.BIBLATEX) ? BibDatabaseMode.BIBLATEX : BibDatabaseMode.BIBTEX,
                getBoolean(LIBRARY_REFORMAT_ON_SAVE_AND_EXPORT, defaultValues.shouldAlwaysReformatOnSave()),
                getBoolean(LIBRARY_AUTO_SAVE, defaultValues.shouldAutoSave()),
                getBoolean(LIBRARY_ADD_IMPORTED_ENTRIES, defaultValues.shouldAddImportedEntries()),
                get(LIBRARY_ADD_IMPORTED_ENTRIES_GROUP_NAME, defaultValues.getAddImportedEntriesGroupName()));

        bindBooleanObject(libraryPreferences.defaultBibDatabaseModeProperty(), LIBRARY_BIBLATEX_DEFAULT_MODE, defaultValues.getDefaultBibDatabaseMode(),
                mode -> mode == BibDatabaseMode.BIBLATEX,
                isBiblatex -> isBiblatex ? BibDatabaseMode.BIBLATEX : BibDatabaseMode.BIBTEX);
        bindBoolean(libraryPreferences.alwaysReformatOnSaveProperty(), LIBRARY_REFORMAT_ON_SAVE_AND_EXPORT, defaultValues.shouldAlwaysReformatOnSave());
        bindBoolean(libraryPreferences.autoSaveProperty(), LIBRARY_AUTO_SAVE, defaultValues.shouldAutoSave());
        bindBoolean(libraryPreferences.addImportedEntriesProperty(), LIBRARY_ADD_IMPORTED_ENTRIES, defaultValues.shouldAddImportedEntries());
        bindString(libraryPreferences.addImportedEntriesGroupNameProperty(), LIBRARY_ADD_IMPORTED_ENTRIES_GROUP_NAME, defaultValues.getAddImportedEntriesGroupName());

        return libraryPreferences;
    }
    // endregion

    // region DOIPreferences
    @Override
    public DOIPreferences getDOIPreferences() {
        if (doiPreferences != null) {
            return doiPreferences;
        }

        DOIPreferences defaultValues = DOIPreferences.getDefault();

        doiPreferences = new DOIPreferences(
                getBoolean(DOI_USE_CUSTOM_URI, defaultValues.shouldUseCustom()),
                get(DOI_BASE_URI, defaultValues.getDefaultBaseURI()));

        bindBoolean(doiPreferences.useCustomProperty(), DOI_USE_CUSTOM_URI, defaultValues.shouldUseCustom());
        bindString(doiPreferences.defaultBaseURIProperty(), DOI_BASE_URI, defaultValues.getDefaultBaseURI());

        return doiPreferences;
    }
    // endregion

    // region OwnerPreferences
    @Override
    public OwnerPreferences getOwnerPreferences() {
        if (ownerPreferences != null) {
            return ownerPreferences;
        }

        OwnerPreferences defaultValues = OwnerPreferences.getDefault();

        ownerPreferences = new OwnerPreferences(
                getBoolean(OWNER_ENABLE, defaultValues.shouldUseOwner()),
                get(OWNER_DEFAULT, defaultValues.getDefaultOwner()),
                getBoolean(OWNER_OVERWRITE, defaultValues.shouldOverwriteOwner()));

        bindBoolean(ownerPreferences.useOwnerProperty(), OWNER_ENABLE, defaultValues.shouldUseOwner());
        bindCustom(ownerPreferences.defaultOwnerProperty(), OWNER_DEFAULT, defaultValues.getDefaultOwner(),
                (_, _, newValue) -> {
                    put(OWNER_DEFAULT, newValue);
                    getInternalPreferences().setUserHostInfo(OS.getUserHostInfo(newValue));
                },
                () -> ownerPreferences.defaultOwnerProperty().set(get(OWNER_DEFAULT, defaultValues.getDefaultOwner())),
                () -> ownerPreferences.defaultOwnerProperty().set(defaultValues.getDefaultOwner()));
        bindBoolean(ownerPreferences.overwriteOwnerProperty(), OWNER_OVERWRITE, defaultValues.shouldOverwriteOwner());

        return ownerPreferences;
    }
    // endregion

    // region TimestampPreferences
    @Override
    public TimestampPreferences getTimestampPreferences() {
        if (timestampPreferences != null) {
            return timestampPreferences;
        }

        TimestampPreferences defaultValues = TimestampPreferences.getDefault();

        timestampPreferences = new TimestampPreferences(
                getBoolean(TIMESTAMP_ADD_CREATION_DATE, defaultValues.shouldAddCreationDate()),
                getBoolean(TIMESTAMP_ADD_MODIFICATION_DATE, defaultValues.shouldAddModificationDate()),

                // legacy pre-5.3 fields for library cleanups
                getBoolean(TIMESTAMP_DEPRECATED_UPDATE, defaultValues.shouldUpdateTimestamp()),
                FieldFactory.parseField(get(TIMESTAMP_DEPRECATED_FIELD, defaultValues.getTimestampField().getName())),
                get(TIMESTAMP_DEPRECATED_FORMAT, defaultValues.getTimestampFormat()));

        bindBoolean(timestampPreferences.addCreationDateProperty(), TIMESTAMP_ADD_CREATION_DATE, defaultValues.shouldAddCreationDate());
        bindBoolean(timestampPreferences.addModificationDateProperty(), TIMESTAMP_ADD_MODIFICATION_DATE, defaultValues.shouldAddModificationDate());

        return timestampPreferences;
    }
    // endregion

    // region RemotePreferences
    @Override
    public RemotePreferences getRemotePreferences() {
        if (remotePreferences != null) {
            return remotePreferences;
        }

        RemotePreferences defaultValues = RemotePreferences.getDefault();

        remotePreferences = new RemotePreferences(
                getBoolean(SERVER_REMOTE_ENABLE, defaultValues.shouldEnableRemoteServer()),
                getInt(SERVER_REMOTE_PORT, defaultValues.getRemoteServerPort()),
                getBoolean(SERVER_HTTP_ENABLE, defaultValues.shouldEnableHttpServer()),
                getInt(SERVER_HTTP_PORT, defaultValues.getHttpServerPort()),
                getBoolean(SERVER_LANGUAGE_ENABLE, defaultValues.shouldEnableLanguageServer()),
                getInt(SERVER_LANGUAGE_PORT, defaultValues.getLanguageServerPort()),
                getBoolean(SERVER_DIRECT_HTTP_IMPORT, defaultValues.directHttpImport()));

        bindInt(remotePreferences.remoteServerPortProperty(), SERVER_REMOTE_PORT, defaultValues.getRemoteServerPort());
        bindBoolean(remotePreferences.enableRemoteServerProperty(), SERVER_REMOTE_ENABLE, defaultValues.shouldEnableRemoteServer());
        bindInt(remotePreferences.httpServerPortProperty(), SERVER_HTTP_PORT, defaultValues.getHttpServerPort());
        bindBoolean(remotePreferences.enableHttpServerProperty(), SERVER_HTTP_ENABLE, defaultValues.shouldEnableHttpServer());
        bindInt(remotePreferences.languageServerPortProperty(), SERVER_LANGUAGE_PORT, defaultValues.getLanguageServerPort());
        bindBoolean(remotePreferences.enableLanguageServerProperty(), SERVER_LANGUAGE_ENABLE, defaultValues.shouldEnableLanguageServer());
        bindBoolean(remotePreferences.directHttpImportProperty(), SERVER_DIRECT_HTTP_IMPORT, defaultValues.directHttpImport());

        return remotePreferences;
    }
    // endregion

    // region ProxyPreferences
    @Override
    public ProxyPreferences getProxyPreferences() {
        if (proxyPreferences != null) {
            return proxyPreferences;
        }

        ProxyPreferences defaultValues = ProxyPreferences.getDefault();
        boolean persistPassword = getBoolean(PROXY_PERSIST_PASSWORD, defaultValues.shouldPersistPassword());

        proxyPreferences = new ProxyPreferences(
                getBoolean(PROXY_USE, defaultValues.shouldUseProxy()),
                get(PROXY_HOSTNAME, defaultValues.getHostname()),
                get(PROXY_PORT, defaultValues.getPort()),
                getBoolean(PROXY_USE_AUTHENTICATION, defaultValues.shouldUseAuthentication()),
                get(PROXY_USERNAME, defaultValues.getUsername()),
                persistPassword ? getProxyPassword().orElse(defaultValues.getPassword()) : defaultValues.getPassword(),
                persistPassword);

        bindBoolean(proxyPreferences.useProxyProperty(), PROXY_USE, defaultValues.shouldUseProxy());
        bindString(proxyPreferences.hostnameProperty(), PROXY_HOSTNAME, defaultValues.getHostname());
        bindString(proxyPreferences.portProperty(), PROXY_PORT, defaultValues.getPort());
        bindBoolean(proxyPreferences.useAuthenticationProperty(), PROXY_USE_AUTHENTICATION, defaultValues.shouldUseAuthentication());
        bindString(proxyPreferences.usernameProperty(), PROXY_USERNAME, defaultValues.getUsername());
        bindToKeyring(proxyPreferences.passwordProperty(), (_, _, newValue) -> setProxyPassword(newValue));
        bindCustom(proxyPreferences.persistPasswordProperty(), PROXY_PERSIST_PASSWORD, defaultValues.shouldPersistPassword(),
                (_, _, newValue) -> {
                    putBoolean(PROXY_PERSIST_PASSWORD, newValue);
                    if (!newValue) {
                        deleteProxyPassword();
                    }
                },
                () -> {
                    boolean shouldPersist = getBoolean(PROXY_PERSIST_PASSWORD, defaultValues.shouldPersistPassword());
                    proxyPreferences.persistPasswordProperty().set(shouldPersist);
                    proxyPreferences.passwordProperty().set(
                            shouldPersist ? getProxyPassword().orElse(defaultValues.getPassword()) : defaultValues.getPassword());
                },
                () -> {
                    proxyPreferences.persistPasswordProperty().set(defaultValues.shouldPersistPassword());
                    proxyPreferences.passwordProperty().set(defaultValues.getPassword());
                });

        return proxyPreferences;
    }

    private Optional<String> getProxyPassword() {
        try (final Keyring keyring = Keyring.create()) {
            return Optional.of(new Password(
                    keyring.getPassword("org.jabref", "proxy"),
                    getInternalPreferences().getUserHostInfo().getUserHostString())
                    .decrypt());
        } catch (PasswordAccessException ex) {
            LOGGER.warn("JabRef uses proxy password from key store but no password is stored");
        } catch (Exception ex) {
            LOGGER.warn("JabRef could not open the key store", ex);
        }

        return Optional.empty();
    }

    private void setProxyPassword(String password) {
        if (getProxyPreferences().shouldPersistPassword()) {
            try (final Keyring keyring = Keyring.create()) {
                if (StringUtil.isBlank(password)) {
                    keyring.deletePassword("org.jabref", "proxy");
                } else {
                    keyring.setPassword("org.jabref", "proxy", new Password(
                            password.trim(),
                            getInternalPreferences().getUserHostInfo().getUserHostString())
                            .encrypt());
                }
            } catch (Exception ex) {
                LOGGER.warn("Unable to open key store", ex);
            }
        }
    }

    private static void deleteProxyPassword() {
        try (final Keyring keyring = Keyring.create()) {
            keyring.deletePassword("org.jabref", "proxy");
        } catch (Exception ex) {
            LOGGER.warn("Unable to remove proxy credentials");
        }
    }
    // endregion

    // region SSLPreferences
    @Override
    public SSLPreferences getSSLPreferences() {
        if (sslPreferences != null) {
            return sslPreferences;
        }

        sslPreferences = getSSLPreferencesFromBackingStore(SSLPreferences.getDefault());

        return sslPreferences;
    }

    private SSLPreferences getSSLPreferencesFromBackingStore(SSLPreferences defaults) {
        return new SSLPreferences(
                Path.of(get(SSL_TRUSTSTORE_PATH, defaults.getTruststorePath().toString())));
    }
    // endregion

    // region CitationKeyPatternPreferences
    @Override
    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        if (citationKeyPatternPreferences != null) {
            return citationKeyPatternPreferences;
        }

        citationKeyPatternPreferences = getCitationKeyPatternPreferencesFromBackingStore(CitationKeyPatternPreferences.getDefault());

        EasyBind.listen(citationKeyPatternPreferences.shouldTransliterateFieldsForCitationKeyProperty(),
                (_, _, newValue) -> putBoolean(CITATION_KEY_TRANSLITERATE_FIELDS, newValue));
        EasyBind.listen(citationKeyPatternPreferences.shouldAvoidOverwriteCiteKeyProperty(),
                (_, _, newValue) -> putBoolean(CITATION_KEY_AVOID_OVERWRITING, newValue));
        EasyBind.listen(citationKeyPatternPreferences.shouldWarnBeforeOverwriteCiteKeyProperty(),
                (_, _, newValue) -> putBoolean(CITATION_KEY_WARN_BEFORE_OVERWRITE, newValue));
        EasyBind.listen(citationKeyPatternPreferences.shouldGenerateCiteKeysBeforeSavingProperty(),
                (_, _, newValue) -> putBoolean(CITATION_KEY_GENERATE_BEFORE_SAVING, newValue));
        EasyBind.listen(citationKeyPatternPreferences.keySuffixProperty(), (_, _, newValue) -> {
            putBoolean(CITATION_KEY_GEN_ALWAYS_ADD_LETTER, newValue == CitationKeyPatternPreferences.KeySuffix.ALWAYS);
            putBoolean(CITATION_KEY_GEN_FIRST_LETTER_A, newValue == CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A);
        });
        EasyBind.listen(citationKeyPatternPreferences.keyPatternRegexProperty(),
                (_, _, newValue) -> put(CITATION_KEY_PATTERN_REGEX, newValue));
        EasyBind.listen(citationKeyPatternPreferences.keyPatternReplacementProperty(),
                (_, _, newValue) -> put(CITATION_KEY_PATTERN_REPLACEMENT, newValue));
        EasyBind.listen(citationKeyPatternPreferences.unwantedCharactersProperty(),
                (_, _, newValue) -> put(CITATION_KEY_UNWANTED_CHARACTERS, newValue));
        EasyBind.listen(citationKeyPatternPreferences.keyPatternsProperty(),
                (_, _, newValue) -> storeGlobalCitationKeyPattern(newValue));

        return citationKeyPatternPreferences;
    }

    private @NonNull CitationKeyPatternPreferences getCitationKeyPatternPreferencesFromBackingStore(CitationKeyPatternPreferences defaults) {
        return new CitationKeyPatternPreferences(
                getBoolean(CITATION_KEY_TRANSLITERATE_FIELDS, defaults.shouldTransliterateFieldsForCitationKey()),
                getBoolean(CITATION_KEY_AVOID_OVERWRITING, defaults.shouldAvoidOverwriteCiteKey()),
                getBoolean(CITATION_KEY_WARN_BEFORE_OVERWRITE, defaults.shouldWarnBeforeOverwriteCiteKey()),
                getBoolean(CITATION_KEY_GENERATE_BEFORE_SAVING, defaults.shouldGenerateCiteKeysBeforeSaving()),
                getKeySuffix(defaults),
                get(CITATION_KEY_PATTERN_REGEX, defaults.getKeyPatternRegex()),
                get(CITATION_KEY_PATTERN_REPLACEMENT, defaults.getKeyPatternReplacement()),
                get(CITATION_KEY_UNWANTED_CHARACTERS, defaults.getUnwantedCharacters()),
                getGlobalCitationKeyPattern(defaults),
                getBibEntryPreferences().keywordSeparatorProperty());
    }

    private CitationKeyPatternPreferences.KeySuffix getKeySuffix(CitationKeyPatternPreferences defaults) {
        if (!hasKey(CITATION_KEY_GEN_ALWAYS_ADD_LETTER) && !hasKey(CITATION_KEY_GEN_FIRST_LETTER_A)) {
            return defaults.getKeySuffix();
        }

        boolean alwaysAddLetter = getBoolean(CITATION_KEY_GEN_ALWAYS_ADD_LETTER, false);
        boolean firstLetterA = getBoolean(CITATION_KEY_GEN_FIRST_LETTER_A, false);

        if (alwaysAddLetter && !firstLetterA) {
            return CitationKeyPatternPreferences.KeySuffix.ALWAYS;
        } else if (!alwaysAddLetter && firstLetterA) {
            return CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A;
        } else {
            return CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B;
        }
    }

    private @NonNull GlobalCitationKeyPatterns getGlobalCitationKeyPattern(CitationKeyPatternPreferences defaults) {
        GlobalCitationKeyPatterns citationKeyPattern = GlobalCitationKeyPatterns.fromPattern(
                get(CITATION_KEY_DEFAULT_PATTERN, defaults.getKeyPatterns().getDefaultValue().stringRepresentation()));
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
            put(CITATION_KEY_DEFAULT_PATTERN, "");
        } else {
            put(CITATION_KEY_DEFAULT_PATTERN, pattern.getDefaultValue().stringRepresentation());
        }

        // Store overridden definitions in Preferences.
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
    }
    // endregion

    // region BibEntryPreferences
    @Override
    public BibEntryPreferences getBibEntryPreferences() {
        if (bibEntryPreferences != null) {
            return bibEntryPreferences;
        }

        bibEntryPreferences = getBibEntryPreferencesFromBackingStore(BibEntryPreferences.getDefault());

        EasyBind.listen(bibEntryPreferences.keywordSeparatorProperty(), (_, _, newValue) -> put(KEYWORD_SEPARATOR, String.valueOf(newValue)));

        return bibEntryPreferences;
    }

    private BibEntryPreferences getBibEntryPreferencesFromBackingStore(BibEntryPreferences defaults) {
        return new BibEntryPreferences(
                get(KEYWORD_SEPARATOR, String.valueOf(defaults.getKeywordSeparator())).charAt(0)
        );
    }
    // endregion

    // region InternalPreferences
    @Override
    public InternalPreferences getInternalPreferences() {
        if (internalPreferences != null) {
            return internalPreferences;
        }

        internalPreferences = getInternalPreferencesFromBackingStore(InternalPreferences.getDefault());

        EasyBind.listen(internalPreferences.ignoredVersionProperty(),
                (_, _, newValue) -> put(VERSION_IGNORED_UPDATE, newValue.toString()));
        EasyBind.listen(internalPreferences.versionCheckEnabledProperty(),
                (_, _, newValue) -> putBoolean(VERSION_CHECK_ENABLED, newValue));
        EasyBind.listen(internalPreferences.lastPreferencesExportPathProperty(),
                (_, _, newValue) -> put(PREFS_EXPORT_PATH, newValue.toString()));
        // user is a static value, should only be changed for debugging
        EasyBind.listen(internalPreferences.memoryStickModeProperty(), (_, _, newValue) -> {
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

    private InternalPreferences getInternalPreferencesFromBackingStore(InternalPreferences defaults) {
        return new InternalPreferences(
                Version.parse(get(VERSION_IGNORED_UPDATE, defaults.getIgnoredVersion().toString())),
                getBoolean(VERSION_CHECK_ENABLED, defaults.isVersionCheckEnabled()),
                getPath(PREFS_EXPORT_PATH, defaults.getLastPreferencesExportPath()),
                OS.getUserHostInfo(get(OWNER_DEFAULT, OwnerPreferences.getDefault().getDefaultOwner())),
                getBoolean(MEMORY_STICK_MODE, defaults.isMemoryStickMode())
        );
    }
    // endregion

    // region FieldPreferences
    @Override
    public FieldPreferences getFieldPreferences() {
        if (fieldPreferences != null) {
            return fieldPreferences;
        }

        fieldPreferences = getFieldPreferencesFromBackingStore(FieldPreferences.getDefault());

        EasyBind.listen(fieldPreferences.resolveStringsProperty(), (_, _, newValue) -> putBoolean(DO_NOT_RESOLVE_STRINGS, !newValue));
        fieldPreferences.getResolvableFields().addListener((InvalidationListener) _ ->
                put(RESOLVE_STRINGS_FOR_FIELDS, FieldFactory.serializeFieldsList(fieldPreferences.getResolvableFields())));
        fieldPreferences.getNonWrappableFields().addListener((InvalidationListener) _ ->
                put(NON_WRAPPABLE_FIELDS, FieldFactory.serializeFieldsList(fieldPreferences.getNonWrappableFields())));

        return fieldPreferences;
    }

    private FieldPreferences getFieldPreferencesFromBackingStore(FieldPreferences defaults) {
        return new FieldPreferences(
                !getBoolean(DO_NOT_RESOLVE_STRINGS, !defaults.shouldResolveStrings()),
                List.copyOf(FieldFactory.parseFieldList(get(RESOLVE_STRINGS_FOR_FIELDS,
                        FieldFactory.serializeFieldsList(defaults.getResolvableFields())))),
                List.copyOf(FieldFactory.parseFieldList(get(NON_WRAPPABLE_FIELDS,
                        FieldFactory.serializeFieldsList(defaults.getNonWrappableFields()))))
        );
    }
    // endregion

    // region (Linked)FilePreferences
    @Override
    public FilePreferences getFilePreferences() {
        if (filePreferences != null) {
            return filePreferences;
        }

        filePreferences = getFilePreferencesFromBackingStore(FilePreferences.getDefault());

        EasyBind.listen(filePreferences.mainFileDirectoryProperty(), (_, _, newValue) -> put(FILES_MAIN_DIRECTORY, newValue != null ? newValue.toString() : ""));
        EasyBind.listen(filePreferences.storeFilesRelativeToBibFileProperty(), (_, _, newValue) -> putBoolean(FILES_STORE_RELATIVE_TO_BIB, newValue));
        EasyBind.listen(filePreferences.autoRenameFilesOnChangeProperty(), (_, _, newValue) -> putBoolean(FILES_AUTO_RENAME_ON_CHANGE, newValue));
        EasyBind.listen(filePreferences.fileNamePatternProperty(), (_, _, newValue) -> put(FILES_IMPORT_NAMEPATTERN, newValue));
        EasyBind.listen(filePreferences.fileDirectoryPatternProperty(), (_, _, newValue) -> put(FILES_IMPORT_DIRPATTERN, newValue));
        EasyBind.listen(filePreferences.downloadLinkedFilesProperty(), (_, _, newValue) -> putBoolean(FILES_DOWNLOAD_LINKED, newValue));
        EasyBind.listen(filePreferences.fulltextIndexLinkedFilesProperty(), (_, _, newValue) -> putBoolean(FILES_FULLTEXT_INDEX, newValue));
        EasyBind.listen(filePreferences.workingDirectoryProperty(), (_, _, newValue) -> put(FILES_WORKING_DIRECTORY, newValue.toString()));
        EasyBind.listen(filePreferences.createBackupProperty(), (_, _, newValue) -> putBoolean(BACKUP_ENABLED, newValue));
        EasyBind.listen(filePreferences.backupDirectoryProperty(), (_, _, newValue) -> put(BACKUP_DIRECTORY, newValue.toString()));
        EasyBind.listen(filePreferences.confirmDeleteLinkedFileProperty(), (_, _, newValue) -> putBoolean(FILES_CONFIRM_DELETE_LINKED, newValue));
        EasyBind.listen(filePreferences.moveToTrashProperty(), (_, _, newValue) -> putBoolean(FILES_TRASH_INSTEAD_OF_DELETE, newValue));
        EasyBind.listen(filePreferences.adjustFileLinksOnTransferProperty(), (_, _, newValue) -> putBoolean(FILES_ADJUST_FILE_LINKS_ON_TRANSFER, newValue));
        EasyBind.listen(filePreferences.copyLinkedFilesOnTransferProperty(), (_, _, newValue) -> putBoolean(FILES_COPY_LINKED_FILES_ON_TRANSFER, newValue));
        EasyBind.listen(filePreferences.moveLinkedFilesOnTransferPropertyProperty(), (_, _, newValue) -> putBoolean(FILES_MOVE_LINKED_FILES_ON_TRANSFER, newValue));
        EasyBind.listen(filePreferences.shouldKeepDownloadUrlProperty(), (_, _, newValue) -> putBoolean(FILES_KEEP_DOWNLOAD_URL, newValue));
        EasyBind.listen(filePreferences.lastUsedDirectoryProperty(), (_, _, newValue) -> put(FILES_LAST_USED_DIRECTORY, newValue.toString()));
        EasyBind.listen(filePreferences.openFileExplorerInFileDirectoryProperty(), (_, _, newValue) -> putBoolean(FILES_OPEN_FILE_EXPLORER_IN_FILE_DIRECTORY, newValue));
        EasyBind.listen(filePreferences.openFileExplorerInLastUsedDirectoryProperty(), (_, _, newValue) -> putBoolean(FILES_OPEN_FILE_EXPLORER_IN_LAST_USED_DIRECTORY, newValue));

        return filePreferences;
    }

    private FilePreferences getFilePreferencesFromBackingStore(FilePreferences defaults) {
        return new FilePreferences(
                getInternalPreferences().getUserAndHostInfoProperty(),
                getPath(FILES_MAIN_DIRECTORY, defaults.mainFileDirectoryProperty().get()),
                getBoolean(FILES_STORE_RELATIVE_TO_BIB, defaults.shouldStoreFilesRelativeToBibFile()),
                getBoolean(FILES_AUTO_RENAME_ON_CHANGE, defaults.shouldAutoRenameFilesOnChange()),
                get(FILES_IMPORT_NAMEPATTERN, defaults.getFileNamePattern()),
                get(FILES_IMPORT_DIRPATTERN, defaults.getFileDirectoryPattern()),
                getBoolean(FILES_DOWNLOAD_LINKED, defaults.shouldDownloadLinkedFiles()),
                getBoolean(FILES_FULLTEXT_INDEX, defaults.shouldFulltextIndexLinkedFiles()),
                Path.of(get(FILES_WORKING_DIRECTORY, defaults.getWorkingDirectory().toString())),
                getBoolean(BACKUP_ENABLED, defaults.shouldCreateBackup()),
                // We choose the data directory, because a ".bak" file should survive cache cleanups
                getPath(BACKUP_DIRECTORY, defaults.getBackupDirectory()),
                getBoolean(FILES_CONFIRM_DELETE_LINKED, defaults.confirmDeleteLinkedFile()),
                // We make use of the fallback, because we need AWT being initialized, which is not the case at the constructor JabRefPreferences()
                getBoolean(FILES_TRASH_INSTEAD_OF_DELETE, moveToTrashSupported()),
                getBoolean(FILES_ADJUST_FILE_LINKS_ON_TRANSFER, defaults.shouldAdjustFileLinksOnTransfer()),
                getBoolean(FILES_COPY_LINKED_FILES_ON_TRANSFER, defaults.shouldCopyLinkedFilesOnTransfer()),
                getBoolean(FILES_MOVE_LINKED_FILES_ON_TRANSFER, defaults.shouldMoveLinkedFilesOnTransfer()),
                getBoolean(FILES_KEEP_DOWNLOAD_URL, defaults.shouldKeepDownloadUrl()),
                getPath(FILES_LAST_USED_DIRECTORY, defaults.getLastUsedDirectory()),
                getBoolean(FILES_OPEN_FILE_EXPLORER_IN_FILE_DIRECTORY, defaults.shouldOpenFileExplorerInFileDirectory()),
                getBoolean(FILES_OPEN_FILE_EXPLORER_IN_LAST_USED_DIRECTORY, defaults.shouldOpenFileExplorerInLastUsedDirectory()));
    }

    protected boolean moveToTrashSupported() {
        return false;
    }
    // endregion

    // region AutoLinkPreferences
    @Override
    public AutoLinkPreferences getAutoLinkPreferences() {
        if (autoLinkPreferences != null) {
            return autoLinkPreferences;
        }

        autoLinkPreferences = getAutoLinkPreferencesFromBackingStore(AutoLinkPreferences.getDefault());

        EasyBind.listen(autoLinkPreferences.citationKeyDependencyProperty(), (_, _, newValue) -> {
            // Starts bibtex only omitted, as it is not being saved
            putBoolean(AUTOLINK_EXACT_KEY_ONLY, newValue == AutoLinkPreferences.CitationKeyDependency.EXACT);
            putBoolean(AUTOLINK_USE_REG_EXP_SEARCH_KEY, newValue == AutoLinkPreferences.CitationKeyDependency.REGEX);
        });
        EasyBind.listen(autoLinkPreferences.askAutoNamingPdfsProperty(),
                (_, _, newValue) -> putBoolean(ASK_AUTO_NAMING_PDFS_AGAIN, newValue));
        EasyBind.listen(autoLinkPreferences.regularExpressionProperty(),
                (_, _, newValue) -> put(AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, newValue));

        return autoLinkPreferences;
    }

    private AutoLinkPreferences getAutoLinkPreferencesFromBackingStore(AutoLinkPreferences defaults) {
        return new AutoLinkPreferences(
                getAutoLinkKeyDependency(defaults.getCitationKeyDependency()),
                get(AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, defaults.getRegularExpression()),
                getBoolean(ASK_AUTO_NAMING_PDFS_AGAIN, defaults.shouldAskAutoNamingPdfs()),
                bibEntryPreferences.keywordSeparatorProperty());
    }

    private AutoLinkPreferences.CitationKeyDependency getAutoLinkKeyDependency(AutoLinkPreferences.CitationKeyDependency defaultDependency) {
        if (getBoolean(AUTOLINK_EXACT_KEY_ONLY, defaultDependency == AutoLinkPreferences.CitationKeyDependency.EXACT)) {
            return AutoLinkPreferences.CitationKeyDependency.EXACT;
        } else if (getBoolean(AUTOLINK_USE_REG_EXP_SEARCH_KEY, defaultDependency == AutoLinkPreferences.CitationKeyDependency.REGEX)) {
            return AutoLinkPreferences.CitationKeyDependency.REGEX;
        }
        return AutoLinkPreferences.CitationKeyDependency.START;
    }
    // endregion

    // region ExportPreferences
    @Override
    public ExportPreferences getExportPreferences() {
        if (exportPreferences != null) {
            return exportPreferences;
        }

        exportPreferences = getExportPreferencesFromBackingStore(ExportPreferences.getDefault());

        EasyBind.listen(exportPreferences.lastExportExtensionProperty(), (_, _, newValue) -> put(LAST_USED_EXPORT, newValue));
        EasyBind.listen(exportPreferences.exportWorkingDirectoryProperty(), (_, _, newValue) -> put(EXPORT_WORKING_DIRECTORY, newValue.toString()));
        EasyBind.listen(exportPreferences.exportSaveOrderProperty(), (_, _, newValue) -> storeExportSaveOrder(newValue));
        exportPreferences.getCustomExporters().addListener((InvalidationListener) _ -> storeCustomExportFormats(exportPreferences.getCustomExporters()));

        return exportPreferences;
    }

    private ExportPreferences getExportPreferencesFromBackingStore(ExportPreferences defaults) {
        return new ExportPreferences(
                get(LAST_USED_EXPORT, defaults.getLastExportExtension()),
                Path.of(get(EXPORT_WORKING_DIRECTORY, defaults.getExportWorkingDirectory().toString())),
                getExportSaveOrder(defaults.getExportSaveOrder()),
                getCustomExportFormats(defaults.getCustomExporters()));
    }

    protected SaveOrder getExportSaveOrder(SaveOrder defaults) {
        List<SaveOrder.SortCriterion> defaultCriteria = defaults.getSortCriteria();
        List<SaveOrder.SortCriterion> sortCriteria = new ArrayList<>();

        // OpenRewrite requires strange rewritings, ideally should follow the pattern for SECONDARY and TERTIARY
        String defaultPrimaryField = defaultCriteria.isEmpty() ? "" : defaultCriteria.getFirst().field().getName();
        boolean defaultPrimaryDesc = !defaultCriteria.isEmpty() && defaultCriteria.getFirst().descending();
        String primaryField = get(EXPORT_PRIMARY_SORT_FIELD, defaultPrimaryField);
        if (!"".equals(primaryField)) {
            sortCriteria.add(new SaveOrder.SortCriterion(FieldFactory.parseField(primaryField), getBoolean(EXPORT_PRIMARY_SORT_DESCENDING, defaultPrimaryDesc)));
        }

        String defaultSecondaryField = defaultCriteria.size() >= 2 ? defaultCriteria.get(1).field().getName() : "";
        boolean defaultSecondaryDesc = defaultCriteria.size() >= 2 && defaultCriteria.get(1).descending();
        String secondaryField = get(EXPORT_SECONDARY_SORT_FIELD, defaultSecondaryField);
        if (!"".equals(secondaryField)) {
            sortCriteria.add(new SaveOrder.SortCriterion(FieldFactory.parseField(secondaryField), getBoolean(EXPORT_SECONDARY_SORT_DESCENDING, defaultSecondaryDesc)));
        }

        String defaultTertiaryField = defaultCriteria.size() >= 3 ? defaultCriteria.get(2).field().getName() : "";
        boolean defaultTertiaryDesc = defaultCriteria.size() >= 3 && defaultCriteria.get(2).descending();
        String tertiaryField = get(EXPORT_TERTIARY_SORT_FIELD, defaultTertiaryField);
        if (!"".equals(tertiaryField)) {
            sortCriteria.add(new SaveOrder.SortCriterion(FieldFactory.parseField(tertiaryField), getBoolean(EXPORT_TERTIARY_SORT_DESCENDING, defaultTertiaryDesc)));
        }

        return new SaveOrder(
                SaveOrder.OrderType.fromBooleans(
                        getBoolean(EXPORT_IN_SPECIFIED_ORDER, defaults.getOrderType() == SaveOrder.OrderType.SPECIFIED),
                        getBoolean(EXPORT_IN_ORIGINAL_ORDER, defaults.getOrderType() == SaveOrder.OrderType.ORIGINAL)),
                sortCriteria
        );
    }

    private void storeExportSaveOrder(SaveOrder saveOrder) {
        putBoolean(EXPORT_IN_ORIGINAL_ORDER, saveOrder.getOrderType() == SaveOrder.OrderType.ORIGINAL);
        putBoolean(EXPORT_IN_SPECIFIED_ORDER, saveOrder.getOrderType() == SaveOrder.OrderType.SPECIFIED);

        long saveOrderCount = saveOrder.getSortCriteria().size();
        if (saveOrderCount >= 1) {
            put(EXPORT_PRIMARY_SORT_FIELD, saveOrder.getSortCriteria().getFirst().field().getName());
            putBoolean(EXPORT_PRIMARY_SORT_DESCENDING, saveOrder.getSortCriteria().getFirst().descending());
        } else {
            put(EXPORT_PRIMARY_SORT_FIELD, "");
            putBoolean(EXPORT_PRIMARY_SORT_DESCENDING, false);
        }
        if (saveOrderCount >= 2) {
            put(EXPORT_SECONDARY_SORT_FIELD, saveOrder.getSortCriteria().get(1).field().getName());
            putBoolean(EXPORT_SECONDARY_SORT_DESCENDING, saveOrder.getSortCriteria().get(1).descending());
        } else {
            put(EXPORT_SECONDARY_SORT_FIELD, "");
            putBoolean(EXPORT_SECONDARY_SORT_DESCENDING, false);
        }
        if (saveOrderCount >= 3) {
            put(EXPORT_TERTIARY_SORT_FIELD, saveOrder.getSortCriteria().get(2).field().getName());
            putBoolean(EXPORT_TERTIARY_SORT_DESCENDING, saveOrder.getSortCriteria().get(2).descending());
        } else {
            put(EXPORT_TERTIARY_SORT_FIELD, "");
            putBoolean(EXPORT_TERTIARY_SORT_DESCENDING, false);
        }
    }

    @Override
    public SelfContainedSaveConfiguration getSelfContainedExportConfiguration() {
        SaveOrder exportSaveOrder = getExportSaveOrder(ExportPreferences.getDefault().getExportSaveOrder());
        SelfContainedSaveOrder saveOrder = switch (exportSaveOrder.getOrderType()) {
            case TABLE -> {
                LOGGER.warn("Table sort order requested, but JabRef is in CLI mode. Falling back to default save order");
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

    private List<TemplateExporter> getCustomExportFormats(List<TemplateExporter> defaults) {
        LayoutFormatterPreferences layoutPreferences = getLayoutFormatterPreferences();
        SelfContainedSaveConfiguration saveConfiguration = getSelfContainedExportConfiguration();
        List<TemplateExporter> formatters = new ArrayList<>();

        List<String> rawFormats = getSeries(CUSTOM_EXPORT_FORMAT);
        if (rawFormats.isEmpty()) {
            return defaults;
        }

        for (String format : rawFormats) {
            List<String> formatData = convertStringToList(format);
            TemplateExporter exporter = new TemplateExporter(
                    formatData.get(EXPORTER_NAME_INDEX),
                    formatData.get(EXPORTER_FILENAME_INDEX),
                    formatData.get(EXPORTER_EXTENSION_INDEX),
                    layoutPreferences,
                    saveConfiguration.getSelfContainedSaveOrder());
            exporter.setCustomExport(true);
            formatters.add(exporter);
        }
        return formatters;
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
    // endregion

    // region CleanupPreferences
    @Override
    public CleanupPreferences getCleanupPreferences() {
        if (cleanupPreferences != null) {
            return cleanupPreferences;
        }

        cleanupPreferences = getCleanupPreferencesFromBackingStore(CleanupPreferences.getDefault());

        cleanupPreferences.getObservableActiveJobs().addListener((SetChangeListener<CleanupPreferences.CleanupStep>) _ -> {
            if (cleanupPreferences.getActiveJobs().isEmpty()) {
                remove(CLEANUP_JOBS);
            } else {
                putStringList(CLEANUP_JOBS, cleanupPreferences.getActiveJobs().stream().map(Enum::name).collect(Collectors.toList()));
            }
        });

        EasyBind.listen(cleanupPreferences.fieldFormatterCleanupsProperty(), (_, _, newValue) -> {
            putBoolean(CLEANUP_FIELD_FORMATTERS_ENABLED, newValue.isEnabled());
            put(CLEANUP_FIELD_FORMATTERS, FieldFormatterCleanupActions.getMetaDataString(newValue.getConfiguredActions(), OS.NEWLINE));
        });

        return cleanupPreferences;
    }

    private CleanupPreferences getCleanupPreferencesFromBackingStore(CleanupPreferences defaults) {
        EnumSet<CleanupPreferences.CleanupStep> activeJobs;
        if (hasKey(CLEANUP_JOBS)) {
            Set<CleanupPreferences.CleanupStep> parsed = getStringList(CLEANUP_JOBS).stream()
                                                                                    .map(CleanupPreferences.CleanupStep::safeValueOf)
                                                                                    .flatMap(Optional::stream)
                                                                                    .collect(Collectors.toSet());
            activeJobs = parsed.isEmpty() ? EnumSet.noneOf(CleanupPreferences.CleanupStep.class) : EnumSet.copyOf(parsed);
        } else {
            activeJobs = EnumSet.copyOf(defaults.getActiveJobs());
        }

        FieldFormatterCleanupActions actions = new FieldFormatterCleanupActions(
                getBoolean(CLEANUP_FIELD_FORMATTERS_ENABLED, defaults.getFieldFormatterCleanups().isEnabled()),
                FieldFormatterCleanupMapper.parseActions(
                        StringUtil.unifyLineBreaks(get(
                                        CLEANUP_FIELD_FORMATTERS,
                                        FieldFormatterCleanupActions.getMetaDataString(defaults.getFieldFormatterCleanups()
                                                                                               .getConfiguredActions(),
                                                OS.NEWLINE)),
                                ""))
        );

        return new CleanupPreferences(activeJobs, actions);
    }
    // endregion

    // region LastFilesOpenedPreferences
    @Override
    public LastFilesOpenedPreferences getLastFilesOpenedPreferences() {
        if (lastFilesOpenedPreferences != null) {
            return lastFilesOpenedPreferences;
        }

        lastFilesOpenedPreferences = getLastFilesOpenedPreferencesFromBackingStore(LastFilesOpenedPreferences.getDefault());

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
        EasyBind.listen(lastFilesOpenedPreferences.lastFocusedFileProperty(), (_, _, newValue) -> {
            if (newValue != null && !newValue.toString().isBlank()) {
                put(LAST_FOCUSED, newValue.toAbsolutePath().toString());
            } else {
                remove(LAST_FOCUSED);
            }
        });
        lastFilesOpenedPreferences.getFileHistory().addListener((InvalidationListener) _ -> storeFileHistory(lastFilesOpenedPreferences.getFileHistory()));

        return lastFilesOpenedPreferences;
    }

    private LastFilesOpenedPreferences getLastFilesOpenedPreferencesFromBackingStore(LastFilesOpenedPreferences defaults) {
        List<Path> lastFilesOpened;
        if (hasKey(LAST_EDITED)) {
            lastFilesOpened = convertStringToList(get(LAST_EDITED, "")).stream()
                                                                       .map(Path::of)
                                                                       .toList();
        } else {
            lastFilesOpened = defaults.getLastFilesOpened();
        }

        Path lastFocused = null;
        if (hasKey(LAST_FOCUSED)) {
            String stored = get(LAST_FOCUSED, null);
            if (StringUtil.isNotBlank(stored)) {
                lastFocused = Path.of(stored);
            }
        } else {
            lastFocused = defaults.getLastFocusedFile();
        }

        return new LastFilesOpenedPreferences(
                lastFilesOpened,
                lastFocused,
                getFileHistory(defaults.getFileHistory()));
    }

    private FileHistory getFileHistory(FileHistory defaults) {
        if (hasKey(RECENT_DATABASES)) {
            return FileHistory.of(convertStringToList(get(RECENT_DATABASES, "")).stream()
                                                                                .map(Path::of)
                                                                                .toList());
        } else {
            return defaults;
        }
    }

    private void storeFileHistory(FileHistory history) {
        putStringList(RECENT_DATABASES, history.stream()
                                               .map(Path::toString)
                                               .toList());
    }
    // endregion

    // region AiPreferences
    @Override
    public AiPreferences getAiPreferences() {
        if (aiPreferences != null) {
            return aiPreferences;
        }

        aiPreferences = getAiPreferencesFromBackingStore(AiPreferences.getDefault());

        EasyBind.listen(aiPreferences.aiFeaturesEnabledCurrentlyProperty(), (_, _, newValue) -> putBoolean(AI_ENABLED, newValue));
        EasyBind.listen(aiPreferences.autoGenerateEmbeddingsProperty(), (_, _, newValue) -> putBoolean(AI_AUTO_GENERATE_EMBEDDINGS, newValue));
        EasyBind.listen(aiPreferences.autoGenerateSummariesProperty(), (_, _, newValue) -> putBoolean(AI_AUTO_GENERATE_SUMMARIES, newValue));
        EasyBind.listen(aiPreferences.generateFollowUpQuestionsProperty(), (_, _, newValue) -> putBoolean(AI_GENERATE_FOLLOW_UP_QUESTIONS, newValue));
        EasyBind.listen(aiPreferences.followUpQuestionsCountProperty(), (_, _, newValue) -> putInt(AI_FOLLOW_UP_QUESTIONS_COUNT, newValue));

        EasyBind.listen(aiPreferences.aiProviderProperty(), (_, _, newValue) -> put(AI_PROVIDER, newValue.name()));

        EasyBind.listen(aiPreferences.openAiChatModelProperty(), (_, _, newValue) -> put(AI_OPEN_AI_CHAT_MODEL, newValue));
        EasyBind.listen(aiPreferences.mistralAiChatModelProperty(), (_, _, newValue) -> put(AI_MISTRAL_AI_CHAT_MODEL, newValue));
        EasyBind.listen(aiPreferences.geminiChatModelProperty(), (_, _, newValue) -> put(AI_GEMINI_CHAT_MODEL, newValue));
        EasyBind.listen(aiPreferences.huggingFaceChatModelProperty(), (_, _, newValue) -> put(AI_HUGGING_FACE_CHAT_MODEL, newValue));

        EasyBind.listen(aiPreferences.customizeExpertSettingsProperty(), (_, _, newValue) -> putBoolean(AI_CUSTOMIZE_SETTINGS, newValue));

        EasyBind.listen(aiPreferences.openAiApiBaseUrlProperty(), (_, _, newValue) -> put(AI_OPEN_AI_API_BASE_URL, newValue));
        EasyBind.listen(aiPreferences.mistralAiApiBaseUrlProperty(), (_, _, newValue) -> put(AI_MISTRAL_AI_API_BASE_URL, newValue));
        EasyBind.listen(aiPreferences.geminiApiBaseUrlProperty(), (_, _, newValue) -> put(AI_GEMINI_API_BASE_URL, newValue));
        EasyBind.listen(aiPreferences.huggingFaceApiBaseUrlProperty(), (_, _, newValue) -> put(AI_HUGGING_FACE_API_BASE_URL, newValue));

        EasyBind.listen(aiPreferences.summarizatorKindProperty(), (_, _, newValue) -> put(AI_SUMMARIZATOR_KIND, newValue.name()));
        EasyBind.listen(aiPreferences.tokenEstimatorKindProperty(), (_, _, newValue) -> put(AI_TOKEN_ESTIMATOR_KIND, newValue.name()));
        EasyBind.listen(aiPreferences.embeddingModelProperty(), (_, _, newValue) -> put(AI_EMBEDDING_MODEL, newValue.name()));
        EasyBind.listen(aiPreferences.temperatureProperty(), (_, _, newValue) -> putDouble(AI_TEMPERATURE, newValue.doubleValue()));
        EasyBind.listen(aiPreferences.contextWindowSizeProperty(), (_, _, newValue) -> putInt(AI_CONTEXT_WINDOW_SIZE, newValue));

        EasyBind.listen(aiPreferences.documentSplitterKindProperty(), (_, _, newValue) -> put(AI_DOCUMENT_SPLITTER_KIND, newValue.name()));
        EasyBind.listen(aiPreferences.documentSplitterChunkSizeProperty(), (_, _, newValue) -> putInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, newValue));
        EasyBind.listen(aiPreferences.documentSplitterOverlapSizeProperty(), (_, _, newValue) -> putInt(AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, newValue));

        EasyBind.listen(aiPreferences.answerEngineKindProperty(), (_, _, newValue) -> put(AI_ANSWER_ENGINE_KIND, newValue.name()));
        EasyBind.listen(aiPreferences.ragMaxResultsCountProperty(), (_, _, newValue) -> putInt(AI_RAG_MAX_RESULTS_COUNT, newValue));
        EasyBind.listen(aiPreferences.ragMinScoreProperty(), (_, _, newValue) -> putDouble(AI_RAG_MIN_SCORE, newValue.doubleValue()));

        EasyBind.listen(aiPreferences.chattingSystemMessageTemplateProperty(), (_, _, newValue) -> put(AI_CHATTING_SYSTEM_MESSAGE_TEMPLATE, newValue));
        EasyBind.listen(aiPreferences.chattingUserMessageTemplateProperty(), (_, _, newValue) -> put(AI_CHATTING_USER_MESSAGE_TEMPLATE, newValue));
        EasyBind.listen(aiPreferences.summarizationChunkSystemMessageTemplateProperty(), (_, _, newValue) -> put(AI_SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE, newValue));
        EasyBind.listen(aiPreferences.summarizationCombineSystemMessageTemplateProperty(), (_, _, newValue) -> put(AI_SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE, newValue));
        EasyBind.listen(aiPreferences.summarizationFullDocumentSystemMessageTemplateProperty(), (_, _, newValue) -> put(AI_SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE, newValue));
        EasyBind.listen(aiPreferences.citationParsingSystemMessageTemplateProperty(), (_, _, newValue) -> put(AI_CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE, newValue));
        EasyBind.listen(aiPreferences.markdownChatExportTemplateProperty(), (_, _, newValue) -> put(AI_MARKDOWN_CHAT_EXPORT_TEMPLATE, newValue));

        EasyBind.listen(aiPreferences.generateFollowUpQuestionsProperty(), (_, _, newValue) -> putBoolean(AI_GENERATE_FOLLOW_UP_QUESTIONS, newValue));
        EasyBind.listen(aiPreferences.followUpQuestionsCountProperty(), (_, _, newValue) -> putInt(AI_FOLLOW_UP_QUESTIONS_COUNT, newValue));
        EasyBind.listen(aiPreferences.followUpQuestionsTemplateProperty(), (_, _, newValue) -> put(AI_FOLLOW_UP_QUESTIONS_TEMPLATE, newValue));

        return aiPreferences;
    }

    private AiPreferences getAiPreferencesFromBackingStore(AiPreferences defaults) {
        return new AiPreferences(
                getBoolean(AI_ENABLED, defaults.getAiFeaturesEnabled()),
                getBoolean(AI_AUTO_GENERATE_EMBEDDINGS, defaults.getAutoGenerateEmbeddings()),
                getBoolean(AI_AUTO_GENERATE_SUMMARIES, defaults.getAutoGenerateSummaries()),
                AiProvider.safeValueOf(get(AI_PROVIDER, defaults.getAiProvider().name())),
                get(AI_OPEN_AI_CHAT_MODEL, defaults.getOpenAiChatModel()),
                get(AI_MISTRAL_AI_CHAT_MODEL, defaults.getMistralAiChatModel()),
                get(AI_GEMINI_CHAT_MODEL, defaults.getGeminiChatModel()),
                get(AI_HUGGING_FACE_CHAT_MODEL, defaults.getHuggingFaceChatModel()),
                getBoolean(AI_CUSTOMIZE_SETTINGS, defaults.getCustomizeExpertSettings()),
                get(AI_OPEN_AI_API_BASE_URL, defaults.getOpenAiApiBaseUrl()),
                get(AI_MISTRAL_AI_API_BASE_URL, defaults.getMistralAiApiBaseUrl()),
                get(AI_GEMINI_API_BASE_URL, defaults.getGeminiApiBaseUrl()),
                get(AI_HUGGING_FACE_API_BASE_URL, defaults.getHuggingFaceApiBaseUrl()),
                SummarizatorKind.safeValueOf(get(AI_SUMMARIZATOR_KIND, defaults.getSummarizatorKind().name())),
                TokenEstimatorKind.safeValueOf(get(AI_TOKEN_ESTIMATOR_KIND, defaults.getTokenEstimatorKind().name())),
                PredefinedEmbeddingModel.safeValueOf(get(AI_EMBEDDING_MODEL, defaults.embeddingModelProperty().get().name())),
                getDouble(AI_TEMPERATURE, defaults.temperatureProperty().get()),
                getInt(AI_CONTEXT_WINDOW_SIZE, defaults.contextWindowSizeProperty().get()),
                DocumentSplitterKind.safeValueOf(get(AI_DOCUMENT_SPLITTER_KIND, defaults.getDocumentSplitterKind().name())),
                getInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, defaults.documentSplitterChunkSizeProperty().get()),
                getInt(AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, defaults.documentSplitterOverlapSizeProperty().get()),
                AnswerEngineKind.safeValueOf(get(AI_ANSWER_ENGINE_KIND, defaults.getAnswerEngineKind().name())),
                getInt(AI_RAG_MAX_RESULTS_COUNT, defaults.ragMaxResultsCountProperty().get()),
                getDouble(AI_RAG_MIN_SCORE, defaults.ragMinScoreProperty().get()),
                get(AI_CHATTING_SYSTEM_MESSAGE_TEMPLATE, defaults.getChattingSystemMessageTemplate()),
                get(AI_CHATTING_USER_MESSAGE_TEMPLATE, defaults.getChattingUserMessageTemplate()),
                get(AI_SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE, defaults.getSummarizationChunkSystemMessageTemplate()),
                get(AI_SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE, defaults.getSummarizationCombineSystemMessageTemplate()),
                get(AI_SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE, defaults.getSummarizationFullDocumentSystemMessageTemplate()),
                get(AI_CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE, defaults.getCitationParsingSystemMessageTemplate()),
                get(AI_MARKDOWN_CHAT_EXPORT_TEMPLATE, defaults.getMarkdownChatExportTemplate()),
                getBoolean(AI_GENERATE_FOLLOW_UP_QUESTIONS, defaults.getGenerateFollowUpQuestions()),
                getInt(AI_FOLLOW_UP_QUESTIONS_COUNT, defaults.getFollowUpQuestionsCount()),
                get(AI_FOLLOW_UP_QUESTIONS_TEMPLATE, defaults.getFollowUpQuestionsTemplate())
        );
    }
    // endregion

    // region OCR preferences
    public OcrPreferences getOcrPreferences() {
        if (ocrPreferences != null) {
            return ocrPreferences;
        }

        ocrPreferences = getOcrPreferencesFromBackingStore(OcrPreferences.getDefault());

        EasyBind.listen(ocrPreferences.ocrEnginePathProperty(), (_, _, newValue) -> put(OCR_ENGINE_PATH, newValue));

        return ocrPreferences;
    }

    private OcrPreferences getOcrPreferencesFromBackingStore(OcrPreferences defaults) {
        return new OcrPreferences(
                get(OCR_ENGINE_PATH, defaults.getOcrEnginePath())
        );
    }
    // endregion

    // region SearchPreferences
    @Override
    public SearchPreferences getSearchPreferences() {
        if (searchPreferences != null) {
            return searchPreferences;
        }

        searchPreferences = getSearchPreferencesFromBackingStore(SearchPreferences.getDefault());

        EasyBind.listen(searchPreferences.searchDisplayModeProperty(), (_, _, newValue) -> putBoolean(SEARCH_DISPLAY_MODE, newValue == SearchDisplayMode.FILTER));
        searchPreferences.getObservableSearchFlags().addListener((SetChangeListener<SearchFlags>) _ ->
                putBoolean(SEARCH_FULLTEXT, searchPreferences.getObservableSearchFlags().contains(SearchFlags.FULLTEXT)));
        // Regular expression and case-sensitive search flags should always be set to default values on startup
        EasyBind.listen(searchPreferences.keepSearchStringProperty(), (_, _, newValue) -> putBoolean(SEARCH_KEEP_SEARCH_STRING, newValue));
        EasyBind.listen(searchPreferences.keepWindowOnTopProperty(), (_, _, _) -> putBoolean(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, searchPreferences.shouldKeepWindowOnTop()));
        EasyBind.listen(searchPreferences.getSearchWindowHeightProperty(), (_, _, _) -> putDouble(SEARCH_WINDOW_HEIGHT, searchPreferences.getSearchWindowHeight()));
        EasyBind.listen(searchPreferences.getSearchWindowWidthProperty(), (_, _, _) -> putDouble(SEARCH_WINDOW_WIDTH, searchPreferences.getSearchWindowWidth()));
        EasyBind.listen(searchPreferences.getSearchWindowDividerPositionProperty(), (_, _, _) -> putDouble(SEARCH_WINDOW_DIVIDER_POS, searchPreferences.getSearchWindowDividerPosition()));
        EasyBind.listen(searchPreferences.usePostgresSearchProperty(), (_, _, newValue) -> putBoolean(SEARCH_USE_POSTGRES, newValue));

        return searchPreferences;
    }

    private SearchPreferences getSearchPreferencesFromBackingStore(SearchPreferences defaults) {
        return new SearchPreferences(
                getBoolean(SEARCH_DISPLAY_MODE, defaults.getSearchDisplayMode() == SearchDisplayMode.FILTER) ? SearchDisplayMode.FILTER : SearchDisplayMode.FLOAT,
                getBoolean(SEARCH_REG_EXP, defaults.isRegularExpression()),
                getBoolean(SEARCH_CASE_SENSITIVE, defaults.isCaseSensitive()),
                getBoolean(SEARCH_FULLTEXT, defaults.isFulltext()),
                getBoolean(SEARCH_USE_POSTGRES, defaults.shouldUsePostgresSearch()),
                getBoolean(SEARCH_KEEP_SEARCH_STRING, defaults.shouldKeepSearchString()),
                getBoolean(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, defaults.shouldKeepWindowOnTop()),
                getDouble(SEARCH_WINDOW_HEIGHT, defaults.getSearchWindowHeight()),
                getDouble(SEARCH_WINDOW_WIDTH, defaults.getSearchWindowWidth()),
                getDouble(SEARCH_WINDOW_DIVIDER_POS, defaults.getSearchWindowDividerPosition()));
    }
    // endregion

    // region XmpPreferences
    @Override
    public XmpPreferences getXmpPreferences() {
        if (xmpPreferences != null) {
            return xmpPreferences;
        }

        xmpPreferences = getXmpPreferencesFromBackingStore(XmpPreferences.getDefault());

        EasyBind.listen(xmpPreferences.useXmpPrivacyFilterProperty(),
                (_, _, newValue) -> putBoolean(XMP_USE_PRIVACY_FILTER, newValue));
        xmpPreferences.getXmpPrivacyFilter().addListener((SetChangeListener<Field>) _ ->
                putStringList(XMP_PRIVACY_FILTERS, xmpPreferences.getXmpPrivacyFilter().stream()
                                                                 .map(Field::getName)
                                                                 .collect(Collectors.toList())));

        return xmpPreferences;
    }

    private XmpPreferences getXmpPreferencesFromBackingStore(XmpPreferences defaults) {
        return new XmpPreferences(
                getBoolean(XMP_USE_PRIVACY_FILTER, defaults.shouldUseXmpPrivacyFilter()),
                convertStringToList(get(XMP_PRIVACY_FILTERS,
                        convertListToString(defaults.getXmpPrivacyFilter().stream().map(Field::getName).toList())))
                        .stream()
                        .map(FieldFactory::parseField)
                        .collect(Collectors.toSet()),
                getBibEntryPreferences().keywordSeparatorProperty());
    }
    // endregion

    // region NameFormatterPreferences
    @Override
    public NameFormatterPreferences getNameFormatterPreferences() {
        if (nameFormatterPreferences != null) {
            return nameFormatterPreferences;
        }

        nameFormatterPreferences = getNameFormatterPreferencesFromBackingStore(NameFormatterPreferences.getDefault());

        nameFormatterPreferences.getNameFormatterKey().addListener((InvalidationListener) _ ->
                putStringList(NAME_FORMATER_KEY, nameFormatterPreferences.getNameFormatterKey()));
        nameFormatterPreferences.getNameFormatterValue().addListener((InvalidationListener) _ ->
                putStringList(NAME_FORMATTER_VALUE, nameFormatterPreferences.getNameFormatterValue()));

        return nameFormatterPreferences;
    }

    private NameFormatterPreferences getNameFormatterPreferencesFromBackingStore(NameFormatterPreferences defaults) {
        return new NameFormatterPreferences(
                convertStringToList(get(NAME_FORMATER_KEY, convertListToString(defaults.getNameFormatterKey()))),
                convertStringToList(get(NAME_FORMATTER_VALUE, convertListToString(defaults.getNameFormatterValue()))));
    }
    // endregion

    // region ProtectedTermsPreferences
    @Override
    public ProtectedTermsPreferences getProtectedTermsPreferences() {
        if (protectedTermsPreferences != null) {
            return protectedTermsPreferences;
        }

        protectedTermsPreferences = getProtectedTermsPreferencesFromBackingStore(ProtectedTermsPreferences.getDefault());

        protectedTermsPreferences.getEnabledExternalTermLists().addListener((InvalidationListener) _ ->
                putStringList(PROTECTED_TERMS_ENABLED_EXTERNAL, protectedTermsPreferences.getEnabledExternalTermLists()));
        protectedTermsPreferences.getDisabledExternalTermLists().addListener((InvalidationListener) _ ->
                putStringList(PROTECTED_TERMS_DISABLED_EXTERNAL, protectedTermsPreferences.getDisabledExternalTermLists()));
        protectedTermsPreferences.getEnabledInternalTermLists().addListener((InvalidationListener) _ ->
                putStringList(PROTECTED_TERMS_ENABLED_INTERNAL, protectedTermsPreferences.getEnabledInternalTermLists()));
        protectedTermsPreferences.getDisabledInternalTermLists().addListener((InvalidationListener) _ ->
                putStringList(PROTECTED_TERMS_DISABLED_INTERNAL, protectedTermsPreferences.getDisabledInternalTermLists()));

        return protectedTermsPreferences;
    }

    private ProtectedTermsPreferences getProtectedTermsPreferencesFromBackingStore(ProtectedTermsPreferences defaults) {
        return new ProtectedTermsPreferences(
                convertStringToList(get(PROTECTED_TERMS_ENABLED_INTERNAL, convertListToString(defaults.getEnabledInternalTermLists()))),
                convertStringToList(get(PROTECTED_TERMS_ENABLED_EXTERNAL, convertListToString(defaults.getEnabledExternalTermLists()))),
                convertStringToList(get(PROTECTED_TERMS_DISABLED_INTERNAL, convertListToString(defaults.getDisabledInternalTermLists()))),
                convertStringToList(get(PROTECTED_TERMS_DISABLED_EXTERNAL, convertListToString(defaults.getDisabledExternalTermLists())))
        );
    }
    // endregion

    // region ImporterPreferences
    @Override
    public ImporterPreferences getImporterPreferences() {
        if (importerPreferences != null) {
            return importerPreferences;
        }

        importerPreferences = getImporterPreferencesFromBackingStore(ImporterPreferences.getDefault());

        EasyBind.listen(importerPreferences.importerEnabledProperty(), (_, _, newValue) -> putBoolean(IMPORTER_ENABLED, newValue));
        EasyBind.listen(importerPreferences.generateNewKeyOnImportProperty(), (_, _, newValue) -> putBoolean(IMPORTER_GENERATE_KEY_ON_IMPORT, newValue));
        EasyBind.listen(importerPreferences.importWorkingDirectoryProperty(), (_, _, newValue) -> put(IMPORTER_WORKING_DIRECTORY, newValue.toString()));
        EasyBind.listen(importerPreferences.warnAboutDuplicatesOnImportProperty(), (_, _, newValue) -> putBoolean(IMPORTER_WARN_ABOUT_DUPLICATES, newValue));
        EasyBind.listen(importerPreferences.persistCustomKeysProperty(), (_, _, newValue) -> putBoolean(FETCHER_CUSTOM_KEY_PERSIST, newValue));
        importerPreferences.getApiKeys().addListener((InvalidationListener) _ -> storeFetcherKeys(importerPreferences));
        importerPreferences.getCustomImporters().addListener((InvalidationListener) _ -> storeCustomImportFormats(importerPreferences.getCustomImporters()));
        importerPreferences.getCatalogs().addListener((InvalidationListener) _ -> putStringList(IMPORTER_CATALOGS, importerPreferences.getCatalogs()));
        EasyBind.listen(importerPreferences.defaultPlainCitationParserProperty(), (_, _, newValue) -> put(IMPORTER_DEFAULT_PLAIN_CITATION_PARSER, newValue.name()));
        EasyBind.listen(importerPreferences.citationsRelationsStoreTTLProperty(), (_, _, newValue) -> put(IMPORTER_CITATIONS_RELATIONS_STORE_TTL, newValue.toString()));

        return importerPreferences;
    }

    private ImporterPreferences getImporterPreferencesFromBackingStore(ImporterPreferences defaults) {
        return new ImporterPreferences(
                getBoolean(IMPORTER_ENABLED, defaults.areImporterEnabled()),
                getBoolean(IMPORTER_GENERATE_KEY_ON_IMPORT, defaults.shouldGenerateNewKeyOnImport()),
                Path.of(get(IMPORTER_WORKING_DIRECTORY, defaults.getImportWorkingDirectory().toString())),
                getBoolean(IMPORTER_WARN_ABOUT_DUPLICATES, defaults.shouldWarnAboutDuplicatesOnImport()),
                getCustomImportFormats(defaults.getCustomImporters()),
                getFetcherKeys(defaults.getApiKeys()),
                getBoolean(FETCHER_CUSTOM_KEY_PERSIST, defaults.shouldPersistCustomKeys()),
                hasKey(IMPORTER_CATALOGS) ? getStringList(IMPORTER_CATALOGS) : defaults.getCatalogs(),
                getDefaultPlainCitationParser(defaults.getDefaultPlainCitationParser()),
                getInt(IMPORTER_CITATIONS_RELATIONS_STORE_TTL, defaults.getCitationsRelationsStoreTTL()),
                Map.of()
        );
    }

    private Set<CustomImporter> getCustomImportFormats(Set<CustomImporter> defaults) {
        if (!hasKey(IMPORTER_CUSTOM_FORMAT + "0")) {
            return defaults;
        }

        Set<CustomImporter> importers = new TreeSet<>();

        for (String toImport : getSeries(IMPORTER_CUSTOM_FORMAT)) {
            List<String> importerString = convertStringToList(toImport);
            try {
                if (importerString.size() == 2) {
                    // New format: basePath, className
                    importers.add(new CustomImporter(importerString.getFirst(), importerString.get(1)));
                } else {
                    // Old format: name, cliId, className, basePath
                    importers.add(new CustomImporter(importerString.get(3), importerString.get(2)));
                }
            } catch (ImportException e) {
                LOGGER.warn("Could not load {} from preferences. Will ignore.", importerString.getFirst(), e);
            }
        }

        return importers;
    }

    private void storeCustomImportFormats(Set<CustomImporter> importers) {
        purgeSeries(IMPORTER_CUSTOM_FORMAT, 0);
        CustomImporter[] importersArray = importers.toArray(new CustomImporter[0]);
        for (int i = 0; i < importersArray.length; i++) {
            putStringList(IMPORTER_CUSTOM_FORMAT + i, importersArray[i].getAsStringList());
        }
    }

    private Set<FetcherApiKey> getFetcherKeys(Set<FetcherApiKey> defaults) {
        if (!hasKey(FETCHER_CUSTOM_KEY_NAMES) || !hasKey(FETCHER_CUSTOM_KEY_USES)) {
            return defaults;
        }

        Set<FetcherApiKey> fetcherApiKeys = new HashSet<>();

        List<String> names = getStringList(FETCHER_CUSTOM_KEY_NAMES);
        List<String> uses = getStringList(FETCHER_CUSTOM_KEY_USES);
        List<String> keys = getFetcherKeysFromKeyring(names);

        if (names.size() != uses.size() || names.size() != keys.size()) {
            LOGGER.warn("Could not load fetcher keys from preferences. Will ignore.");
            return defaults;
        }

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
                            getInternalPreferences().getUserHostInfo().getUserHostString())
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

    private void storeFetcherKeys(ImporterPreferences defaults) {
        List<String> names = new ArrayList<>();
        List<String> uses = new ArrayList<>();
        List<String> keys = new ArrayList<>();

        for (FetcherApiKey apiKey : defaults.getApiKeys()) {
            names.add(apiKey.getName());
            uses.add(String.valueOf(apiKey.shouldUse()));
            keys.add(apiKey.getKey());
        }

        putStringList(FETCHER_CUSTOM_KEY_NAMES, names);
        putStringList(FETCHER_CUSTOM_KEY_USES, uses);

        if (defaults.shouldPersistCustomKeys()) {
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
                            getInternalPreferences().getUserHostInfo().getUserHostString())
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

    private PlainCitationParserChoice getDefaultPlainCitationParser(PlainCitationParserChoice defaultPlainCitationParser) {
        try {
            return PlainCitationParserChoice.valueOf(get(IMPORTER_DEFAULT_PLAIN_CITATION_PARSER, defaultPlainCitationParser.name()));
        } catch (IllegalArgumentException ex) {
            return defaultPlainCitationParser;
        }
    }
    // endregion

    // region GrobidPreferences
    @Override
    public GrobidPreferences getGrobidPreferences() {
        if (grobidPreferences != null) {
            return grobidPreferences;
        }

        grobidPreferences = getGrobidPreferencesFromBackingStore(GrobidPreferences.getDefault());

        EasyBind.listen(grobidPreferences.grobidEnabledProperty(), (_, _, newValue) -> putBoolean(GROBID_ENABLED, newValue));
        EasyBind.listen(grobidPreferences.grobidUseAskedProperty(), (_, _, newValue) -> putBoolean(GROBID_PREFERENCE, newValue));
        EasyBind.listen(grobidPreferences.grobidURLProperty(), (_, _, newValue) -> put(GROBID_URL, newValue));

        return grobidPreferences;
    }

    private GrobidPreferences getGrobidPreferencesFromBackingStore(GrobidPreferences defaults) {
        return new GrobidPreferences(
                getBoolean(GROBID_ENABLED, defaults.isGrobidEnabled()),
                getBoolean(GROBID_PREFERENCE, defaults.isGrobidUseAsked()),
                get(GROBID_URL, defaults.getGrobidURL()));
    }
    // endregion

    // region OpenOfficePreferences
    @Override
    public OpenOfficePreferences getOpenOfficePreferences(JournalAbbreviationRepository journalAbbreviationRepository) {
        if (openOfficePreferences != null) {
            return openOfficePreferences;
        }

        openOfficePreferences = getOpenOfficePreferencesFromBackingStore(OpenOfficePreferences.getDefault(), journalAbbreviationRepository);

        EasyBind.listen(openOfficePreferences.executablePathProperty(), (_, _, newValue) -> put(OO_EXECUTABLE_PATH, newValue));
        EasyBind.listen(openOfficePreferences.useAllDatabasesProperty(), (_, _, newValue) -> putBoolean(OO_USE_ALL_OPEN_BASES, newValue));
        EasyBind.listen(openOfficePreferences.alwaysAddCitedOnPagesProperty(), (_, _, newValue) -> putBoolean(OO_ALWAYS_ADD_CITED_ON_PAGES, newValue));
        EasyBind.listen(openOfficePreferences.syncWhenCitingProperty(), (_, _, newValue) -> putBoolean(OO_SYNC_WHEN_CITING, newValue));
        EasyBind.listen(openOfficePreferences.addSpaceAfterProperty(), (_, _, newValue) -> putBoolean(OO_ADD_SPACE_AFTER, newValue));

        openOfficePreferences.getExternalJStyles().addListener((InvalidationListener) _ ->
                putStringList(OO_EXTERNAL_STYLE_FILES, openOfficePreferences.getExternalJStyles()));
        openOfficePreferences.getExternalCslStyles().addListener((InvalidationListener) _ ->
                putStringList(OO_EXTERNAL_CSL_STYLES, openOfficePreferences.getExternalCslStyles()));
        EasyBind.listen(openOfficePreferences.currentJStyleProperty(), (_, _, newValue) -> put(OO_BIBLIOGRAPHY_STYLE_FILE, newValue));
        EasyBind.listen(openOfficePreferences.currentStyleProperty(), (_, _, newValue) -> put(OO_CURRENT_STYLE, newValue.getPath()));

        EasyBind.listen(openOfficePreferences.cslBibliographyTitleProperty(), (_, _, newValue) -> put(OO_CSL_BIBLIOGRAPHY_TITLE, newValue));
        EasyBind.listen(openOfficePreferences.cslBibliographyHeaderFormatProperty(), (_, _, newValue) -> put(OO_CSL_BIBLIOGRAPHY_HEADER_FORMAT, newValue));
        EasyBind.listen(openOfficePreferences.cslBibliographyBodyFormatProperty(), (_, _, newValue) -> put(OO_CSL_BIBLIOGRAPHY_BODY_FORMAT, newValue));

        return openOfficePreferences;
    }

    private OpenOfficePreferences getOpenOfficePreferencesFromBackingStore(OpenOfficePreferences defaults, JournalAbbreviationRepository journalAbbreviationRepository) {
        String currentStylePath = get(OO_CURRENT_STYLE, defaults.getCurrentStyle().getPath());
        OOStyle currentStyle = defaults.getCurrentStyle();

        // Reassign currentStyle based on actual last used CSL style or JStyle
        if (CSLStyleUtils.isCitationStyleFile(currentStylePath)) {
            currentStyle = CSLStyleUtils.createCitationStyleFromFile(currentStylePath)
                                        .orElse(CSLStyleLoader.getDefaultStyle());
        } else if (journalAbbreviationRepository != null) {
            // For now, must be a JStyle. In future, make separate cases for JStyles (.jstyle) and BibTeX (.bst) styles
            try {
                currentStyle = new JStyle(currentStylePath, getLayoutFormatterPreferences(), journalAbbreviationRepository);
            } catch (IOException ex) {
                LOGGER.warn("Could not create JStyle", ex);
            }
        }

        return new OpenOfficePreferences(
                get(OO_EXECUTABLE_PATH, defaults.getExecutablePath()),
                getBoolean(OO_USE_ALL_OPEN_BASES, defaults.getUseAllDatabases()),
                getBoolean(OO_SYNC_WHEN_CITING, defaults.getSyncWhenCiting()),
                getStringList(OO_EXTERNAL_STYLE_FILES),
                get(OO_BIBLIOGRAPHY_STYLE_FILE, defaults.getCurrentJStyle()),
                currentStyle,
                getBoolean(OO_ALWAYS_ADD_CITED_ON_PAGES, defaults.getAlwaysAddCitedOnPages()),
                get(OO_CSL_BIBLIOGRAPHY_TITLE, defaults.getCslBibliographyTitle()),
                get(OO_CSL_BIBLIOGRAPHY_HEADER_FORMAT, defaults.getCslBibliographyHeaderFormat()),
                get(OO_CSL_BIBLIOGRAPHY_BODY_FORMAT, defaults.getCslBibliographyBodyFormat()),
                getStringList(OO_EXTERNAL_CSL_STYLES),
                getBoolean(OO_ADD_SPACE_AFTER, defaults.getAddSpaceAfter()));
    }
    // endregion

    // region GitPreferences
    @Override
    public GitPreferences getGitPreferences() {
        if (gitPreferences != null) {
            return gitPreferences;
        }

        GitPreferences defaultValues = GitPreferences.getDefault();
        boolean rememberPat = getBoolean(GITHUB_REMEMBER_PAT_KEY, defaultValues.getPersistPat());

        gitPreferences = new GitPreferences(
                get(GITHUB_USERNAME_KEY, defaultValues.getUsername()),
                rememberPat ? getGitHubPat().orElse(defaultValues.getPat()) : defaultValues.getPat(),
                get(GITHUB_REMOTE_URL_KEY, defaultValues.getRepositoryUrl()),
                rememberPat);

        bindString(gitPreferences.usernameProperty(), GITHUB_USERNAME_KEY, defaultValues.getUsername());
        bindString(gitPreferences.repositoryUrlProperty(), GITHUB_REMOTE_URL_KEY, defaultValues.getRepositoryUrl());
        bindToKeyring(gitPreferences.patProperty(), (_, _, newVal) -> setGitHubPat(newVal));
        bindCustom(gitPreferences.rememberPatProperty(), GITHUB_REMEMBER_PAT_KEY, defaultValues.getPersistPat(),
                (_, _, newValue) -> {
                    putBoolean(GITHUB_REMEMBER_PAT_KEY, newValue);
                    if (!newValue) {
                        deleteGitHubPat();
                    }
                },
                () -> {
                    boolean shouldRemember = getBoolean(GITHUB_REMEMBER_PAT_KEY, defaultValues.getPersistPat());
                    gitPreferences.rememberPatProperty().set(shouldRemember);
                    gitPreferences.patProperty().set(
                            shouldRemember ? getGitHubPat().orElse(defaultValues.getPat()) : defaultValues.getPat());
                },
                () -> {
                    gitPreferences.rememberPatProperty().set(defaultValues.getPersistPat());
                    gitPreferences.patProperty().set(defaultValues.getPat());
                });

        return gitPreferences;
    }

    private static void deleteGitHubPat() {
        try (final Keyring keyring = Keyring.create()) {
            keyring.deletePassword("org.jabref", "github");
        } catch (Exception ex) {
            LOGGER.warn("Unable to remove GitHub credentials", ex);
        }
    }

    private Optional<String> getGitHubPat() {
        try (final Keyring keyring = Keyring.create()) {
            return Optional.of(new Password(
                    keyring.getPassword("org.jabref", "github"),
                    getInternalPreferences().getUserHostInfo().getUserHostString())
                    .decrypt());
        } catch (PasswordAccessException ex) {
            LOGGER.warn("No GitHub token stored in keyring");
        } catch (Exception ex) {
            LOGGER.warn("Could not read GitHub token from keyring", ex);
        }
        return Optional.empty();
    }

    private void setGitHubPat(String pat) {
        if (getGitPreferences().rememberPatProperty().get()) {
            try (final Keyring keyring = Keyring.create()) {
                if (StringUtil.isBlank(pat)) {
                    keyring.deletePassword("org.jabref", "github");
                } else {
                    keyring.setPassword("org.jabref", "github", new Password(
                            pat.trim(),
                            getInternalPreferences().getUserHostInfo().getUserHostString())
                            .encrypt());
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to save GitHub token to keyring", ex);
            }
        }
    }
    // endregion
}
