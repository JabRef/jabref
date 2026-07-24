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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

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
import org.jabref.logic.ocr.PagesWithTextHandling;
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
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.pipeline.ResponseEngineKind;
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
import org.jspecify.annotations.Nullable;
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
    private static final String AI_RESPONSE_ENGINE_KIND = "aiResponseEngineKind";
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
    private static final String PAGES_WITH_TEXT = "pagesHaveText";
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
    private static final String IMPORTER_WARN_ABOUT_BIB_FILE = "warnAboutBibFileImport";
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

    /// Identifies a single secret in the system keyring by its `service` and `account`.
    private record KeyringSlot(String service, String account) {
        static final KeyringSlot PROXY_PASSWORD = new KeyringSlot("org.jabref", "proxy");
        static final KeyringSlot GITHUB_PAT = new KeyringSlot("org.jabref", "github");

        /// Slot holding a custom API key for the fetcher with the given name.
        static KeyringSlot customApiKey(String fetcherName) {
            return new KeyringSlot("org.jabref.customapikeys", fetcherName);
        }
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

    /// General binding primitive that all scalar `bind*` helpers delegate to. Registers the property in [#allBindings]
    /// and starts persisting its changes.
    ///
    /// @param persistListener writes value changes to the backing store
    /// @param importFromStore loads the stored value (or the default) into the property
    /// @param resetToDefaults restores the property to its default value
    protected <T> void bindCustom(Property<T> property,
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

    protected void bindBoolean(BooleanProperty property, String key, boolean defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> putBoolean(key, v),
                () -> property.set(getBoolean(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    /// Binds a boolean persisted inverted: the backing store holds `!property`. Useful for legacy keys whose stored
    /// meaning is the negation of the property (e.g. a `useDefault…` key backing a `useCustom…` property). The property
    /// (not its stored form) is the binding's reporting value in [#getPreferences()] and [#getDefaults()]. Should not
    /// be used for new preference options.
    protected void bindBooleanInverted(BooleanProperty property, String key, boolean defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> putBoolean(key, !v),
                () -> property.set(!getBoolean(key, !defaultValue)),
                () -> property.set(defaultValue));
    }

    protected void bindInt(IntegerProperty property, String key, int defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> putInt(key, v),
                () -> property.set(getInt(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    protected void bindDouble(DoubleProperty property, String key, double defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> putDouble(key, v.doubleValue()),
                () -> property.set(getDouble(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    protected void bindString(StringProperty property, String key, String defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> put(key, v),
                () -> property.set(get(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    /// Binds an object-valued property persisted as a String.
    ///
    /// @param serializer   converts a value to its stored String form
    /// @param deserializer reconstructs a value from its stored String form
    protected <T> void bindObject(ObjectProperty<T> property,
                                  String key,
                                  T defaultValue,
                                  Function<T, String> serializer,
                                  Function<String, T> deserializer) {
        bindCustom(property, key, defaultValue,
                (_, _, v) -> put(key, serializer.apply(v)),
                () -> property.set(deserializer.apply(get(key, serializer.apply(defaultValue)))),
                () -> property.set(defaultValue));
    }

    /// Binds a path-valued property persisted as a String (null as ""). A blank stored value falls back to the default
    /// (see [#getPath]).
    private void bindPath(ObjectProperty<@Nullable Path> property, String key, @Nullable Path defaultValue) {
        bindCustom(property, key, defaultValue,
                (_, _, newValue) -> put(key, newValue != null ? newValue.toString() : ""),
                () -> property.set(getPath(key, defaultValue)),
                () -> property.set(defaultValue));
    }

    /// Binds an enum-valued property persisted as a set of mutually exclusive boolean keys, one per `flag`. On change,
    /// each flag is stored `true` only for the selected value, so selecting `implicitValue` stores all flags `false`.
    /// Loading and resetting delegate to [#readExclusiveFlags]; the first flag's key is the binding's reporting key
    /// in [#getPreferences()] and [#getDefaults()].
    ///
    /// @param defaultValue  reported as the default, restored on reset, and returned on load when no flag was ever stored
    /// @param implicitValue the value owning no key, encoded as all flags `false`
    /// @param flags         the backing-store key for each non-implicit value, in load lookup order (first match wins)
    @SafeVarargs
    protected final <T> void bindExclusiveFlags(ObjectProperty<T> property, T defaultValue, T implicitValue, Map.Entry<String, T>... flags) {
        bindCustom(property, flags[0].getKey(), defaultValue,
                (_, _, newValue) -> {
                    for (Map.Entry<String, T> flag : flags) {
                        putBoolean(flag.getKey(), newValue == flag.getValue());
                    }
                },
                () -> property.set(readExclusiveFlags(defaultValue, implicitValue, flags)),
                () -> property.set(defaultValue));
    }

    /// Reads an enum persisted as mutually exclusive boolean keys (see [#bindExclusiveFlags]). Returns `defaultValue`
    /// when no flag key was ever stored; otherwise the first flag stored `true` (in `flags` order), or `implicitValue`
    /// when all are `false`. An earlier `true` flag wins, so the non-canonical "multiple true" state resolves to the
    /// first match rather than failing.
    ///
    /// @param defaultValue  returned when none of the flag keys exist
    /// @param implicitValue returned when all flag keys are stored `false`
    /// @param flags         the backing-store key for each non-implicit value, in lookup order (first match wins)
    /// @return the stored enum value, or `defaultValue`/`implicitValue` per the rules above
    @SafeVarargs
    protected final <T> T readExclusiveFlags(T defaultValue, T implicitValue, Map.Entry<String, T>... flags) {
        boolean anyStored = false;
        for (Map.Entry<String, T> flag : flags) {
            if (hasKey(flag.getKey())) {
                anyStored = true;
                break;
            }
        }
        if (!anyStored) {
            return defaultValue;
        }
        for (Map.Entry<String, T> flag : flags) {
            if (getBoolean(flag.getKey(), false)) {
                return flag.getValue();
            }
        }
        return implicitValue;
    }

    /// Binds a map-valued property. Unlike the scalar helpers, persistence is delegated to `serializer`, since entries
    /// may be stored under several backing-store keys.
    ///
    /// @param serializer   persists individual entry changes to the backing store
    /// @param deserializer reads the stored map, falling back to `defaultMap` for missing entries
    protected <K, V> void bindMap(ObservableMap<K, V> map,
                                  String key,
                                  Map<K, V> defaultMap,
                                  MapChangeListener<? super K, ? super V> serializer,
                                  Function<Map<K, V>, Map<K, V>> deserializer) {
        Map<K, V> defaultCopy = Map.copyOf(defaultMap);
        map.addListener(serializer);
        allBindings.add(new PreferenceBinding(
                map,
                defaultCopy,
                key,
                () -> {
                    map.clear();
                    map.putAll(deserializer.apply(defaultCopy));
                },
                () -> {
                    map.clear();
                    map.putAll(defaultCopy);
                }));
    }

    /// Binds an observable list with caller-supplied persistence; the general primitive behind the other
    /// [#bindCustomList]. Entries may be stored under one or several backing-store keys (e.g. a numbered series).
    /// Persistence is wholesale: `persist` runs on every change and rewrites the entire stored representation. Compare
    /// [#bindMap], which persists entries individually under per-entry keys.
    ///
    /// @param key           the binding's reporting key in [#getPreferences()] and [#getDefaults()]
    /// @param defaultList   restored on reset and reported as the default
    /// @param persist       rewrites the whole list to the backing store; receives the bound list, runs on every change
    /// @param loadFromStore reads the stored list, falling back to `defaultList` for absent entries
    protected <T> void bindCustomList(ObservableList<T> list,
                                      String key,
                                      List<T> defaultList,
                                      Consumer<? super ObservableList<T>> persist,
                                      Supplier<? extends Collection<? extends T>> loadFromStore) {
        List<T> defaultCopy = List.copyOf(defaultList);
        list.addListener((InvalidationListener) _ -> persist.accept(list));
        allBindings.add(new PreferenceBinding(
                list,
                defaultCopy,
                key,
                () -> list.setAll(loadFromStore.get()),
                () -> list.setAll(defaultCopy)));
    }

    /// Binds an observable list persisted as a single String. The callbacks are pure transforms; the backing-store
    /// read/write is performed here (delegating to the primitive [#bindCustomList]).
    ///
    /// @param serializer   converts the list to its stored String form
    /// @param deserializer reconstructs the list elements from their stored String form
    private <T> void bindCustomList(ObservableList<T> list,
                                    String key,
                                    List<T> defaultList,
                                    Function<List<T>, String> serializer,
                                    Function<String, ? extends Collection<T>> deserializer) {
        bindCustomList(list, key, defaultList,
                boundList -> put(key, serializer.apply(boundList)),
                () -> deserializer.apply(get(key, serializer.apply(defaultList))));
    }

    /// Binds an observable list of paths persisted as a single delimited String (each path via [Path#toString], read
    /// back via [Path#of]). Paths are stored verbatim; any normalization (e.g. to absolute paths) is the caller's job.
    ///
    /// @param key         the binding's reporting key in [#getPreferences()] and [#getDefaults()]
    /// @param defaultList restored on reset and reported as the default
    protected void bindPathList(ObservableList<Path> list, String key, List<Path> defaultList) {
        bindCustomList(list, key, defaultList,
                paths -> convertListToString(paths.stream().map(Path::toString).toList()),
                stored -> convertStringToList(stored).stream().map(Path::of).toList());
    }

    /// Binds an observable set; the set counterpart of the primitive [#bindCustomList]. Persistence and loading are
    /// delegated to the callbacks, so entries may be stored under one or several backing-store keys. Persistence is
    /// wholesale: `serializer` runs on every change and rewrites the entire stored representation.
    ///
    /// @param key          the binding's reporting key in [#getPreferences()] and [#getDefaults()]
    /// @param defaultSet   restored on reset and reported as the default
    /// @param serializer   rewrites the whole set to the backing store; receives the bound set, runs on every change
    /// @param deserializer reads the stored set, falling back to `defaultSet` for absent entries
    protected <T> void bindSet(ObservableSet<T> set,
                               String key,
                               Set<T> defaultSet,
                               Consumer<? super ObservableSet<T>> serializer,
                               Supplier<? extends Collection<? extends T>> deserializer) {
        Set<T> defaultCopy = Set.copyOf(defaultSet);
        set.addListener((InvalidationListener) _ -> serializer.accept(set));
        allBindings.add(new PreferenceBinding(
                set,
                defaultCopy,
                key,
                () -> {
                    set.clear();
                    set.addAll(deserializer.get());
                },
                () -> {
                    set.clear();
                    set.addAll(defaultCopy);
                }));
    }

    /// Persist-only binding for secrets kept in the system keyring rather than the backing store. Registers no
    /// [PreferenceBinding], so the value is excluded from [#getPreferences()] and [#getDefaults()]; loading and
    /// resetting are owned by the accompanying persist flag, whose binding also clears the keyring when persistence
    /// is turned off.
    ///
    /// @param slot          the keyring location to write to
    /// @param shouldPersist gate evaluated on every change; the value is written only while it returns true
    private void bindToKeyring(StringProperty property, KeyringSlot slot, BooleanSupplier shouldPersist) {
        EasyBind.listen(property, (_, _, newValue) -> {
            if (shouldPersist.getAsBoolean()) {
                writeKeyring(slot, newValue);
            }
        });
    }

    /// Reads the secret at a single slot, or empty if it is absent/blank or the keyring is unavailable.
    private Optional<String> readKeyring(KeyringSlot slot) {
        return Optional.ofNullable(readKeyring(List.of(slot)).get(slot))
                       .filter(StringUtil::isNotBlank);
    }

    /// Reads and decrypts the secrets at the given slots in a single keyring session. On a successful open the result
    /// holds an entry for **every** requested slot — the decrypted secret, or "" when that slot is empty. Returns an
    /// empty map if the keyring cannot be opened, so callers tell failure from "all slots empty" by size
    /// (`result.size() == slots.size()` iff the open succeeded; an empty `slots` is a no-op).
    private Map<KeyringSlot, String> readKeyring(List<KeyringSlot> slots) {
        Map<KeyringSlot, String> result = new HashMap<>();
        try (Keyring keyring = Keyring.create()) {
            for (KeyringSlot slot : slots) {
                try {
                    result.put(slot, new Password(
                            keyring.getPassword(slot.service(), slot.account()),
                            getInternalPreferences().getUserHostInfo().getUserHostString())
                            .decrypt());
                } catch (PasswordAccessException ex) {
                    LOGGER.debug("No secret stored in keyring for {}/{}", slot.service(), slot.account());
                    result.put(slot, "");
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not open keyring", ex);
            return Map.of();
        }
        return result;
    }

    /// Writes (or, for a blank secret, clears) a single slot.
    private void writeKeyring(KeyringSlot slot, String secret) {
        Map<KeyringSlot, String> single = new HashMap<>();
        single.put(slot, secret);
        writeKeyring(single);
    }

    /// Encrypts and writes each secret to its slot in a single keyring session. A blank secret clears its slot.
    private void writeKeyring(Map<KeyringSlot, String> secrets) {
        try (Keyring keyring = Keyring.create()) {
            for (Map.Entry<KeyringSlot, String> entry : secrets.entrySet()) {
                KeyringSlot slot = entry.getKey();
                if (StringUtil.isBlank(entry.getValue())) {
                    try {
                        keyring.deletePassword(slot.service(), slot.account());
                    } catch (PasswordAccessException ex) {
                        // already absent, nothing to clear
                    }
                } else {
                    keyring.setPassword(slot.service(), slot.account(), new Password(
                            entry.getValue().trim(),
                            getInternalPreferences().getUserHostInfo().getUserHostString())
                            .encrypt());
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not open keyring", ex);
        }
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
        } else if (observable instanceof DoubleProperty doubleProperty) {
            return doubleProperty.get();
        } else if (observable instanceof StringProperty stringProperty) {
            return stringProperty.get();
        } else if (observable instanceof ObservableList<?> observableList) {
            return observableList;
        } else if (observable instanceof ObservableSet<?> observableSet) {
            return observableSet;
        } else if (observable instanceof ObservableMap<?, ?> observableMap) {
            return observableMap;
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

        initializeAll();

        allBindings.forEach(binding -> binding.resetToDefaults().run());
    }

    /// Imports Preferences from an XML file.
    ///
    /// @param path Path of file to import from
    /// @throws JabRefException thrown if importing the preferences failed due to an InvalidPreferencesFormatException or an IOException
    @Override
    public void importPreferences(Path path) throws JabRefException {
        importPreferencesToBackingStore(path);

        initializeAll();

        allBindings.forEach(binding -> binding.importFromStore().run());
    }

    /// Instantiates every preference group so its bindings are registered in [#allBindings] before a bulk reset/import
    /// runs. Overridden by subclasses to additionally register their own preference groups.
    protected void initializeAll() {
        getInternalPreferences();
        getFieldPreferences();
        getFilePreferences();
        getProxyPreferences();
        getLibraryPreferences();
        getDOIPreferences();
        getOwnerPreferences();
        getTimestampPreferences();
        getRemotePreferences();
        getPushToApplicationPreferences();
        getAbbreviationPreferences();
        getGitPreferences();
        getSSLPreferences();
        getBibEntryPreferences();
        getCitationKeyPatternPreferences();
        getAutoLinkPreferences();
        getExportPreferences();
        getCleanupPreferences();
        getLastFilesOpenedPreferences();
        getAiPreferences();
        getOcrPreferences();
        getSearchPreferences();
        getXmpPreferences();
        getProtectedTermsPreferences();
        getNameFormatterPreferences();
        getImporterPreferences();
        getGrobidPreferences();
        getOpenOfficePreferences(JournalAbbreviationLoader.loadRepository(getAbbreviationPreferences()));
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

        bindCustomList(abbreviationPreferences.getExternalJournalLists(), EXTERNAL_JOURNAL_LISTS, defaultValues.getExternalJournalLists(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
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
        this.<String, String>bindMap(pushToApplicationPreferences.getCommandPaths(), PUSH_APPLICATIONS_PATHS_KEY, defaultValues.getCommandPaths(),
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

        // defaultBibDatabaseMode is persisted as a single boolean: BIBLATEX owns the key, BIBTEX is the implicit (false) value.
        bindExclusiveFlags(libraryPreferences.defaultBibDatabaseModeProperty(),
                defaultValues.getDefaultBibDatabaseMode(),
                BibDatabaseMode.BIBTEX,
                Map.entry(LIBRARY_BIBLATEX_DEFAULT_MODE, BibDatabaseMode.BIBLATEX));
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
                () -> {
                    String owner = get(OWNER_DEFAULT, defaultValues.getDefaultOwner());
                    ownerPreferences.defaultOwnerProperty().set(owner);
                    getInternalPreferences().setUserHostInfo(OS.getUserHostInfo(owner));
                },
                () -> {
                    ownerPreferences.defaultOwnerProperty().set(defaultValues.getDefaultOwner());
                    getInternalPreferences().setUserHostInfo(OS.getUserHostInfo(defaultValues.getDefaultOwner()));
                });
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
                persistPassword ? readKeyring(KeyringSlot.PROXY_PASSWORD).orElse(defaultValues.getPassword())
                                : defaultValues.getPassword(),
                persistPassword);

        bindBoolean(proxyPreferences.useProxyProperty(), PROXY_USE, defaultValues.shouldUseProxy());
        bindString(proxyPreferences.hostnameProperty(), PROXY_HOSTNAME, defaultValues.getHostname());
        bindString(proxyPreferences.portProperty(), PROXY_PORT, defaultValues.getPort());
        bindBoolean(proxyPreferences.useAuthenticationProperty(), PROXY_USE_AUTHENTICATION, defaultValues.shouldUseAuthentication());
        bindString(proxyPreferences.usernameProperty(), PROXY_USERNAME, defaultValues.getUsername());
        bindToKeyring(proxyPreferences.passwordProperty(), KeyringSlot.PROXY_PASSWORD, proxyPreferences::shouldPersistPassword);
        bindCustom(proxyPreferences.persistPasswordProperty(), PROXY_PERSIST_PASSWORD, defaultValues.shouldPersistPassword(),
                (_, _, newValue) -> {
                    putBoolean(PROXY_PERSIST_PASSWORD, newValue);
                    if (!newValue) {
                        writeKeyring(KeyringSlot.PROXY_PASSWORD, "");
                    }
                },
                () -> {
                    boolean shouldPersist = getBoolean(PROXY_PERSIST_PASSWORD, defaultValues.shouldPersistPassword());
                    proxyPreferences.persistPasswordProperty().set(shouldPersist);
                    proxyPreferences.passwordProperty().set(
                            shouldPersist ? readKeyring(KeyringSlot.PROXY_PASSWORD).orElse(defaultValues.getPassword())
                                          : defaultValues.getPassword());
                },
                () -> {
                    proxyPreferences.persistPasswordProperty().set(defaultValues.shouldPersistPassword());
                    proxyPreferences.passwordProperty().set(defaultValues.getPassword());
                });

        return proxyPreferences;
    }
    // endregion

    // region SSLPreferences
    @Override
    public SSLPreferences getSSLPreferences() {
        if (sslPreferences != null) {
            return sslPreferences;
        }

        SSLPreferences defaultValues = SSLPreferences.getDefault();

        sslPreferences = new SSLPreferences(
                Path.of(get(SSL_TRUSTSTORE_PATH, defaultValues.getTruststorePath().toString())));

        bindObject(sslPreferences.truststorePathProperty(), SSL_TRUSTSTORE_PATH, defaultValues.getTruststorePath(),
                Path::toString, Path::of);

        return sslPreferences;
    }
    // endregion

    // region CitationKeyPatternPreferences
    @Override
    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        if (citationKeyPatternPreferences != null) {
            return citationKeyPatternPreferences;
        }

        CitationKeyPatternPreferences defaultValues = CitationKeyPatternPreferences.getDefault();

        citationKeyPatternPreferences = new CitationKeyPatternPreferences(
                getBoolean(CITATION_KEY_TRANSLITERATE_FIELDS, defaultValues.shouldTransliterateFieldsForCitationKey()),
                getBoolean(CITATION_KEY_AVOID_OVERWRITING, defaultValues.shouldAvoidOverwriteCiteKey()),
                getBoolean(CITATION_KEY_WARN_BEFORE_OVERWRITE, defaultValues.shouldWarnBeforeOverwriteCiteKey()),
                getBoolean(CITATION_KEY_GENERATE_BEFORE_SAVING, defaultValues.shouldGenerateCiteKeysBeforeSaving()),
                readExclusiveFlags(defaultValues.getKeySuffix(),
                        CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B,
                        Map.entry(CITATION_KEY_GEN_ALWAYS_ADD_LETTER, CitationKeyPatternPreferences.KeySuffix.ALWAYS),
                        Map.entry(CITATION_KEY_GEN_FIRST_LETTER_A, CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A)),
                get(CITATION_KEY_PATTERN_REGEX, defaultValues.getKeyPatternRegex()),
                get(CITATION_KEY_PATTERN_REPLACEMENT, defaultValues.getKeyPatternReplacement()),
                get(CITATION_KEY_UNWANTED_CHARACTERS, defaultValues.getUnwantedCharacters()),
                getGlobalCitationKeyPattern(defaultValues),
                getBibEntryPreferences().keywordSeparatorProperty());

        bindBoolean(citationKeyPatternPreferences.shouldTransliterateFieldsForCitationKeyProperty(), CITATION_KEY_TRANSLITERATE_FIELDS, defaultValues.shouldTransliterateFieldsForCitationKey());
        bindBoolean(citationKeyPatternPreferences.shouldAvoidOverwriteCiteKeyProperty(), CITATION_KEY_AVOID_OVERWRITING, defaultValues.shouldAvoidOverwriteCiteKey());
        bindBoolean(citationKeyPatternPreferences.shouldWarnBeforeOverwriteCiteKeyProperty(), CITATION_KEY_WARN_BEFORE_OVERWRITE, defaultValues.shouldWarnBeforeOverwriteCiteKey());
        bindBoolean(citationKeyPatternPreferences.shouldGenerateCiteKeysBeforeSavingProperty(), CITATION_KEY_GENERATE_BEFORE_SAVING, defaultValues.shouldGenerateCiteKeysBeforeSaving());
        bindString(citationKeyPatternPreferences.keyPatternRegexProperty(), CITATION_KEY_PATTERN_REGEX, defaultValues.getKeyPatternRegex());
        bindString(citationKeyPatternPreferences.keyPatternReplacementProperty(), CITATION_KEY_PATTERN_REPLACEMENT, defaultValues.getKeyPatternReplacement());
        bindString(citationKeyPatternPreferences.unwantedCharactersProperty(), CITATION_KEY_UNWANTED_CHARACTERS, defaultValues.getUnwantedCharacters());

        // KeySuffix is persisted across two boolean keys; SECOND_WITH_B owns no key (the absence of both).
        bindExclusiveFlags(citationKeyPatternPreferences.keySuffixProperty(),
                defaultValues.getKeySuffix(),
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B,
                Map.entry(CITATION_KEY_GEN_ALWAYS_ADD_LETTER, CitationKeyPatternPreferences.KeySuffix.ALWAYS),
                Map.entry(CITATION_KEY_GEN_FIRST_LETTER_A, CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A));

        // KeyPatterns are persisted in a dedicated child node, so they need a custom binding.
        bindCustom(citationKeyPatternPreferences.keyPatternsProperty(), CITATION_KEY_DEFAULT_PATTERN, defaultValues.getKeyPatterns(),
                (_, _, newValue) -> storeGlobalCitationKeyPattern(newValue),
                () -> citationKeyPatternPreferences.keyPatternsProperty().set(getGlobalCitationKeyPattern(defaultValues)),
                () -> citationKeyPatternPreferences.keyPatternsProperty().set(defaultValues.getKeyPatterns()));

        return citationKeyPatternPreferences;
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

        BibEntryPreferences defaultValues = BibEntryPreferences.getDefault();

        bibEntryPreferences = new BibEntryPreferences(
                get(KEYWORD_SEPARATOR, String.valueOf(defaultValues.getKeywordSeparator())).charAt(0));

        bindObject(bibEntryPreferences.keywordSeparatorProperty(), KEYWORD_SEPARATOR, defaultValues.getKeywordSeparator(),
                String::valueOf, separator -> separator.charAt(0));

        return bibEntryPreferences;
    }
    // endregion

    // region InternalPreferences
    @Override
    public InternalPreferences getInternalPreferences() {
        if (internalPreferences != null) {
            return internalPreferences;
        }

        InternalPreferences defaultValues = InternalPreferences.getDefault();

        internalPreferences = new InternalPreferences(
                Version.parse(get(VERSION_IGNORED_UPDATE, defaultValues.getIgnoredVersion().toString())),
                getBoolean(VERSION_CHECK_ENABLED, defaultValues.isVersionCheckEnabled()),
                getPath(PREFS_EXPORT_PATH, defaultValues.getLastPreferencesExportPath()),
                // userHostInfo is derived from the owner and persisted/restored by the OwnerPreferences binding
                OS.getUserHostInfo(get(OWNER_DEFAULT, OwnerPreferences.getDefault().getDefaultOwner())),
                getBoolean(MEMORY_STICK_MODE, defaultValues.isMemoryStickMode()));

        bindObject(internalPreferences.ignoredVersionProperty(), VERSION_IGNORED_UPDATE, defaultValues.getIgnoredVersion(),
                Version::toString, Version::parse);
        bindBoolean(internalPreferences.versionCheckEnabledProperty(), VERSION_CHECK_ENABLED, defaultValues.isVersionCheckEnabled());
        bindCustom(internalPreferences.lastPreferencesExportPathProperty(), PREFS_EXPORT_PATH, defaultValues.getLastPreferencesExportPath(),
                (_, _, newValue) -> put(PREFS_EXPORT_PATH, newValue.toString()),
                () -> internalPreferences.lastPreferencesExportPathProperty().set(getPath(PREFS_EXPORT_PATH, defaultValues.getLastPreferencesExportPath())),
                () -> internalPreferences.lastPreferencesExportPathProperty().set(defaultValues.getLastPreferencesExportPath()));
        bindCustom(internalPreferences.memoryStickModeProperty(), MEMORY_STICK_MODE, defaultValues.isMemoryStickMode(),
                (_, _, newValue) -> {
                    putBoolean(MEMORY_STICK_MODE, newValue);
                    if (!newValue) {
                        try {
                            Files.deleteIfExists(Path.of("jabref.xml"));
                        } catch (IOException e) {
                            LOGGER.warn("Error accessing filesystem", e);
                        }
                    }
                },
                () -> internalPreferences.memoryStickModeProperty().set(getBoolean(MEMORY_STICK_MODE, defaultValues.isMemoryStickMode())),
                () -> internalPreferences.memoryStickModeProperty().set(defaultValues.isMemoryStickMode()));

        return internalPreferences;
    }
    // endregion

    // region FieldPreferences
    @Override
    public FieldPreferences getFieldPreferences() {
        if (fieldPreferences != null) {
            return fieldPreferences;
        }

        FieldPreferences defaultValues = FieldPreferences.getDefault();

        fieldPreferences = new FieldPreferences(
                !getBoolean(DO_NOT_RESOLVE_STRINGS, !defaultValues.shouldResolveStrings()),
                List.copyOf(FieldFactory.parseFieldList(get(RESOLVE_STRINGS_FOR_FIELDS,
                        FieldFactory.serializeFieldsList(defaultValues.getResolvableFields())))),
                List.copyOf(FieldFactory.parseFieldList(get(NON_WRAPPABLE_FIELDS,
                        FieldFactory.serializeFieldsList(defaultValues.getNonWrappableFields())))));

        bindBooleanInverted(fieldPreferences.resolveStringsProperty(), DO_NOT_RESOLVE_STRINGS, defaultValues.shouldResolveStrings());
        bindCustomList(fieldPreferences.getResolvableFields(), RESOLVE_STRINGS_FOR_FIELDS, List.copyOf(defaultValues.getResolvableFields()),
                FieldFactory::serializeFieldsList, FieldFactory::parseFieldList);
        bindCustomList(fieldPreferences.getNonWrappableFields(), NON_WRAPPABLE_FIELDS, List.copyOf(defaultValues.getNonWrappableFields()),
                FieldFactory::serializeFieldsList, FieldFactory::parseFieldList);

        return fieldPreferences;
    }
    // endregion

    // region (Linked)FilePreferences
    @Override
    public FilePreferences getFilePreferences() {
        if (filePreferences != null) {
            return filePreferences;
        }

        FilePreferences defaultValues = FilePreferences.getDefault();

        filePreferences = new FilePreferences(
                getInternalPreferences().getUserAndHostInfoProperty(),
                getPath(FILES_MAIN_DIRECTORY, defaultValues.mainFileDirectoryProperty().get()),
                getBoolean(FILES_STORE_RELATIVE_TO_BIB, defaultValues.shouldStoreFilesRelativeToBibFile()),
                getBoolean(FILES_AUTO_RENAME_ON_CHANGE, defaultValues.shouldAutoRenameFilesOnChange()),
                get(FILES_IMPORT_NAMEPATTERN, defaultValues.getFileNamePattern()),
                get(FILES_IMPORT_DIRPATTERN, defaultValues.getFileDirectoryPattern()),
                getBoolean(FILES_DOWNLOAD_LINKED, defaultValues.shouldDownloadLinkedFiles()),
                getBoolean(FILES_FULLTEXT_INDEX, defaultValues.shouldFulltextIndexLinkedFiles()),
                getPath(FILES_WORKING_DIRECTORY, defaultValues.getWorkingDirectory()),
                getBoolean(BACKUP_ENABLED, defaultValues.shouldCreateBackup()),
                // Backups should sit in the data directory, because a ".bak" file should survive cache cleanups
                getPath(BACKUP_DIRECTORY, defaultValues.getBackupDirectory()),
                getBoolean(FILES_CONFIRM_DELETE_LINKED, defaultValues.confirmDeleteLinkedFile()),
                // Use fallback method in case AWT is not initialized in headless (JabKit) mode
                getBoolean(FILES_TRASH_INSTEAD_OF_DELETE, moveToTrashSupported()),
                getBoolean(FILES_ADJUST_FILE_LINKS_ON_TRANSFER, defaultValues.shouldAdjustFileLinksOnTransfer()),
                getBoolean(FILES_COPY_LINKED_FILES_ON_TRANSFER, defaultValues.shouldCopyLinkedFilesOnTransfer()),
                getBoolean(FILES_MOVE_LINKED_FILES_ON_TRANSFER, defaultValues.shouldMoveLinkedFilesOnTransfer()),
                getBoolean(FILES_KEEP_DOWNLOAD_URL, defaultValues.shouldKeepDownloadUrl()),
                getPath(FILES_LAST_USED_DIRECTORY, defaultValues.getLastUsedDirectory()),
                getBoolean(FILES_OPEN_FILE_EXPLORER_IN_FILE_DIRECTORY, defaultValues.shouldOpenFileExplorerInFileDirectory()),
                getBoolean(FILES_OPEN_FILE_EXPLORER_IN_LAST_USED_DIRECTORY, defaultValues.shouldOpenFileExplorerInLastUsedDirectory()));

        // mainFileDirectory defaults to getDefaultPath(), which the GUI overrides to a meaningful location.
        bindPath(filePreferences.mainFileDirectoryProperty(), FILES_MAIN_DIRECTORY, getDefaultPath());
        bindBoolean(filePreferences.storeFilesRelativeToBibFileProperty(), FILES_STORE_RELATIVE_TO_BIB, defaultValues.shouldStoreFilesRelativeToBibFile());
        bindBoolean(filePreferences.autoRenameFilesOnChangeProperty(), FILES_AUTO_RENAME_ON_CHANGE, defaultValues.shouldAutoRenameFilesOnChange());
        bindString(filePreferences.fileNamePatternProperty(), FILES_IMPORT_NAMEPATTERN, defaultValues.getFileNamePattern());
        bindString(filePreferences.fileDirectoryPatternProperty(), FILES_IMPORT_DIRPATTERN, defaultValues.getFileDirectoryPattern());
        bindBoolean(filePreferences.downloadLinkedFilesProperty(), FILES_DOWNLOAD_LINKED, defaultValues.shouldDownloadLinkedFiles());
        bindBoolean(filePreferences.fulltextIndexLinkedFilesProperty(), FILES_FULLTEXT_INDEX, defaultValues.shouldFulltextIndexLinkedFiles());
        bindPath(filePreferences.workingDirectoryProperty(), FILES_WORKING_DIRECTORY, defaultValues.getWorkingDirectory());
        bindBoolean(filePreferences.createBackupProperty(), BACKUP_ENABLED, defaultValues.shouldCreateBackup());
        bindPath(filePreferences.backupDirectoryProperty(), BACKUP_DIRECTORY, defaultValues.getBackupDirectory());
        bindBoolean(filePreferences.confirmDeleteLinkedFileProperty(), FILES_CONFIRM_DELETE_LINKED, defaultValues.confirmDeleteLinkedFile());
        // moveToTrash falls back to moveToTrashSupported(), which the GUI overrides when AWT is initialized.
        bindBoolean(filePreferences.moveToTrashProperty(), FILES_TRASH_INSTEAD_OF_DELETE, moveToTrashSupported());
        bindBoolean(filePreferences.adjustFileLinksOnTransferProperty(), FILES_ADJUST_FILE_LINKS_ON_TRANSFER, defaultValues.shouldAdjustFileLinksOnTransfer());
        bindBoolean(filePreferences.copyLinkedFilesOnTransferProperty(), FILES_COPY_LINKED_FILES_ON_TRANSFER, defaultValues.shouldCopyLinkedFilesOnTransfer());
        bindBoolean(filePreferences.moveLinkedFilesOnTransferPropertyProperty(), FILES_MOVE_LINKED_FILES_ON_TRANSFER, defaultValues.shouldMoveLinkedFilesOnTransfer());
        bindBoolean(filePreferences.shouldKeepDownloadUrlProperty(), FILES_KEEP_DOWNLOAD_URL, defaultValues.shouldKeepDownloadUrl());
        // lastUsedDirectory defaults to getDefaultPath(), which the GUI overrides to a meaningful location.
        bindPath(filePreferences.lastUsedDirectoryProperty(), FILES_LAST_USED_DIRECTORY, getDefaultPath());
        bindBoolean(filePreferences.openFileExplorerInFileDirectoryProperty(), FILES_OPEN_FILE_EXPLORER_IN_FILE_DIRECTORY, defaultValues.shouldOpenFileExplorerInFileDirectory());
        bindBoolean(filePreferences.openFileExplorerInLastUsedDirectoryProperty(), FILES_OPEN_FILE_EXPLORER_IN_LAST_USED_DIRECTORY, defaultValues.shouldOpenFileExplorerInLastUsedDirectory());

        return filePreferences;
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

        AutoLinkPreferences defaultValues = AutoLinkPreferences.getDefault();

        // citationKeyDependency is persisted across two boolean keys; START owns no key (the absence of both).
        autoLinkPreferences = new AutoLinkPreferences(
                readExclusiveFlags(defaultValues.getCitationKeyDependency(),
                        AutoLinkPreferences.CitationKeyDependency.START,
                        Map.entry(AUTOLINK_EXACT_KEY_ONLY, AutoLinkPreferences.CitationKeyDependency.EXACT),
                        Map.entry(AUTOLINK_USE_REG_EXP_SEARCH_KEY, AutoLinkPreferences.CitationKeyDependency.REGEX)),
                get(AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, defaultValues.getRegularExpression()),
                getBoolean(ASK_AUTO_NAMING_PDFS_AGAIN, defaultValues.shouldAskAutoNamingPdfs()),
                bibEntryPreferences.keywordSeparatorProperty());

        bindExclusiveFlags(autoLinkPreferences.citationKeyDependencyProperty(),
                defaultValues.getCitationKeyDependency(), AutoLinkPreferences.CitationKeyDependency.START,
                Map.entry(AUTOLINK_EXACT_KEY_ONLY, AutoLinkPreferences.CitationKeyDependency.EXACT),
                Map.entry(AUTOLINK_USE_REG_EXP_SEARCH_KEY, AutoLinkPreferences.CitationKeyDependency.REGEX));
        bindString(autoLinkPreferences.regularExpressionProperty(), AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, defaultValues.getRegularExpression());
        bindBoolean(autoLinkPreferences.askAutoNamingPdfsProperty(), ASK_AUTO_NAMING_PDFS_AGAIN, defaultValues.shouldAskAutoNamingPdfs());

        return autoLinkPreferences;
    }
    // endregion

    // region ExportPreferences
    @Override
    public ExportPreferences getExportPreferences() {
        if (exportPreferences != null) {
            return exportPreferences;
        }

        ExportPreferences defaultValues = ExportPreferences.getDefault();

        exportPreferences = new ExportPreferences(
                get(LAST_USED_EXPORT, defaultValues.getLastExportExtension()),
                Path.of(get(EXPORT_WORKING_DIRECTORY, defaultValues.getExportWorkingDirectory().toString())),
                getExportSaveOrder(defaultValues.getExportSaveOrder()),
                getCustomExportFormats(defaultValues.getCustomExporters()));

        bindString(exportPreferences.lastExportExtensionProperty(), LAST_USED_EXPORT, defaultValues.getLastExportExtension());
        bindObject(exportPreferences.exportWorkingDirectoryProperty(), EXPORT_WORKING_DIRECTORY, defaultValues.getExportWorkingDirectory(),
                Path::toString, Path::of);
        // exportSaveOrder is persisted across several backing-store keys, so it needs a custom binding.
        bindCustom(exportPreferences.exportSaveOrderProperty(), EXPORT_PRIMARY_SORT_FIELD, defaultValues.getExportSaveOrder(),
                (_, _, newValue) -> storeExportSaveOrder(newValue),
                () -> exportPreferences.exportSaveOrderProperty().set(getExportSaveOrder(defaultValues.getExportSaveOrder())),
                () -> exportPreferences.exportSaveOrderProperty().set(defaultValues.getExportSaveOrder()));
        // customExporters are persisted as a numbered series, so they need a delegated list binding.
        bindCustomList(exportPreferences.getCustomExporters(), CUSTOM_EXPORT_FORMAT, defaultValues.getCustomExporters(),
                this::storeCustomExportFormats,
                () -> getCustomExportFormats(defaultValues.getCustomExporters()));

        return exportPreferences;
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

        CleanupPreferences defaultValues = CleanupPreferences.getDefault();

        cleanupPreferences = new CleanupPreferences(
                getCleanupActiveJobs(defaultValues.getActiveJobs()),
                getCleanupFieldFormatters(defaultValues.getFieldFormatterCleanups()));

        // activeJobs is persisted as a single string list, removing the key entirely when empty.
        bindSet(cleanupPreferences.getObservableActiveJobs(), CLEANUP_JOBS, defaultValues.getActiveJobs(),
                activeJobs -> {
                    if (activeJobs.isEmpty()) {
                        remove(CLEANUP_JOBS);
                    } else {
                        putStringList(CLEANUP_JOBS, activeJobs.stream()
                                                              .map(Enum::name)
                                                              .collect(Collectors.toList()));
                    }
                },
                () -> getCleanupActiveJobs(defaultValues.getActiveJobs()));

        // fieldFormatterCleanups is persisted across two backing-store keys, so it needs a custom binding.
        bindCustom(cleanupPreferences.fieldFormatterCleanupsProperty(), CLEANUP_FIELD_FORMATTERS_ENABLED, defaultValues.getFieldFormatterCleanups(),
                (_, _, newValue) -> {
                    putBoolean(CLEANUP_FIELD_FORMATTERS_ENABLED, newValue.isEnabled());
                    put(CLEANUP_FIELD_FORMATTERS, FieldFormatterCleanupActions.getMetaDataString(newValue.getConfiguredActions(), OS.NEWLINE));
                },
                () -> cleanupPreferences.setFieldFormatterCleanups(getCleanupFieldFormatters(defaultValues.getFieldFormatterCleanups())),
                () -> cleanupPreferences.setFieldFormatterCleanups(defaultValues.getFieldFormatterCleanups()));

        return cleanupPreferences;
    }

    private Set<CleanupPreferences.CleanupStep> getCleanupActiveJobs(Set<CleanupPreferences.CleanupStep> defaults) {
        if (!hasKey(CLEANUP_JOBS)) {
            return EnumSet.copyOf(defaults);
        }
        Set<CleanupPreferences.CleanupStep> parsed = getStringList(CLEANUP_JOBS).stream()
                                                                                .map(CleanupPreferences.CleanupStep::safeValueOf)
                                                                                .flatMap(Optional::stream)
                                                                                .collect(Collectors.toSet());
        return parsed.isEmpty() ? EnumSet.noneOf(CleanupPreferences.CleanupStep.class) : EnumSet.copyOf(parsed);
    }

    private FieldFormatterCleanupActions getCleanupFieldFormatters(FieldFormatterCleanupActions defaults) {
        return new FieldFormatterCleanupActions(
                getBoolean(CLEANUP_FIELD_FORMATTERS_ENABLED, defaults.isEnabled()),
                FieldFormatterCleanupMapper.parseActions(
                        StringUtil.unifyLineBreaks(get(
                                        CLEANUP_FIELD_FORMATTERS,
                                        FieldFormatterCleanupActions.getMetaDataString(defaults.getConfiguredActions(), OS.NEWLINE)),
                                ""))
        );
    }
    // endregion

    // region LastFilesOpenedPreferences
    @Override
    public LastFilesOpenedPreferences getLastFilesOpenedPreferences() {
        if (lastFilesOpenedPreferences != null) {
            return lastFilesOpenedPreferences;
        }

        LastFilesOpenedPreferences defaultValues = LastFilesOpenedPreferences.getDefault();

        lastFilesOpenedPreferences = new LastFilesOpenedPreferences(
                getStringList(LAST_EDITED).stream().map(Path::of).toList(),
                getPath(LAST_FOCUSED, defaultValues.getLastFocusedFile()),
                FileHistory.of(getStringList(RECENT_DATABASES).stream().map(Path::of).toList()));

        bindPathList(lastFilesOpenedPreferences.getLastFilesOpened(), LAST_EDITED, defaultValues.getLastFilesOpened());
        bindPathList(lastFilesOpenedPreferences.getFileHistory(), RECENT_DATABASES, defaultValues.getFileHistory());
        bindPath(lastFilesOpenedPreferences.lastFocusedFileProperty(), LAST_FOCUSED, defaultValues.getLastFocusedFile());

        return lastFilesOpenedPreferences;
    }
    // endregion

    // region AiPreferences
    @Override
    public AiPreferences getAiPreferences() {
        if (aiPreferences != null) {
            return aiPreferences;
        }

        AiPreferences defaultValues = AiPreferences.getDefault();
        migrateLegacyAiResponseEngineKind(defaultValues);

        aiPreferences = new AiPreferences(
                getBoolean(AI_ENABLED, defaultValues.getAiFeaturesEnabled()),
                getBoolean(AI_AUTO_GENERATE_EMBEDDINGS, defaultValues.getAutoGenerateEmbeddings()),
                getBoolean(AI_AUTO_GENERATE_SUMMARIES, defaultValues.getAutoGenerateSummaries()),
                AiProvider.safeValueOf(get(AI_PROVIDER, defaultValues.getAiProvider().name())),
                get(AI_OPEN_AI_CHAT_MODEL, defaultValues.getOpenAiChatModel()),
                get(AI_MISTRAL_AI_CHAT_MODEL, defaultValues.getMistralAiChatModel()),
                get(AI_GEMINI_CHAT_MODEL, defaultValues.getGeminiChatModel()),
                get(AI_HUGGING_FACE_CHAT_MODEL, defaultValues.getHuggingFaceChatModel()),
                getBoolean(AI_CUSTOMIZE_SETTINGS, defaultValues.getCustomizeExpertSettings()),
                get(AI_OPEN_AI_API_BASE_URL, defaultValues.getOpenAiApiBaseUrl()),
                get(AI_MISTRAL_AI_API_BASE_URL, defaultValues.getMistralAiApiBaseUrl()),
                get(AI_GEMINI_API_BASE_URL, defaultValues.getGeminiApiBaseUrl()),
                get(AI_HUGGING_FACE_API_BASE_URL, defaultValues.getHuggingFaceApiBaseUrl()),
                SummarizatorKind.safeValueOf(get(AI_SUMMARIZATOR_KIND, defaultValues.getSummarizatorKind().name())),
                TokenEstimatorKind.safeValueOf(get(AI_TOKEN_ESTIMATOR_KIND, defaultValues.getTokenEstimatorKind().name())),
                PredefinedEmbeddingModel.safeValueOf(get(AI_EMBEDDING_MODEL, defaultValues.embeddingModelProperty().get().name())),
                getDouble(AI_TEMPERATURE, defaultValues.temperatureProperty().get()),
                getInt(AI_CONTEXT_WINDOW_SIZE, defaultValues.contextWindowSizeProperty().get()),
                DocumentSplitterKind.safeValueOf(get(AI_DOCUMENT_SPLITTER_KIND, defaultValues.getDocumentSplitterKind().name())),
                getInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, defaultValues.documentSplitterChunkSizeProperty().get()),
                getInt(AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, defaultValues.documentSplitterOverlapSizeProperty().get()),
                ResponseEngineKind.safeValueOf(get(AI_RESPONSE_ENGINE_KIND, defaultValues.getResponseEngineKind().name())),
                getInt(AI_RAG_MAX_RESULTS_COUNT, defaultValues.ragMaxResultsCountProperty().get()),
                getDouble(AI_RAG_MIN_SCORE, defaultValues.ragMinScoreProperty().get()),
                get(AI_CHATTING_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getChattingSystemMessageTemplate()),
                get(AI_CHATTING_USER_MESSAGE_TEMPLATE, defaultValues.getChattingUserMessageTemplate()),
                get(AI_SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getSummarizationChunkSystemMessageTemplate()),
                get(AI_SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getSummarizationCombineSystemMessageTemplate()),
                get(AI_SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getSummarizationFullDocumentSystemMessageTemplate()),
                get(AI_CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getCitationParsingSystemMessageTemplate()),
                get(AI_MARKDOWN_CHAT_EXPORT_TEMPLATE, defaultValues.getMarkdownChatExportTemplate()),
                getBoolean(AI_GENERATE_FOLLOW_UP_QUESTIONS, defaultValues.getGenerateFollowUpQuestions()),
                getInt(AI_FOLLOW_UP_QUESTIONS_COUNT, defaultValues.getFollowUpQuestionsCount()),
                get(AI_FOLLOW_UP_QUESTIONS_TEMPLATE, defaultValues.getFollowUpQuestionsTemplate()));

        bindBoolean(aiPreferences.aiFeaturesEnabledCurrentlyProperty(), AI_ENABLED, defaultValues.getAiFeaturesEnabled());
        bindBoolean(aiPreferences.autoGenerateEmbeddingsProperty(), AI_AUTO_GENERATE_EMBEDDINGS, defaultValues.getAutoGenerateEmbeddings());
        bindBoolean(aiPreferences.autoGenerateSummariesProperty(), AI_AUTO_GENERATE_SUMMARIES, defaultValues.getAutoGenerateSummaries());

        bindObject(aiPreferences.aiProviderProperty(), AI_PROVIDER, defaultValues.getAiProvider(), AiProvider::name, AiProvider::safeValueOf);

        bindString(aiPreferences.openAiChatModelProperty(), AI_OPEN_AI_CHAT_MODEL, defaultValues.getOpenAiChatModel());
        bindString(aiPreferences.mistralAiChatModelProperty(), AI_MISTRAL_AI_CHAT_MODEL, defaultValues.getMistralAiChatModel());
        bindString(aiPreferences.geminiChatModelProperty(), AI_GEMINI_CHAT_MODEL, defaultValues.getGeminiChatModel());
        bindString(aiPreferences.huggingFaceChatModelProperty(), AI_HUGGING_FACE_CHAT_MODEL, defaultValues.getHuggingFaceChatModel());

        bindBoolean(aiPreferences.customizeExpertSettingsProperty(), AI_CUSTOMIZE_SETTINGS, defaultValues.getCustomizeExpertSettings());

        bindString(aiPreferences.openAiApiBaseUrlProperty(), AI_OPEN_AI_API_BASE_URL, defaultValues.getOpenAiApiBaseUrl());
        bindString(aiPreferences.mistralAiApiBaseUrlProperty(), AI_MISTRAL_AI_API_BASE_URL, defaultValues.getMistralAiApiBaseUrl());
        bindString(aiPreferences.geminiApiBaseUrlProperty(), AI_GEMINI_API_BASE_URL, defaultValues.getGeminiApiBaseUrl());
        bindString(aiPreferences.huggingFaceApiBaseUrlProperty(), AI_HUGGING_FACE_API_BASE_URL, defaultValues.getHuggingFaceApiBaseUrl());

        bindObject(aiPreferences.summarizatorKindProperty(), AI_SUMMARIZATOR_KIND, defaultValues.getSummarizatorKind(), SummarizatorKind::name, SummarizatorKind::safeValueOf);
        bindObject(aiPreferences.tokenEstimatorKindProperty(), AI_TOKEN_ESTIMATOR_KIND, defaultValues.getTokenEstimatorKind(), TokenEstimatorKind::name, TokenEstimatorKind::safeValueOf);
        bindObject(aiPreferences.embeddingModelProperty(), AI_EMBEDDING_MODEL, defaultValues.embeddingModelProperty().get(), PredefinedEmbeddingModel::name, PredefinedEmbeddingModel::safeValueOf);
        bindDouble(aiPreferences.temperatureProperty(), AI_TEMPERATURE, defaultValues.temperatureProperty().get());
        bindInt(aiPreferences.contextWindowSizeProperty(), AI_CONTEXT_WINDOW_SIZE, defaultValues.contextWindowSizeProperty().get());

        bindObject(aiPreferences.documentSplitterKindProperty(), AI_DOCUMENT_SPLITTER_KIND, defaultValues.getDocumentSplitterKind(), DocumentSplitterKind::name, DocumentSplitterKind::safeValueOf);
        bindInt(aiPreferences.documentSplitterChunkSizeProperty(), AI_DOCUMENT_SPLITTER_CHUNK_SIZE, defaultValues.documentSplitterChunkSizeProperty().get());
        bindInt(aiPreferences.documentSplitterOverlapSizeProperty(), AI_DOCUMENT_SPLITTER_OVERLAP_SIZE, defaultValues.documentSplitterOverlapSizeProperty().get());

        bindObject(aiPreferences.responseEngineKindProperty(), AI_RESPONSE_ENGINE_KIND, defaultValues.getResponseEngineKind(), ResponseEngineKind::name, ResponseEngineKind::safeValueOf);
        bindInt(aiPreferences.ragMaxResultsCountProperty(), AI_RAG_MAX_RESULTS_COUNT, defaultValues.ragMaxResultsCountProperty().get());
        bindDouble(aiPreferences.ragMinScoreProperty(), AI_RAG_MIN_SCORE, defaultValues.ragMinScoreProperty().get());

        bindString(aiPreferences.chattingSystemMessageTemplateProperty(), AI_CHATTING_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getChattingSystemMessageTemplate());
        bindString(aiPreferences.chattingUserMessageTemplateProperty(), AI_CHATTING_USER_MESSAGE_TEMPLATE, defaultValues.getChattingUserMessageTemplate());
        bindString(aiPreferences.summarizationChunkSystemMessageTemplateProperty(), AI_SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getSummarizationChunkSystemMessageTemplate());
        bindString(aiPreferences.summarizationCombineSystemMessageTemplateProperty(), AI_SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getSummarizationCombineSystemMessageTemplate());
        bindString(aiPreferences.summarizationFullDocumentSystemMessageTemplateProperty(), AI_SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getSummarizationFullDocumentSystemMessageTemplate());
        bindString(aiPreferences.citationParsingSystemMessageTemplateProperty(), AI_CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE, defaultValues.getCitationParsingSystemMessageTemplate());
        bindString(aiPreferences.markdownChatExportTemplateProperty(), AI_MARKDOWN_CHAT_EXPORT_TEMPLATE, defaultValues.getMarkdownChatExportTemplate());

        bindBoolean(aiPreferences.generateFollowUpQuestionsProperty(), AI_GENERATE_FOLLOW_UP_QUESTIONS, defaultValues.getGenerateFollowUpQuestions());
        bindInt(aiPreferences.followUpQuestionsCountProperty(), AI_FOLLOW_UP_QUESTIONS_COUNT, defaultValues.getFollowUpQuestionsCount());
        bindString(aiPreferences.followUpQuestionsTemplateProperty(), AI_FOLLOW_UP_QUESTIONS_TEMPLATE, defaultValues.getFollowUpQuestionsTemplate());

        return aiPreferences;
    }

    private void migrateLegacyAiResponseEngineKind(AiPreferences defaultValues) {
        if (!hasKey(AI_RESPONSE_ENGINE_KIND) && hasKey(AI_ANSWER_ENGINE_KIND)) {
            put(AI_RESPONSE_ENGINE_KIND, get(AI_ANSWER_ENGINE_KIND, defaultValues.getResponseEngineKind().name()));
        }
    }
    // endregion

    // region OCR preferences
    public OcrPreferences getOcrPreferences() {
        if (ocrPreferences != null) {
            return ocrPreferences;
        }

        OcrPreferences defaultValues = OcrPreferences.getDefault();

        ocrPreferences = new OcrPreferences(
                get(OCR_ENGINE_PATH, defaultValues.getOcrEnginePath()),
                PagesWithTextHandling.safeValueOf(get(PAGES_WITH_TEXT, defaultValues.getPagesHaveText().name())));

        bindString(ocrPreferences.ocrEnginePathProperty(), OCR_ENGINE_PATH, defaultValues.getOcrEnginePath());
        bindObject(ocrPreferences.pagesHaveTextProperty(), PAGES_WITH_TEXT, defaultValues.getPagesHaveText(), PagesWithTextHandling::name, PagesWithTextHandling::safeValueOf);

        return ocrPreferences;
    }
    // endregion

    // region SearchPreferences
    @Override
    public SearchPreferences getSearchPreferences() {
        if (searchPreferences != null) {
            return searchPreferences;
        }

        SearchPreferences defaultValues = SearchPreferences.getDefault();

        searchPreferences = new SearchPreferences(
                getBoolean(SEARCH_DISPLAY_MODE, defaultValues.getSearchDisplayMode() == SearchDisplayMode.FILTER) ? SearchDisplayMode.FILTER : SearchDisplayMode.FLOAT,
                getBoolean(SEARCH_REG_EXP, defaultValues.isRegularExpression()),
                getBoolean(SEARCH_CASE_SENSITIVE, defaultValues.isCaseSensitive()),
                getBoolean(SEARCH_FULLTEXT, defaultValues.isFulltext()),
                getBoolean(SEARCH_USE_POSTGRES, defaultValues.shouldUsePostgresSearch()),
                getBoolean(SEARCH_KEEP_SEARCH_STRING, defaultValues.shouldKeepSearchString()),
                getBoolean(SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, defaultValues.shouldKeepWindowOnTop()),
                getDouble(SEARCH_WINDOW_HEIGHT, defaultValues.getSearchWindowHeight()),
                getDouble(SEARCH_WINDOW_WIDTH, defaultValues.getSearchWindowWidth()),
                getDouble(SEARCH_WINDOW_DIVIDER_POS, defaultValues.getSearchWindowDividerPosition()));

        // searchDisplayMode is persisted as a single boolean: FILTER owns the key, FLOAT is the implicit (key=false) value.
        bindExclusiveFlags(searchPreferences.searchDisplayModeProperty(), defaultValues.getSearchDisplayMode(),
                SearchDisplayMode.FLOAT,
                Map.entry(SEARCH_DISPLAY_MODE, SearchDisplayMode.FILTER));

        bindSet(searchPreferences.getObservableSearchFlags(), SEARCH_FULLTEXT, getSearchFlags(defaultValues),
                flags -> {
                    // Only the fulltext flag is persisted; the regular expression and case-sensitive flags should always be
                    // set to default values on startup, as nothing ever writes their keys.
                    putBoolean(SEARCH_FULLTEXT, flags.contains(SearchFlags.FULLTEXT));
                },
                () -> getSearchFlags(defaultValues));

        bindBoolean(searchPreferences.keepSearchStringProperty(), SEARCH_KEEP_SEARCH_STRING, defaultValues.shouldKeepSearchString());
        bindBoolean(searchPreferences.keepWindowOnTopProperty(), SEARCH_KEEP_GLOBAL_WINDOW_ON_TOP, defaultValues.shouldKeepWindowOnTop());
        bindDouble(searchPreferences.getSearchWindowHeightProperty(), SEARCH_WINDOW_HEIGHT, defaultValues.getSearchWindowHeight());
        bindDouble(searchPreferences.getSearchWindowWidthProperty(), SEARCH_WINDOW_WIDTH, defaultValues.getSearchWindowWidth());
        bindDouble(searchPreferences.getSearchWindowDividerPositionProperty(), SEARCH_WINDOW_DIVIDER_POS, defaultValues.getSearchWindowDividerPosition());
        bindBoolean(searchPreferences.usePostgresSearchProperty(), SEARCH_USE_POSTGRES, defaultValues.shouldUsePostgresSearch());

        return searchPreferences;
    }

    private Set<SearchFlags> getSearchFlags(SearchPreferences defaults) {
        EnumSet<SearchFlags> flags = EnumSet.noneOf(SearchFlags.class);
        if (getBoolean(SEARCH_REG_EXP, defaults.isRegularExpression())) {
            flags.add(SearchFlags.REGULAR_EXPRESSION);
        }
        if (getBoolean(SEARCH_CASE_SENSITIVE, defaults.isCaseSensitive())) {
            flags.add(SearchFlags.CASE_SENSITIVE);
        }
        if (getBoolean(SEARCH_FULLTEXT, defaults.isFulltext())) {
            flags.add(SearchFlags.FULLTEXT);
        }
        return flags;
    }
    // endregion

    // region XmpPreferences
    @Override
    public XmpPreferences getXmpPreferences() {
        if (xmpPreferences != null) {
            return xmpPreferences;
        }

        XmpPreferences defaultValues = XmpPreferences.getDefault();

        xmpPreferences = new XmpPreferences(
                getBoolean(XMP_USE_PRIVACY_FILTER, defaultValues.shouldUseXmpPrivacyFilter()),
                getXmpPrivacyFilter(defaultValues.getXmpPrivacyFilter()),
                getBibEntryPreferences().keywordSeparatorProperty());

        bindBoolean(xmpPreferences.useXmpPrivacyFilterProperty(), XMP_USE_PRIVACY_FILTER, defaultValues.shouldUseXmpPrivacyFilter());
        bindSet(xmpPreferences.getXmpPrivacyFilter(), XMP_PRIVACY_FILTERS, defaultValues.getXmpPrivacyFilter(),
                filter -> putStringList(XMP_PRIVACY_FILTERS, filter.stream()
                                                                   .map(Field::getName)
                                                                   .collect(Collectors.toList())),
                () -> getXmpPrivacyFilter(defaultValues.getXmpPrivacyFilter()));

        return xmpPreferences;
    }

    private Set<Field> getXmpPrivacyFilter(Set<Field> defaults) {
        return convertStringToList(get(XMP_PRIVACY_FILTERS,
                convertListToString(defaults.stream().map(Field::getName).toList())))
                .stream()
                .map(FieldFactory::parseField)
                .collect(Collectors.toSet());
    }
    // endregion

    // region NameFormatterPreferences
    @Override
    public NameFormatterPreferences getNameFormatterPreferences() {
        if (nameFormatterPreferences != null) {
            return nameFormatterPreferences;
        }

        NameFormatterPreferences defaultValues = NameFormatterPreferences.getDefault();

        nameFormatterPreferences = new NameFormatterPreferences(
                convertStringToList(get(NAME_FORMATER_KEY, convertListToString(defaultValues.getNameFormatterKey()))),
                convertStringToList(get(NAME_FORMATTER_VALUE, convertListToString(defaultValues.getNameFormatterValue()))));

        bindCustomList(nameFormatterPreferences.getNameFormatterKey(), NAME_FORMATER_KEY, defaultValues.getNameFormatterKey(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
        bindCustomList(nameFormatterPreferences.getNameFormatterValue(), NAME_FORMATTER_VALUE, defaultValues.getNameFormatterValue(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);

        return nameFormatterPreferences;
    }
    // endregion

    // region ProtectedTermsPreferences
    @Override
    public ProtectedTermsPreferences getProtectedTermsPreferences() {
        if (protectedTermsPreferences != null) {
            return protectedTermsPreferences;
        }

        ProtectedTermsPreferences defaultValues = ProtectedTermsPreferences.getDefault();

        protectedTermsPreferences = new ProtectedTermsPreferences(
                convertStringToList(get(PROTECTED_TERMS_ENABLED_INTERNAL, convertListToString(defaultValues.getEnabledInternalTermLists()))),
                convertStringToList(get(PROTECTED_TERMS_ENABLED_EXTERNAL, convertListToString(defaultValues.getEnabledExternalTermLists()))),
                convertStringToList(get(PROTECTED_TERMS_DISABLED_INTERNAL, convertListToString(defaultValues.getDisabledInternalTermLists()))),
                convertStringToList(get(PROTECTED_TERMS_DISABLED_EXTERNAL, convertListToString(defaultValues.getDisabledExternalTermLists()))));

        bindCustomList(protectedTermsPreferences.getEnabledInternalTermLists(), PROTECTED_TERMS_ENABLED_INTERNAL, defaultValues.getEnabledInternalTermLists(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
        bindCustomList(protectedTermsPreferences.getEnabledExternalTermLists(), PROTECTED_TERMS_ENABLED_EXTERNAL, defaultValues.getEnabledExternalTermLists(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
        bindCustomList(protectedTermsPreferences.getDisabledInternalTermLists(), PROTECTED_TERMS_DISABLED_INTERNAL, defaultValues.getDisabledInternalTermLists(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
        bindCustomList(protectedTermsPreferences.getDisabledExternalTermLists(), PROTECTED_TERMS_DISABLED_EXTERNAL, defaultValues.getDisabledExternalTermLists(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);

        return protectedTermsPreferences;
    }
    // endregion

    // region ImporterPreferences
    @Override
    public ImporterPreferences getImporterPreferences() {
        if (importerPreferences != null) {
            return importerPreferences;
        }

        ImporterPreferences defaultValues = ImporterPreferences.getDefault();

        importerPreferences = new ImporterPreferences(
                getBoolean(IMPORTER_ENABLED, defaultValues.areImporterEnabled()),
                getBoolean(IMPORTER_GENERATE_KEY_ON_IMPORT, defaultValues.shouldGenerateNewKeyOnImport()),
                Path.of(get(IMPORTER_WORKING_DIRECTORY, defaultValues.getImportWorkingDirectory().toString())),
                getBoolean(IMPORTER_WARN_ABOUT_DUPLICATES, defaultValues.shouldWarnAboutDuplicatesOnImport()),
                getBoolean(IMPORTER_WARN_ABOUT_BIB_FILE, defaultValues.shouldWarnAboutBibFileImport()),
                getCustomImportFormats(defaultValues.getCustomImporters()),
                getFetcherKeys(defaultValues.getApiKeys()),
                getBoolean(FETCHER_CUSTOM_KEY_PERSIST, defaultValues.shouldPersistCustomKeys()),
                hasKey(IMPORTER_CATALOGS) ? getStringList(IMPORTER_CATALOGS) : defaultValues.getCatalogs(),
                getDefaultPlainCitationParser(defaultValues.getDefaultPlainCitationParser()),
                getInt(IMPORTER_CITATIONS_RELATIONS_STORE_TTL, defaultValues.getCitationsRelationsStoreTTL()),
                Map.of());

        bindBoolean(importerPreferences.importerEnabledProperty(), IMPORTER_ENABLED, defaultValues.areImporterEnabled());
        bindBoolean(importerPreferences.generateNewKeyOnImportProperty(), IMPORTER_GENERATE_KEY_ON_IMPORT, defaultValues.shouldGenerateNewKeyOnImport());
        bindObject(importerPreferences.importWorkingDirectoryProperty(), IMPORTER_WORKING_DIRECTORY, defaultValues.getImportWorkingDirectory(),
                Path::toString, Path::of);
        bindBoolean(importerPreferences.warnAboutDuplicatesOnImportProperty(), IMPORTER_WARN_ABOUT_DUPLICATES, defaultValues.shouldWarnAboutDuplicatesOnImport());
        bindBoolean(importerPreferences.warnAboutBibFileImportProperty(), IMPORTER_WARN_ABOUT_BIB_FILE, defaultValues.shouldWarnAboutBibFileImport());
        // persistCustomKeys must be bound before apiKeys: loading the keys re-persists them and reads this flag to
        // decide whether to write the keyring, so the flag has to be in place first.
        bindBoolean(importerPreferences.persistCustomKeysProperty(), FETCHER_CUSTOM_KEY_PERSIST, defaultValues.shouldPersistCustomKeys());
        bindSet(importerPreferences.getApiKeys(), FETCHER_CUSTOM_KEY_NAMES, defaultValues.getApiKeys(),
                _ -> storeFetcherKeys(importerPreferences),
                () -> getFetcherKeys(defaultValues.getApiKeys()));
        bindSet(importerPreferences.getCustomImporters(), IMPORTER_CUSTOM_FORMAT, defaultValues.getCustomImporters(),
                this::storeCustomImportFormats,
                () -> getCustomImportFormats(defaultValues.getCustomImporters()));
        bindCustomList(importerPreferences.getCatalogs(), IMPORTER_CATALOGS, defaultValues.getCatalogs(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
        // defaultPlainCitationParser falls back to the default on an unparseable stored value, so it needs a custom binding.
        bindCustom(importerPreferences.defaultPlainCitationParserProperty(), IMPORTER_DEFAULT_PLAIN_CITATION_PARSER, defaultValues.getDefaultPlainCitationParser(),
                (_, _, newValue) -> put(IMPORTER_DEFAULT_PLAIN_CITATION_PARSER, newValue.name()),
                () -> importerPreferences.defaultPlainCitationParserProperty().set(getDefaultPlainCitationParser(defaultValues.getDefaultPlainCitationParser())),
                () -> importerPreferences.defaultPlainCitationParserProperty().set(defaultValues.getDefaultPlainCitationParser()));
        bindInt(importerPreferences.citationsRelationsStoreTTLProperty(), IMPORTER_CITATIONS_RELATIONS_STORE_TTL, defaultValues.getCitationsRelationsStoreTTL());

        return importerPreferences;
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

    /// Reads the stored API keys aligned to `names` order ("" where a key is absent), or an empty list if the
    /// keyring is unavailable, so the caller falls back to default keys on a size mismatch.
    private List<String> getFetcherKeysFromKeyring(List<String> names) {
        List<KeyringSlot> slots = names.stream().map(KeyringSlot::customApiKey).toList();
        Map<KeyringSlot, String> stored = readKeyring(slots);
        if (stored.size() != slots.size()) {
            return List.of();
        }
        return slots.stream().map(stored::get).toList();
    }

    private void storeFetcherKeys(ImporterPreferences defaults) {
        List<String> names = new ArrayList<>();
        List<String> uses = new ArrayList<>();
        Map<KeyringSlot, String> keys = new HashMap<>();

        for (FetcherApiKey apiKey : defaults.getApiKeys()) {
            names.add(apiKey.getName());
            uses.add(String.valueOf(apiKey.shouldUse()));
            keys.put(KeyringSlot.customApiKey(apiKey.getName()), apiKey.getKey());
        }

        putStringList(FETCHER_CUSTOM_KEY_NAMES, names);
        putStringList(FETCHER_CUSTOM_KEY_USES, uses);

        if (defaults.shouldPersistCustomKeys()) {
            writeKeyring(keys);
        } else {
            clearCustomFetcherKeys();
        }
    }

    private void clearCustomFetcherKeys() {
        Map<KeyringSlot, String> cleared = new HashMap<>();
        for (String name : getStringList(FETCHER_CUSTOM_KEY_NAMES)) {
            cleared.put(KeyringSlot.customApiKey(name), "");
        }
        writeKeyring(cleared);
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

        GrobidPreferences defaultValues = GrobidPreferences.getDefault();

        grobidPreferences = new GrobidPreferences(
                getBoolean(GROBID_ENABLED, defaultValues.isGrobidEnabled()),
                getBoolean(GROBID_PREFERENCE, defaultValues.isGrobidUseAsked()),
                get(GROBID_URL, defaultValues.getGrobidURL()));

        bindBoolean(grobidPreferences.grobidEnabledProperty(), GROBID_ENABLED, defaultValues.isGrobidEnabled());
        bindBoolean(grobidPreferences.grobidUseAskedProperty(), GROBID_PREFERENCE, defaultValues.isGrobidUseAsked());
        bindString(grobidPreferences.grobidURLProperty(), GROBID_URL, defaultValues.getGrobidURL());

        return grobidPreferences;
    }
    // endregion

    // region OpenOfficePreferences
    @Override
    public OpenOfficePreferences getOpenOfficePreferences(JournalAbbreviationRepository journalAbbreviationRepository) {
        if (openOfficePreferences != null) {
            return openOfficePreferences;
        }

        OpenOfficePreferences defaultValues = OpenOfficePreferences.getDefault();

        openOfficePreferences = new OpenOfficePreferences(
                get(OO_EXECUTABLE_PATH, defaultValues.getExecutablePath()),
                getBoolean(OO_USE_ALL_OPEN_BASES, defaultValues.getUseAllDatabases()),
                getBoolean(OO_SYNC_WHEN_CITING, defaultValues.getSyncWhenCiting()),
                getStringList(OO_EXTERNAL_STYLE_FILES),
                get(OO_BIBLIOGRAPHY_STYLE_FILE, defaultValues.getCurrentJStyle()),
                getCurrentOOStyle(defaultValues.getCurrentStyle(), journalAbbreviationRepository),
                getBoolean(OO_ALWAYS_ADD_CITED_ON_PAGES, defaultValues.getAlwaysAddCitedOnPages()),
                get(OO_CSL_BIBLIOGRAPHY_TITLE, defaultValues.getCslBibliographyTitle()),
                get(OO_CSL_BIBLIOGRAPHY_HEADER_FORMAT, defaultValues.getCslBibliographyHeaderFormat()),
                get(OO_CSL_BIBLIOGRAPHY_BODY_FORMAT, defaultValues.getCslBibliographyBodyFormat()),
                getStringList(OO_EXTERNAL_CSL_STYLES),
                getBoolean(OO_ADD_SPACE_AFTER, defaultValues.getAddSpaceAfter()));

        bindString(openOfficePreferences.executablePathProperty(), OO_EXECUTABLE_PATH, defaultValues.getExecutablePath());
        bindBoolean(openOfficePreferences.useAllDatabasesProperty(), OO_USE_ALL_OPEN_BASES, defaultValues.getUseAllDatabases());
        bindBoolean(openOfficePreferences.alwaysAddCitedOnPagesProperty(), OO_ALWAYS_ADD_CITED_ON_PAGES, defaultValues.getAlwaysAddCitedOnPages());
        bindBoolean(openOfficePreferences.syncWhenCitingProperty(), OO_SYNC_WHEN_CITING, defaultValues.getSyncWhenCiting());
        bindBoolean(openOfficePreferences.addSpaceAfterProperty(), OO_ADD_SPACE_AFTER, defaultValues.getAddSpaceAfter());

        bindCustomList(openOfficePreferences.getExternalJStyles(), OO_EXTERNAL_STYLE_FILES, defaultValues.getExternalJStyles(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
        bindCustomList(openOfficePreferences.getExternalCslStyles(), OO_EXTERNAL_CSL_STYLES, defaultValues.getExternalCslStyles(),
                JabRefCliPreferences::convertListToString, JabRefCliPreferences::convertStringToList);
        bindString(openOfficePreferences.currentJStyleProperty(), OO_BIBLIOGRAPHY_STYLE_FILE, defaultValues.getCurrentJStyle());
        // currentStyle is persisted as a style path and reconstructed into a CSL style or JStyle on load, so it needs a custom binding.
        bindCustom(openOfficePreferences.currentStyleProperty(), OO_CURRENT_STYLE, defaultValues.getCurrentStyle(),
                (_, _, newValue) -> put(OO_CURRENT_STYLE, newValue.getPath()),
                () -> openOfficePreferences.currentStyleProperty().set(getCurrentOOStyle(defaultValues.getCurrentStyle(), journalAbbreviationRepository)),
                () -> openOfficePreferences.currentStyleProperty().set(defaultValues.getCurrentStyle()));

        bindString(openOfficePreferences.cslBibliographyTitleProperty(), OO_CSL_BIBLIOGRAPHY_TITLE, defaultValues.getCslBibliographyTitle());
        bindString(openOfficePreferences.cslBibliographyHeaderFormatProperty(), OO_CSL_BIBLIOGRAPHY_HEADER_FORMAT, defaultValues.getCslBibliographyHeaderFormat());
        bindString(openOfficePreferences.cslBibliographyBodyFormatProperty(), OO_CSL_BIBLIOGRAPHY_BODY_FORMAT, defaultValues.getCslBibliographyBodyFormat());

        return openOfficePreferences;
    }

    /// Reconstructs the persisted [OOStyle] from its stored path: a CSL style file becomes a [org.jabref.logic.citationstyle.CitationStyle], otherwise
    /// it is treated as a [JStyle] (requiring `journalAbbreviationRepository`). Falls back to `defaultStyle` when the
    /// path is absent, the repository is missing, or the JStyle cannot be created.
    private OOStyle getCurrentOOStyle(OOStyle defaultStyle, JournalAbbreviationRepository journalAbbreviationRepository) {
        String currentStylePath = get(OO_CURRENT_STYLE, defaultStyle.getPath());

        if (CSLStyleUtils.isCitationStyleFile(currentStylePath)) {
            return CSLStyleUtils.createCitationStyleFromFile(currentStylePath)
                                .orElse(CSLStyleLoader.getDefaultStyle());
        } else if (journalAbbreviationRepository != null) {
            // For now, must be a JStyle. In the future, make separate cases for JStyles (.jstyle) and BibTeX (.bst) styles
            try {
                return new JStyle(currentStylePath, getLayoutFormatterPreferences(), journalAbbreviationRepository);
            } catch (IOException ex) {
                LOGGER.warn("Could not create JStyle", ex);
            }
        }
        return defaultStyle;
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
                rememberPat ? readKeyring(KeyringSlot.GITHUB_PAT).orElse(defaultValues.getPat())
                            : defaultValues.getPat(),
                get(GITHUB_REMOTE_URL_KEY, defaultValues.getRepositoryUrl()),
                rememberPat);

        bindString(gitPreferences.usernameProperty(), GITHUB_USERNAME_KEY, defaultValues.getUsername());
        bindString(gitPreferences.repositoryUrlProperty(), GITHUB_REMOTE_URL_KEY, defaultValues.getRepositoryUrl());
        bindToKeyring(gitPreferences.patProperty(), KeyringSlot.GITHUB_PAT, gitPreferences::getPersistPat);
        bindCustom(gitPreferences.rememberPatProperty(), GITHUB_REMEMBER_PAT_KEY, defaultValues.getPersistPat(),
                (_, _, newValue) -> {
                    putBoolean(GITHUB_REMEMBER_PAT_KEY, newValue);
                    if (!newValue) {
                        writeKeyring(KeyringSlot.GITHUB_PAT, "");
                    }
                },
                () -> {
                    boolean shouldRemember = getBoolean(GITHUB_REMEMBER_PAT_KEY, defaultValues.getPersistPat());
                    gitPreferences.rememberPatProperty().set(shouldRemember);
                    gitPreferences.patProperty().set(
                            shouldRemember ? readKeyring(KeyringSlot.GITHUB_PAT).orElse(defaultValues.getPat())
                                           : defaultValues.getPat());
                },
                () -> {
                    gitPreferences.rememberPatProperty().set(defaultValues.getPersistPat());
                    gitPreferences.patProperty().set(defaultValues.getPat());
                });

        return gitPreferences;
    }
    // endregion
}
