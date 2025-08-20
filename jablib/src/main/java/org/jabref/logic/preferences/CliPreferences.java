package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.InternalPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

public interface CliPreferences {
    void clear() throws BackingStoreException;

    void deleteKey(String key) throws IllegalArgumentException;

    void flush();

    void exportPreferences(Path file) throws JabRefException;

    void importPreferences(Path file) throws JabRefException;

    InternalPreferences getInternalPreferences();

    BibEntryPreferences getBibEntryPreferences();

    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    FilePreferences getFilePreferences();

    FieldPreferences getFieldPreferences();

    Map<String, Object> getPreferences();

    Map<String, Object> getDefaults();

    LayoutFormatterPreferences getLayoutFormatterPreferences();

    ImportFormatPreferences getImportFormatPreferences();

    /**
     * Returns the export configuration. The contained SaveConfiguration is a {@link org.jabref.model.metadata.SelfContainedSaveOrder}
     */
    SelfContainedSaveConfiguration getSelfContainedExportConfiguration();

    BibEntryTypesManager getCustomEntryTypesRepository();

    void storeCustomEntryTypesRepository(BibEntryTypesManager entryTypesManager);

    CleanupPreferences getCleanupPreferences();

    CleanupPreferences getDefaultCleanupPreset();

    LibraryPreferences getLibraryPreferences();

    DOIPreferences getDOIPreferences();

    OwnerPreferences getOwnerPreferences();

    TimestampPreferences getTimestampPreferences();

    RemotePreferences getRemotePreferences();

    ProxyPreferences getProxyPreferences();

    SSLPreferences getSSLPreferences();

    CitationKeyPatternPreferences getCitationKeyPatternPreferences();

    AutoLinkPreferences getAutoLinkPreferences();

    ExportPreferences getExportPreferences();

    ImporterPreferences getImporterPreferences();

    GrobidPreferences getGrobidPreferences();

    XmpPreferences getXmpPreferences();

    NameFormatterPreferences getNameFormatterPreferences();

    SearchPreferences getSearchPreferences();

    MrDlibPreferences getMrDlibPreferences();

    ProtectedTermsPreferences getProtectedTermsPreferences();

    AiPreferences getAiPreferences();

    LastFilesOpenedPreferences getLastFilesOpenedPreferences();

    OpenOfficePreferences getOpenOfficePreferences(JournalAbbreviationRepository journalAbbreviationRepository);

    PushToApplicationPreferences getPushToApplicationPreferences();

    GitPreferences getGitPreferences();
}
