package org.jabref.preferences;

import java.nio.file.Path;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface PreferencesService {

    void clear() throws BackingStoreException;

    void deleteKey(String key) throws IllegalArgumentException;

    void flush();

    void exportPreferences(Path file) throws JabRefException;

    void importPreferences(Path file) throws JabRefException;

    InternalPreferences getInternalPreferences();

    BibEntryPreferences getBibEntryPreferences();

    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository);

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

    TelemetryPreferences getTelemetryPreferences();

    DOIPreferences getDOIPreferences();

    OwnerPreferences getOwnerPreferences();

    TimestampPreferences getTimestampPreferences();

    GroupsPreferences getGroupsPreferences();

    EntryEditorPreferences getEntryEditorPreferences();

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

    GuiPreferences getGuiPreferences();

    XmpPreferences getXmpPreferences();

    NameFormatterPreferences getNameFormatterPreferences();

    AutoCompletePreferences getAutoCompletePreferences();

    SpecialFieldsPreferences getSpecialFieldsPreferences();

    SearchPreferences getSearchPreferences();

    MrDlibPreferences getMrDlibPreferences();

    ProtectedTermsPreferences getProtectedTermsPreferences();

    MergeDialogPreferences getMergeDialogPreferences();
}
