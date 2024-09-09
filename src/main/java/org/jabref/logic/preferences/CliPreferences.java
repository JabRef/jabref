package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import org.jabref.gui.CoreGuiPreferences;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.externalfiles.UnlinkedFilesDialogPreferences;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.frame.SidePanePreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.mergeentries.MergeDialogPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.push.PushToApplicationPreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
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
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface CliPreferences {
    void clear() throws BackingStoreException;

    void deleteKey(String key) throws IllegalArgumentException;

    void flush();

    void exportPreferences(Path file) throws JabRefException;

    void importPreferences(Path file) throws JabRefException;

    InternalPreferences getInternalPreferences();

    BibEntryPreferences getBibEntryPreferences();

    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    KeyBindingRepository getKeyBindingRepository();

    FilePreferences getFilePreferences();

    FieldPreferences getFieldPreferences();

    OpenOfficePreferences getOpenOfficePreferences();

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

    GroupsPreferences getGroupsPreferences();

    RemotePreferences getRemotePreferences();

    ProxyPreferences getProxyPreferences();

    SSLPreferences getSSLPreferences();

    CitationKeyPatternPreferences getCitationKeyPatternPreferences();

    PushToApplicationPreferences getPushToApplicationPreferences();

    ExternalApplicationsPreferences getExternalApplicationsPreferences();

    ColumnPreferences getMainTableColumnPreferences();

    MainTablePreferences getMainTablePreferences();

    NameDisplayPreferences getNameDisplayPreferences();

    ColumnPreferences getSearchDialogColumnPreferences();

    WorkspacePreferences getWorkspacePreferences();

    AutoLinkPreferences getAutoLinkPreferences();

    ExportPreferences getExportPreferences();

    ImporterPreferences getImporterPreferences();

    GrobidPreferences getGrobidPreferences();

    PreviewPreferences getPreviewPreferences();

    SidePanePreferences getSidePanePreferences();

    CoreGuiPreferences getGuiPreferences();

    XmpPreferences getXmpPreferences();

    NameFormatterPreferences getNameFormatterPreferences();

    AutoCompletePreferences getAutoCompletePreferences();

    SpecialFieldsPreferences getSpecialFieldsPreferences();

    SearchPreferences getSearchPreferences();

    MrDlibPreferences getMrDlibPreferences();

    ProtectedTermsPreferences getProtectedTermsPreferences();

    MergeDialogPreferences getMergeDialogPreferences();

    UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferences();

    AiPreferences getAiPreferences();
}
