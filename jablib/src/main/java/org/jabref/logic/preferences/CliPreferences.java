package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.InternalPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.ocr.OcrPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;

public interface CliPreferences {
    void clear() throws BackingStoreException;

    void deleteKey(String key) throws IllegalArgumentException;

    void flush();

    /// Captures the current storage options on the preference-owning thread.
    ///
    /// @return the operation that flushes the backing store on a worker thread
    Runnable prepareFlush();

    void exportPreferences(Path file) throws JabRefException;

    void importPreferences(Path file) throws JabRefException;

    InternalPreferences getInternalPreferences();

    BibEntryPreferences getBibEntryPreferences();

    AbbreviationPreferences getAbbreviationPreferences();

    FilePreferences getFilePreferences();

    FieldPreferences getFieldPreferences();

    Map<String, Object> getPreferences();

    Map<String, Object> getDefaults();

    /// Returns the export configuration. The contained SaveConfiguration is a [org.jabref.model.metadata.SelfContainedSaveOrder]
    SelfContainedSaveConfiguration getSelfContainedExportConfiguration();

    BibEntryTypesManager getCustomEntryTypesRepository();

    void storeCustomEntryTypesRepository(BibEntryTypesManager entryTypesManager);

    /// Returns the custom entry type decisions the user declined to store, as created by the caller
    Set<String> getDeclinedCustomEntryTypes();

    void addDeclinedCustomEntryTypes(Collection<String> declinedEntryTypes);

    CleanupPreferences getCleanupPreferences();

    LibraryPreferences getLibraryPreferences();

    DOIPreferences getDOIPreferences();

    OwnerPreferences getOwnerPreferences();

    TimestampPreferences getTimestampPreferences();

    RemotePreferences getRemotePreferences();

    ProxyPreferences getProxyPreferences();

    CiteDrivePreferences getCiteDrivePreferences();

    SSLPreferences getSSLPreferences();

    CitationKeyPatternPreferences getCitationKeyPatternPreferences();

    AutoLinkPreferences getAutoLinkPreferences();

    ExportPreferences getExportPreferences();

    ImporterPreferences getImporterPreferences();

    /// Takes a snapshot of the complete API-key configuration for background persistence.
    ///
    /// @return the save operation for replacing the current API-key bundle
    CredentialSave prepareSavedFetcherApiKeys(Set<FetcherApiKey> apiKeys);

    /// Takes a snapshot of API keys to use for this session after clearing any saved keys.
    ///
    /// @return the save operation for clearing the saved API keys
    CredentialSave prepareSessionFetcherApiKeys(Set<FetcherApiKey> apiKeys);

    /// Prepares a change to the proxy password or its persistence on the preference-owning thread.
    ///
    /// @param password    the password to use for proxy authentication
    /// @param persistence whether the password should be available in later sessions
    /// @return the save operation, or an empty optional if the password and its persistence are unchanged
    Optional<CredentialSave> prepareProxyPasswordSave(String password, CredentialPersistence persistence);

    /// Prepares a change to the GitHub personal access token or its persistence on the preference-owning thread.
    ///
    /// @param token       the personal access token to use for Git authentication
    /// @param persistence whether the token should be available in later sessions
    /// @return the save operation, or an empty optional if the token and its persistence are unchanged
    Optional<CredentialSave> prepareGitHubTokenSave(String token, CredentialPersistence persistence);

    /// Prepares changes to CiteDrive's server or token persistence on the preference-owning thread.
    /// Changing servers clears any saved token for the previous server and invalidates the current login.
    ///
    /// @param apiBaseUrl  the server address to use after saving
    /// @param persistence whether the token should be available in later sessions
    /// @return the save operation, or an empty optional if the address and persistence are unchanged
    Optional<CredentialSave> prepareCiteDriveTokenSave(String apiBaseUrl, CredentialPersistence persistence);

    /// Determines whether a credential is saved for later sessions.
    @NullMarked
    enum CredentialPersistence {
        /// Keep the credential in the system keyring.
        SAVE,
        /// Keep the credential in memory for this session and clear any saved copy.
        SESSION_ONLY
    }

    /// A prepared credential update. Create it on the thread that owns the preferences.
    /// Run its keyring phase on a worker and apply the returned operation on the preference-owning thread.
    @FunctionalInterface
    @NullMarked
    interface CredentialSave {
        /// Performs any required keyring write or deletion without applying the new preference values.
        ///
        /// @return the update to apply once on the preference-owning thread after the operation succeeds
        /// @throws JabRefException if the keyring update fails
        CredentialCommit writeKeyring() throws JabRefException;
    }

    /// Applies a credential update on the preference-owning thread, or requests another worker pass.
    @FunctionalInterface
    @NullMarked
    interface CredentialCommit {
        Optional<CredentialSave> apply();

        static CredentialCommit of(Runnable update) {
            return () -> {
                update.run();
                return Optional.empty();
            };
        }
    }

    GrobidPreferences getGrobidPreferences();

    XmpPreferences getXmpPreferences();

    NameFormatterPreferences getNameFormatterPreferences();

    SearchPreferences getSearchPreferences();

    ProtectedTermsPreferences getProtectedTermsPreferences();

    AiPreferences getAiPreferences();

    LastFilesOpenedPreferences getLastFilesOpenedPreferences();

    /// @return the shared database connection used last; the other stored connections are reached through it
    SharedDatabasePreferences getSharedDatabasePreferences();

    OpenOfficePreferences getOpenOfficePreferences(JournalAbbreviationRepository journalAbbreviationRepository);

    PushToApplicationPreferences getPushToApplicationPreferences();

    GitPreferences getGitPreferences();

    default ImportFormatPreferences getImportFormatPreferences() {
        return new ImportFormatPreferences(
                getBibEntryPreferences(),
                getCitationKeyPatternPreferences(),
                getFieldPreferences(),
                getXmpPreferences(),
                getDOIPreferences(),
                getGrobidPreferences(),
                getFilePreferences());
    }

    default LayoutFormatterPreferences getLayoutFormatterPreferences() {
        return new LayoutFormatterPreferences(
                getNameFormatterPreferences(),
                getDOIPreferences(),
                getFilePreferences().mainFileDirectoryProperty());
    }

    OcrPreferences getOcrPreferences();
}
